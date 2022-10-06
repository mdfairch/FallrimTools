/*
 * Copyright 2020 Mark.
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
package resaver;

/**
 *
 * @author Mark
 */
@SuppressWarnings("serial")
public class ListException extends RuntimeException {

    public ListException(String msg, int index, int total, Throwable cause) {
        super(String.format("%s : %d/%d", msg, index, total), cause);
        this.INDEX = index;
        this.TOTAL = total;
    }
    
    public ListException(int index, int total, Throwable cause) {
        super(String.format("%d/%d", index, total), cause);
        this.INDEX = index;
        this.TOTAL = total;
    }
    
    final public int INDEX;
    final public int TOTAL;
}
