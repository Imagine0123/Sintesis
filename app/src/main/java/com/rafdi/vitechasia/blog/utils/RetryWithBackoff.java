package com.rafdi.vitechasia.blog.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Utility class that provides retry mechanism with exponential backoff for network operations.
 */
public class RetryWithBackoff<T> {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    private final Executor executor;
    private final Handler mainHandler;
    private final Context context;
    
    public interface Operation<T> {
        /**
         * Perform the operation that might need to be retried.
         * @return The result of the operation if successful
         * @throws Exception if the operation fails
         */
        T perform() throws Exception;
    }
    
    public interface Callback<T> {
        /**
         * Called when the operation succeeds.
         * @param result The result of the operation
         */
        void onSuccess(T result);
        
        /**
         * Called when all retry attempts have failed.
         * @param errorMessage The error message from the last attempt
         * @param isNetworkError True if the error is related to network connectivity
         */
        void onError(String errorMessage, boolean isNetworkError);
    }
    
    public RetryWithBackoff(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Execute an operation with retry logic and exponential backoff.
     * @param operation The operation to execute
     * @param callback The callback to receive the result or error
     */
    public void execute(Operation<T> operation, Callback<T> callback) {
        executeWithRetry(operation, callback, 0, INITIAL_RETRY_DELAY_MS);
    }
    
    private void executeWithRetry(Operation<T> operation, Callback<T> callback, int attempt, long delayMs) {
        executor.execute(() -> {
            try {
                // Check network connectivity before each attempt
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    postError("No internet connection. Please check your connection and try again.", 
                             true, callback);
                    return;
                }
                
                T result = operation.perform();
                postSuccess(result, callback);
            } catch (Exception e) {
                boolean isNetworkError = isNetworkError(e);
                String errorMessage = getErrorMessage(e, isNetworkError);
                
                if (attempt < MAX_RETRIES - 1 && isRetryableError(e)) {
                    // Calculate next delay with exponential backoff
                    long nextDelay = (long) (delayMs * BACKOFF_MULTIPLIER);
                    
                    // Schedule the next retry
                    mainHandler.postDelayed(() -> 
                        executeWithRetry(operation, callback, attempt + 1, nextDelay), 
                        delayMs);
                } else {
                    postError(errorMessage, isNetworkError, callback);
                }
            }
        });
    }
    
    private boolean isRetryableError(Throwable throwable) {
        // Retry on network-related errors and server errors (5xx)
        return isNetworkError(throwable) || 
               (throwable instanceof IOException) ||
               (throwable.getCause() != null && isNetworkError(throwable.getCause()));
    }
    
    private boolean isNetworkError(Throwable throwable) {
        return throwable instanceof SocketTimeoutException ||
               (throwable.getMessage() != null && 
                (throwable.getMessage().contains("timeout") || 
                 throwable.getMessage().contains("network")));
    }
    
    private String getErrorMessage(Throwable throwable, boolean isNetworkError) {
        if (isNetworkError) {
            return "Network error. Please check your connection and try again.";
        }
        
        // Add more specific error messages as needed
        if (throwable instanceof SocketTimeoutException) {
            return "Request timed out. The server is taking too long to respond.";
        }
        
        String message = throwable.getMessage();
        return message != null ? message : "An unknown error occurred";
    }
    
    private <T> void postSuccess(final T result, final Callback<T> callback) {
        mainHandler.post(() -> callback.onSuccess(result));
    }
    
    private <T> void postError(final String errorMessage, final boolean isNetworkError, 
                              final Callback<T> callback) {
        mainHandler.post(() -> callback.onError(errorMessage, isNetworkError));
    }
}
