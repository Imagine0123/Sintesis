package com.rafdi.vitechasia.blog.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * A singleton class that provides a configured Retrofit instance and API service instances.
 * This class handles the network client configuration including timeouts and logging.
 * 
 * <p>Use {@link #getClient()} to get a configured Retrofit instance or
 * {@link #getArticleApiService()} to get a ready-to-use ArticleApiService instance.
 */

public class ApiClient {
    private static final String BASE_URL = "https://dummyimpossivleiofddtdhno.com/api/";
    private static Retrofit retrofit = null;
    
    /**
     * Gets the singleton Retrofit instance, creating it if necessary.
     * The client is configured with a 30-second timeout and HTTP logging.
     *
     * @return Configured Retrofit instance
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(logging);
            
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return retrofit;
    }
    
    /**
     * Gets a ready-to-use instance of ArticleApiService.
     * This is a convenience method that uses the shared Retrofit client.
     *
     * @return Configured ArticleApiService instance
     */
    public static ArticleApiService getArticleApiService() {
        return getClient().create(ArticleApiService.class);
    }
}
