package com.mvp.artplatform.exception;

import lombok.Getter;

import java.time.Duration;

@Getter
public class AiProviderRateLimitException extends AiProviderException {
  private final Duration retryAfter;

  public AiProviderRateLimitException(String message, Duration retryAfter) {
    super("Rate limit exceeded: " + message);
    this.retryAfter = retryAfter;
  }
}
