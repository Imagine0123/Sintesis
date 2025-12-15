package com.rafdi.vitechasia.blog.api;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class PocketBaseResponse<T> {
    @SerializedName("items")
    private List<T> items;

    @SerializedName("page")
    private int page;

    @SerializedName("perPage")
    private int perPage;

    @SerializedName("totalItems")
    private int totalItems;

    @SerializedName("totalPages")
    private int totalPages;

    // Getters and setters
    public List<T> getItems() {
        return items != null ? items : new ArrayList<>();
    }

    public int getPage() {
        return page;
    }

    public int getPerPage() {
        return perPage;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }
}