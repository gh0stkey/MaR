package mar.component.rule;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.api.montoya.ui.editor.RawEditor;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import mar.instances.HttpMessageModifier;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class Tester extends JPanel {
    private static final String REQUEST_CARD = "REQUEST";
    private static final String RESPONSE_CARD = "RESPONSE";
    private static final String RAW_CARD = "RAW";

    private enum EditorMode {REQUEST, RESPONSE, RAW}

    private static final String DEFAULT_REQUEST =
            "POST /update HTTP/2\r\n" +
                    "Host: example.com\r\n" +
                    "Cookie: example=123; test=456\r\n" +
                    "Accept-Language: zh-CN,zh;q=0.9\r\n" +
                    "User-Agent: Mozilla/5.0\r\n" +
                    "Accept: text/html\r\n" +
                    "Accept-Encoding: gzip, deflate, br\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: 49\r\n" +
                    "Connection: keep-alive\r\n" +
                    "\r\n" +
                    "{\r\n" +
                    "  \"key1\": \"value1\",\r\n" +
                    "  \"key2\": \"value2\"\r\n" +
                    "}";

    private static final String DEFAULT_RESPONSE =
            "HTTP/2 200 OK\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Server: server\r\n" +
                    "Content-Length: 95\r\n" +
                    "\r\n" +
                    "<!DOCTYPE html>\r\n" +
                    "<html>\r\n" +
                    "  <head>\r\n" +
                    "    <title>Example</title>\r\n" +
                    "  </head>\r\n" +
                    "  <body>\r\n" +
                    "  </body>\r\n" +
                    "</html>";

    private final MontoyaApi api;
    private final HttpMessageModifier modifier;

    private final HttpRequestEditor originalRequestEditor;
    private final HttpRequestEditor modifiedRequestEditor;

    private final HttpResponseEditor originalResponseEditor;
    private final HttpResponseEditor modifiedResponseEditor;

    private final RawEditor originalRawEditor;
    private final RawEditor modifiedRawEditor;

    private final CardLayout originalCardLayout;
    private final CardLayout modifiedCardLayout;
    private final JPanel originalCardPanel;
    private final JPanel modifiedCardPanel;

    // Diff 面板
    private final JTextPane diffTextPane;
    private final Style deletedStyle;
    private final Style addedStyle;
    private final Style contextStyle;

    // 模式切换
    private final ButtonGroup modeButtonGroup;
    private final JRadioButton requestModeBtn;
    private final JRadioButton responseModeBtn;
    private final JRadioButton rawModeBtn;

    private EditorMode currentMode = null;

    private Vector<Object> selectedRuleData;

    public Tester(MontoyaApi api) {
        this.api = api;
        this.modifier = new HttpMessageModifier(api);

        this.originalRequestEditor = api.userInterface().createHttpRequestEditor();
        this.modifiedRequestEditor = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);

        this.originalResponseEditor = api.userInterface().createHttpResponseEditor();
        this.modifiedResponseEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);

        this.originalRawEditor = api.userInterface().createRawEditor();
        this.modifiedRawEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);

        this.originalCardLayout = new CardLayout();
        this.modifiedCardLayout = new CardLayout();
        this.originalCardPanel = new JPanel(originalCardLayout);
        this.modifiedCardPanel = new JPanel(modifiedCardLayout);

        // 初始化 Diff 面板
        this.diffTextPane = new JTextPane();
        this.diffTextPane.setEditable(false);
        this.diffTextPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // 初始化样式
        StyledDocument doc = diffTextPane.getStyledDocument();
        deletedStyle = doc.addStyle("deleted", null);
        StyleConstants.setForeground(deletedStyle, new Color(220, 50, 47));  // 红色
        StyleConstants.setBackground(deletedStyle, new Color(255, 238, 238));

        addedStyle = doc.addStyle("added", null);
        StyleConstants.setForeground(addedStyle, new Color(40, 160, 40));    // 绿色
        StyleConstants.setBackground(addedStyle, new Color(238, 255, 238));

        contextStyle = doc.addStyle("context", null);
        StyleConstants.setForeground(contextStyle, Color.GRAY);

        // 初始化模式切换按钮
        modeButtonGroup = new ButtonGroup();
        requestModeBtn = new JRadioButton("Request");
        responseModeBtn = new JRadioButton("Response");
        rawModeBtn = new JRadioButton("Raw");
        modeButtonGroup.add(requestModeBtn);
        modeButtonGroup.add(responseModeBtn);
        modeButtonGroup.add(rawModeBtn);

        requestModeBtn.addActionListener(e -> switchEditorMode(EditorMode.REQUEST));
        responseModeBtn.addActionListener(e -> switchEditorMode(EditorMode.RESPONSE));
        rawModeBtn.addActionListener(e -> switchEditorMode(EditorMode.RAW));

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel titleLabel = new JLabel("Tester");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        leftPanel.add(titleLabel);
        leftPanel.add(Box.createHorizontalStrut(10));
        leftPanel.add(requestModeBtn);
        leftPanel.add(responseModeBtn);
        leftPanel.add(rawModeBtn);
        headerPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetContent());
        rightPanel.add(resetButton);

        JButton testButton = new JButton("Test");
        testButton.addActionListener(e -> applyRule());
        rightPanel.add(testButton);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        originalCardPanel.add(originalRequestEditor.uiComponent(), REQUEST_CARD);
        originalCardPanel.add(originalResponseEditor.uiComponent(), RESPONSE_CARD);
        originalCardPanel.add(originalRawEditor.uiComponent(), RAW_CARD);

        modifiedCardPanel.add(modifiedRequestEditor.uiComponent(), REQUEST_CARD);
        modifiedCardPanel.add(modifiedResponseEditor.uiComponent(), RESPONSE_CARD);
        modifiedCardPanel.add(modifiedRawEditor.uiComponent(), RAW_CARD);

        JSplitPane editorSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JPanel originalPanel = new JPanel(new BorderLayout());
        originalPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Original",
                TitledBorder.LEFT, TitledBorder.TOP));
        originalPanel.add(originalCardPanel, BorderLayout.CENTER);

        JPanel modifiedPanel = new JPanel(new BorderLayout());
        modifiedPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Modified",
                TitledBorder.LEFT, TitledBorder.TOP));
        modifiedPanel.add(modifiedCardPanel, BorderLayout.CENTER);

        editorSplitPane.setLeftComponent(originalPanel);
        editorSplitPane.setRightComponent(modifiedPanel);

        editorSplitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                editorSplitPane.setDividerLocation(0.5);
            }
        });

        JPanel diffPanel = new JPanel(new BorderLayout());
        diffPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Diff",
                TitledBorder.LEFT, TitledBorder.TOP));
        JScrollPane diffScrollPane = new JScrollPane(diffTextPane);
        diffPanel.add(diffScrollPane, BorderLayout.CENTER);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(editorSplitPane);
        mainSplitPane.setBottomComponent(diffPanel);

        mainSplitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                mainSplitPane.setDividerLocation(0.7);
            }
        });

        add(headerPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);

        switchEditorMode(EditorMode.REQUEST);
    }

    private void switchEditorMode(EditorMode mode) {
        if (currentMode == mode) {
            return;
        }

        this.currentMode = mode;
        String card = switch (mode) {
            case REQUEST -> REQUEST_CARD;
            case RESPONSE -> RESPONSE_CARD;
            case RAW -> RAW_CARD;
        };
        originalCardLayout.show(originalCardPanel, card);
        modifiedCardLayout.show(modifiedCardPanel, card);

        switch (mode) {
            case REQUEST -> requestModeBtn.setSelected(true);
            case RESPONSE -> responseModeBtn.setSelected(true);
            case RAW -> rawModeBtn.setSelected(true);
        }

        switch (mode) {
            case REQUEST -> originalRequestEditor.setRequest(HttpRequest.httpRequest(DEFAULT_REQUEST));
            case RESPONSE -> originalResponseEditor.setResponse(HttpResponse.httpResponse(DEFAULT_RESPONSE));
        }
    }

    private void resetContent() {
        switch (currentMode) {
            case REQUEST -> originalRequestEditor.setRequest(HttpRequest.httpRequest(DEFAULT_REQUEST));
            case RESPONSE -> originalResponseEditor.setResponse(HttpResponse.httpResponse(DEFAULT_RESPONSE));
            case RAW ->
                    originalRawEditor.setContents(ByteArray.byteArray(DEFAULT_REQUEST.getBytes(StandardCharsets.UTF_8)));
        }
    }

    public void setSelectedRule(Vector<Object> ruleData) {
        this.selectedRuleData = ruleData;
        updateEditorMode();
    }

    public void setSelectedRuleFromTable(JTable ruleTable) {
        int selectedRow = ruleTable.getSelectedRow();
        if (selectedRow >= 0) {
            DefaultTableModel model = (DefaultTableModel) ruleTable.getModel();
            int modelIndex = ruleTable.convertRowIndexToModel(selectedRow);
            this.selectedRuleData = (Vector<Object>) model.getDataVector().get(modelIndex);

            updateEditorMode();
            applyRuleQuietly();
        }
    }

    private void updateEditorMode() {
        if (selectedRuleData != null && currentMode != EditorMode.RAW) {
            String matchScope = selectedRuleData.get(6).toString();
            boolean isResponse = matchScope.contains("response");
            switchEditorMode(isResponse ? EditorMode.RESPONSE : EditorMode.REQUEST);
        }
    }


    private void applyRuleQuietly() {
        if (selectedRuleData == null) {
            return;
        }

        try {
            String conditionScope = selectedRuleData.get(2).toString();
            String relationship = selectedRuleData.get(3).toString();
            String condition = selectedRuleData.get(4).toString();
            boolean conditionRegex = (Boolean) selectedRuleData.get(5);
            String matchScope = selectedRuleData.get(6).toString();
            String match = selectedRuleData.get(7).toString();
            String replace = selectedRuleData.get(8).toString();
            boolean matchRegex = (Boolean) selectedRuleData.get(9);

            switch (currentMode) {
                case RESPONSE -> {
                    HttpResponse originalResponse = originalResponseEditor.getResponse();
                    if (originalResponse == null) return;

                    String targetContent = getTargetContent(conditionScope, originalResponse);
                    if (modifier.checkCondition(targetContent, condition, relationship, conditionRegex)) {
                        HttpResponse modifiedResponse = modifier.modifyResponse(originalResponse, matchScope, match, replace, matchRegex);
                        modifiedResponseEditor.setResponse(modifiedResponse);
                        showDiff(originalResponse.toString(), modifiedResponse.toString());
                    } else {
                        modifiedResponseEditor.setResponse(originalResponse);
                        showNoDiff("Condition not matched");
                    }
                }
                case REQUEST -> {
                    HttpRequest originalRequest = originalRequestEditor.getRequest();
                    if (originalRequest == null) return;

                    String targetContent = getTargetContent(conditionScope, originalRequest);
                    if (modifier.checkCondition(targetContent, condition, relationship, conditionRegex)) {
                        HttpRequest modifiedRequest = modifier.modifyRequest(originalRequest, matchScope, match, replace, matchRegex);
                        modifiedRequestEditor.setRequest(modifiedRequest);
                        showDiff(originalRequest.toString(), modifiedRequest.toString());
                    } else {
                        modifiedRequestEditor.setRequest(originalRequest);
                        showNoDiff("Condition not matched");
                    }
                }
                case RAW -> {
                    byte[] originalBytes = originalRawEditor.getContents().getBytes();
                    if (originalBytes == null || originalBytes.length == 0) return;

                    String originalText = new String(originalBytes, StandardCharsets.UTF_8);
                    // Raw 模式：直接替换，忽略 condition 和 scope
                    String modifiedText = modifier.matchAndReplace(originalText, match, replace, matchRegex);
                    modifiedRawEditor.setContents(ByteArray.byteArray(modifiedText.getBytes(StandardCharsets.UTF_8)));
                    showDiff(originalText, modifiedText);
                }
            }
        } catch (Exception e) {
            api.logging().logToError("Tester apply rule quietly error: " + e.getMessage());
        }
    }

    private void applyRule() {
        if (selectedRuleData == null) {
            JOptionPane.showMessageDialog(this, "Please select a rule first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            String conditionScope = selectedRuleData.get(2).toString();
            String relationship = selectedRuleData.get(3).toString();
            String condition = selectedRuleData.get(4).toString();
            boolean conditionRegex = (Boolean) selectedRuleData.get(5);
            String matchScope = selectedRuleData.get(6).toString();
            String match = selectedRuleData.get(7).toString();
            String replace = selectedRuleData.get(8).toString();
            boolean matchRegex = (Boolean) selectedRuleData.get(9);

            switch (currentMode) {
                case RESPONSE -> {
                    HttpResponse originalResponse = originalResponseEditor.getResponse();
                    if (originalResponse == null) {
                        JOptionPane.showMessageDialog(this, "Please input response content in Original editor.", "Info", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    String targetContent = getTargetContent(conditionScope, originalResponse);
                    if (modifier.checkCondition(targetContent, condition, relationship, conditionRegex)) {
                        HttpResponse modifiedResponse = modifier.modifyResponse(originalResponse, matchScope, match, replace, matchRegex);
                        modifiedResponseEditor.setResponse(modifiedResponse);
                        showDiff(originalResponse.toString(), modifiedResponse.toString());
                    } else {
                        modifiedResponseEditor.setResponse(originalResponse);
                        showNoDiff("Condition not matched");
                    }
                }
                case REQUEST -> {
                    HttpRequest originalRequest = originalRequestEditor.getRequest();
                    if (originalRequest == null) {
                        JOptionPane.showMessageDialog(this, "Please input request content in Original editor.", "Info", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    String targetContent = getTargetContent(conditionScope, originalRequest);
                    if (modifier.checkCondition(targetContent, condition, relationship, conditionRegex)) {
                        HttpRequest modifiedRequest = modifier.modifyRequest(originalRequest, matchScope, match, replace, matchRegex);
                        modifiedRequestEditor.setRequest(modifiedRequest);
                        showDiff(originalRequest.toString(), modifiedRequest.toString());
                    } else {
                        modifiedRequestEditor.setRequest(originalRequest);
                        showNoDiff("Condition not matched");
                    }
                }
                case RAW -> {
                    byte[] originalBytes = originalRawEditor.getContents().getBytes();
                    if (originalBytes == null || originalBytes.length == 0) {
                        JOptionPane.showMessageDialog(this, "Please input content in Original editor.", "Info", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    String originalText = new String(originalBytes, StandardCharsets.UTF_8);
                    String modifiedText = modifier.matchAndReplace(originalText, match, replace, matchRegex);
                    modifiedRawEditor.setContents(ByteArray.byteArray(modifiedText.getBytes(StandardCharsets.UTF_8)));
                    showDiff(originalText, modifiedText);
                }
            }

        } catch (Exception e) {
            api.logging().logToError("Tester apply rule error: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error applying rule: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDiff(String original, String modified) {
        try {
            StyledDocument doc = diffTextPane.getStyledDocument();
            doc.remove(0, doc.getLength());

            List<String> originalLines = Arrays.asList(original.split("\n"));
            List<String> modifiedLines = Arrays.asList(modified.split("\n"));

            Patch<String> patch = DiffUtils.diff(originalLines, modifiedLines);

            if (patch.getDeltas().isEmpty()) {
                doc.insertString(doc.getLength(), "No changes\n", contextStyle);
                return;
            }

            for (AbstractDelta<String> delta : patch.getDeltas()) {
                for (String line : delta.getSource().getLines()) {
                    doc.insertString(doc.getLength(), "- " + line + "\n", deletedStyle);
                }
                for (String line : delta.getTarget().getLines()) {
                    doc.insertString(doc.getLength(), "+ " + line + "\n", addedStyle);
                }
            }
        } catch (BadLocationException e) {
            api.logging().logToError("Diff display error: " + e.getMessage());
        }
    }

    private void showNoDiff(String message) {
        try {
            StyledDocument doc = diffTextPane.getStyledDocument();
            doc.remove(0, doc.getLength());
            doc.insertString(0, message + "\n", contextStyle);
        } catch (BadLocationException ignored) {
        }
    }

    private String getTargetContent(String scope, HttpRequest request) {
        return switch (scope) {
            case "request" -> request.toString();
            case "request method" -> request.method();
            case "request uri" -> request.path();
            case "request header" -> request.headers().stream()
                    .map(h -> h.name() + ": " + h.value())
                    .reduce((a, b) -> a + "\r\n" + b)
                    .orElse("");
            case "request body" -> request.bodyToString();
            default -> request.toString();
        };
    }

    private String getTargetContent(String scope, HttpResponse response) {
        return switch (scope) {
            case "response" -> response.toString();
            case "response status" -> String.valueOf(response.statusCode());
            case "response header" -> response.headers().stream()
                    .map(h -> h.name() + ": " + h.value())
                    .reduce((a, b) -> a + "\r\n" + b)
                    .orElse("");
            case "response body" -> response.bodyToString();
            default -> response.toString();
        };
    }
}
