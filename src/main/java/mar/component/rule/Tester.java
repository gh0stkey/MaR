package mar.component.rule;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
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
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class Tester extends JPanel {

    private static final int REQUEST_TAB = 0;
    private static final int RESPONSE_TAB = 1;
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
    private final JTabbedPane modifiedTabbedPane;
    private final JTextPane diffTextPane;
    private final Style deletedStyle;
    private final Style addedStyle;
    private final Style contextStyle;
    private Vector<Object> selectedRuleData;

    public Tester(MontoyaApi api) {
        this.api = api;
        this.modifier = new HttpMessageModifier(api);

        this.originalRequestEditor = api
                .userInterface()
                .createHttpRequestEditor();
        this.modifiedRequestEditor = api
                .userInterface()
                .createHttpRequestEditor(EditorOptions.READ_ONLY);

        this.originalResponseEditor = api
                .userInterface()
                .createHttpResponseEditor();
        this.modifiedResponseEditor = api
                .userInterface()
                .createHttpResponseEditor(EditorOptions.READ_ONLY);

        this.modifiedTabbedPane = new JTabbedPane();

        this.diffTextPane = new JTextPane();
        this.diffTextPane.setEditable(false);
        this.diffTextPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        StyledDocument doc = diffTextPane.getStyledDocument();
        deletedStyle = doc.addStyle("deleted", null);
        StyleConstants.setForeground(deletedStyle, new Color(220, 50, 47));
        StyleConstants.setBackground(deletedStyle, new Color(255, 238, 238));

        addedStyle = doc.addStyle("added", null);
        StyleConstants.setForeground(addedStyle, new Color(40, 160, 40));
        StyleConstants.setBackground(addedStyle, new Color(238, 255, 238));

        contextStyle = doc.addStyle("context", null);
        StyleConstants.setForeground(contextStyle, Color.GRAY);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel titleLabel = new JLabel("Tester");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        leftPanel.add(titleLabel);
        headerPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetContent());
        rightPanel.add(resetButton);
        JButton testButton = new JButton("Test");
        testButton.addActionListener(e -> applyRule());
        rightPanel.add(testButton);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        // Original: TitledBorder + TabbedPane(Request / Response)
        JTabbedPane originalTabbedPane = new JTabbedPane();
        originalTabbedPane.addTab(
                "Request",
                originalRequestEditor.uiComponent()
        );
        originalTabbedPane.addTab(
                "Response",
                originalResponseEditor.uiComponent()
        );
        JPanel originalPanel = wrapWithBorder(originalTabbedPane, "Original");

        // Modified: TitledBorder + TabbedPane(Request / Response), 自动跳转
        modifiedTabbedPane.addTab(
                "Request",
                modifiedRequestEditor.uiComponent()
        );
        modifiedTabbedPane.addTab(
                "Response",
                modifiedResponseEditor.uiComponent()
        );
        JPanel modifiedPanel = wrapWithBorder(modifiedTabbedPane, "Modified");

        // Diff
        JPanel diffPanel = wrapWithBorder(
                new JScrollPane(diffTextPane),
                "Diff"
        );

        // 布局: Original(左) | Modified(右), Diff(下)
        JSplitPane editorSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT
        );
        editorSplitPane.setLeftComponent(originalPanel);
        editorSplitPane.setRightComponent(modifiedPanel);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(editorSplitPane);
        mainSplitPane.setBottomComponent(diffPanel);

        editorSplitPane.addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        editorSplitPane.setDividerLocation(0.5);
                    }
                }
        );

        mainSplitPane.addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        mainSplitPane.setDividerLocation(0.8);
                    }
                }
        );

        add(headerPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);

        // 初始化默认内容
        originalRequestEditor.setRequest(
                HttpRequest.httpRequest(DEFAULT_REQUEST)
        );
        originalResponseEditor.setResponse(
                HttpResponse.httpResponse(DEFAULT_RESPONSE)
        );
    }

    private JPanel wrapWithBorder(Component component, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP
                )
        );
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void resetContent() {
        originalRequestEditor.setRequest(
                HttpRequest.httpRequest(DEFAULT_REQUEST)
        );
        originalResponseEditor.setResponse(
                HttpResponse.httpResponse(DEFAULT_RESPONSE)
        );
    }

    public void setSelectedRule(Vector<Object> ruleData) {
        this.selectedRuleData = ruleData;
    }

    public void setSelectedRuleFromTable(JTable ruleTable) {
        int selectedRow = ruleTable.getSelectedRow();
        if (selectedRow >= 0) {
            DefaultTableModel model = (DefaultTableModel) ruleTable.getModel();
            int modelIndex = ruleTable.convertRowIndexToModel(selectedRow);
            this.selectedRuleData = (Vector<Object>) model
                    .getDataVector()
                    .get(modelIndex);
            applyRuleQuietly();
        }
    }

    private void applyRuleQuietly() {
        if (selectedRuleData == null) {
            return;
        }

        try {
            executeRule();
        } catch (Exception e) {
            api
                    .logging()
                    .logToError(
                            "Tester apply rule quietly error: " + e.getMessage()
                    );
        }
    }

    private void applyRule() {
        if (selectedRuleData == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a rule first.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        try {
            executeRule();
        } catch (Exception e) {
            api
                    .logging()
                    .logToError("Tester apply rule error: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "Error applying rule: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void executeRule() {
        String conditionScope = selectedRuleData.get(2).toString();
        String relationship = selectedRuleData.get(3).toString();
        String condition = selectedRuleData.get(4).toString();
        boolean conditionRegex = (Boolean) selectedRuleData.get(5);
        String eScope = selectedRuleData.get(6).toString();
        String fRegex = selectedRuleData.get(7).toString();
        String sRegex = selectedRuleData.get(8).toString();
        String matchScope = selectedRuleData.get(9).toString();
        String match = selectedRuleData.get(10).toString();
        String replace = selectedRuleData.get(11).toString();
        boolean matchRegex = (Boolean) selectedRuleData.get(12);

        HttpRequest request = originalRequestEditor.getRequest();
        HttpResponse response = originalResponseEditor.getResponse();

        // 1. Condition
        String conditionContent = getScopedContent(
                conditionScope,
                request,
                response
        );
        if (
                !modifier.checkCondition(
                        conditionContent,
                        condition,
                        relationship,
                        conditionRegex
                )
        ) {
            showNoDiff("Condition not matched");
            return;
        }

        // 2. Extract
        String resolvedMatch = match;
        String resolvedReplace = replace;
        if (!fRegex.isEmpty()) {
            String extractScope = eScope.isEmpty() ? matchScope : eScope;
            String extractContent = getScopedContent(
                    extractScope,
                    request,
                    response
            );
            resolvedMatch = modifier.resolveRegexIdentifiers(
                    extractContent,
                    fRegex,
                    sRegex,
                    match
            );
            resolvedReplace = modifier.resolveRegexIdentifiers(
                    extractContent,
                    fRegex,
                    sRegex,
                    replace
            );
            // 如果没有提取到东西，不进入 match and replace 流程
            if (resolvedMatch == null || resolvedReplace == null) {
                showNoDiff("Extract not matched");
                return;
            }
        }

        // 3. Match and Replace
        if (matchScope.startsWith("request")) {
            if (request == null) return;
            HttpRequest modifiedRequest = modifier.modifyRequest(
                    request,
                    matchScope,
                    resolvedMatch,
                    resolvedReplace,
                    matchRegex
            );
            modifiedTabbedPane.setSelectedIndex(REQUEST_TAB);
            modifiedRequestEditor.setRequest(modifiedRequest);
            showDiff(request.toString(), modifiedRequest.toString());
        } else {
            if (response == null) return;
            HttpResponse modifiedResponse = modifier.modifyResponse(
                    response,
                    matchScope,
                    resolvedMatch,
                    resolvedReplace,
                    matchRegex
            );
            modifiedTabbedPane.setSelectedIndex(RESPONSE_TAB);
            modifiedResponseEditor.setResponse(modifiedResponse);
            showDiff(response.toString(), modifiedResponse.toString());
        }
    }

    private String getScopedContent(
            String scope,
            HttpRequest request,
            HttpResponse response
    ) {
        if (scope.startsWith("request") && request != null) {
            return getTargetContent(scope, request);
        } else if (response != null) {
            return getTargetContent(scope, response);
        }
        return "";
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
                    doc.insertString(
                            doc.getLength(),
                            "- " + line + "\n",
                            deletedStyle
                    );
                }
                for (String line : delta.getTarget().getLines()) {
                    doc.insertString(
                            doc.getLength(),
                            "+ " + line + "\n",
                            addedStyle
                    );
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
            case "request header" -> request
                    .headers()
                    .stream()
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
            case "response header" -> response
                    .headers()
                    .stream()
                    .map(h -> h.name() + ": " + h.value())
                    .reduce((a, b) -> a + "\r\n" + b)
                    .orElse("");
            case "response body" -> response.bodyToString();
            default -> response.toString();
        };
    }
}
