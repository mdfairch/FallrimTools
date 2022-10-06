/*
 * Copyright 2016 Mark Fairchild.
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
package resaver.ess.papyrus;

import java.nio.ByteBuffer;

/**
 * Describes a function parameter in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
final public class FunctionParam extends MemberDesc {

    /**
     * @see MemberDesc#MemberData(resaver.ByteBuffer,
     * resaver.ess.papyrus.StringTable)
     * @param input The input stream.
     * @param context The <code>PapyrusContext</code> info.
     * @throws PapyrusFormatException
     */
    public FunctionParam(ByteBuffer input, PapyrusContext context) throws PapyrusFormatException {
        super(input, context);
    }

    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return "(param) " + super.toString();
    }

}
