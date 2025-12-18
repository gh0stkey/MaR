package mar.utils;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.Arrays;
import java.util.List;

public class HttpUtils {
    private final MontoyaApi api;
    private final ConfigLoader configLoader;

    public HttpUtils(MontoyaApi api, ConfigLoader configLoader) {
        this.api = api;
        this.configLoader = configLoader;
    }

    public boolean verifyHttpRequestResponse(HttpRequest request, String toolType) {
        boolean retStatus = false;
        try {
            String host = StringProcessor.getHostByUrl(request.url());

            boolean isBlockHost = false;
            String blockHost = configLoader.getBlockHost();
            if (!blockHost.isBlank()) {
                String[] hostList = configLoader.getBlockHost().split("\\|");
                isBlockHost = isBlockHost(hostList, host);
            }

            boolean isExcludeSuffix = false;
            String suffix = configLoader.getExcludeSuffix();
            if (!suffix.isBlank()) {
                List<String> suffixList = Arrays.asList(configLoader.getExcludeSuffix().split("\\|"));
                isExcludeSuffix = suffixList.contains(request.fileExtension().toLowerCase());
            }

            boolean isToolScope = !configLoader.getScope().contains(toolType);

            retStatus = isExcludeSuffix || isBlockHost || isToolScope;
        } catch (Exception ignored) {
        }

        return retStatus;
    }

    private boolean isBlockHost(String[] hostList, String host) {
        boolean isBlockHost = false;
        for (String hostName : hostList) {
            String cleanedHost = StringProcessor.replaceFirstOccurrence(hostName, "*.", "");
            if (hostName.contains("*.") && StringProcessor.matchFromEnd(host, cleanedHost)) {
                isBlockHost = true;
            } else if (host.equals(hostName) || hostName.equals("*")) {
                isBlockHost = true;
            }
        }
        return isBlockHost;
    }
}
