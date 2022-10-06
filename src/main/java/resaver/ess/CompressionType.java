/*
 * Copyright 2020 Mark Fairchild.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package resaver.ess;

import java.nio.ByteBuffer;

/**
 *
 * @author Mark Fairchild
 */
public enum CompressionType implements Element {
    UNCOMPRESSED,
    ZLIB,
    LZ4;

    static CompressionType read(ByteBuffer input) {
        return CompressionType.values()[input.getShort()];
    }

    private CompressionType() {
        this.VALUE = (short) super.ordinal();
    }

    @Override
    public void write(ByteBuffer output) {
        output.putShort(this.VALUE);
    }

    @Override
    public int calculateSize() {
        return 2;
    }

    public boolean isCompressed() {
        return this != UNCOMPRESSED;
    }

    private final short VALUE;

}
