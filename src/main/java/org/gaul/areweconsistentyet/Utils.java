/*
 * Copyright 2014 Andrew Gaul <andrew@gaul.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gaul.areweconsistentyet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.google.common.io.ByteSource;

final class Utils {
    private Utils() {
        throw new AssertionError("intentionally unimplemented");
    }

    static ByteSource infiniteByteSource(byte fill) {
        return new InfiniteByteSource(fill);
    }

    private static class InfiniteByteSource extends ByteSource {
        private final byte fill;

        InfiniteByteSource(byte fill) {
            this.fill = fill;
        }

        @Override
        public InputStream openStream() {
            return new InfiniteByteStream(fill);
        }
    }

    private static class InfiniteByteStream extends InputStream {
        private final byte fill;

        InfiniteByteStream(byte fill) {
            this.fill = fill;
        }

        @Override
        public synchronized int read() {
            return fill;
        }

        @Override
        public synchronized int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len)
                throws IOException {
            Arrays.fill(b, off, len, fill);
            return len;
        }
    }
}
