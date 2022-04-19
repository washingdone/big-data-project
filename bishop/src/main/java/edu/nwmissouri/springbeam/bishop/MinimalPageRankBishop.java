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
package org.apache.beam.examples;

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
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.sdk.values.TypeDescriptors;

/**
 * An example that counts words in Shakespeare.
 *
 * <p>This class, {@link MinimalWordCount}, is the first in a series of four successively more
 * detailed 'word count' examples. Here, for simplicity, we don't show any error-checking or
 * argument processing, and focus on construction of the pipeline, which chains together the
 * application of core transforms.
 *
 * <p>Next, see the {@link WordCount} pipeline, then the {@link DebuggingWordCount}, and finally the
 * {@link WindowedWordCount} pipeline, for more detailed examples that introduce additional
 * concepts.
 *
 * <p>Concepts:
 *
 * <pre>
 *   1. Reading data from text files
 *   2. Specifying 'inline' transforms
 *   3. Counting items in a PCollection
 *   4. Writing data to text files
 * </pre>
 *
 * <p>No arguments are required to run this pipeline. It will be executed with the DirectRunner. You
 * can see the results in the output files in your current working directory, with names like
 * "wordcounts-00001-of-00005. When running on a distributed service, you would use an appropriate
 * file service.
 */
public class MinimalPageRankBishop {

  private static PCollection<KV<String, String>> bishopMapper(Pipeline p, String path, String dataFile){
    PCollection<String> pcolInputLines = p.apply(TextIO.read().from(path + '/' + file));

    PCollection<String> pcolLinkLines = pcolInputLines.apply(Filter.by((String line) -> line.startsWith("[")));

    PCollection<String> pcolLinks = pcolLinkLines.apply(
      FlatMapElements.into(TypeDescriptors.strings())
      .via((String linkline) -> Arrays.asList(linkline.substring(linkline.indexOf("(") + 1, linkline.length()-1)))
    );

    PCollection<KV<String, String >> KVJob1 = pcolLinks.apply(
      MapElements.into(TypeDescriptors.kvs(TypeDescriptor.strings(), TypeDescriptors.strings()))
      .via(
        (String linkedPage) -> KV.of(file, linkedPage)));
    
    return KVJob1;
  }

  public static void main(String[] args) {
 
    PipelineOptions options = PipelineOptionsFactory.create();

    Pipeline p = Pipeline.create(options);

    PCollection<KV<String, String>> collectionKV00 = bishopMapper(p, dataFolder, "go.md");
    PCollection<KV<String, String>> collectionKV01 = bishopMapper(p, dataFolder, "java.md");
    PCollection<KV<String, String>> collectionKV02 = bishopMapper(p, dataFolder, "python.md");
    PCollection<KV<String, String>> collectionKV03 = bishopMapper(p, dataFolder, "README.md");

    PCollectionList<KV<String, String>> pcList = PCollectionList.of(collectionKV00).and(collectionKV01).and(collectionKV02).and(collectionKV03);

    PCollection<KV<String, String>> mergedList = pcList.apply(Flatten.<KV<String, String>>pCollections());


    //Job 1: Reduce
    PCollection<KV<String, Iterable <String>>> reducedPairs = mergedList.apply(GroupByKey.<String, String>create());

    PCollection<String> KVOut = reducedPairs.apply(MapElements.into(TypeDescriptors.strings())
      .via((kvpairs) -> kvpairs.toString()));

      KVOut.apply(TextIO.write().to("bishopout"));

    p.run().waitUntilFinish();
  }
}
