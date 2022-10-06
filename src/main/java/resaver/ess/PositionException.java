/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package resaver.ess;

import java.text.MessageFormat;

/**
 * A <code>RuntimeException</code> which indicates that the data being read is
 * mismatched with what is expected.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
public class PositionException extends RuntimeException {

    /**
     * Constructs an instance of <code>PositionMismatch</code> with the
     * specified detail message.
     *
     * @param source The section of the file in which the mismatch occurred.
     * @param expected The expected file position.
     * @param actual The actual file position.
     */
    public PositionException(String source, int expected, int actual) {
        super(MessageFormat.format(java.util.ResourceBundle.getBundle("Strings").getString("POSITION_EXCEPTION"), source, expected, actual));
    }
}
