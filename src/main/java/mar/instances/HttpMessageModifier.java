package mar.instances;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class HttpMessageModifier {
    private final MontoyaApi api;

    public HttpMessageModifier(MontoyaApi api) {
        this.api = api;
    }

    private static List<Integer> getIntegerList(byte[] content, byte[] matchBytes) {
        List<Integer> matchPositions = new ArrayList<>();
        for (int i = 0; i <= content.length - matchBytes.length; i++) {
            boolean found = true;
            for (int j = 0; j < matchBytes.length; j++) {
                if (content[i + j] != matchBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                matchPositions.add(i);
                i += matchBytes.length - 1; // 跳过已匹配部分
            }
        }
        return matchPositions;
    }

    public boolean checkCondition(String target, String condition, String relationship, boolean regex) {
        if (target == null || condition == null) {
            return false;
        }

        boolean conditionFound;
        if (regex) {
            try {
                Pattern pattern = Pattern.compile(condition);
                conditionFound = pattern.matcher(target).find();
            } catch (PatternSyntaxException e) {
                api.logging().logToError("PatternSyntaxException: " + e.getMessage());
                return false;
            }
        } else {
            conditionFound = target.contains(condition);
        }

        return relationship.equals("Does not match") != conditionFound;
    }

    public String getTargetContent(String scope, String full, String firstPart, String secondPart, String header, String body) {
        return switch (scope) {
            case "request", "response" -> full;
            case "request method", "response status" -> firstPart;
            case "request uri" -> secondPart;
            case "request header", "response header" -> header;
            case "request body", "response body" -> body;
            default -> "";
        };
    }

    public byte[] matchAndReplaceBytes(byte[] content, String match, String replace, boolean regex) {
        if (content == null || content.length == 0 || match == null || replace == null) {
            return content;
        }

        try {
            String contentStr = new String(content, StandardCharsets.ISO_8859_1);

            if (regex) {
                try {
                    Pattern pattern = Pattern.compile(match);
                    Matcher matcher = pattern.matcher(contentStr);

                    if (!matcher.find()) {
                        return content;
                    }

                    // 使用StringBuffer来支持正则替换
                    StringBuffer result = new StringBuffer();
                    matcher.reset();

                    while (matcher.find()) {
                        try {
                            String processedReplace = replace.replace("\\r", "\r")
                                    .replace("\\n", "\n")
                                    .replace("\\t", "\t");

                            // 使用appendReplacement支持分组替换
                            matcher.appendReplacement(result, processedReplace);
                        } catch (IndexOutOfBoundsException e) {
                            // 处理替换字符串中的组引用无效的情况
                            api.logging().logToError("IndexOutOfBoundsException: " + e.getMessage());
                            return content;
                        }
                    }
                    matcher.appendTail(result);

                    return result.toString().getBytes(StandardCharsets.ISO_8859_1);
                } catch (PatternSyntaxException e) {
                    api.logging().logToError("PatternSyntaxException: " + e.getMessage());
                    return content;
                }
            } else {
                // 非正则表达式替换
                byte[] matchBytes = match.getBytes(StandardCharsets.UTF_8);
                byte[] replaceBytes = replace.getBytes(StandardCharsets.UTF_8);

                // 查找所有匹配位置
                List<Integer> matchPositions = getIntegerList(content, matchBytes);

                // 如果没有匹配，直接返回原内容
                if (matchPositions.isEmpty()) {
                    return content;
                }

                // 构建结果
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                int lastEnd = 0;

                for (int pos : matchPositions) {
                    // 写入匹配前的内容
                    result.write(content, lastEnd, pos - lastEnd);

                    // 写入替换内容
                    result.write(replaceBytes);

                    lastEnd = pos + matchBytes.length;
                }

                // 写入剩余内容
                if (lastEnd < content.length) {
                    result.write(content, lastEnd, content.length - lastEnd);
                }

                return result.toByteArray();
            }
        } catch (Exception e) {
            api.logging().logToError("matchAndReplaceBytes: " + e.getMessage());
            return content;
        }
    }

    public String matchAndReplace(String content, String match, String replace, boolean regex) {
        if (content == null || match == null || replace == null) {
            return content;
        }

        byte[] result = matchAndReplaceBytes(content.getBytes(StandardCharsets.UTF_8), match, replace, regex);
        return new String(result, StandardCharsets.UTF_8);
    }

    public String handleStringEncoding(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    private byte[] modifyMessageBody(byte[] bodyBytes, String match, String replace, boolean regex) {
        byte[] modifiedBody = matchAndReplaceBytes(bodyBytes, match, replace, regex);

        // 如果内容没有变化，返回null表示无需更新
        if (Arrays.equals(modifiedBody, bodyBytes)) {
            return null;
        }

        return modifiedBody;
    }

    public HttpRequest modifyRequest(HttpRequest request, String scope, String match, String replace, boolean regex) {
        return switch (scope) {
            case "request" -> {
                // 直接在字节级别操作
                byte[] requestBytes = request.toByteArray().getBytes();
                byte[] modified = matchAndReplaceBytes(requestBytes, match, replace, regex);

                // 如果内容没有变化，直接返回原始请求
                if (Arrays.equals(modified, requestBytes)) {
                    yield request;
                }

                yield HttpRequest.httpRequest(request.httpService(), new String(modified, StandardCharsets.ISO_8859_1));
            }
            case "request method" -> {
                String modifiedMethod = matchAndReplace(request.method(), match, replace, regex);
                yield request.withMethod(modifiedMethod);
            }
            case "request uri" -> {
                String modifiedPath = matchAndReplace(request.path(), match, replace, regex);
                yield request.withPath(modifiedPath);
            }
            case "request header" -> modifyRequestHeaders(request, match, replace, regex);
            case "request body" -> {
                byte[] modifiedBody = modifyMessageBody(request.body().getBytes(), match, replace, regex);
                yield modifiedBody == null ? request : request.withBody(ByteArray.byteArray(modifiedBody))
                        .withUpdatedHeader(HttpHeader.httpHeader("Content-Length", String.valueOf(modifiedBody.length)));
            }
            default -> request;
        };
    }

    public HttpResponse modifyResponse(HttpResponse response, String scope, String match, String replace, boolean regex) {
        return switch (scope) {
            case "response" -> {
                // 直接在字节级别操作
                byte[] responseBytes = response.toByteArray().getBytes();
                byte[] modified = matchAndReplaceBytes(responseBytes, match, replace, regex);

                // 如果内容没有变化，直接返回原始响应
                if (Arrays.equals(modified, responseBytes)) {
                    yield response;
                }

                yield HttpResponse.httpResponse(new String(modified, StandardCharsets.ISO_8859_1));
            }
            case "response status" -> {
                String code = String.valueOf(response.statusCode());
                String modifiedCode = matchAndReplace(code, match, replace, regex);
                try {
                    yield response.withStatusCode(Short.parseShort(modifiedCode));
                } catch (NumberFormatException e) {
                    api.logging().logToError("NumberFormatException: " + modifiedCode);
                    yield response;
                }
            }
            case "response header" -> modifyResponseHeaders(response, match, replace, regex);
            case "response body" -> {
                byte[] modifiedBody = modifyMessageBody(response.body().getBytes(), match, replace, regex);
                yield modifiedBody == null ? response : response.withBody(ByteArray.byteArray(modifiedBody))
                        .withUpdatedHeader(HttpHeader.httpHeader("Content-Length", String.valueOf(modifiedBody.length)));
            }
            default -> response;
        };
    }

    private <T> T modifyHeaders(T message, List<HttpHeader> headers, String match, String replace, boolean regex,
                                Function<List<HttpHeader>, T> headerUpdater) {
        // 将所有header合并成字符串
        String headersStr = headers.stream()
                .map(header -> header.name() + ": " + header.value())
                .collect(Collectors.joining("\r\n"));

        // 一次性处理所有header
        String modifiedHeaders = matchAndReplace(headersStr, match, replace, regex);

        // 如果没有变化，直接返回原始消息
        if (modifiedHeaders.equals(headersStr)) {
            return message;
        }

        // 将修改后的字符串转换回header列表
        List<HttpHeader> newHeaders = Arrays.stream(modifiedHeaders.split("\r\n"))
                .filter(line -> line.contains(": "))
                .map(line -> {
                    int colonIndex = line.indexOf(": ");
                    String name = line.substring(0, colonIndex);
                    String value = line.substring(colonIndex + 2);
                    return HttpHeader.httpHeader(name, value);
                })
                .collect(Collectors.toList());

        return headerUpdater.apply(newHeaders);
    }

    public HttpRequest modifyRequestHeaders(HttpRequest request, String match, String replace, boolean regex) {
        return modifyHeaders(request, request.headers(), match, replace, regex,
                newHeaders -> {
                    HttpRequest modified = request;
                    for (HttpHeader header : request.headers()) {
                        modified = modified.withRemovedHeader(header);
                    }
                    for (HttpHeader header : newHeaders) {
                        modified = modified.withAddedHeader(header);
                    }
                    return modified;
                });
    }

    public HttpResponse modifyResponseHeaders(HttpResponse response, String match, String replace, boolean regex) {
        return modifyHeaders(response, response.headers(), match, replace, regex,
                newHeaders -> {
                    HttpResponse modified = response;
                    for (HttpHeader header : response.headers()) {
                        modified = modified.withRemovedHeader(header);
                    }
                    for (HttpHeader header : newHeaders) {
                        modified = modified.withAddedHeader(header);
                    }
                    return modified;
                });
    }
}