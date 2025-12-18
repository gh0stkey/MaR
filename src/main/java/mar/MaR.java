package mar;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import mar.component.Main;
import mar.instances.ContextMenuHandler;
import mar.instances.HttpMessageActiveHandler;
import mar.utils.ConfigLoader;

public class MaR implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        // 设置扩展名称
        api.extension().setName("MaR - Matcher and Replacement");
        String version = "1.0";

        // 加载扩展后输出的项目信息
        Logging logging = api.logging();
        logging.logToOutput("[ HACK THE WORLD - TO DO IT ]");
        logging.logToOutput("[#] Author: EvilChen && 0chencc");
        logging.logToOutput("[#] Github: https://github.com/gh0stkey/MaR");
        logging.logToOutput("[#] Version: " + version);

        // 配置文件加载
        ConfigLoader configLoader = new ConfigLoader(api);

        // 注册Tab页
        api.userInterface().registerSuiteTab("MaR", new Main(api, configLoader));

        // 注册HTTP消息处理器
        api.http().registerHttpHandler(new HttpMessageActiveHandler(api, configLoader));
        
        // 注册右键菜单处理器
        api.userInterface().registerContextMenuItemsProvider(new ContextMenuHandler(api, configLoader));
    }
}
