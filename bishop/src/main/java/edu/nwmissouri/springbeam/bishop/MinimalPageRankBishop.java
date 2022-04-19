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
package edu.nwmissouri.springbeam.bishop;

// beam-playground:
//   name: MinimalWordCount
//   description: An example that counts words in Shakespeare's works.
//   multifile: false
//   pipeline_options:
//   categories:
//     - Combiners
//     - Filtering
//     - IO
//     - Core Transforms

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.beam.sdk.Pipeline;
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
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TypeDescriptors;


public class MinimalPageRankBishop {
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

  private static PCollection<KV<String, String>> bishopMapper(Pipeline p, String path, String file){
    PCollection<String> pcolInputLines = p.apply(TextIO.read().from(path + '/' + file));

    PCollection<String> pcolLinkLines = pcolInputLines.apply(Filter.by((String line) -> line.startsWith("[")));

    PCollection<String> pcolLinks = pcolLinkLines.apply(
      FlatMapElements.into(TypeDescriptors.strings())
      .via((String linkline) -> Arrays.asList(linkline.substring(linkline.indexOf("(") + 1, linkline.length()-1)))
    );

    PCollection<KV<String, String >> KVJob1 = pcolLinks.apply(
      MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.strings()))
      .via(
        (String linkedPage) -> KV.of(file, linkedPage)));
    
    return KVJob1;
  }

  public static void main(String[] args) {
 
    String dataFolder = "./web04";

    PipelineOptions options = PipelineOptionsFactory.create();

    Pipeline p = Pipeline.create(options);

    PCollection<KV<String, String>> collectionKV00 = bishopMapper(p, dataFolder, "go.md");
    PCollection<KV<String, String>> collectionKV01 = bishopMapper(p, dataFolder, "java.md");
    PCollection<KV<String, String>> collectionKV02 = bishopMapper(p, dataFolder, "python.md");
    PCollection<KV<String, String>> collectionKV03 = bishopMapper(p, dataFolder, "README.md");

    PCollectionList<KV<String, String>> pcList = PCollectionList.of(collectionKV00).and(collectionKV01).and(collectionKV02).and(collectionKV03);

    //Job1: Mapper
    PCollection<KV<String, String>> mergedList = pcList.apply(Flatten.<KV<String, String>>pCollections());

    //Job 1: Reduce
    PCollection<KV<String, Iterable <String>>> reducedPairs = mergedList.apply(GroupByKey.<String, String>create());

    // Convert to a custom Value object (RankedPage) in preparation for Job 2
    PCollection<KV<String, RankedPage>> job2in = kvStringReducedPairs.apply(ParDo.of(new Job1Finalizer()));

    // END JOB 1
    // ========================================
    // KV{python.md, python.md, 1.00000, 0, [README.md, 1.00000,1]}
    // KV{go.md, go.md, 1.00000, 0, [README.md, 1.00000,1]}
    // KV{README.md, README.md, 1.00000, 0, [go.md, 1.00000,3, java.md, 1.00000,3,
    // python.md, 1.00000,3]}
    // ========================================
    // BEGIN ITERATIVE JOB 2

    PCollection<KV<String, RankedPage>> job2out = null; 
    int iterations = 2;
    for (int i = 1; i <= iterations; i++) {
      // use job2in to calculate job2 out
      // .... write code here
      // update job2in so it equals the new job2out
      // ... write code here
    }

    // END ITERATIVE JOB 2
    // ========================================
    // after 40 - output might look like this:
    // KV{java.md, java.md, 0.69415, 0, [README.md, 1.92054,3]}
    // KV{python.md, python.md, 0.69415, 0, [README.md, 1.92054,3]}
    // KV{README.md, README.md, 1.91754, 0, [go.md, 0.69315,1, java.md, 0.69315,1,
    // python.md, 0.69315,1]}

    // Map KVs to strings before outputting
    PCollection<String> output = job2out.apply(MapElements.into(
        TypeDescriptors.strings())
        .via(kv -> kv.toString()));

    // Write from Beam back out into the real world

    PCollection<String> KVOut = reducedPairs.apply(MapElements.into(TypeDescriptors.strings())
      .via((kvpairs) -> kvpairs.toString()));

      KVOut.apply(TextIO.write().to("bishopout"));

    p.run().waitUntilFinish();
  }
}
