package mar.component.rule;

import mar.Config;

import javax.swing.*;
import java.awt.*;

public class Display extends JPanel {

    private static final int FIELD_WIDTH = 300;
    public JComboBox<String> conditionScopeComboBox;
    public JComboBox<String> relationshipComboBox;
    public JTextField conditionTextField;
    public JTextField ruleNameTextField;
    public JTextField fRegexTextField;
    public JTextField sRegexTextField;
    public JComboBox<String> extractScopeComboBox;
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
        c.insets = new Insets(5, 5, 5, 5);

        addLabel("Name:", 0, c, this);
        ruleNameTextField = addTextField(0, c, this);

        createSection("Condition", 1, c, (innerC, panel) -> {
            addLabel("Scope:", 2, innerC, panel);
            conditionScopeComboBox = addComboBox(
                    Config.scope,
                    2,
                    innerC,
                    panel
            );
            addLabel("Relationship:", 3, innerC, panel);
            relationshipComboBox = addComboBox(
                    Config.relationship,
                    3,
                    innerC,
                    panel
            );
            addLabel("Condition:", 4, innerC, panel);
            conditionTextField = addTextField(4, innerC, panel);
            addLabel("Regex:", 5, innerC, panel);
            conditionRegexComboBox = addComboBox(
                    new Boolean[]{false, true},
                    5,
                    innerC,
                    panel
            );

            conditionScopeComboBox.addActionListener(e ->
                    updateMatchReplaceScopeComboBox()
            );
        });

        createSection("Extract", 6, c, (innerC, panel) -> {
            addLabel("Scope:", 0, innerC, panel);
            extractScopeComboBox = addComboBox(Config.scope, 0, innerC, panel);

            addLabel("F-Regex:", 1, innerC, panel);
            fRegexTextField = addTextField(1, innerC, panel);

            addLabel("S-Regex:", 2, innerC, panel);
            sRegexTextField = addTextField(2, innerC, panel);
        });

        createSection("Match and Replace", 7, c, (innerC, panel) -> {
            addLabel("Scope:", 0, innerC, panel);
            matchReplaceScopeComboBox = addComboBox(
                    Config.scope,
                    0,
                    innerC,
                    panel
            );
            addLabel("Match:", 1, innerC, panel);
            matchTextField = addTextField(1, innerC, panel);
            addLabel("Replace:", 2, innerC, panel);
            replaceTextField = addTextField(2, innerC, panel);
            addLabel("Regex:", 3, innerC, panel);
            matchReplaceRegexComboBox = addComboBox(
                    new Boolean[]{false, true},
                    3,
                    innerC,
                    panel
            );
        });
    }

    private void updateMatchReplaceScopeComboBox() {
        String selectedScope =
                (String) conditionScopeComboBox.getSelectedItem();
        String[] newScope =
                selectedScope != null && selectedScope.contains("response")
                        ? Config.responseScope
                        : Config.scope;

        String currentSelection =
                (String) matchReplaceScopeComboBox.getSelectedItem();

        ComboBoxModel<String> currentModel =
                matchReplaceScopeComboBox.getModel();
        boolean needUpdate = currentModel.getSize() != newScope.length;

        if (!needUpdate) {
            for (int i = 0; i < newScope.length; i++) {
                if (!newScope[i].equals(currentModel.getElementAt(i))) {
                    needUpdate = true;
                    break;
                }
            }
        }

        if (needUpdate) {
            String newSelection = null;
            if (currentSelection != null) {
                for (String scope : newScope) {
                    if (currentSelection.equals(scope)) {
                        newSelection = currentSelection;
                        break;
                    }
                }
            }

            matchReplaceScopeComboBox.setModel(
                    new DefaultComboBoxModel<>(newScope)
            );

            if (newSelection != null) {
                matchReplaceScopeComboBox.setSelectedItem(newSelection);
            }
        }
    }

    private void addLabel(
            String text,
            int y,
            GridBagConstraints c,
            Container container
    ) {
        JLabel label = new JLabel(text);
        c.gridx = 0;
        c.gridy = y;
        container.add(label, c);
    }

    private JTextField addTextField(
            int y,
            GridBagConstraints c,
            Container container
    ) {
        JTextField textField = new JTextField();
        textField.setPreferredSize(
            new Dimension(FIELD_WIDTH, textField.getPreferredSize().height)
        );
        c.gridx = 1;
        c.gridy = y;
        container.add(textField, c);
        return textField;
    }

    private <T> JComboBox<T> addComboBox(
            T[] items,
            int y,
            GridBagConstraints c,
            Container container
    ) {
        JComboBox<T> comboBox = new JComboBox<>(items);
        comboBox.setPreferredSize(
            new Dimension(FIELD_WIDTH, comboBox.getPreferredSize().height)
        );
        c.gridx = 1;
        c.gridy = y;
        container.add(comboBox, c);
        return comboBox;
    }

    private void createSection(
            String title,
            int startY,
            GridBagConstraints outerC,
            SectionContent sectionContent
    ) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints innerC = new GridBagConstraints();
        innerC.fill = GridBagConstraints.HORIZONTAL;
        innerC.weightx = 1.0;
        innerC.insets = new Insets(3, 5, 3, 5);

        panel.setBorder(BorderFactory.createTitledBorder(title));

        sectionContent.apply(innerC, panel);

        outerC.gridx = 0;
        outerC.gridy = startY;
        outerC.gridwidth = 2;
        add(panel, outerC);
    }

    @FunctionalInterface
    interface SectionContent {
        void apply(GridBagConstraints c, Container container);
    }
}
