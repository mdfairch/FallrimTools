/*
 * Copyright 2018 Mark.
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
package resaver.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyleConstants;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;
import javax.swing.text.Utilities;
import resaver.Analysis;
import resaver.ess.ESS;
import resaver.ess.Flags;
import resaver.ess.Linkable;
import resaver.ess.RefID;
import resaver.ess.VSVal;
import resaver.ess.papyrus.PapyrusContext;
import resaver.ess.papyrus.PapyrusFormatException;
import resaver.ess.papyrus.Variable;

/**
 *
 * @author Mark
 */
@SuppressWarnings("serial")
public class DataAnalyzer extends JSplitPane {

    static public void showDataAnalyzer(Window window, ByteBuffer data, ESS ess) {
        final DataAnalyzer ANALYZER = new DataAnalyzer(data, ess, ess.getContext());
        final JDialog DIALOG = new JDialog(window, "Analyze");
        DIALOG.setContentPane(ANALYZER);
        DIALOG.pack();
        DIALOG.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        DIALOG.setVisible(true);
    }

    static public void showDataAnalyzer(Window window, ByteBuffer data, ESS.ESSContext ctx) {
        final DataAnalyzer ANALYZER = new DataAnalyzer(data, null, ctx);
        final JDialog DIALOG = new JDialog(window, "Analyze");
        DIALOG.setContentPane(ANALYZER);
        DIALOG.pack();
        DIALOG.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        DIALOG.setVisible(true);
    }

    private DataAnalyzer(ByteBuffer newData, ESS save, ESS.ESSContext ctx) {
        super(JSplitPane.HORIZONTAL_SPLIT);
        this.setPreferredSize(new Dimension(800, 600));
        //super(new BorderLayout());

        this.DATA = Objects.requireNonNull(newData.duplicate()).order(ByteOrder.LITTLE_ENDIAN);
        this.SAVE = save;
        this.ANALYSIS = save == null || save.getAnalysis() == null ? Optional.empty() : Optional.of(save.getAnalysis());
        this.ESS_CONTEXT = ctx;
        this.PAPYRUS_CONTEXT = ctx instanceof PapyrusContext ? (PapyrusContext) ctx : null;
        
        this.currentSlice = this.DATA.slice();
        this.SIZE = Math.min(2048, this.currentSlice.limit());
        this.currentSlice.limit(SIZE);
        this.currentSlice.order(ByteOrder.LITTLE_ENDIAN);

        this.BINARY_PANEL = new JPanel(new WrapLayout(FlowLayout.LEFT));
        this.BINARY_DISPLAY = new ArrayList<>(this.DATA.remaining());
        this.BINARY_VALUES = new byte[this.DATA.remaining()];
        
        this.TEXTPANE = new JTextPane();
        //this.SCROLLER = new JScrollPane(this.TEXTPANE);
        this.SCROLLER = new JScrollPane(this.BINARY_PANEL);
        this.SCROLLER.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        this.SIDEPANE = new JPanel();
        this.HIGHLIGHTS = new java.util.LinkedList<>();
        this.FIELDS = new java.util.HashMap<>();
        this.SEARCH = new java.util.HashMap<>();
        this.COLORS = new java.util.HashMap<>();
        this.TEXTPANE.setDocument(new DefaultStyledDocument());
        final StyledDocument DOC = this.TEXTPANE.getStyledDocument();

        this.BINARY = DOC.addStyle("default", null);
        StyleConstants.setFontFamily(this.BINARY, "Courier New");

        this.CURSOR = DOC.addStyle("cursor", this.BINARY);
        StyleConstants.setBackground(this.CURSOR, Color.CYAN);
        StyleConstants.setBold(this.CURSOR, true);

        this.STRING = DOC.addStyle("string", this.BINARY);
        StyleConstants.setUnderline(this.STRING, true);

        this.VALIDREF = DOC.addStyle("string", this.BINARY);
        StyleConstants.setForeground(this.VALIDREF, Color.MAGENTA);

        this.FLOAT = DOC.addStyle("float", this.BINARY);
        StyleConstants.setItalic(this.FLOAT, true);

        this.DATAPOS = DOC.addStyle("datapos", this.CURSOR);
        StyleConstants.setBackground(this.DATAPOS, Color.LIGHT_GRAY);

        initComponents();
    }

    private void initComponents() {
        this.setLeftComponent(this.SCROLLER);
        this.setRightComponent(this.SIDEPANE);
        this.setResizeWeight(1.0);
        //this.add(this.SCROLLER, BorderLayout.CENTER);
        //this.add(this.SIDEPANE, BorderLayout.EAST);

        this.TEXTPANE.setEditable(false);
        this.TEXTPANE.getCaret().setVisible(true);

        this.SCROLLER.setMinimumSize(new Dimension(200, 200));
        this.SCROLLER.setPreferredSize(new Dimension(200, 200));

        this.SCROLLER.setBorder(BorderFactory.createTitledBorder("Raw Data"));
        this.SIDEPANE.setBorder(BorderFactory.createTitledBorder("Interpretation"));
        this.SIDEPANE.setLayout(new FlowLayout());

        final JPanel SIDE_INT = new JPanel(new GridBagLayout());
        this.SIDEPANE.add(SIDE_INT);

        final GridBagConstraints C1 = new GridBagConstraints();
        final GridBagConstraints C2 = new GridBagConstraints();
        C1.anchor = GridBagConstraints.LINE_END;
        C2.anchor = GridBagConstraints.LINE_START;
        C2.fill = GridBagConstraints.HORIZONTAL;
        C1.gridx = 0;
        C1.gridy = 0;
        C2.gridx = 1;
        C2.gridy = 0;
        C1.ipadx = 5;
        C1.ipady = 5;
        C1.weightx = 1.0;
        C2.weightx = 4.0;

        SIDE_INT.add(new JLabel("Data Type"), C1);
        SIDE_INT.add(new JLabel("Data Value"), C2);

        for (DataType type : DataType.values()) {
            final JPanel CONTAINER1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            final JPanel CONTAINER2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            final JTextPane DATAFIELD = new JTextPane();
            final JLabel LABEL = new JLabel(type.name());
            DATAFIELD.setBackground(Color.WHITE);
            DATAFIELD.setBorder(BorderFactory.createEtchedBorder());
            DATAFIELD.setContentType("text/html");
            LABEL.setLabelFor(DATAFIELD);
            CONTAINER1.add(LABEL);
            CONTAINER2.add(DATAFIELD);

            C1.gridy++;
            C2.gridy++;
            SIDE_INT.add(LABEL, C1);
            SIDE_INT.add(DATAFIELD, C2);
            this.FIELDS.put(type, DATAFIELD);
        }

        for (int i = 0; i < 6; i++) {
            final JPanel CONTAINER1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            final JPanel CONTAINER2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            final JTextField DATAFIELD = new JTextField(8);
            final JButton COLOR = new JButton("COLOR");
            DATAFIELD.setBackground(Color.WHITE);
            DATAFIELD.setBorder(BorderFactory.createEtchedBorder());

            CONTAINER1.add(DATAFIELD);
            CONTAINER2.add(COLOR);

            C1.gridy++;
            C2.gridy++;
            SIDE_INT.add(DATAFIELD, C1);
            SIDE_INT.add(COLOR, C2);
            this.SEARCH.put(DATAFIELD, COLOR);

            DATAFIELD.addActionListener(l -> updateFormatting1());

            COLOR.addActionListener(l -> {
                JButton source = (JButton) l.getSource();
                Color color = JColorChooser.showDialog(this, "Select Color", source.getBackground());
                source.setBackground(color);
            });
        }

        this.TEXTPANE.addCaretListener(e -> updateFormatting1());
        this.TEXTPANE.addCaretListener(e -> updateFormatting1());
        this.refill2();
    }

    public void addHyperlinkListener(HyperlinkListener listener) {
        this.FIELDS.values().forEach((field) -> {
            field.addHyperlinkListener(listener);
        });
    }

    public void refill2() {
        this.currentSlice = this.DATA.slice();
        this.currentSlice.limit(SIZE);
        this.currentSlice.order(ByteOrder.LITTLE_ENDIAN);

        BINARY_DISPLAY.clear();
        int refidMatcher = 0;
        int index = 0;

        while (this.currentSlice.hasRemaining()) {
            try {
                RefID val = this.ESS_CONTEXT.readRefID(this.currentSlice.slice());
                refidMatcher = val.isValid() ? 3 : refidMatcher-1;
            } catch (RuntimeException ex) {
                refidMatcher -= 1;
            }

            final byte B = this.currentSlice.get();
            JLabel D = new JLabel(String.format("%02x ", B));
            BINARY_VALUES[index] = B;
            BINARY_DISPLAY.add(D);
            BINARY_PANEL.add(D);

            D.setFont(Font.getFont("monospaced"));
            D.setBackground(Color.WHITE);

            if (refidMatcher > 0) {
                D.setForeground(Color.RED);
            } else {
                D.setForeground(Color.BLACK);
            } 
            
            D.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    updateFormatting2(D);
                }
            });
        }
        ((Buffer) this.currentSlice).flip();
    }

    synchronized private void updateFormatting2(JLabel newSelected) {
        if (selected != null) {
            selected.setBackground(Color.WHITE);
            selected.setBorder(null);
        }
        
        selected = newSelected;
        if (newSelected == null) {
            return;
        }

        selected.setBackground(Color.CYAN);
        selected.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        
        int index = this.BINARY_DISPLAY.indexOf(selected);
        if (index < 0 || index >= this.BINARY_VALUES.length) {
            return;
        }
        
        final String TITLE = String.format("Offset = %02x (%d) bytes", index, index);
        ((TitledBorder) this.SCROLLER.getBorder()).setTitle(TITLE);
        this.SCROLLER.updateUI();

        this.fillFields(index);
    }

    public void refill1() {
        this.currentSlice = this.DATA.slice();
        this.currentSlice.limit(SIZE);
        this.currentSlice.order(ByteOrder.LITTLE_ENDIAN);

        int refidMatcher = 0;
        
        try {
            final StyledDocument DOC = new DefaultStyledDocument();
            while (this.currentSlice.hasRemaining()) {
                try {
                    RefID val = this.ESS_CONTEXT.readRefID(this.currentSlice.slice());
                    refidMatcher = val.isValid() ? 3 : refidMatcher-1;
                } catch (RuntimeException ex) {
                    refidMatcher -= 1;
                }
                                      
                final byte B = this.currentSlice.get();
                final String STR = String.format("%02x ", B);
                if (refidMatcher > 0) {
                    DOC.insertString(DOC.getLength(), STR, VALIDREF);
                } else {
                    DOC.insertString(DOC.getLength(), STR, BINARY);
                }
            }
            ((Buffer) this.currentSlice).flip();

            this.TEXTPANE.setDocument(DOC);
            if (this.DATA.limit() > 0) {
                this.TEXTPANE.setCaretPosition(1);
                this.TEXTPANE.setCaretPosition(0);
            }

        } catch (BadLocationException ex) {
            ex.printStackTrace(System.err);
            assert false;
        }
    }

    private void updateFormatting1() {
        final StyledDocument DOC = this.TEXTPANE.getStyledDocument();
        final javax.swing.text.Caret CARET = this.TEXTPANE.getCaret();
        final int TEXT_POS = CARET.getDot();
        final int TEXT_MARK = CARET.getMark();

        if (TEXT_POS == TEXT_MARK) {
            final int DATA_POS = TEXT_POS / 3;
            final int TEXT_START = DATA_POS * 3;

            final int BUFFER_POS = 0;

            if (TEXT_POS % 3 == 1) {
                this.TEXTPANE.setCaretPosition(TEXT_START + 3);
                return;
            } else if (TEXT_POS % 3 == 2) {
                this.TEXTPANE.setCaretPosition(TEXT_START);
                return;
            }

            this.HIGHLIGHTS.forEach(h -> {
                Style style = this.COLORS.computeIfAbsent(h.COLOR, color -> {
                    Style s = DOC.addStyle("binary_" + color.toString(), this.BINARY);
                    StyleConstants.setBackground(s, color);
                    return s;
                });
                DOC.setCharacterAttributes(h.C1 * 3, h.C2 * 3, style, false);
            });

            DOC.setCharacterAttributes(0, DOC.getLength(), BINARY, true);
            DOC.setCharacterAttributes(BUFFER_POS, 2, DATAPOS, false);
            DOC.setCharacterAttributes(TEXT_START, 2, CURSOR, false);

            this.SEARCH.forEach((field, button) -> {
                Color color = button.getBackground();
                Style style = this.COLORS.computeIfAbsent(color, c -> {
                    Style s = DOC.addStyle("binary_" + c.toString(), this.BINARY);
                    StyleConstants.setBackground(s, c);
                    return s;
                });

                String text = field.getText().toLowerCase();
                int length = text.length();
                if (length > 0) {
                    for (int i = 0; i < DOC.getLength() - length; i += 3) {
                        try {
                            if (DOC.getText(i, length).equals(text)) {
                                DOC.setCharacterAttributes(i, length, style, false);
                            }
                        } catch (BadLocationException ex) {
                            int breakpoint = 0;
                        }
                    }
                }
            });

            /*for (int i = 0; i < DOC.getLength() / 3; i++) {
                this.currentSlice.s
                ByteBuffer slice = this.DATA.slice(i, DOC.getLength() - i);
                try {
                    String f = mf.BufferUtil.getWString(slice);
                    if (validString(f)) {
                        DOC.setCharacterAttributes(i * 3, 6 + 3 * f.length(), STRING, false);
                    }
                } catch (BufferUnderflowException ex) {

                }
            }*/
            try {
                final int COLUMN = (TEXT_POS - Utilities.getRowStart(TEXTPANE, TEXT_POS)) / 3;
                final String TITLE = String.format("[%2x], Offset = %02x (%d) bytes", COLUMN, DATA_POS, DATA_POS);
                ((TitledBorder) this.SCROLLER.getBorder()).setTitle(TITLE);
                this.SCROLLER.updateUI();
            } catch (BadLocationException ex) {
                final String TITLE = String.format("Offset = %02x (%d) bytes", DATA_POS, DATA_POS);
                ((TitledBorder) this.SCROLLER.getBorder()).setTitle(TITLE);
                this.SCROLLER.updateUI();
            }

            this.fillFields(DATA_POS);

        } else {
            int start = Math.min(TEXT_POS, TEXT_MARK);
            int end = Math.max(TEXT_POS, TEXT_MARK);
            int dst = start / 3;
            int den = end / 3;
            int newStart = dst * 3;
            int newEnd = den * 3;

            if (newStart != start) {
                TEXTPANE.setSelectionStart(newStart);
            }
            if (newEnd != end) {
                TEXTPANE.setSelectionEnd(newEnd);
            }
        }
    }

    /**
     * Data types that will be interpreted.
     */
    private enum DataType {
        Integer_10((in, ess) -> Integer.toString(in.getInt())),
        Integer_16((in, ess) -> String.format("%08x", in.getInt())),
        Integer_2((in, ess) -> Flags.readIntFlags(in).toString()), 
        Float((in, ess) -> java.lang.Float.toString(in.getFloat())),
        Boolean((in, ess) -> java.lang.Boolean.toString(in.get() != 0)),
        VSVal((in, ess) -> new VSVal(in).toString()), 
        BString((in, ess) -> trim(mf.BufferUtil.getBString(in), 30)),
        WString((in, ess) -> trim(mf.BufferUtil.getWString(in), 30)),
        LString((in, ess) -> trim(mf.BufferUtil.getLString(in), 30)),
        ZString((in, ess) -> trim(mf.BufferUtil.getZString(in), 30)),
        RefID((in, ess) -> ess.getContext().readRefID(in).toString()), 
        EID32((in, ess) -> {
            int val = in.getInt();
            if (ess == null || ess.getPapyrus() == null) {
                return String.format("%08x", val);
            } else {
                Linkable link = ess.getPapyrus().getContext().broadSpectrumSearch(val);
                if (null != link) {
                    return String.format("%s: %s", link.getClass().getName(), link.toHTML(null));
                } else {
                    return String.format("%08x", val);                    
                }
            }
        }), 
        EID64((in, ess) -> {
            long val = in.getLong();
            if (ess == null || ess.getPapyrus() == null) {
                return String.format("%16x", val);
            } else {
                Linkable link = ess.getPapyrus().getContext().broadSpectrumSearch(val);
                if (null != link) {
                    return String.format("%s: %s", link.getClass().getName(), link.toHTML(null));
                } else {
                    return String.format("%16x", val);                    
                }
            }
        }), 
        TString((in, ess) -> {
            if (ess == null || ess.getPapyrus() == null) {
                return null;
            } else {
                try {
                    return ess.getPapyrus().getContext().readTString(in).toString();
                } catch (PapyrusFormatException ex) {
                    return null;
                }
            }
        }),
        Var((in, ess) -> {
            if (ess == null || ess.getPapyrus() == null) {
                return null;
            } else {
                try {
                    return Variable.read(in, ess.getPapyrus().getContext()).toHTML(null);
                } catch (PapyrusFormatException ex) {
                    return null;
                }
            }
        });
        
        
        DataType(java.util.function.BiFunction<ByteBuffer, ESS, String> parser) {
            this.PARSER = parser;
        }
        
        final java.util.function.BiFunction<ByteBuffer, ESS, String> PARSER;
        
    };

    /**
     *
     * @param c1
     * @param c2
     * @param color
     */
    public void addHighlight(int c1, int c2, Color color) {
        Objects.requireNonNull(color);
        
        /*
        int len = this.TEXTPANE.getDocument().getLength();
        if (c1 < 0 || c1 >= len) {
            throw new IllegalArgumentException();
        }
        if (c2 < 0 || c2 >= len) {
            throw new IllegalArgumentException();
        }
        this.HIGHLIGHTS.add(new Highlight(c1, c2, color));
        */
    }

    /**
     * Fill the interpretation fields.
     *
     * @param dataPos
     */
    private void fillFields(int dataPos) {
        for (DataType type : DataType.values()) {
            final JTextPane FIELD = this.FIELDS.get(type);
            FIELD.setText("");
            FIELD.setBackground(Color.WHITE);

            final StringBuilder BUF = new StringBuilder();
            BUF.append("<code>");

            this.currentSlice.position(dataPos);

            try {
            
                String parsed = type.PARSER.apply(this.currentSlice, this.SAVE);
                BUF.append(parsed);

                if (!validString(parsed)) {
                    FIELD.setBackground(Color.LIGHT_GRAY);                
                }

                if (type == DataType.Var) {
                    if (parsed.startsWith("BLAH"));
                    /*
                    if (var instanceof Variable.Array) {
                        this.addHighlight(dataPos, var.calculateSize(), Color.PINK);
                    } else if (var instanceof Variable.Ref) {
                        this.addHighlight(dataPos, var.calculateSize(), Color.GREEN);
                    } else if (var instanceof Variable.Null) {
                        //this.addHighlight(dataPos, var.calculateSize(), Color.GREEN);                            
                    } else if (var instanceof Variable.Null) {
                        this.addHighlight(dataPos, var.calculateSize(), Color.YELLOW);
                    }*/
                }
            
                BUF.append("</code>");
                FIELD.setText(BUF.toString());

            } catch (RuntimeException ex) {
                FIELD.setBackground(Color.LIGHT_GRAY);
            } finally {
                this.SIDEPANE.revalidate();
            }
        }
    }

    final ByteBuffer DATA;
    final private ESS SAVE;
    final private ESS.ESSContext ESS_CONTEXT;
    final private PapyrusContext PAPYRUS_CONTEXT;
    final private Optional<Analysis> ANALYSIS;
    private ByteBuffer currentSlice;

    final private int SIZE;

    final private JPanel BINARY_PANEL;
    final private ArrayList<JLabel> BINARY_DISPLAY;
    final private byte[] BINARY_VALUES;
    private JLabel selected = null;
    
    final private JScrollPane SCROLLER;
    final private JTextPane TEXTPANE;
    final private JPanel SIDEPANE;
    final private Map<DataType, JTextPane> FIELDS;
    final private Map<JTextField, JButton> SEARCH;
    final private Style BINARY;
    final private Style DATAPOS;
    final private Style CURSOR;
    final private Style STRING;
    final private Style FLOAT;
    final private Style VALIDREF;
    final private Map<Color, Style> COLORS;
    final private List<Highlight> HIGHLIGHTS;

    static private class Highlight {

        public Highlight(int c1, int c2, Color color) {
            this.C1 = c1;
            this.C2 = c2;
            this.COLOR = color;
        }
        final public int C1;
        final public int C2;
        final public Color COLOR;
    }

    /**
     *
     * @param s
     * @return
     */
    static public boolean validString(String s) {
        return stringValidity(s) > 0.5;
    }

    /**
     *
     * @param s
     * @return
     */
    static public double stringValidity(String s) {
        if (s == null || s.length() == 0) {
            return 0.0;
        }

        long invalid = s.chars().filter(ch -> Character.isISOControl(ch)).count();
        if (invalid > 0) {
            return 0.0;
        }

        long valid = s.chars().filter(ch -> Character.isLetterOrDigit(ch)).count();
        return (double) valid / s.length();
    }

    static private String trim(String s, int length) {
        return s.length() <= length ? s : s.substring(0, length) + "...";
    }
}
