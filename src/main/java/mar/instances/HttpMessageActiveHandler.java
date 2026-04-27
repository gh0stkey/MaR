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
    private final ThreadLocal<List<Modification>> responseModifications =
            ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<ResponseRule>> responseRules =
            ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<Boolean> isScope = ThreadLocal.withInitial(() ->
            true
    );

    public HttpMessageActiveHandler(MontoyaApi api, ConfigLoader configLoader) {
        this.api = api;
        this.configLoader = configLoader;
        this.httpUtils = new HttpUtils(api, configLoader);
        this.modifier = new HttpMessageModifier(api);
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(
            HttpRequestToBeSent httpRequestToBeSent
    ) {
        HttpRequest request = httpRequestToBeSent;
        Annotations annotations = httpRequestToBeSent.annotations();
        List<Modification> modifications = new ArrayList<>();
        List<ResponseRule> rules = new ArrayList<>();

        String toolType = httpRequestToBeSent
                .toolSource()
                .toolType()
                .toolName();
        boolean isScopeMatch = !httpUtils.verifyHttpRequestResponse(
                request,
                toolType
        );
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
                    String e_scope = ((String) ruleObj[6]).toLowerCase();
                    String f_regex = (String) ruleObj[7];
                    String s_regex = (String) ruleObj[8];
                    String m_scope = ((String) ruleObj[9]).toLowerCase();
                    String match = (String) ruleObj[10];
                    String replace = (String) ruleObj[11];
                    boolean m_regex = (Boolean) ruleObj[12];

                    if (!c_scope.startsWith("request")) {
                        // 添加响应规则
                        rules.add(
                                new ResponseRule(
                                        c_scope,
                                        relationship,
                                        condition,
                                        c_regex,
                                        e_scope,
                                        f_regex,
                                        s_regex,
                                        m_scope,
                                        match,
                                        replace,
                                        m_regex
                                )
                        );
                        continue;
                    }

                    // 获取请求的各个部分
                    String fullRequest = modifier.handleStringEncoding(
                            request.toByteArray().getBytes()
                    );
                    String method = request.method();
                    String uri = request.path();
                    String header = request
                            .headers()
                            .stream()
                            .map(HttpHeader::toString)
                            .collect(Collectors.joining("\r\n"));
                    String body = modifier.handleStringEncoding(
                            request.body().getBytes()
                    );

                    // 检查条件是否满足
                    boolean conditionMet = modifier.checkCondition(
                            modifier.getTargetContent(
                                    c_scope,
                                    fullRequest,
                                    method,
                                    uri,
                                    header,
                                    body
                            ),
                            condition,
                            relationship,
                            c_regex
                    );

                    if (conditionMet) {
                        String resolvedMatch = match;
                        String resolvedReplace = replace;
                        String pendingEScope = "";
                        String pendingFRegex = "";
                        String pendingSRegex = "";

                        if (!f_regex.isEmpty()) {
                            String extractScope = e_scope.isEmpty()
                                    ? m_scope
                                    : e_scope;
                            if (extractScope.startsWith("request")) {
                                // Extract 目标在 request，立即 resolve
                                String regexTarget = modifier.getTargetContent(
                                        extractScope,
                                        fullRequest,
                                        method,
                                        uri,
                                        header,
                                        body
                                );
                                resolvedMatch =
                                        modifier.resolveRegexIdentifiers(
                                                regexTarget,
                                                f_regex,
                                                s_regex,
                                                match
                                        );
                                resolvedReplace =
                                        modifier.resolveRegexIdentifiers(
                                                regexTarget,
                                                f_regex,
                                                s_regex,
                                                replace
                                        );
                                // 如果没有提取到东西，跳过 match and replace
                                if (
                                        resolvedMatch == null ||
                                                resolvedReplace == null
                                ) {
                                    continue;
                                }
                            } else {
                                // Extract 目标在 response，延迟到响应阶段
                                pendingEScope = e_scope;
                                pendingFRegex = f_regex;
                                pendingSRegex = s_regex;
                            }
                        }

                        if (m_scope.startsWith("request")) {
                            request = modifier.modifyRequest(
                                    request,
                                    m_scope,
                                    resolvedMatch,
                                    resolvedReplace,
                                    m_regex
                            );
                        } else if (m_scope.startsWith("response")) {
                            modifications.add(
                                    new Modification(
                                            m_scope,
                                            resolvedMatch,
                                            resolvedReplace,
                                            m_regex,
                                            pendingEScope,
                                            pendingFRegex,
                                            pendingSRegex
                                    )
                            );
                        }
                    }
                }
            }

            if (
                    !Arrays.equals(
                            request.toByteArray().getBytes(),
                            httpRequestToBeSent.toByteArray().getBytes()
                    )
            ) {
                String requestKey = CachePool.generateRequestKey(
                        httpRequestToBeSent
                );
                CachePool.cacheModifiedRequest(requestKey, request);
            }

            // 设置ThreadLocal变量
            responseRules.set(rules);
            responseModifications.set(modifications);
        }

        return RequestToBeSentAction.continueWith(request, annotations);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(
            HttpResponseReceived httpResponseReceived
    ) {
        try {
            HttpResponse response = httpResponseReceived;
            Annotations annotations = httpResponseReceived.annotations();
            List<Modification> modifications = responseModifications.get();
            List<ResponseRule> rules = responseRules.get();

            if (isScope.get()) {
                // 应用请求阶段存储的响应修改规则
                for (Modification modification : modifications) {
                    String resolvedMatch = modification.match;
                    String resolvedReplace = modification.replace;

                    if (!modification.f_regex.isEmpty()) {
                        // 延迟到响应阶段的 extract
                        String fullResponse = modifier.handleStringEncoding(
                                response.toByteArray().getBytes()
                        );
                        String status = String.valueOf(response.statusCode());
                        String respHeader = response
                                .headers()
                                .stream()
                                .map(HttpHeader::toString)
                                .collect(Collectors.joining("\r\n"));
                        String respBody = modifier.handleStringEncoding(
                                response.body().getBytes()
                        );

                        String extractScope = modification.e_scope.isEmpty()
                                ? modification.modifyScope
                                : modification.e_scope;
                        String regexTarget = modifier.getTargetContent(
                                extractScope,
                                fullResponse,
                                status,
                                "",
                                respHeader,
                                respBody
                        );
                        resolvedMatch = modifier.resolveRegexIdentifiers(
                                regexTarget,
                                modification.f_regex,
                                modification.s_regex,
                                modification.match
                        );
                        resolvedReplace = modifier.resolveRegexIdentifiers(
                                regexTarget,
                                modification.f_regex,
                                modification.s_regex,
                                modification.replace
                        );
                        // 如果没有提取到东西，跳过 match and replace
                        if (resolvedMatch == null || resolvedReplace == null) {
                            continue;
                        }
                    }

                    response = modifier.modifyResponse(
                            response,
                            modification.modifyScope,
                            resolvedMatch,
                            resolvedReplace,
                            modification.regex
                    );
                }

                // 应用响应规则
                for (ResponseRule rule : rules) {
                    // 获取响应的各个部分
                    String fullResponse = modifier.handleStringEncoding(
                            response.toByteArray().getBytes()
                    );
                    String status = String.valueOf(response.statusCode());
                    String header = response
                            .headers()
                            .stream()
                            .map(HttpHeader::toString)
                            .collect(Collectors.joining("\r\n"));
                    String body = modifier.handleStringEncoding(
                            response.body().getBytes()
                    );

                    // 检查条件是否满足
                    boolean conditionMet = modifier.checkCondition(
                            modifier.getTargetContent(
                                    rule.c_scope,
                                    fullResponse,
                                    status,
                                    "",
                                    header,
                                    body
                            ),
                            rule.condition,
                            rule.relationship,
                            rule.c_regex
                    );

                    if (conditionMet && rule.m_scope.startsWith("response")) {
                        String resolvedMatch = rule.match;
                        String resolvedReplace = rule.replace;

                        if (!rule.f_regex.isEmpty()) {
                            String extractScope = rule.e_scope.isEmpty()
                                    ? rule.m_scope
                                    : rule.e_scope;
                            String regexTarget = modifier.getTargetContent(
                                    extractScope,
                                    fullResponse,
                                    status,
                                    "",
                                    header,
                                    body
                            );
                            resolvedMatch = modifier.resolveRegexIdentifiers(
                                    regexTarget,
                                    rule.f_regex,
                                    rule.s_regex,
                                    rule.match
                            );
                            resolvedReplace = modifier.resolveRegexIdentifiers(
                                    regexTarget,
                                    rule.f_regex,
                                    rule.s_regex,
                                    rule.replace
                            );
                            // 如果没有提取到东西，跳过 match and replace
                            if (
                                    resolvedMatch == null || resolvedReplace == null
                            ) {
                                continue;
                            }
                        }

                        response = modifier.modifyResponse(
                                response,
                                rule.m_scope,
                                resolvedMatch,
                                resolvedReplace,
                                rule.m_regex
                        );
                    }
                }

                if (
                        !Arrays.equals(
                                response.toByteArray().getBytes(),
                                httpResponseReceived.toByteArray().getBytes()
                        )
                ) {
                    String responseKey = CachePool.generateResponseKey(
                            response
                    );
                    CachePool.cacheOriginalResponse(
                            responseKey,
                            httpResponseReceived
                    );
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

    private record Modification(
            String modifyScope,
            String match,
            String replace,
            boolean regex,
            String e_scope,
            String f_regex,
            String s_regex
    ) {
    }

    private record ResponseRule(
            String c_scope,
            String relationship,
            String condition,
            boolean c_regex,
            String e_scope,
            String f_regex,
            String s_regex,
            String m_scope,
            String match,
            String replace,
            boolean m_regex
    ) {
    }
}
