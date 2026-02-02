package mar.component.rule;

import burp.api.montoya.MontoyaApi;
import mar.Config;
import mar.utils.ConfigLoader;
import mar.utils.rule.RuleProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.HierarchyEvent;

public class Rules extends JPanel {
    private final MontoyaApi api;
    private final RuleProcessor ruleProcessor;
    private final JTextField ruleGroupNameTextField;
    private final JTabbedPane ruleTabbedPane;
    private final Tester tester;
    private ConfigLoader configLoader;
    private Component tabComponent;
    private int selectedIndex;
    private final Action cancelActionPerformed = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectedIndex >= 0) {
                ruleTabbedPane.setTabComponentAt(selectedIndex, tabComponent);

                ruleGroupNameTextField.setVisible(false);
                ruleGroupNameTextField.setPreferredSize(null);
                selectedIndex = -1;
                tabComponent = null;

                ruleTabbedPane.requestFocusInWindow();
            }
        }
    };
    private final Action renameTitleActionPerformed = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String title = ruleGroupNameTextField.getText();
            if (!title.isEmpty() && selectedIndex >= 0) {
                String oldName = ruleTabbedPane.getTitleAt(selectedIndex);
                ruleTabbedPane.setTitleAt(selectedIndex, title);

                if (!oldName.equals(title)) {
                    ruleProcessor.renameRuleGroup(oldName, title);
                }
            }
            cancelActionPerformed.actionPerformed(null);
        }
    };

    public Rules(MontoyaApi api, ConfigLoader configLoader) {
        this.api = api;
        this.configLoader = configLoader;
        this.ruleProcessor = new RuleProcessor(api, configLoader);
        this.ruleGroupNameTextField = new JTextField();
        this.ruleTabbedPane = new JTabbedPane();
        this.tester = new Tester(api);

        initComponents();

        configLoader.setOnReloadRules(this::reloadRuleGroup);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        reloadRuleGroup();
        splitPane.setTopComponent(ruleTabbedPane);

        splitPane.setBottomComponent(tester);

        add(splitPane, BorderLayout.CENTER);

        JMenuItem deleteMenuItem = new JMenuItem("Delete");
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(deleteMenuItem);

        deleteMenuItem.addActionListener(this::deleteRuleGroupActionPerformed);

        ruleGroupNameTextField.setBorder(BorderFactory.createEmptyBorder());
        ruleGroupNameTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                renameTitleActionPerformed.actionPerformed(null);
            }
        });

        ruleTabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = ruleTabbedPane.indexAtLocation(e.getX(), e.getY());
                if (index < 0) {
                    return;
                }

                switch (e.getButton()) {
                    case MouseEvent.BUTTON1:
                        if (e.getClickCount() == 2) {
                            selectedIndex = index;
                            tabComponent = ruleTabbedPane.getTabComponentAt(selectedIndex);
                            String ruleGroupName = ruleTabbedPane.getTitleAt(selectedIndex);

                            if (!"...".equals(ruleGroupName)) {
                                ruleTabbedPane.setTabComponentAt(selectedIndex, ruleGroupNameTextField);
                                ruleGroupNameTextField.setVisible(true);
                                ruleGroupNameTextField.setText(ruleGroupName);
                                ruleGroupNameTextField.selectAll();
                                ruleGroupNameTextField.requestFocusInWindow();
                                ruleGroupNameTextField.setMinimumSize(ruleGroupNameTextField.getPreferredSize());
                            }
                        } else if (e.getClickCount() == 1) {
                            String title = ruleTabbedPane.getTitleAt(index);
                            if ("...".equals(title)) {
                                // 阻止默认的选中行为
                                e.consume();
                                // 直接创建新标签
                                String newTitle = ruleProcessor.newRule();
                                Rule newRule = new Rule(api, configLoader, Config.ruleTemplate, ruleTabbedPane, tester);
                                ruleTabbedPane.insertTab(newTitle, null, newRule, null, ruleTabbedPane.getTabCount() - 1);
                                ruleTabbedPane.setSelectedIndex(ruleTabbedPane.getTabCount() - 2);
                            } else {
                                renameTitleActionPerformed.actionPerformed(null);
                            }
                        }
                        break;
                    case MouseEvent.BUTTON3:
                        if (!"...".equals(ruleTabbedPane.getTitleAt(index))) {
                            popupMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        InputMap im = ruleGroupNameTextField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = ruleGroupNameTextField.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        am.put("cancel", cancelActionPerformed);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "rename");
        am.put("rename", renameTitleActionPerformed);

        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                splitPane.setDividerLocation(0.5);
            }
        });
    }

    public void reloadRuleGroup() {
        ruleTabbedPane.removeAll();

        this.configLoader = new ConfigLoader(api);
        Config.globalRules.keySet().forEach(i -> ruleTabbedPane.addTab(i, new Rule(api, configLoader, mar.Config.globalRules.get(i), ruleTabbedPane, tester)));
        ruleTabbedPane.addTab("...", null);
    }

    private void deleteRuleGroupActionPerformed(ActionEvent e) {
        if (ruleTabbedPane.getTabCount() > 2) {
            int retCode = JOptionPane.showConfirmDialog(this, "Do you want to delete this rule group?", "Info",
                    JOptionPane.YES_NO_OPTION);
            if (retCode == JOptionPane.YES_OPTION) {
                String title = ruleTabbedPane.getTitleAt(ruleTabbedPane.getSelectedIndex());
                ruleProcessor.deleteRuleGroup(title);
                ruleTabbedPane.remove(ruleTabbedPane.getSelectedIndex());
                ruleTabbedPane.setSelectedIndex(ruleTabbedPane.getSelectedIndex() - 1);
            }
        }
    }
}