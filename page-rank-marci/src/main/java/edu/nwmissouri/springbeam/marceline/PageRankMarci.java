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
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.Flatten;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TypeDescriptors;

public class PageRankMarci {

  public static void main(String[] args) {

    // Create a PipelineOptions object. This object lets us set various execution
    // options for our pipeline, such as the runner you wish to use. This example
    // will run with the DirectRunner by default, based on the class path configured
    // in its dependencies.
    PipelineOptions options = PipelineOptionsFactory.create();

    // In order to run your pipeline, you need to make following runner specific
    // changes:
    //
    // CHANGE 1/3: Select a Beam runner, such as BlockingDataflowRunner
    // or FlinkRunner.
    // CHANGE 2/3: Specify runner-required options.
    // For BlockingDataflowRunner, set project and temp location as follows:
    // DataflowPipelineOptions dataflowOptions =
    // options.as(DataflowPipelineOptions.class);
    // dataflowOptions.setRunner(BlockingDataflowRunner.class);
    // dataflowOptions.setProject("SET_YOUR_PROJECT_ID_HERE");
    // dataflowOptions.setTempLocation("gs://SET_YOUR_BUCKET_NAME_HERE/AND_TEMP_DIRECTORY");
    // For FlinkRunner, set the runner as follows. See {@code FlinkPipelineOptions}
    // for more details.
    // options.as(FlinkPipelineOptions.class)
    // .setRunner(FlinkRunner.class);

    // Create the Pipeline object with the options we defined above
    Pipeline p = Pipeline.create(options);
    String inputFolder = "pages";
    String outputFolder = "MarciOutputs";
    PCollectionList<KV<String, String>> combinedData = PCollectionList.empty(p);

    File outputDir = new File(outputFolder);
    if (outputDir.exists()) {
      for (File file : outputDir.listFiles()) {
        file.delete();
      }
    }

    File dataDir = new File(inputFolder);
    for (File file : dataDir.listFiles()) {
      if (file.isDirectory()) {
        continue;
      }
      PCollection<KV<String, String>> processedData = mapper1(p, file.getAbsolutePath(), file.getName());
      PCollection<KV<String, String>> previousData = combinedData.apply(Flatten.<KV<String, String>>pCollections());
      combinedData = PCollectionList.of(previousData.setCoder(KvCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())))
          .and(processedData.setCoder(KvCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())));
    }

    combinedData.apply(Flatten.<KV<String, String>>pCollections())
        .apply(GroupByKey.create())
        .apply(MapElements.into(TypeDescriptors.strings())
            .via((kvpairs) -> kvpairs.toString()))
        .apply(TextIO.write().to(outputFolder + "\\"));

    p.run().waitUntilFinish();
  }

  private static PCollection<KV<String, String>> mapper1(Pipeline p, String filepath, String filename) {
    System.out.println(filename + filepath);
    return p.apply(TextIO.read().from(filepath))
        .apply(Filter.by((String line) -> line.startsWith("[")))
        .apply(MapElements.into(TypeDescriptors.strings())
            .via(linkLine -> linkLine.substring(linkLine.indexOf("(") + 1, linkLine.indexOf(")"))))
        .apply(MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.strings()))
            .via(link -> KV.of(filename, link)));
  }
}
