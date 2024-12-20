package com.mvp.artplatform.exception;

public class AiProviderTimeoutException extends AiProviderException {
    public AiProviderTimeoutException(String message) {
        super("AI provider request timed out: " + message);
    }
}
