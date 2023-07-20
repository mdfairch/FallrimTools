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

import static j2html.TagCreator.*;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.text.MessageFormat;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Mark
 */
abstract public class ResaverFormatting {

    static public String makeMetric(int value, String unit, Integer total, Float percentage) {
        return makeMetric(withTotal(withUnit(value, unit), withUnit(total, unit)), percentage);
    }
    
    @SuppressWarnings("UnnecessaryUnboxing")
    static private String makeMetric(String countPart, Float percentage) {
        return percentage != null
                ? MessageFormat.format("{0} ({1,number,#.##%})", countPart, percentage.floatValue())
                : countPart;
    }
    
    static private String withTotal(String subTotal, String total) {
        return total != null
                ? MessageFormat.format("{0} out of {1}", subTotal, total)
                : subTotal;
    }
    
    static private String withUnit(Integer value, String unit) {
        if (value != null) {
        return unit != null
                ? MessageFormat.format("{0} {1}", value, unit)
                : MessageFormat.format("{0}", value);
        } else {
            return null;
        }
    }
    
    static public <T> DomContent makeHTMLList(String msg, java.util.Collection<T> items, int limit) {
        return makeHTMLList(msg, items, limit, s -> s.toString());
    }
    
    static public <T> CharSequence makeTextList(String msg, java.util.Collection<T> items, int limit) {
        return makeTextList(msg, items, limit, s -> s.toString());
    }
    
    static public <T> DomContent makeHTMLList(String msg, java.util.Collection<T> items, int limit, Function<T, ? extends CharSequence> namer) {
        int excess = items.size() - limit;
        
        List<String> names = items.stream()
                .limit(limit)
                .map(namer)
                .map(CharSequence::toString)
                .collect(Collectors.toList());
        
        return p(
                text(msg),
                text("(%d items)".formatted(items.size())),
                ol(each(names, name -> li(rawHtml(name)))),
                text(excess > 0 
                        ? String.format("(+ %d more)", excess) 
                        : "")
        );
    }

    static public <T> CharSequence makeTextList(String msg, java.util.Collection<T> items, int limit, Function<T, ? extends CharSequence> namer) {
        final StringBuilder BUF = new StringBuilder();
        BUF.append(String.format(msg, items.size()));

        items.stream().limit(limit).map(namer).forEach(item -> {
            BUF.append(NLDOT).append(item).append("\n");
        });

        int excess = items.size() - limit;
        if (excess > 0) {
            BUF.append(String.format("(+ %d more\n", excess));
        }

        BUF.append('\n');
        return BUF;
    }

    /**
     * Zero-pads the hexadecimal representation of an integer so that it is a
     * full 4 bytes long.
     *
     * @param val The value to convert to hexadecimal and pad.
     * @return The zero-padded string.
     */
    static public String zeroPad8(int val) {
        String hex = Integer.toHexString(val);
        int length = hex.length();
        return ZEROES[8 - length] + hex;
    }

    /**
     * Zero-pads the hexadecimal representation of an integer so that it is a
     * full 3 bytes long.
     *
     * @param val The value to convert to hexadecimal and pad.
     * @return The zero-padded string.
     */
    static public String zeroPad6(int val) {
        String hex = Long.toHexString(val);
        int length = hex.length();
        return ZEROES[6 - length] + hex;
    }

    /**
     *
     * @return
     */
    static private String[] makeZeroes() {
        String[] zeroes = new String[16];
        zeroes[0] = "";

        for (int i = 1; i < zeroes.length; i++) {
            zeroes[i] = zeroes[i - 1] + "0";
        }

        return zeroes;
    }

    /**
     * An array of strings of zeroes with the length matching the index.
     */
    static final private String[] ZEROES = makeZeroes();

    static final private String NLDOT = "\u00b7 ";
}
