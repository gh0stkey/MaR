package mar.instances;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import mar.Config;
import mar.component.rule.Display;
import mar.component.rule.Rule;
import mar.utils.ConfigLoader;
import mar.utils.rule.RuleProcessor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class ContextMenuHandler implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final ConfigLoader configLoader;
    private final RuleProcessor ruleProcessor;

    public ContextMenuHandler(MontoyaApi api, ConfigLoader configLoader) {
        this.api = api;
        this.configLoader = configLoader;
        this.ruleProcessor = new RuleProcessor(api, configLoader);
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();

        Optional<MessageEditorHttpRequestResponse> messageEditor = event.messageEditorRequestResponse();

        if (messageEditor.isPresent()) {
            MessageEditorHttpRequestResponse editor = messageEditor.get();

            if (editor.selectionOffsets().isPresent()) {
                SelectionContext context = getSelectedTextAndContext(editor);

                if (context != null && !context.selectedText.isEmpty()) {
                    JMenuItem createRuleItem = new JMenuItem("Create MaR Rule");
                    createRuleItem.addActionListener(e -> showCreateRuleDialog(context));
                    menuItems.add(createRuleItem);
                }
            }
        }

        return menuItems;
    }

    private SelectionContext getSelectedTextAndContext(MessageEditorHttpRequestResponse editor) {
        try {
            if (editor.selectionOffsets().isEmpty()) {
                return null;
            }

            var offsets = editor.selectionOffsets().get();
            int start = offsets.startIndexInclusive();
            int end = offsets.endIndexExclusive();

            // 判断选中文字是请求还是响应
            boolean isRequest = editor.selectionContext() == MessageEditorHttpRequestResponse.SelectionContext.REQUEST;

            byte[] contentBytes;

            if (isRequest) {
                contentBytes = editor.requestResponse().request().toByteArray().getBytes();
            } else {
                contentBytes = editor.requestResponse().response().toByteArray().getBytes();
            }

            if (contentBytes != null && start >= 0 && end <= contentBytes.length) {
                byte[] selectedBytes = new byte[end - start];
                System.arraycopy(contentBytes, start, selectedBytes, 0, end - start);
                return new SelectionContext(new String(selectedBytes), isRequest);
            }
        } catch (Exception e) {
            api.logging().logToError("Failed to get selected text: " + e.getMessage());
        }

        return null;
    }

    private void showCreateRuleDialog(SelectionContext context) {
        SwingUtilities.invokeLater(() -> {
            try {
                Display ruleDisplay = new Display();

                String defaultScope = context.isRequest ? "request" : "response";
                ruleDisplay.conditionScopeComboBox.setSelectedItem(defaultScope);
                ruleDisplay.matchReplaceScopeComboBox.setSelectedItem(defaultScope);

                ruleDisplay.conditionTextField.setText(context.selectedText);
                ruleDisplay.matchTextField.setText(context.selectedText);

                String[] groups = Config.globalRules.keySet().toArray(new String[0]);
                JComboBox<String> groupComboBox = new JComboBox<>(groups);

                JPanel panel = new JPanel(new BorderLayout(5, 5));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                JPanel groupPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(5, 5, 0, 5);
                gbc.gridy = 0;
                gbc.gridx = 0;
                gbc.anchor = GridBagConstraints.WEST;
                groupPanel.add(new JLabel("Rule Group:"), gbc);
                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;
                groupPanel.add(groupComboBox, gbc);
                panel.add(groupPanel, BorderLayout.NORTH);
                panel.add(ruleDisplay, BorderLayout.CENTER);

                // 创建非模态对话框
                JDialog dialog = new JDialog((Frame) null, "MaR - New Rule", false);
                dialog.setLayout(new BorderLayout());
                dialog.add(panel, BorderLayout.CENTER);

                // 创建按钮面板
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton okButton = new JButton("OK");
                JButton cancelButton = new JButton("Cancel");
                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);
                dialog.add(buttonPanel, BorderLayout.SOUTH);

                // 设置按钮事件
                okButton.addActionListener(e -> {
                    String selectedGroup = (String) groupComboBox.getSelectedItem();
                    saveRule(ruleDisplay, selectedGroup);
                    dialog.dispose();
                });

                cancelButton.addActionListener(e -> dialog.dispose());

                // 设置窗口属性：自动适应内容大小、置顶
                dialog.pack();
                Dimension preferredSize = dialog.getPreferredSize();
                dialog.setMinimumSize(new Dimension(preferredSize.width + 50, preferredSize.height + 50));
                dialog.setSize(dialog.getMinimumSize());
                dialog.setAlwaysOnTop(true);
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                dialog.setResizable(false);
            } catch (Exception e) {
                api.logging().logToError("Failed to create rule dialog: " + e.getMessage());
            }
        });
    }

    private void saveRule(Display ruleDisplay, String ruleGroup) {
        try {
            Vector<Object> ruleData = Rule.createRuleDataFromDisplay(ruleDisplay, true);

            ruleProcessor.addRule(ruleData, ruleGroup);

            // 刷新Rules界面
            configLoader.reloadRules();
        } catch (Exception e) {
            api.logging().logToError("Failed to save rule: " + e.getMessage());
        }
    }

    private record SelectionContext(String selectedText, boolean isRequest) {
    }
}
