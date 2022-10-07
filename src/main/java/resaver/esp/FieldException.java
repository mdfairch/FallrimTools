/*
 * Copyright 2022 Mark Fairchild.
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
package resaver.esp;

/**
 * An exception that includes a second string that provides some context information, for recursive processes.
 * 
 * @author Mark Fairchild
 * 
 */
public class FieldException extends ContextException {

    public FieldException(String message, Throwable cause, String context) {
        super(message, cause);
        this.CONTEXT = context;
    }
    
	public String getContext() {
		return CONTEXT;
	}

    final public String CONTEXT;    

}
