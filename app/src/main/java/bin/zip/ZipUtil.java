/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package bin.zip;

/**
 * Utility class for handling DOS and Java time conversions.
 *
 * @since Ant 1.8.1
 */
public abstract class ZipUtil {
    /**
     * Create a copy of the given array - or return null if the
     * argument is null.
     */
    public static byte[] copy(byte[] from) {
        if (from != null) {
            byte[] to = new byte[from.length];
            System.arraycopy(from, 0, to, 0, to.length);
            return to;
        }
        return null;
    }

}