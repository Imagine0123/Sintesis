package com.rafdi.vitechasia.blog.api;

import com.rafdi.vitechasia.blog.models.Article;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Retrofit service interface for the Article API endpoints.
 * Defines the HTTP methods and endpoints for interacting with the article resources.
 * 
 * <p>This interface is used by Retrofit to create the actual implementation of the API calls.
 * All methods return {@link Call} objects that can be executed asynchronously.
 */

public interface ArticleApiService {
    /**
     * Fetches a paginated list of articles with optional filtering.
     *
     * @param page The 1-based page number for pagination. Must be >= 1.
     * @param perPage The number of items per page. Typically between 1-100.
     * @param filters Optional map of filter parameters. Common filters include:
     *               - "filter": Filter string (e.g., "category='tech'")
     *               - "sort": Sort order (e.g., "-created" for newest first)
     *               - "expand": Comma-separated relations to expand
     * @return A {@link Call} that yields a {@link PocketBaseResponse} containing
     *         the paginated list of {@link Article} objects.
     * @throws IllegalArgumentException if page < 1 or perPage is invalid
     */
    @GET("collections/Article/records")
    Call<PocketBaseResponse<Article>> getArticles(
            @Query("page") int page,
            @Query("perPage") int perPage,
            @QueryMap Map<String, String> filters
    );

    /**
            * Retrieves all articles sorted by the specified field.
            *
            * @param sort The sort order string (e.g., "-created" for newest first, "title" for A-Z)
     * @return A {@link Call} that yields a {@link PocketBaseResponse} with all articles
     *         sorted according to the specified criteria.
     */
    @GET("collections/Article/records")
    Call<PocketBaseResponse<Article>> getAllArticles(
            @Query("sort") String sort
    );


    /**
     * Fetches the first article matching the given filter criteria.
     *
     * @param filter The filter criteria (e.g., "title~'%search_term%'")
     * @param expand Comma-separated list of relations to expand
     * @return A {@link Call} that yields a {@link PocketBaseResponse} containing
     *         the first matching {@link Article} or an empty result if none found.
     */
    @GET("collections/Article/records")
    Call<PocketBaseResponse<Article>> getFirstMatchingArticle(
            @Query("filter") String filter,
            @Query("expand") String expand
    );

    /**
     * Fetches a single article by its unique identifier.
     *
     * @param id The unique identifier of the article to retrieve.
     * @return A {@link Call} that represents the HTTP request for a single article.
     */
    @GET("collections/Article/records/{id}")
    Call<PocketBaseResponse<Article>> getArticleById(
            @Path("id") String id,
            @Query("expand") String expand
    );
}
