package GUI;

import RheaDB.QueryResult;
import RheaDB.RheaDB;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RheaDBGui extends JFrame {
    private static final Color BACKGROUND = new Color(0xF6F7F9);
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = new Color(0xD8DEE8);
    private static final Color TEXT = new Color(0x18202A);
    private static final Color MUTED = new Color(0x657386);
    private static final Color ACCENT = new Color(0x246BFE);
    private static final Color ACCENT_HOVER = new Color(0x1C59D8);
    private static final Color BUTTON_HOVER = new Color(0xEEF2F7);
    private static final Color SQL_KEYWORD = new Color(0x1C59D8);
    private static final Color SQL_STRING = new Color(0x16845B);
    private static final Color SQL_NUMBER = new Color(0xB45F06);
    private static final Color SQL_OPERATOR = new Color(0x7A4CC2);
    private static final Color SQL_COMMENT = new Color(0x8A94A6);
    private static final String LOGO_RESOURCE = "/GUI/assets/rheadb-logo.png";
    private static final String LOGO_FILE = "src/GUI/assets/rheadb-logo.png";
    private static final Font UI_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font MONO_FONT = preferredMonoFont();
    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
            "\\b(SELECT|FROM|WHERE|INSERT|INTO|VALUES|CREATE|TABLE|DROP|DELETE|UPDATE|SET|INDEX|DESCRIBE|COMPACT|AND|OR|NOT|NULL|INT|FLOAT|STRING)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    private static final Pattern OPERATOR_PATTERN = Pattern.compile("[=<>!]+|[(),;*]");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("--[^\\n]*");

    private final JTextField directoryField;
    private final JTextPane sqlEditor;
    private final JTextArea outputArea;
    private boolean highlightingSql;
    private RheaDB database;

    private RheaDBGui() {
        super("RheaDB");
        installTheme();

        try {
            BufferedImage raw = loadLogoImage();
            BufferedImage cropped = cropOuterWhitespace(raw);
            BufferedImage iconSource = cropIconArea(cropped);
            int iconSize = 48;
            Image scaledIcon = iconSource.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
            setIconImage(scaledIcon);
        } catch (IOException ignored) {
            // If the logo isn't available, just continue without an icon.
        }

        this.directoryField = new JTextField(defaultDirectory());
        this.sqlEditor = new JTextPane();
        this.sqlEditor.setText(sampleSql());
        this.outputArea = new JTextArea(16, 80);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setContentPane(buildContent());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDatabase();
                dispose();
            }
        });

        openDatabase();
        pack();
        setMinimumSize(new Dimension(840, 620));
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RheaDBGui gui = new RheaDBGui();
            gui.setVisible(true);
        });
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        root.add(buildToolbar(), BorderLayout.NORTH);
        root.add(buildEditorSplit(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildToolbar() {
        JButton browseButton = secondaryButton("Browse");
        browseButton.addActionListener(e -> browseForDirectory());

        JButton openButton = secondaryButton("Open");
        openButton.addActionListener(e -> openDatabase());

        JButton runButton = primaryButton("Run");
        runButton.addActionListener(e -> runSql());

        JButton clearButton = secondaryButton("Clear");
        clearButton.addActionListener(e -> outputArea.setText(""));

        JPanel pathRow = new JPanel(new BorderLayout(10, 0));
        pathRow.setOpaque(false);
        JLabel pathLabel = label("Data");
        styleTextField(directoryField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(browseButton);
        buttons.add(openButton);
        buttons.add(clearButton);
        buttons.add(runButton);

        pathRow.add(pathLabel, BorderLayout.WEST);
        pathRow.add(directoryField, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        controls.add(pathRow, constraints);

        constraints.gridy = 1;
        constraints.insets = new Insets(8, 0, 0, 0);
        controls.add(buttons, constraints);

        JPanel toolbar = new JPanel(new BorderLayout(18, 0));
        toolbar.setOpaque(false);
        toolbar.add(logoLabel(), BorderLayout.WEST);
        toolbar.add(controls, BorderLayout.CENTER);
        return toolbar;
    }

    private JSplitPane buildEditorSplit() {
        styleSqlEditor(sqlEditor);

        styleTextArea(outputArea, false);
        outputArea.setEditable(false);

        JPanel editorPanel = panelWithLabel("SQL", sqlEditor);
        JPanel outputPanel = panelWithLabel("Output", outputArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, outputPanel);
        splitPane.setResizeWeight(0.38);
        splitPane.setBorder(null);
        splitPane.setDividerSize(10);
        splitPane.setOpaque(false);
        splitPane.setBackground(BACKGROUND);
        return splitPane;
    }

    private void browseForDirectory() {
        JFileChooser chooser = new JFileChooser(directoryField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Open RheaDB Directory");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
            openDatabase();
        }
    }

    private void openDatabase() {
        closeDatabase();
        database = new RheaDB(directoryField.getText());
        database.setLazyCommit(false);
        appendOutput("Opened " + directoryField.getText());
    }

    private void closeDatabase() {
        if (database == null || database.isClosed()) {
            return;
        }

        database.close();
    }

    private void runSql() {
        if (database == null || database.isClosed()) {
            openDatabase();
        }

        for (String statement : sqlEditor.getText().split(";")) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            executeStatement(trimmed + ";");
        }
    }

    private void executeStatement(String sql) {
        appendOutput("\nSQL> " + sql);

        PrintStream originalOut = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
            System.setOut(printStream);
            QueryResult result = database.executeStatement(sql);
            printStream.flush();

            String messages = capturedOutput.toString(StandardCharsets.UTF_8);
            if (!messages.isBlank()) {
                appendOutput(messages.stripTrailing());
            } else if (result == null) {
                appendOutput("Statement completed.");
            } else {
                appendOutput(result.toString().stripTrailing());
            }
        } finally {
            System.setOut(originalOut);
        }
    }

    private void appendOutput(String text) {
        outputArea.append(text);
        outputArea.append(System.lineSeparator());
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private static void installTheme() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        UIManager.put("Label.font", UI_FONT);
        UIManager.put("Button.font", UI_FONT);
        UIManager.put("TextField.font", UI_FONT);
        UIManager.put("TextArea.font", MONO_FONT);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("SplitPane.background", BACKGROUND);
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(MUTED);
        return label;
    }

    private static JLabel logoLabel() {
        JLabel fallback = new JLabel("RheaDB");
        fallback.setFont(TITLE_FONT);
        fallback.setForeground(TEXT);

        try {
            BufferedImage source = cropOuterWhitespace(loadLogoImage());
            int targetHeight = 76;
            int targetWidth = Math.max(1, source.getWidth() * targetHeight / source.getHeight());
            Image scaledLogo = source.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);

            JLabel label = new JLabel(new ImageIcon(scaledLogo));
            label.setBorder(new EmptyBorder(0, 0, 2, 0));
            return label;
        } catch (IOException exception) {
            return fallback;
        }
    }

    private static BufferedImage loadLogoImage() throws IOException {
        URL resource = RheaDBGui.class.getResource(LOGO_RESOURCE);
        if (resource != null) {
            return ImageIO.read(resource);
        }

        File logoFile = new File(LOGO_FILE);
        if (logoFile.exists()) {
            return ImageIO.read(logoFile);
        }

        throw new IOException("Logo image not found.");
    }

    private static BufferedImage cropOuterWhitespace(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = 0;
        int maxY = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!isNearWhite(image.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (minX > maxX || minY > maxY) {
            return image;
        }

        int padding = 28;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(image.getWidth() - 1, maxX + padding);
        maxY = Math.min(image.getHeight() - 1, maxY + padding);
        return image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static BufferedImage cropIconArea(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        if (w <= 0 || h <= 0) return image;

        int side = Math.min(w, Math.max(1, (int) (h * 0.8)));
        int x = 0;
        int y = 40; // take the top portion where the mark sits

        try {
            return image.getSubimage(x, y, side, side);
        } catch (java.awt.image.RasterFormatException e) {
            return image;
        }
    }

    private static boolean isNearWhite(int rgb) {
        Color color = new Color(rgb, true);
        return color.getRed() > 245 && color.getGreen() > 245 && color.getBlue() > 245;
    }

    private static JButton primaryButton(String text) {
        return flatButton(text, SURFACE, BUTTON_HOVER, Color.BLUE, false);
    }

    private static JButton secondaryButton(String text) {
        return flatButton(text, SURFACE, BUTTON_HOVER, TEXT, false);
    }

    private static JButton flatButton(String text, Color normal, Color hover, Color foreground, boolean filled) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D graphics = (Graphics2D) g.create();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(getBackground());
                graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                graphics.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(UI_FONT);
        button.setForeground(foreground);
        button.setBackground(normal);
        button.setOpaque(true);
        button.setBorder(new CompoundBorder(
                filled ? new LineBorder(normal, 1) : new LineBorder(BORDER, 1),
                new EmptyBorder(7, 14, 7, 14)
        ));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
                if (filled) {
                    button.setBorder(new CompoundBorder(new LineBorder(hover, 1), new EmptyBorder(7, 14, 7, 14)));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normal);
                if (filled) {
                    button.setBorder(new CompoundBorder(new LineBorder(normal, 1), new EmptyBorder(7, 14, 7, 14)));
                }
            }
        });
        return button;
    }

    private static void styleTextField(JTextField field) {
        field.setFont(UI_FONT);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setCaretColor(TEXT);
        field.setSelectionColor(new Color(0xDCE8FF));
        field.setSelectedTextColor(TEXT);
        field.setBorder(fieldBorder());
    }

    private static void styleTextArea(JTextArea area, boolean editable) {
        area.setFont(MONO_FONT);
        area.setForeground(TEXT);
        area.setBackground(editable ? SURFACE : new Color(0xFBFCFE));
        area.setCaretColor(TEXT);
        area.setSelectionColor(new Color(0xDCE8FF));
        area.setSelectedTextColor(TEXT);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));
    }

    private void styleSqlEditor(JTextPane editor) {
        editor.setFont(MONO_FONT);
        editor.setForeground(TEXT);
        editor.setBackground(SURFACE);
        editor.setCaretColor(TEXT);
        editor.setSelectionColor(new Color(0xDCE8FF));
        editor.setSelectedTextColor(TEXT);
        editor.setBorder(new EmptyBorder(12, 12, 12, 12));
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleSqlHighlight();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleSqlHighlight();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleSqlHighlight();
            }
        });
        applySqlHighlighting();
    }

    private void scheduleSqlHighlight() {
        if (!highlightingSql) {
            SwingUtilities.invokeLater(this::applySqlHighlighting);
        }
    }

    private void applySqlHighlighting() {
        if (highlightingSql) {
            return;
        }

        highlightingSql = true;
        try {
            StyledDocument document = sqlEditor.getStyledDocument();
            String text = sqlEditor.getText();
            document.setCharacterAttributes(0, text.length(), sqlStyle(TEXT, false, false), true);
            applyStyle(document, text, KEYWORD_PATTERN, sqlStyle(SQL_KEYWORD, true, false));
            applyStyle(document, text, NUMBER_PATTERN, sqlStyle(SQL_NUMBER, false, false));
            applyStyle(document, text, OPERATOR_PATTERN, sqlStyle(SQL_OPERATOR, false, false));
            applyStyle(document, text, STRING_PATTERN, sqlStyle(SQL_STRING, false, false));
            applyStyle(document, text, COMMENT_PATTERN, sqlStyle(SQL_COMMENT, false, true));
        } finally {
            highlightingSql = false;
        }
    }

    private static void applyStyle(StyledDocument document, String text, Pattern pattern, SimpleAttributeSet style) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            document.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, true);
        }
    }

    private static SimpleAttributeSet sqlStyle(Color color, boolean bold, boolean italic) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);
        StyleConstants.setItalic(style, italic);
        StyleConstants.setFontFamily(style, MONO_FONT.getFamily());
        StyleConstants.setFontSize(style, MONO_FONT.getSize());
        return style;
    }

    private static JPanel panelWithLabel(String title, JComponent component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(new LineBorder(BORDER, 1));
        scrollPane.getViewport().setBackground(component.getBackground());

        JLabel label = label(title);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.add(label, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private static Border fieldBorder() {
        return new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(8, 10, 8, 10));
    }

    private static Font preferredMonoFont() {
        String[] preferredFamilies = {
                "JetBrains Mono",
                "Cascadia Mono",
                "Cascadia Code",
                "Consolas",
                "Menlo",
                "DejaVu Sans Mono",
                Font.MONOSPACED
        };

        String[] availableFamilies = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();

        for (String preferredFamily : preferredFamilies) {
            for (String availableFamily : availableFamilies) {
                if (availableFamily.equalsIgnoreCase(preferredFamily)) {
                    return new Font(availableFamily, Font.PLAIN, 14);
                }
            }
        }

        return new Font(Font.MONOSPACED, Font.PLAIN, 14);
    }

    private static String defaultDirectory() {
        return System.getProperty("user.home") + File.separator + "RheaDBGuiData";
    }

    private static String sampleSql() {
        return """
                CREATE TABLE Planets (id INT, name STRING, mass FLOAT);
                INSERT INTO Planets VALUES (1, "Mercury", 0.3);
                INSERT INTO Planets VALUES (2, "Venus", 4.9);
                INSERT INTO Planets VALUES (3, "Earth", 6.0);
                SELECT * FROM Planets;
                """;
    }
}
