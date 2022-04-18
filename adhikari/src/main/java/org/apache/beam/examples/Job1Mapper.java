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

import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Count;
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
 * <p>
 * This class, {@link MinimalWordCount}, is the first in a series of four
 * successively more
 * detailed 'word count' examples. Here, for simplicity, we don't show any
 * error-checking or
 * argument processing, and focus on construction of the pipeline, which chains
 * together the
 * application of core transforms.
 *
 * <p>
 * Next, see the {@link WordCount} pipeline, then the
 * {@link DebuggingWordCount}, and finally the
 * {@link WindowedWordCount} pipeline, for more detailed examples that introduce
 * additional
 * concepts.
 *
 * <p>
 * Concepts:
 *
 * <pre>
 *   1. Reading data from text files
 *   2. Specifying 'inline' transforms
 *   3. Counting items in a PCollection
 *   4. Writing data to text files
 * </pre>
 *
 * <p>
 * No arguments are required to run this pipeline. It will be executed with the
 * DirectRunner. You
 * can see the results in the output files in your current working directory,
 * with names like
 * "wordcounts-00001-of-00005. When running on a distributed service, you would
 * use an appropriate
 * file service.
 */

public class Job1Mapper {

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


    //Changing to be able to write using TextIO
    PCollection<String> writableFile = pColReduced.apply(MapElements.into(TypeDescriptors.strings())
        .via((kvpairs) -> kvpairs.toString()));

    //writing the result
    writableFile.apply(TextIO.write().to("AdhikariPR"));
    p.run().waitUntilFinish();
  }

  public static PCollection<KV<String, String>> adhikariKVPairGenerator(Pipeline p, String folderName, String fileName) {

    String dataPath = "./" + folderName + "/" + fileName;
    PCollection<String> pColLine = p.apply(TextIO.read().from(dataPath));

    // .apply(Filter.by((String line) -> !line.isEmpty()))
    // .apply(Filter.by((String line) -> !line.contentEquals(" ")))

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
}
