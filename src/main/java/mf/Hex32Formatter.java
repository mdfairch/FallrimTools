/*
 * Copyright 2019 Mark.
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
package mf;

import java.text.ParseException;
import javax.swing.JFormattedTextField;

/**
 *
 * @author Mark
 */
public class Hex32Formatter extends JFormattedTextField.AbstractFormatter {

    @Override
    public Integer stringToValue(String text) throws ParseException {
        try {
            return Integer.parseUnsignedInt(text, 16);
        } catch (NumberFormatException ex) {
            throw new ParseException(text, 0);
        }
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        if (null == value) {
            return "";
        } else if (value instanceof Number) {
            Number num = (Number) value;
            int i = num.intValue();
            String s = String.format("%08x", i);
            return s;
        } else {
            throw new ParseException(value.toString(), 0);
        }
    }

}
