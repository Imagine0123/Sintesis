package com.rafdi.vitechasia.blog.api;

import com.rafdi.vitechasia.blog.models.Article;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PocketBaseClient {
    private static PocketBaseClient instance;
    private final ArticleApiService apiService;

    private PocketBaseClient() {
        apiService = ApiClient.getArticleApiService();
    }

    public static synchronized PocketBaseClient getInstance() {
        if (instance == null) {
            instance = new PocketBaseClient();
        }
        return instance;
    }

    // Get paginated articles with filter
    public void getArticles(int page, int perPage, String filter, PocketBaseCallback<List<Article>> callback) {
        Map<String, String> filters = new HashMap<>();
        if (filter != null && !filter.isEmpty()) {
            filters.put("filter", filter);
        }

        apiService.getArticles(page, perPage, filters).enqueue(new Callback<PocketBaseResponse<Article>>() {
            @Override
            public void onResponse(Call<PocketBaseResponse<Article>> call,
                                   Response<PocketBaseResponse<Article>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Article> articles = response.body().getItems();
                    callback.onSuccess(articles);
                } else {
                    callback.onError("Failed to fetch articles: " +
                            (response.errorBody() != null ?
                                    response.errorBody().toString() : response.message()));
                }
            }

            @Override
            public void onFailure(Call<PocketBaseResponse<Article>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Get all articles with sorting
    public void getAllArticles(String sort, PocketBaseCallback<List<Article>> callback) {
        apiService.getAllArticles(sort).enqueue(new Callback<PocketBaseResponse<Article>>() {
            @Override
            public void onResponse(Call<PocketBaseResponse<Article>> call,
                                   Response<PocketBaseResponse<Article>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Article> articles = response.body().getItems();
                    callback.onSuccess(articles);
                } else {
                    callback.onError("Failed to fetch all articles: " +
                            (response.errorBody() != null ?
                                    response.errorBody().toString() : response.message()));
                }
            }

            @Override
            public void onFailure(Call<PocketBaseResponse<Article>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Get first matching article
    public void getFirstMatchingArticle(String filter, String expand, PocketBaseCallback<Article> callback) {
        apiService.getFirstMatchingArticle(filter, expand).enqueue(new Callback<PocketBaseResponse<Article>>() {
            @Override
            public void onResponse(Call<PocketBaseResponse<Article>> call,
                                   Response<PocketBaseResponse<Article>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Article> items = response.body().getItems();
                    if (!items.isEmpty()) {
                        callback.onSuccess(items.get(0));
                    } else {
                        callback.onError("No matching article found");
                    }
                } else {
                    callback.onError("Failed to fetch article: " +
                            (response.errorBody() != null ?
                                    response.errorBody().toString() : response.message()));
                }
            }

            @Override
            public void onFailure(Call<PocketBaseResponse<Article>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Get article by ID
    public void getArticleById(String id, String expand, PocketBaseCallback<Article> callback) {
        apiService.getArticleById(id, expand).enqueue(new Callback<PocketBaseResponse<Article>>() {
            @Override
            public void onResponse(Call<PocketBaseResponse<Article>> call,
                                   Response<PocketBaseResponse<Article>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Article> items = response.body().getItems();
                    if (!items.isEmpty()) {
                        callback.onSuccess(items.get(0));
                    } else {
                        callback.onError("Article not found");
                    }
                } else {
                    callback.onError("Failed to fetch article: " +
                            (response.errorBody() != null ?
                                    response.errorBody().toString() : response.message()));
                }
            }

            @Override
            public void onFailure(Call<PocketBaseResponse<Article>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Callback interface for async operations
    public interface PocketBaseCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}