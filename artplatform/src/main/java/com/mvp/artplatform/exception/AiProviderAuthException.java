package com.mvp.artplatform.exception;

public class AiProviderAuthException extends AiProviderException {
    public AiProviderAuthException(String message) {
        super("AI provider authentication failed: " + message);
    }
}
