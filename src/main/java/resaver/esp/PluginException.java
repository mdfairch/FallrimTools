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
 *
 * @author Mark
 */
public class PluginException extends Exception {

    public PluginException(Throwable cause, String plugin, String context) {
        super(String.format("Error reading plugin: %s", plugin), cause);
        this.PLUGIN = plugin;
        this.CONTEXT = context;
    }
    
    final public String CONTEXT;
    final public String PLUGIN;

}
