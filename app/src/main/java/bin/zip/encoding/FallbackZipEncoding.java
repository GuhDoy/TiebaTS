/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package bin.zip.encoding;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A fallback ZipEncoding, which uses a java.io means to encode names.
 * <p>
 * <p>This implementation is not favorable for encodings other than
 * utf-8, because java.io encodes unmappable character as question
 * marks leading to unreadable ZIP entries on some operating
 * systems.</p>
 * <p>
 * <p>Furthermore this implementation is unable to tell, whether a
 * given name can be safely encoded or not.</p>
 * <p>
 * <p>The methods of this class are reentrant.</p>
 */
@SuppressWarnings("ConstantConditions")
class FallbackZipEncoding implements ZipEncoding {
    private final String charset;
    private final DetectEncoding de;

    /**
     * Construct a fallback zip encoding, which uses the platform's
     * default charset.
     */
    public FallbackZipEncoding() {
        this.charset = null;
        de = new DetectEncoding();
    }

    /**
     * Construct a fallback zip encoding, which uses the given charset.
     *
     * @param charset The name of the charset or {@code null} for
     *                the platform's default character set.
     */
    public FallbackZipEncoding(final String charset) {
        this.charset = charset;
        de = charset != null ? null : new DetectEncoding();
    }

    public boolean canEncode(final String name) {
        return true;
    }

    public ByteBuffer encode(final String name) throws IOException {
        if (this.charset == null) { // i.e. use default charset, see no-args constructor
            return ByteBuffer.wrap(name.getBytes(de.getEncode()));
        } else {
            return ByteBuffer.wrap(name.getBytes(this.charset));
        }
    }

    public String decode(final byte[] data) throws IOException {
        if (this.charset == null) {
            de.update(data);
            return new String(data, de.getEncode());
        } else {
            return new String(data, this.charset);
        }
    }

    @Override
    public String getEncoding() {
        if (this.charset == null)
            return de.getEncode().name();
        else
            return this.charset;
    }
}
