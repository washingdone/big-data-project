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
package edu.nwmissouri.springbeam.marceline;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.Flatten;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.apache.beam.sdk.values.TypeDescriptor;

public class PageRankMarci {

  static class Job1Map2 extends DoFn<KV<String, Iterable<String>>, KV<String, RankedPage>> {
    @ProcessElement
    public void processElement(@Element KV<String, Iterable<String>> inputData,
        OutputReceiver<KV<String, RankedPage>> returner) {
      Integer contributorVotes = 0;
      if (inputData.getValue() instanceof Collection) {
        contributorVotes = ((Collection<String>) inputData.getValue()).size();
      }
      ArrayList<VotingPage> voters = new ArrayList<VotingPage>();
      for (String voterName : inputData.getValue()) {
        if (!voterName.isEmpty()) {
          voters.add(new VotingPage(voterName, contributorVotes));
        }
      }
      returner.output(KV.of(inputData.getKey(), new RankedPage(inputData.getKey(), voters)));
    }
  }

  static class Job2Map1 extends DoFn<KV<String, RankedPage>, KV<String, RankedPage>> {
    @ProcessElement
    public void processElement(@Element KV<String, RankedPage> inputData,
        OutputReceiver<KV<String, RankedPage>> returner) {
      Integer votes = 0;
      ArrayList<VotingPage> voters = inputData.getValue().getVoters();
      if (voters instanceof Collection) {
        votes = voters.size();
      }
      for (VotingPage votingPage : voters) {
        String votingPageName = votingPage.getVoterName();
        double votingPageRank = votingPage.getRank();
        String contribPageName = inputData.getKey();
        double contribPageRank = inputData.getValue().getRank();
        // System.out.printf("CURRENT DATA STATE: %s, %d, %s, %d\n", votingPageName, votingPageRank, contribPageName, contribPageRank );
        VotingPage contrib = new VotingPage(contribPageName, votes, contribPageRank);
        ArrayList<VotingPage> votingPageArray = new ArrayList<VotingPage>();
        votingPageArray.add(contrib);
        returner
            .output(KV.of(votingPage.getVoterName(), new RankedPage(votingPageName, votingPageArray, votingPageRank)));
      }
    }
  }

  static class Job2Logic extends DoFn<KV<String, Iterable<RankedPage>>, KV<String, RankedPage>> {
    @ProcessElement
    public void processElement(KV<String, Iterable<RankedPage>> inputData,
        OutputReceiver<KV<String, RankedPage>> returner) {
      // Integer voteWeight = 0;
      // String key = inputData.getKey();
      // Iterable<RankedPage> value = inputData.getValue();
      // ArrayList<VotingPage> vpa = new ArrayList<VotingPage>();

      returner.output(KV.of("String", new RankedPage()));

      // for (RankedPage item : value) {
      // returner.output(KV.of(key, item));
    }
  }

  private static PCollection<KV<String, String>> mapper1(Pipeline p, String filepath, String filename) {
    return p.apply(TextIO.read().from(filepath))
        .apply(Filter.by((String line) -> line.startsWith("[")))
        .apply(MapElements.into(TypeDescriptors.strings())
            .via(linkLine -> linkLine.substring(linkLine.indexOf("(") + 1, linkLine.indexOf(")"))))
        .apply(MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.strings()))
            .via(link -> KV.of(filename, link)));
  }

  public static void main(String[] args) {

    // Build Pipeline
    PipelineOptions options = PipelineOptionsFactory.create();
    Pipeline p = Pipeline.create(options);

    // Generate initial variables
    String inputFolder = "pages";
    String outputFolder = "MarciOutputs";
    PCollectionList<KV<String, String>> combinedData = PCollectionList.empty(p); // an empty list of pcollections to be used for storing two data sets later on

    // empty output directory so it can be filled with new data
    File outputDir = new File(outputFolder);
    if (outputDir.exists()) {
      for (File file : outputDir.listFiles()) {
        file.delete();
      }
    }

    // read data folder and process files, ignoring directories
    File dataDir = new File(inputFolder);
    for (File file : dataDir.listFiles()) {
      if (file.isDirectory()) {
        continue;
      }
      PCollection<KV<String, String>> processedData = mapper1(p, file.getAbsolutePath(), file.getName()); // run the collected data through the first mapper
      PCollection<KV<String, String>> previousData = combinedData.apply(Flatten.<KV<String, String>>pCollections()); // extract the previously generated data from the list of pcollections by flattening it
      
      // This nightmare needs a bit more explaining - The goal is to combine the output previous data and the currently processed data
      // and reassign it to the combined data list, allowing us to read X number of data files. Unfortunately, due to the way beam works,
      // you must manually assign the coders of the datasets, as something does not allow it to automatically pull that information like
      // it's supposed to be able to do. i did try using combinedData.and(processedData); in a different workflow configuration, however
      // that never actually added the pcollections to combined data and just returned an empty pcollectionlist
      combinedData = PCollectionList.of(previousData.setCoder(KvCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())))
        .and(processedData.setCoder(KvCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())));
    }

    // System.out.println("OUTPUT: " + combinedData.getAll().toString());
    // System.out.println("DATA DATA DATA:::::::: " + TypeDescriptor.of(Job2Logic.class));

    combinedData.apply(Flatten.<KV<String, String>>pCollections())//.setCoder(KvCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of()))
        .apply(GroupByKey.create())
        .apply(ParDo.of(new Job1Map2()))
        .apply(ParDo.of(new Job2Map1()))
        .apply(GroupByKey.create())
        .apply(ParDo.of(new Job2Logic()))
        .apply(MapElements.into(TypeDescriptors.strings())
            .via((kvpairs) -> kvpairs.toString()))
        .apply(TextIO.write().to(outputFolder + "\\"));

    p.run().waitUntilFinish();
  }

}
