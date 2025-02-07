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

package org.apache.inlong.sdk.transform.encode;

import org.apache.inlong.sdk.transform.pojo.CsvSinkInfo;
import org.apache.inlong.sdk.transform.pojo.KvSinkInfo;
import org.apache.inlong.sdk.transform.pojo.MapSinkInfo;
import org.apache.inlong.sdk.transform.pojo.ParquetSinkInfo;
import org.apache.inlong.sdk.transform.pojo.PbSinkInfo;
import org.apache.inlong.sdk.transform.pojo.RowDataSinkInfo;

public class SinkEncoderFactory {

    public static CsvSinkEncoder createCsvEncoder(CsvSinkInfo csvSinkInfo) {
        return new CsvSinkEncoder(csvSinkInfo);
    }

    public static KvSinkEncoder createKvEncoder(KvSinkInfo kvSinkInfo) {
        return new KvSinkEncoder(kvSinkInfo);
    }

    public static MapSinkEncoder createMapEncoder(MapSinkInfo mapSinkInfo) {
        return new MapSinkEncoder(mapSinkInfo);
    }

    public static ParquetSinkEncoder createParquetEncoder(ParquetSinkInfo parquetSinkInfo) {
        return new ParquetSinkEncoder(parquetSinkInfo);
    }

    public static PbSinkEncoder createPbEncoder(PbSinkInfo pbSinkInfo) {
        return new PbSinkEncoder(pbSinkInfo);
    }

    public static RowDataSinkEncoder createRowEncoder(RowDataSinkInfo rowDataSinkInfo) {
        return new RowDataSinkEncoder(rowDataSinkInfo);
    }

}
