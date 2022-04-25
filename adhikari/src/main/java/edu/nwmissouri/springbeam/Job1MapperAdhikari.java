/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.nwmissouri.springbeam;

import java.util.*;
import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.FlatMapElements;
import org.apache.beam.sdk.transforms.Flatten;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.sdk.values.TypeDescriptors;



public class Job1MapperAdhikari {
    // DEFINE DOFNS
  // ==================================================================
  // You can make your pipeline assembly code less verbose by defining
  // your DoFns statically out-of-line.
  // Each DoFn<InputT, OutputT> takes previous output
  // as input of type InputT
  // and transforms it to OutputT.
  // We pass this DoFn to a ParDo in our pipeline.

  /**
   * DoFn Job1Finalizer takes KV(String, String List of outlinks) and transforms
   * the value into our custom RankedPage Value holding the page's rank and list
   * of voters.
   * 
   * The output of the Job1 Finalizer creates the initial input into our
   * iterative Job 2.
   */
  
  static class Job1Finalizer extends DoFn<KV<String, Iterable<String>>, KV<String, RankedPage>> {
    @ProcessElement
    public void processElement(@Element KV<String, Iterable<String>> element,
        OutputReceiver<KV<String, RankedPage>> receiver) {
      Integer contributorVotes = 0;
      if (element.getValue() instanceof Collection) {
        contributorVotes = ((Collection<String>) element.getValue()).size();
      }
      ArrayList<VotingPage> voters = new ArrayList<VotingPage>();
      for (String voterName : element.getValue()) {
        if (!voterName.isEmpty()) {
          voters.add(new VotingPage(voterName, contributorVotes));
        }
      }
      receiver.output(KV.of(element.getKey(), new RankedPage(element.getKey(), voters)));
    }
  }

  static class Job2Mapper extends DoFn<KV<String, RankedPage>, KV<String, RankedPage>> {
    @ProcessElement
    public void processElement(@Element KV<String, RankedPage> element,
        OutputReceiver<KV<String, RankedPage>> receiver) {
      Integer NumOfVotes=0;
      ArrayList<VotingPage> voters = element.getValue().getVoters();

      if (element.getValue().getVoters() instanceof Collection){
        NumOfVotes = ((Collection<VotingPage>)element.getValue().getVoters()).size();
      }

      for(VotingPage page: voters){
        String pageName = page.getNames();
        Double pageRank = page.getRank();
        String contributingPageName = element.getKey();
        Double contributingPageRank = element.getValue().getRank();
        VotingPage contributor = new VotingPage(contributingPageName, contributingPageRank, NumOfVotes);
        ArrayList<VotingPage> arr = new ArrayList<VotingPage>();
        arr.add(contributor);
        receiver.output(KV.of(page.getNames(), new RankedPage(pageName, pageRank,arr)));
      }
      
    }
  }

  static class Job2Updater extends DoFn<KV<String, Iterable<RankedPage>>, KV<String, RankedPage>> {
    @ProcessElement
    public void processElement(@Element KV<String, Iterable<RankedPage>> element,
        OutputReceiver<KV<String, RankedPage>> receiver) {
    String thisPage = element.getKey();
    Iterable<RankedPage> rankedPages = element.getValue();
    Double dampingFactor = 0.85;
    Double updatedRank = (1-dampingFactor);
    ArrayList<VotingPage> newVoters = new ArrayList<VotingPage>();
    for(RankedPage pg : rankedPages){
      if(pg != null){
        for(VotingPage vp : pg.getVoters()){
          newVoters.add(vp);
          updatedRank += (dampingFactor) * vp.getRank() / (double)vp.getVotes();
        }
      }
    }
    receiver.output(KV.of(thisPage, new RankedPage(thisPage, updatedRank, newVoters)));
    }

  }

  public static PCollection<KV<String, String>> adhikariKVPairGenerator(Pipeline p, String folderName, String fileName) {

    String dataPath = "./" + folderName + "/" + fileName;
    PCollection<String> pColLine = p.apply(TextIO.read().from(dataPath));

    PCollection<String> pColLinkLine = pColLine.apply(Filter.by((String linkline) -> linkline.startsWith("[")))
        .apply(
            MapElements.into(TypeDescriptors.strings())
                .via((String linkline) -> linkline.strip()));

    PCollection<String> pColLinks = pColLinkLine.apply(
        MapElements.into(TypeDescriptors.strings())
            .via((String linkword) -> (findLink(linkword))));

    PCollection<KV<String, String>> pColKVPairs = pColLinks.apply(
        MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(),
            TypeDescriptors.strings()))
            .via(
                (String links) -> KV.of((String) fileName, (String) links)));

    return pColKVPairs;
  }

  public static String findLink(String line) {
    String link = "";
    int beginIndex = line.indexOf("(");
    int endIndex = line.indexOf(")");
    link = line.substring(beginIndex + 1, endIndex);
    return link;
  }

 /**
   * Run one iteration of the Job 2 Map-Reduce process
   * Notice how the Input Type to Job 2.
   * Matches the Output Type from Job 2.
   * How important is that for an iterative process?
   * 
   * @param kvReducedPairs - takes a PCollection<KV<String, RankedPage>> with
   *                       initial ranks.
   * @return - returns a PCollection<KV<String, RankedPage>> with updated ranks.
   */
  /*
  private static PCollection<KV<String, RankedPage>> runJOb2Iteration(
   PCollection<KV<String, RankedPage>> kvReducedPairs){
    PCollection<KV<String, RankedPage>> mappedKVs = kvReducedPairs
      .apply(ParDo.of(new Job2Mapper()));
    PCollection<KV<String, Iterable<RankedPage>>> reducedKVs = mappedKVs
      .apply(GroupByKey.<String, RankedPage>create());
    PCollection<KV<String, RankedPage>> updatedOutput = reducedKVs
      .apply(ParDo.of(new Job2Updater()));

    return reducedKVs;
     
   }
   */
 
  public static void main(String[] args) {
    // Create a PipelineOptions object. This object lets us set various execution
    // options for our pipeline, such as the runner you wish to use. This example
    // will run with the DirectRunner by default, based on the class path configured
    // in its dependencies.
    PipelineOptions options = PipelineOptionsFactory.create();

    // Create the Pipeline object with the options we defined above
    Pipeline p = Pipeline.create(options);

    // Initiating the variable for web04 and file name
    String folderName = "web04";
    // String fileName = "go.md";

    //generating kv pairs for each webpage 
    PCollection<KV<String, String>> pColKV1 = adhikariKVPairGenerator(p, folderName, "go.md");
    PCollection<KV<String, String>> pColKV2 = adhikariKVPairGenerator(p, folderName, "java.md");
    PCollection<KV<String, String>> pColKV3 = adhikariKVPairGenerator(p, folderName, "python.md");
    PCollection<KV<String, String>> pColKV4 = adhikariKVPairGenerator(p, folderName, "README.md");
    
    //merging all kv pairs into one PCollectionList then PCollection
    PCollectionList<KV<String, String>> pColKVList = PCollectionList.of(pColKV1).and(pColKV2).and(pColKV3).and(pColKV4);
    PCollection<KV<String, String>> mergedList = pColKVList.apply(Flatten.<KV<String, String>>pCollections());

    PCollection<KV<String, Iterable<String>>> pColReduced =
     mergedList.apply(GroupByKey.<String, String>create());

    // Convert to a custom Value object (RankedPage) in preparation for Job 2
    PCollection<KV<String, RankedPage>> job1output = pColReduced.apply(ParDo.of(new Job1Finalizer()));


    //END OF JOB1
    PCollection<KV<String, RankedPage>> updatedOutput = null;
    int iterations =50;
    for (int i =0; i<iterations; i++){
      PCollection<KV<String, RankedPage>> mappedKVs = job1output
        .apply(ParDo.of(new Job2Mapper()));
      PCollection<KV<String, Iterable<RankedPage>>> reducedKVs = mappedKVs
        .apply(GroupByKey.<String, RankedPage>create());
      updatedOutput = reducedKVs.apply(ParDo.of(new Job2Updater()));
    }
    //Changing to be able to write using TextIO
    PCollection<String> writableFile = updatedOutput.apply(MapElements.into(TypeDescriptors.strings())
    .via((kvpairs) -> kvpairs.toString()));
    //writing the result
    writableFile.apply(TextIO.write().to("AdhikariPR"));
    p.run().waitUntilFinish();
  }
}
