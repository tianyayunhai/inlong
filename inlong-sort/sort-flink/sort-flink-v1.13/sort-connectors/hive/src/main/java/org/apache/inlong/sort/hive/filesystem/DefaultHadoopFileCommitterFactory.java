/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.hive.filesystem;

import org.apache.flink.formats.hadoop.bulk.HadoopFileCommitter;
import org.apache.flink.formats.hadoop.bulk.HadoopFileCommitterFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * The default hadoop file committer factory which always use {@link HadoopRenameFileCommitter}.
 */
public class DefaultHadoopFileCommitterFactory implements HadoopFileCommitterFactory {

    private static final long serialVersionUID = 1L;

    private final boolean sinkMultipleEnable;

    public DefaultHadoopFileCommitterFactory(boolean sinkMultipleEnable) {
        this.sinkMultipleEnable = sinkMultipleEnable;
    }

    @Override
    public HadoopFileCommitter create(Configuration configuration, Path targetFilePath) throws IOException {
        return new HadoopRenameFileCommitter(configuration, targetFilePath, sinkMultipleEnable);
    }

    @Override
    public HadoopFileCommitter recoverForCommit(Configuration configuration, Path targetFilePath, Path tempFilePath) {
        return new HadoopRenameFileCommitter(configuration, targetFilePath, tempFilePath, sinkMultipleEnable);
    }
}