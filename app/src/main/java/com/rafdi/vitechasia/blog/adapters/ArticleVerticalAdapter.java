package com.rafdi.vitechasia.blog.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.rafdi.vitechasia.blog.R;
import com.rafdi.vitechasia.blog.models.Article;

import java.util.List;

/**
 * RecyclerView adapter for displaying articles in vertical layout.
 * Now extends BaseArticleAdapter to reduce code duplication and improve maintainability.
 */
public class ArticleVerticalAdapter extends BaseArticleAdapter<ArticleVerticalAdapter.ArticleViewHolder> {

    public ArticleVerticalAdapter(List<Article> articleList, OnArticleClickListener listener) {
        super(listener);
        setArticles(articleList);
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ArticleViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_article_vertical, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        Article article = articles.get(position);
        // Use base adapter's common binding
        bindArticleData(holder, article);

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onArticleClick(article);
            }
        });
    }

    public static class ArticleViewHolder extends BaseArticleAdapter.ArticleViewHolder {
        public ArticleViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
        }
    }
}
