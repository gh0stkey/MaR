package mar.component;

import burp.api.montoya.MontoyaApi;
import mar.component.rule.Rules;
import mar.utils.ConfigLoader;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class Main extends JPanel {
    private final MontoyaApi api;
    private final ConfigLoader configLoader;

    public Main(MontoyaApi api, ConfigLoader configLoader) {
        this.api = api;
        this.configLoader = configLoader;

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        ((GridBagLayout) getLayout()).columnWidths = new int[]{0, 0};
        ((GridBagLayout) getLayout()).rowHeights = new int[]{0, 0};
        ((GridBagLayout) getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
        ((GridBagLayout) getLayout()).rowWeights = new double[]{1.0, 1.0E-4};

        JTabbedPane mainTabbedPane = new JTabbedPane();

        JTabbedPane MaRTabbedPane = new JTabbedPane();
        boolean isDarkBg = isDarkBg(MaRTabbedPane);
        MaRTabbedPane.addTab("", getImageIcon(isDarkBg), mainTabbedPane);
        // 中文Slogan：精准匹配，智能替换！
        MaRTabbedPane.addTab(" Matcher and Replacement - Perform intelligent replacement based on precise matching. ", null);
        MaRTabbedPane.setEnabledAt(1, false);
        MaRTabbedPane.addPropertyChangeListener("background", e -> MaRTabbedPane.setIconAt(0, getImageIcon(isDarkBg(MaRTabbedPane))));

        add(MaRTabbedPane, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

        Rules rules = new Rules(api, configLoader);
        Config config = new Config(api, configLoader, rules);
        mainTabbedPane.addTab("Rules", rules);
        mainTabbedPane.addTab("Config", config);
    }

    private boolean isDarkBg(JTabbedPane MaRTabbedPane) {
        Color bg = MaRTabbedPane.getBackground();
        int r = bg.getRed();
        int g = bg.getGreen();
        int b = bg.getBlue();
        int avg = (r + g + b) / 3;

        return avg < 128;
    }

    private ImageIcon getImageIcon(boolean isDark) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL imageURL;
        if (isDark) {
            imageURL = classLoader.getResource("logo/logo.png");
        } else {
            imageURL = classLoader.getResource("logo/logo_black.png");
        }
        ImageIcon originalIcon = new ImageIcon(imageURL);
        Image originalImage = originalIcon.getImage();
        Image scaledImage = originalImage.getScaledInstance(30, 20, Image.SCALE_AREA_AVERAGING);
        return new ImageIcon(scaledImage);
    }
}
