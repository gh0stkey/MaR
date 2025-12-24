package mar.instances.editor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Range;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.ui.editor.extension.HttpResponseEditorProvider;
import mar.cache.CachePool;
import mar.utils.ConfigLoader;
import mar.utils.HttpUtils;

import java.awt.*;

public class OriginalReponseEditor implements HttpResponseEditorProvider {
    private final MontoyaApi api;
    private final ConfigLoader configLoader;

    public OriginalReponseEditor(MontoyaApi api, ConfigLoader configLoader) {
        this.api = api;
        this.configLoader = configLoader;
    }

    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext editorCreationContext) {
        return new Editor(api, configLoader, editorCreationContext);
    }

    private static class Editor implements ExtensionProvidedHttpResponseEditor {
        private final MontoyaApi api;
        private final ConfigLoader configLoader;
        private final HttpUtils httpUtils;
        private final EditorCreationContext creationContext;
        private final HttpResponseEditor responseEditor;
        private HttpRequestResponse requestResponse;
        private HttpResponse originalResponse;

        public Editor(MontoyaApi api, ConfigLoader configLoader, EditorCreationContext creationContext) {
            this.api = api;
            this.configLoader = configLoader;
            this.httpUtils = new HttpUtils(api, configLoader);
            this.creationContext = creationContext;
            this.responseEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
        }

        @Override
        public HttpResponse getResponse() {
            return requestResponse.response();
        }

        @Override
        public void setRequestResponse(HttpRequestResponse requestResponse) {
            this.requestResponse = requestResponse;
            updateUI();
        }

        private void updateUI() {
            if (originalResponse != null) {
                responseEditor.setResponse(originalResponse);
            }
        }

        @Override
        public synchronized boolean isEnabledFor(HttpRequestResponse requestResponse) {
            HttpResponse response = requestResponse.response();
            if (response != null) {
                try {
                    String toolType = creationContext.toolSource().toolType().toolName();
                    boolean shouldSkip = httpUtils.verifyHttpRequestResponse(requestResponse.request(), toolType);

                    if (!shouldSkip) {
                        String requestKey = CachePool.generateResponseKey(response);
                        this.originalResponse = CachePool.getModifiedResponse(requestKey);
                        return this.originalResponse != null;
                    }
                } catch (Exception e) {
                    api.logging().logToError("RequestEditor error: " + e.getMessage());
                }
            }
            return false;
        }

        @Override
        public String caption() {
            return "Original Request";
        }

        @Override
        public Component uiComponent() {
            return responseEditor.uiComponent();
        }

        @Override
        public Selection selectedData() {
            return new Selection() {
                @Override
                public ByteArray contents() {
                    if (originalResponse != null) {
                        return originalResponse.toByteArray();
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
