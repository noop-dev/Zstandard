/*
 * Copyright (C) 2011 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.airlift.compress.snappy;

import io.airlift.compress.HadoopNative;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestSnappyCodecLegacy
        extends AbstractSnappyTest
{
    static {
        HadoopNative.initialize();
    }

    private final CompressionCodec airliftSnappyCodec = new SnappyCodec();
    private final CompressionCodec hadoopSnappyCodec;

    public TestSnappyCodecLegacy()
    {

        org.apache.hadoop.io.compress.SnappyCodec hadoopSnappyCodec = new org.apache.hadoop.io.compress.SnappyCodec();
        hadoopSnappyCodec.setConf(new Configuration());
        this.hadoopSnappyCodec = hadoopSnappyCodec;
    }

    @Test
    public void testGetCodec()
    {
        Path path = new Path("test.json.snappy");

        Configuration conf = new Configuration();
        conf.set("io.compression.codecs", SnappyCodec.class.getName());

        CompressionCodec codec = new CompressionCodecFactory(conf).getCodec(path);

        assertNotNull(codec);
        assertTrue(codec instanceof SnappyCodec);
    }


    @Override
    protected void verifyCompression(byte[] input, int offset, int length)
            throws Exception
    {
        // compress with airlift
        byte[] airliftCompressed = compress(input, offset, length, airliftSnappyCodec);

        // decompress with hadoop
        byte[] uncompressed = uncompress(airliftCompressed, 0, airliftCompressed.length, hadoopSnappyCodec);

        if (!SnappyInternalUtils.equals(uncompressed, 0, input, offset, length)) {
            Assert.fail("Invalid uncompressed output for input length " + length + " at offset " + offset);
        }
    }

    private static byte[] compress(byte[] original, int offset, int length, CompressionCodec codec)
            throws IOException
    {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        OutputStream snappyOut = codec.createOutputStream(out);
        snappyOut.write(original, offset, length);
        snappyOut.close();
        return out.toByteArray();
    }

    private static byte[] uncompress(byte[] compressed, int offset, int length, CompressionCodec codec)
            throws IOException
    {
        return toByteArray(codec.createInputStream(new ByteArrayInputStream(compressed, offset, length)));
    }
}