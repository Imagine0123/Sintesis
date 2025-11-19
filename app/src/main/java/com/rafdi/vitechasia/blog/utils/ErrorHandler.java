package com.rafdi.vitechasia.blog.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.StringRes;

import com.rafdi.vitechasia.blog.R;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.HttpException;

/**
 * Utility class for handling and formatting errors consistently across the app.
 */
public class ErrorHandler {
    private static final String TAG = "ErrorHandler";

    /**
     * Handles an error and returns a user-friendly error message.
     *
     * @param context   The context for accessing resources
     * @param throwable The error to handle
     * @return A user-friendly error message string resource ID
     */
    @StringRes
    public static int handleError(Context context, Throwable throwable) {
        Log.e(TAG, "Error occurred: ", throwable);

        if (throwable instanceof SocketTimeoutException) {
            return R.string.error_network_timeout;
        } else if (throwable instanceof UnknownHostException || throwable instanceof ConnectException) {
            return R.string.error_network_connection;
        } else if (throwable instanceof IOException) {
            return R.string.error_network_retry;
        } else if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            int statusCode = httpException.code();
            
            if (statusCode >= 500) {
                return R.string.error_server;
            } else if (statusCode == 401 || statusCode == 403) {
                return R.string.error_unauthorized;
            } else if (statusCode == 404) {
                return R.string.error_not_found;
            }
        }

        return R.string.error_unknown;
    }

    /**
     * Gets a user-friendly error message for a given error code.
     *
     * @param context The context for accessing resources
     * @param errorCode The error code
     * @return A user-friendly error message
     */
    public static String getErrorMessage(Context context, @StringRes int errorCode) {
        return context.getString(errorCode);
    }

    /**
     * Logs an error with a custom tag and message.
     *
     * @param tag     The log tag
     * @param message The error message
     * @param e       The exception (can be null)
     */
    public static void logError(String tag, String message, Throwable e) {
        if (e != null) {
            Log.e(tag, message, e);
        } else {
            Log.e(tag, message);
        }
    }

    /**
     * Checks if the error is a network-related error.
     *
     * @param throwable The error to check
     * @return true if it's a network error, false otherwise
     */
    public static boolean isNetworkError(Throwable throwable) {
        return throwable instanceof IOException || 
               throwable instanceof SocketTimeoutException ||
               throwable instanceof UnknownHostException ||
               throwable instanceof ConnectException;
    }
}
