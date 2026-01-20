package com.ved.accessChecker;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class AccessCheckerApp extends JFrame {

    private enum Theme {SYSTEM, LIGHT, DARK, DRACULA}

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(AccessCheckerApp.class);
    private static final String PREF_THEME = "ui.theme";

    private static final boolean isDark = UIManager.getLookAndFeel().getName().toLowerCase().contains("dark");


    static {
        try {

            Theme theme = Theme.valueOf(
                    PREFS.get(PREF_THEME, Theme.SYSTEM.name())
            );

            switch (theme) {
                case LIGHT -> FlatLightLaf.setup();
                case DARK -> FlatDarkLaf.setup();
                case DRACULA -> FlatDraculaIJTheme.setup();
                case SYSTEM -> {
                    if (isDark) FlatDarkLaf.setup();
                    else FlatLightLaf.setup();
                }
            }

            UIManager.put("defaultFont",
                    new Font("Segoe UI", Font.PLAIN, 13));

        } catch (Exception ignored) {
        }
    }

    private JTextArea inputArea;
    private JTextPane resultPane;
    private JTextField timeoutField;
    private JProgressBar progressBar;
    private JRadioButton hostBtn, tcpBtn;

    private final List<Result> results = new ArrayList<>();

    public AccessCheckerApp() {
        setTitle("Access Checker");
        setSize(980, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        Image icon = Toolkit.getDefaultToolkit()
                .getImage(getClass().getResource("/icon.png"));
        setIconImage(icon);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        add(createCenter(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
    }

    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Access Checker");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel copyright = new JLabel("V1.0 © rugved.d");
        copyright.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copyright.setForeground(Color.GRAY);

        JComboBox<Theme> themeBox = new JComboBox<>(Theme.values());
        themeBox.setSelectedItem(
                Theme.valueOf(PREFS.get(PREF_THEME, Theme.SYSTEM.name()))
        );
        themeBox.setPreferredSize(new Dimension(150, 28));
        themeBox.addActionListener(e ->
                switchTheme((Theme) themeBox.getSelectedItem())
        );

        header.add(title, BorderLayout.WEST);
        header.add(themeBox, BorderLayout.EAST);
        header.add(copyright, BorderLayout.SOUTH);
        return header;
    }

    private JComponent createCenter() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        Font mono = new Font("JetBrains Mono", Font.PLAIN, 13);

        gbc.gridx = 0;
        gbc.weightx = 0.45;

        inputArea = new JTextArea();
        inputArea.setFont(mono);
        center.add(new JScrollPane(inputArea), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.55;

        resultPane = new JTextPane();
        resultPane.setEditable(false);
        resultPane.setFont(mono);
        center.add(new JScrollPane(resultPane), gbc);

        return center;
    }

    private JComponent createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

        hostBtn = new JRadioButton("Host (HTTP)", true);
        tcpBtn = new JRadioButton("TCP Port");



        ButtonGroup bg = new ButtonGroup();
        bg.add(hostBtn);
        bg.add(tcpBtn);

        left.add(hostBtn);
        left.add(tcpBtn);
        left.add(new JLabel("Timeout (ms):"));

        timeoutField = new JTextField("3000", 6);
        left.add(timeoutField);

        JButton checkBtn = new JButton("Check");
        JButton exportBtn = new JButton("Export CSV");
        JButton resetBtn = new JButton("Reset");

        left.add(checkBtn);
        left.add(exportBtn);
        left.add(resetBtn);

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(220, 18));
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");



        footer.add(left, BorderLayout.WEST);
        footer.add(progressBar, BorderLayout.EAST);


        checkBtn.addActionListener(e -> runChecks());
        exportBtn.addActionListener(e -> exportCSV());
        resetBtn.addActionListener(e -> resetAll());

        return footer;
    }

    private void runChecks() {
        results.clear();
        resultPane.setText("");

        Integer timeout = validateTimeout();
        if (timeout == null) return;

        List<String> targets = validateInput();
        if (targets.isEmpty()) return;

        progressBar.setIndeterminate(true);
        progressBar.setString("Checking...");

        SwingWorker<Void, Result> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() {
                for (String line : targets) {
                    try {
                        Result r;
                        if (hostBtn.isSelected()) {
                            r = NetworkChecker.hostReachable(line.split(":")[0], timeout);
                        } else {
                            String[] p = line.split(":");
                            if (p.length != 2)
                                r = new Result(line, false, "INVALID FORMAT");
                            else
                                r = NetworkChecker.tcp(p[0], Integer.parseInt(p[1]), timeout);
                        }
                        publish(r);
                    } catch (Exception ex) {
                        publish(new Result(line, false, ex.getMessage()));
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Result> chunks) {
                progressBar.setIndeterminate(false);
                for (Result r : chunks) {
                    results.add(r);
                    appendColored(r);
                }
            }

            @Override
            protected void done() {
                progressBar.setValue(100);
                progressBar.setString("Completed");
            }
        };

        worker.execute();
    }


    private void appendColored(Result r) {
        try {
            boolean dark = UIManager.getLookAndFeel().getName().toLowerCase().contains("dark");

            Color successColor = dark
                    ? new Color(80, 250, 123)
                    : new Color(27, 127, 58);

            Color failColor = dark
                    ? new Color(255, 85, 85)
                    : new Color(198, 40, 40);

            StyledDocument doc = resultPane.getStyledDocument();
            Style style = resultPane.addStyle("style", null);
            StyleConstants.setForeground(style,
                    r.success() ? successColor : failColor);

            doc.insertString(doc.getLength(),
                    r.target() + " → " + r.message() + "\n", style);

        } catch (Exception ignored) {
        }
    }

    private void exportCSV() {
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data to export");
            return;
        }
        try (FileWriter fw = new FileWriter("access_result.csv")) {
            fw.write("Target,Status,Message\n");
            for (Result r : results)
                fw.write(r.target() + "," +
                        (r.success() ? "SUCCESS" : "FAILED") + "," +
                        r.message() + "\n");
            JOptionPane.showMessageDialog(this, "CSV exported");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void resetAll() {
        inputArea.setText("");
        resultPane.setText("");
        results.clear();
        progressBar.setValue(0);
        progressBar.setString("Ready");
    }

    private Integer validateTimeout() {
        try {
            int t = Integer.parseInt(timeoutField.getText().trim());
            if (t <= 0 || t > 60000) throw new NumberFormatException();
            return t;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Timeout must be 1–60000 ms");
            return null;
        }
    }

    private List<String> validateInput() {
        List<String> valid = new ArrayList<>();
        for (String line : inputArea.getText().split("\\n")) {
            if (!line.trim().isEmpty())
                valid.add(line.trim());
        }
        return valid;
    }

    private void switchTheme(Theme theme) {
        try {
            PREFS.put(PREF_THEME, theme.name());
            switch (theme) {
                case LIGHT -> FlatLightLaf.setup();
                case DARK -> FlatDarkLaf.setup();
                case DRACULA -> FlatDraculaIJTheme.setup();
                case SYSTEM -> {
                    if (isDark)
                        FlatDarkLaf.setup();
                    else
                        FlatLightLaf.setup();
                }
            }
            FlatLaf.updateUI();
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new AccessCheckerApp().setVisible(true));
    }
}
