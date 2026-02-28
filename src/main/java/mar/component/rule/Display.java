package mar.component.rule;

import static java.awt.Toolkit.getDefaultToolkit;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import mar.Config;

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

    private final int scaledWidth;

    public Display() {
        scaledWidth = (int) ((300 * getDefaultToolkit().getScreenResolution()) /
                96.0);
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
                    new Boolean[] { false, true },
                    5,
                    innerC,
                    panel
            );

            conditionScopeComboBox.addActionListener(e ->
                    updateMatchReplaceScopeComboBox()
            );
        });

        createSection("Match and Replace", 6, c, (innerC, panel) -> {
            addLabel("Scope:", 7, innerC, panel);
            matchReplaceScopeComboBox = addComboBox(
                    Config.scope,
                    7,
                    innerC,
                    panel
            );
            addLabel("Match:", 8, innerC, panel);
            matchTextField = addTextField(8, innerC, panel);
            addLabel("Replace:", 9, innerC, panel);
            replaceTextField = addTextField(9, innerC, panel);
            addLabel("Regex:", 10, innerC, panel);
            matchReplaceRegexComboBox = addComboBox(
                    new Boolean[] { false, true },
                    10,
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
                new Dimension(scaledWidth, textField.getPreferredSize().height)
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
                new Dimension(scaledWidth, comboBox.getPreferredSize().height)
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

        TitledBorder titledBorder = BorderFactory.createTitledBorder(title);

        sectionContent.apply(innerC, panel);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(titledBorder);
        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        outerC.gridx = 0;
        outerC.gridy = startY;
        outerC.gridwidth = 2;
        add(scrollPane, outerC);
    }

    @FunctionalInterface
    interface SectionContent {
        void apply(GridBagConstraints c, Container container);
    }
}
