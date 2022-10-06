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
package resaver.archive;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Calculates filename and directory name hashes.
 *
 * INCOMPLETE
 * 
 * @author Mark Fairchild
 */
abstract public class BSAHash {

    /**
     *
     * @param file
     * @return
     */
    static public long genHashFile(java.io.File file) {
        
        long hash = 0;
        long hash2 = 0;

        final Matcher MATCHER = FILENAME_PATTERN.matcher(file.getName());
        if (!MATCHER.matches()) {
            throw new IllegalArgumentException("Filename does not have the form \"filename.extension\"");
        }

        String fileName = (MATCHER.matches() ? MATCHER.group(1).toLowerCase() : file. getName().toLowerCase());
        String fileExt = (MATCHER.matches() ? MATCHER.group(2).toLowerCase() : "");

        for (char ch : fileExt.toCharArray()) {
            hash *= 0x1003f;
            hash += ch;
        }

        int len = fileName.length();
        char[] chars = fileName.toCharArray();

        for (int i = 1; i < len - 2; i++) {
            hash2 *= 0x1003f;
            hash2 += chars[i];
        }

        hash += hash2;
        hash2 = 0;
        hash <<= 32;

        hash2 = chars[len - 1];
        hash2 |= (len > 2 ? chars[len - 2] : 0);
        hash2 |= (len << 16);
        hash2 |= (chars[0] << 24);
        
        throw new UnsupportedOperationException();
    }

    static final private String FILENAME_REGEX = "^(.*)(\\.\\w+)$";
    static final private Pattern FILENAME_PATTERN = Pattern.compile(FILENAME_REGEX);

}
