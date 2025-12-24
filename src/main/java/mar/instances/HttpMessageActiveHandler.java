package mar.instances;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import mar.Config;
import mar.cache.CachePool;
import mar.utils.ConfigLoader;
import mar.utils.HttpUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HttpMessageActiveHandler implements HttpHandler {
    private final MontoyaApi api;
    private final ConfigLoader configLoader;
    private final HttpUtils httpUtils;
    private final HttpMessageModifier modifier;
    private final ThreadLocal<List<Modification>> responseModifications = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<ResponseRule>> responseRules = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<Boolean> isScope = ThreadLocal.withInitial(() -> true);

    public HttpMessageActiveHandler(MontoyaApi api, ConfigLoader configLoader) {
        this.api = api;
        this.configLoader = configLoader;
        this.httpUtils = new HttpUtils(api, configLoader);
        this.modifier = new HttpMessageModifier(api);
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
        HttpRequest request = httpRequestToBeSent;
        Annotations annotations = httpRequestToBeSent.annotations();
        List<Modification> modifications = new ArrayList<>();
        List<ResponseRule> rules = new ArrayList<>();

        String toolType = httpRequestToBeSent.toolSource().toolType().toolName();
        boolean isScopeMatch = !httpUtils.verifyHttpRequestResponse(request, toolType);
        isScope.set(isScopeMatch);

        if (isScopeMatch) {
            for (String group : Config.globalRules.keySet()) {
                for (Object[] ruleObj : Config.globalRules.get(group)) {
                    // 解析规则参数
                    boolean loaded = (Boolean) ruleObj[0];
                    if (!loaded) continue;

                    String c_scope = ((String) ruleObj[2]).toLowerCase();
                    String relationship = (String) ruleObj[3];
                    String condition = (String) ruleObj[4];
                    boolean c_regex = (Boolean) ruleObj[5];
                    String m_scope = ((String) ruleObj[6]).toLowerCase();
                    String match = (String) ruleObj[7];
                    String replace = (String) ruleObj[8];
                    boolean m_regex = (Boolean) ruleObj[9];

                    if (!c_scope.startsWith("request")) {
                        // 添加响应规则
                        rules.add(new ResponseRule(c_scope, relationship, condition, c_regex, m_scope, match, replace, m_regex));
                        continue;
                    }

                    // 获取请求的各个部分
                    String fullRequest = modifier.handleStringEncoding(request.toByteArray().getBytes());
                    String method = request.method();
                    String uri = request.path();
                    String header = request.headers().stream()
                            .map(HttpHeader::toString)
                            .collect(Collectors.joining("\r\n"));
                    String body = modifier.handleStringEncoding(request.body().getBytes());

                    // 检查条件是否满足
                    boolean conditionMet = modifier.checkCondition(
                            modifier.getTargetContent(c_scope, fullRequest, method, uri, header, body),
                            condition, relationship, c_regex);

                    if (conditionMet) {
                        if (m_scope.startsWith("request")) {
                            request = modifier.modifyRequest(request, m_scope, match, replace, m_regex);
                        } else if (m_scope.startsWith("response")) {
                            modifications.add(new Modification(m_scope, match, replace, m_regex));
                        }
                    }
                }
            }

            if (!Arrays.equals(request.toByteArray().getBytes(), httpRequestToBeSent.toByteArray().getBytes())) {
                String requestKey = CachePool.generateRequestKey(httpRequestToBeSent);
                CachePool.cacheModifiedRequest(requestKey, request);
            }

            // 设置ThreadLocal变量
            responseRules.set(rules);
            responseModifications.set(modifications);
        }

        return RequestToBeSentAction.continueWith(request, annotations);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
        try {
            HttpResponse response = httpResponseReceived;
            Annotations annotations = httpResponseReceived.annotations();
            List<Modification> modifications = responseModifications.get();
            List<ResponseRule> rules = responseRules.get();

            if (isScope.get()) {
                // 应用请求阶段存储的响应修改规则
                for (Modification modification : modifications) {
                    response = modifier.modifyResponse(response, modification.modifyScope,
                            modification.match, modification.replace, modification.regex);
                }

                // 应用响应规则
                for (ResponseRule rule : rules) {
                    // 获取响应的各个部分
                    String fullResponse = modifier.handleStringEncoding(response.toByteArray().getBytes());
                    String status = String.valueOf(response.statusCode());
                    String header = response.headers().stream()
                            .map(HttpHeader::toString)
                            .collect(Collectors.joining("\r\n"));
                    String body = modifier.handleStringEncoding(response.body().getBytes());

                    // 检查条件是否满足
                    boolean conditionMet = modifier.checkCondition(
                            modifier.getTargetContent(rule.c_scope, fullResponse, status, "", header, body),
                            rule.condition, rule.relationship, rule.c_regex);

                    if (conditionMet && rule.m_scope.startsWith("response")) {
                        response = modifier.modifyResponse(response, rule.m_scope,
                                rule.match, rule.replace, rule.m_regex);
                    }
                }

                if (!Arrays.equals(response.toByteArray().getBytes(), httpResponseReceived.toByteArray().getBytes())) {
                    String responseKey = CachePool.generateResponseKey(response);
                    CachePool.cacheOriginalResponse(responseKey, httpResponseReceived);
                }
            }

            return ResponseReceivedAction.continueWith(response, annotations);
        } finally {
            // 清理ThreadLocal变量，防止内存泄漏
            responseModifications.remove();
            responseRules.remove();
            isScope.remove();
        }
    }

    private record Modification(String modifyScope, String match, String replace, boolean regex) {
    }

    private record ResponseRule(String c_scope, String relationship, String condition, boolean c_regex, String m_scope,
                                String match, String replace, boolean m_regex) {
    }
}