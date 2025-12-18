package mar.component.rule;

import mar.Config;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class Display extends JPanel {
    public JComboBox<String> conditionScopeComboBox;
    public JComboBox<String> relationshipComboBox;
    public JTextField conditionTextField;
    public JTextField ruleNameTextField;
    public JComboBox<String> matchReplaceScopeComboBox;
    public JTextField matchTextField;
    public JTextField replaceTextField;
    public JComboBox<Boolean> matchReplaceRegexComboBox;
    public JComboBox<Boolean> conditionRegexComboBox;

    public Display() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(5, 5, 5, 5); // 添加一些内边距

        // Rule Name
        addLabel("Name:", 0, c, this);
        ruleNameTextField = addTextField(0, c, this);

        // Condition Section
        createSection("Condition", 1, c, (innerC, panel) -> {
            addLabel("Scope:", 2, innerC, panel);
            conditionScopeComboBox = addComboBox(Config.scope, 2, innerC, panel);
            addLabel("Relationship:", 3, innerC, panel);
            relationshipComboBox = addComboBox(Config.relationship, 3, innerC, panel);
            addLabel("Condition:", 4, innerC, panel);
            conditionTextField = addTextField(4, innerC, panel);
            addLabel("Regex:", 5, innerC, panel);
            conditionRegexComboBox = addComboBox(new Boolean[]{false, true}, 5, innerC, panel);

            conditionScopeComboBox.addActionListener(e -> updateMatchReplaceScopeComboBox());
        });

        // Match and Replace Section
        createSection("Match and Replace", 6, c, (innerC, panel) -> {
            addLabel("Scope:", 7, innerC, panel);
            matchReplaceScopeComboBox = addComboBox(Config.scope, 7, innerC, panel);
            addLabel("Match:", 8, innerC, panel);
            matchTextField = addTextField(8, innerC, panel);
            addLabel("Replace:", 9, innerC, panel);
            replaceTextField = addTextField(9, innerC, panel);
            addLabel("Regex:", 10, innerC, panel);
            matchReplaceRegexComboBox = addComboBox(new Boolean[]{false, true}, 10, innerC, panel);
        });
    }

    private void updateMatchReplaceScopeComboBox() {
        String selectedScope = (String) conditionScopeComboBox.getSelectedItem();
        String[] newScope = selectedScope != null && selectedScope.contains("response") ?
                Config.responseScope : Config.scope;

        // 保存当前选中的值
        String currentSelection = (String) matchReplaceScopeComboBox.getSelectedItem();

        // 检查当前model是否需要更新
        ComboBoxModel<String> currentModel = matchReplaceScopeComboBox.getModel();
        boolean needUpdate = currentModel.getSize() != newScope.length;

        // 如果长度相同，还需要检查内容是否完全一致
        if (!needUpdate) {
            for (int i = 0; i < newScope.length; i++) {
                if (!newScope[i].equals(currentModel.getElementAt(i))) {
                    needUpdate = true;
                    break;
                }
            }
        }

        if (needUpdate) {
            // 检查当前选择是否在新scope中
            String newSelection = null;
            if (currentSelection != null) {
                for (String scope : newScope) {
                    if (currentSelection.equals(scope)) {
                        newSelection = currentSelection;
                        break;
                    }
                }
            }

            matchReplaceScopeComboBox.setModel(new DefaultComboBoxModel<>(newScope));

            // 如果原来的选择在新scope中存在，则保持原来的选择
            if (newSelection != null) {
                matchReplaceScopeComboBox.setSelectedItem(newSelection);
            }
        }
    }

    private void addLabel(String text, int y, GridBagConstraints c, Container container) {
        JLabel label = new JLabel(text);
        c.gridx = 0;
        c.gridy = y;
        container.add(label, c);
    }

    private JTextField addTextField(int y, GridBagConstraints c, Container container) {
        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(300, textField.getPreferredSize().height)); // 设置首选宽度
        c.gridx = 1;
        c.gridy = y;
        container.add(textField, c);
        return textField;
    }

    private <T> JComboBox<T> addComboBox(T[] items, int y, GridBagConstraints c, Container container) {
        JComboBox<T> comboBox = new JComboBox<>(items);
        comboBox.setPreferredSize(new Dimension(300, comboBox.getPreferredSize().height)); // 设置首选宽度
        c.gridx = 1;
        c.gridy = y;
        container.add(comboBox, c);
        return comboBox;
    }

    private void createSection(String title, int startY, GridBagConstraints outerC, SectionContent sectionContent) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints innerC = new GridBagConstraints();
        innerC.fill = GridBagConstraints.HORIZONTAL;
        innerC.weightx = 1.0;

        TitledBorder titledBorder = BorderFactory.createTitledBorder(title);

        // 重置y坐标以便从0开始
        sectionContent.apply(innerC, panel);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(titledBorder);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        outerC.gridx = 0;
        outerC.gridy = startY;
        outerC.gridwidth = 2; // 占用两列
        add(scrollPane, outerC);
    }

    @FunctionalInterface
    interface SectionContent {
        void apply(GridBagConstraints c, Container container);
    }
}