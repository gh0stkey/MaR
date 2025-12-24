package mar.instances.editor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Range;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider;
import mar.cache.CachePool;
import mar.utils.ConfigLoader;
import mar.utils.HttpUtils;

import java.awt.*;

public class ModifiedRequestEditor implements HttpRequestEditorProvider {
    private final MontoyaApi api;
    private final ConfigLoader configLoader;

    public ModifiedRequestEditor(MontoyaApi api, ConfigLoader configLoader) {
        this.api = api;
        this.configLoader = configLoader;
    }

    @Override
    public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext editorCreationContext) {
        return new Editor(api, configLoader, editorCreationContext);
    }

    private static class Editor implements ExtensionProvidedHttpRequestEditor {
        private final MontoyaApi api;
        private final ConfigLoader configLoader;
        private final HttpUtils httpUtils;
        private final EditorCreationContext creationContext;
        private final HttpRequestEditor requestEditor;
        private HttpRequestResponse requestResponse;
        private HttpRequest modifiedRequest;

        public Editor(MontoyaApi api, ConfigLoader configLoader, EditorCreationContext creationContext) {
            this.api = api;
            this.configLoader = configLoader;
            this.httpUtils = new HttpUtils(api, configLoader);
            this.creationContext = creationContext;
            this.requestEditor = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        }

        @Override
        public HttpRequest getRequest() {
            return requestResponse.request();
        }

        @Override
        public void setRequestResponse(HttpRequestResponse requestResponse) {
            this.requestResponse = requestResponse;
            updateUI();
        }

        private void updateUI() {
            if (modifiedRequest != null) {
                requestEditor.setRequest(modifiedRequest);
            }
        }

        @Override
        public synchronized boolean isEnabledFor(HttpRequestResponse requestResponse) {
            HttpRequest request = requestResponse.request();
            if (request != null) {
                try {
                    String toolType = creationContext.toolSource().toolType().toolName();
                    boolean shouldSkip = httpUtils.verifyHttpRequestResponse(request, toolType);

                    if (!shouldSkip) {
                        String requestKey = CachePool.generateRequestKey(request);
                        this.modifiedRequest = CachePool.getModifiedRequest(requestKey);
                        return this.modifiedRequest != null;
                    }
                } catch (Exception e) {
                    api.logging().logToError("RequestEditor error: " + e.getMessage());
                }
            }
            return false;
        }

        @Override
        public String caption() {
            return "Modified Request";
        }

        @Override
        public Component uiComponent() {
            return requestEditor.uiComponent();
        }

        @Override
        public Selection selectedData() {
            return new Selection() {
                @Override
                public ByteArray contents() {
                    if (modifiedRequest != null) {
                        return modifiedRequest.toByteArray();
                    }
                    return ByteArray.byteArray("");
                }

                @Override
                public Range offsets() {
                    return null;
                }
            };
        }

        @Override
        public boolean isModified() {
            return false;
        }
    }
}
