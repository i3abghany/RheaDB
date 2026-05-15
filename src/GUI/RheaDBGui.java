package GUI;

import RheaDB.QueryResult;
import RheaDB.RheaDB;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class RheaDBGui extends JFrame {
    private static final Color BACKGROUND = new Color(0xF6F7F9);
    private static final Color SURFACE = Color.WHITE;
    private static final Color BORDER = new Color(0xD8DEE8);
    private static final Color TEXT = new Color(0x18202A);
    private static final Color MUTED = new Color(0x657386);
    private static final Color ACCENT = new Color(0x246BFE);
    private static final Color ACCENT_HOVER = new Color(0x1C59D8);
    private static final Color BUTTON_HOVER = new Color(0xEEF2F7);
    private static final Font UI_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font MONO_FONT = preferredMonoFont();

    private final JTextField directoryField;
    private final JTextArea sqlEditor;
    private final JTextArea outputArea;
    private RheaDB database;

    private RheaDBGui() {
        super("RheaDB");
        installTheme();

        this.directoryField = new JTextField(defaultDirectory());
        this.sqlEditor = new JTextArea(sampleSql(), 8, 80);
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

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("RheaDB");
        title.setFont(TITLE_FONT);
        title.setForeground(TEXT);
        titleRow.add(title, BorderLayout.WEST);

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
        pathRow.add(buttons, BorderLayout.EAST);

        JPanel toolbar = new JPanel(new BorderLayout(0, 12));
        toolbar.setOpaque(false);
        toolbar.add(titleRow, BorderLayout.NORTH);
        toolbar.add(pathRow, BorderLayout.SOUTH);
        return toolbar;
    }

    private JSplitPane buildEditorSplit() {
        styleTextArea(sqlEditor, true);
        sqlEditor.setLineWrap(true);
        sqlEditor.setWrapStyleWord(true);

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

    private static JPanel panelWithLabel(String title, JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(new LineBorder(BORDER, 1));
        scrollPane.getViewport().setBackground(area.getBackground());

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
