package mar.cache;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class CachePool {
    private static final ConcurrentHashMap<String, HttpRequest> modifiedRequests = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, HttpResponse> modifiedResponses = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000;

    public static String generateRequestKey(HttpRequest request) {
        if (request == null) {
            return "";
        }
        String content = request.method() + request.url() + request.toByteArray().toString();
        return md5(content);
    }

    public static String generateResponseKey(HttpResponse response) {
        if (response == null) {
            return "";
        }
        String content = response.statusCode() + response.toByteArray().toString();
        return md5(content);
    }

    public static void cacheModifiedRequest(String key, HttpRequest modifiedRequest) {
        if (key == null || key.isEmpty() || modifiedRequest == null) {
            return;
        }

        if (modifiedRequests.size() >= MAX_CACHE_SIZE) {
            int removeCount = MAX_CACHE_SIZE / 2;
            modifiedRequests.keySet().stream()
                    .limit(removeCount)
                    .forEach(modifiedRequests::remove);
        }

        modifiedRequests.put(key, modifiedRequest);
    }

    public static void cacheOriginalResponse(String key, HttpResponse modifiedResponse) {
        if (key == null || key.isEmpty() || modifiedResponse == null) {
            return;
        }

        if (modifiedResponses.size() >= MAX_CACHE_SIZE) {
            int removeCount = MAX_CACHE_SIZE / 2;
            modifiedResponses.keySet().stream()
                    .limit(removeCount)
                    .forEach(modifiedResponses::remove);
        }

        modifiedResponses.put(key, modifiedResponse);
    }

    public static HttpRequest getModifiedRequest(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return modifiedRequests.get(key);
    }

    public static HttpResponse getModifiedResponse(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return modifiedResponses.get(key);
    }

    public static void clear() {
        modifiedRequests.clear();
        modifiedResponses.clear();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
