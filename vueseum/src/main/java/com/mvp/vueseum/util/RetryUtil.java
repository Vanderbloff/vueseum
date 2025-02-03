package com.mvp.vueseum.util;

import com.mvp.vueseum.exception.RetryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Slf4j
public class RetryUtil {

    /**
     * Executes an operation with retry logic
     * @param operation The operation to execute
     * @param operationName Name of operation for logging
     * @param maxRetries Maximum number of retry attempts
     * @param <T> Return type of the operation
     * @return The operation result
     * @throws RetryException if all retries fail
     */
    public <T> T withRetry(Supplier<T> operation,
                           String operationName,
                           int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempts++;
                log.warn("Failed {} (attempt {}/{})",
                        operationName, attempts, maxRetries, e);

                if (attempts == maxRetries) {
                    throw new RetryException(
                            "Failed " + operationName + " after " + maxRetries + " attempts",
                            e
                    );
                }

                try {
                    Thread.sleep((long) Math.pow(2, attempts) * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RetryException(
                            "Interrupted while retrying " + operationName,
                            ie
                    );
                }
            }
        }
        throw new RetryException("Failed " + operationName);
    }
}
