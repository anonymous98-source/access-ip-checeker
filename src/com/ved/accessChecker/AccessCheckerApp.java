package com.ved.accessChecker;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class AccessCheckerApp extends JFrame {

    private JTextArea inputArea;
    private JTextPane resultPane;
    private JProgressBar progressBar;
    private JTextField timeoutField;
    private JRadioButton hostBtn, tcpBtn;

    private final List<Result> results = new ArrayList<>();

    public AccessCheckerApp() {
        FlatDarkLaf.setup();
        setTitle("Access Checker");
        setSize(960, 540);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        Image appIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
        setIconImage(appIcon);
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar.getTaskbar().setIconImage(appIcon);
            }
        } catch (Exception ignored) {
        }
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(null);
        setContentPane(panel);

        JLabel header = new JLabel(
                "<html><div style='text-align:right;'> © rugved.dev <br/>Version 1.0</div></html>");
        header.setBounds(835, 10, 180, 40);
        panel.add(header);

        JLabel inputLabel = new JLabel("Enter HOST or HOST:PORT");
        inputLabel.setBounds(30, 40, 400, 20);
        panel.add(inputLabel);

        inputArea = new JTextArea();
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBounds(30, 65, 380, 260);
        panel.add(inputScroll);

        JLabel resultLabel = new JLabel("Result");
        resultLabel.setBounds(460, 40, 200, 20);
        panel.add(resultLabel);

        resultPane = new JTextPane();
        resultPane.setEditable(false);
        JScrollPane resultScroll = new JScrollPane(resultPane);
        resultScroll.setBounds(460, 65, 450, 260);
        panel.add(resultScroll);

        hostBtn = new JRadioButton("Host Reachability (HTTP)", true);
        tcpBtn = new JRadioButton("TCP Port Check");

        ButtonGroup bg = new ButtonGroup();
        bg.add(hostBtn);
        bg.add(tcpBtn);

        hostBtn.setBounds(30, 340, 220, 25);
        tcpBtn.setBounds(260, 340, 160, 25);

        panel.add(hostBtn);
        panel.add(tcpBtn);

        JLabel timeoutLabel = new JLabel("Timeout (ms):");
        timeoutLabel.setBounds(460, 340, 100, 25);
        panel.add(timeoutLabel);

        timeoutField = new JTextField("3000");
        timeoutField.setBounds(560, 340, 80, 25);
        panel.add(timeoutField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBounds(0, 380, 960, 40); // full width, next line
        buttonPanel.setOpaque(false);


        JButton checkBtn = new JButton("Check");
        JButton exportBtn = new JButton("Export CSV");
        JButton copyBtn = new JButton("Copy Result");
        JButton resetBtn = new JButton("Reset");

        Dimension btnSize = new Dimension(120, 32);
        checkBtn.setPreferredSize(btnSize);
        exportBtn.setPreferredSize(btnSize);
        copyBtn.setPreferredSize(btnSize);
        resetBtn.setPreferredSize(btnSize);

        Font btnFont = new Font("Segoe UI", Font.PLAIN, 13);
        checkBtn.setFont(btnFont);
        exportBtn.setFont(btnFont);
        copyBtn.setFont(btnFont);
        resetBtn.setFont(btnFont);

        buttonPanel.add(checkBtn);
        buttonPanel.add(exportBtn);
        buttonPanel.add(copyBtn);
        buttonPanel.add(resetBtn);

        panel.add(buttonPanel);

        progressBar = new JProgressBar(0, 100);
        progressBar.setBounds(30, 430, 880, 22);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setString("Ready");
        panel.add(progressBar);

        checkBtn.addActionListener(e -> runChecks());
        exportBtn.addActionListener(e -> exportCSV());
        resetBtn.addActionListener(e -> resetAll());
    }

    private void runChecks() {
        results.clear();
        resultPane.setText("");

        String[] lines = inputArea.getText().split("\\n");
        int timeout = Integer.parseInt(timeoutField.getText());

        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        progressBar.setString("Checking access...");

        SwingWorker<Void, Result> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws MalformedURLException {
                int processed = 0;

                for (String line : lines) {
                    if (line.isBlank()) continue;

                    Result r;

                    if (hostBtn.isSelected()) {
                        String host = line.trim();
                        if (host.contains(":")) {
                            host = host.substring(0, host.indexOf(":"));
                        }
                        r = NetworkChecker.hostReachable(host, timeout);
                    } else {
                        String[] p = line.trim().split(":");
                        if (p.length != 2) {
                            r = new Result(line.trim(), false,
                                    "INVALID FORMAT (use host:port)");
                        } else {
                            r = NetworkChecker.tcp(
                                    p[0],
                                    Integer.parseInt(p[1]),
                                    timeout
                            );
                        }
                    }

                    publish(r);
                    processed++;
                    setProgress((int) ((processed * 100.0) / lines.length));
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
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                progressBar.setString("Completed");
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressBar.setValue((Integer) evt.getNewValue());
                progressBar.setString(evt.getNewValue() + "%");
            }
        });

        worker.execute();
    }

    private void appendColored(Result r) {
        try {
            StyledDocument doc = resultPane.getStyledDocument();
            Style style = resultPane.addStyle("style", null);
            StyleConstants.setForeground(style,
                    r.success ? new Color(0, 220, 140) : Color.RED);

            doc.insertString(
                    doc.getLength(),
                    r.target + " ? " + r.message + "\n",
                    style
            );
        } catch (Exception ignored) {
        }
    }

    private void exportCSV() {
        try (FileWriter fw = new FileWriter("access_result.csv")) {
            fw.write("Target,Status,Message\n");
            for (Result r : results) {
                fw.write(r.target + "," +
                        (r.success ? "SUCCESS" : "FAILED") + "," +
                        r.message + "\n");
            }
            JOptionPane.showMessageDialog(this, "Exported access_result.csv");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void resetAll() {
        inputArea.setText("");
        resultPane.setText("");
        results.clear();
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setString("Ready");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new AccessCheckerApp().setVisible(true));
    }
}
