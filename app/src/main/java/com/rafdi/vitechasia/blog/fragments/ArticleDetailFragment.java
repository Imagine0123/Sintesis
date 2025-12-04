package com.rafdi.vitechasia.blog.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.rafdi.vitechasia.blog.R;
import com.rafdi.vitechasia.blog.models.Article;
import com.rafdi.vitechasia.blog.utils.BookmarkManager;
import android.widget.ScrollView;

import com.rafdi.vitechasia.blog.utils.ReadingProgressManager;
import com.rafdi.vitechasia.blog.utils.SocialInteractionManager;
import com.rafdi.vitechasia.blog.utils.ShareUtils;

/**
 * Fragment for displaying detailed view of a single article.
 * Includes bookmark functionality and navigation to related content.
 */
public class ArticleDetailFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    
    private static final String ARG_ARTICLE = "article";
    
    private ImageView articleImage;
    private TextView articleCategory;
    private TextView articleSubcategory;
    private TextView articleTitle;
    private ImageView authorImage;
    private TextView articleAuthor;
    private TextView articleDate;
    private TextView articleContent;
    private ImageButton bookmarkButton;
    private ImageButton likeButton;
    private ImageButton shareButton;
    private ImageButton bookmarkButton2;
    private TextView likeCounter;
    private TextView shareCounter;
    private TextView viewCounter;
    private TextView commentsCount;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;

    private Article article;
    private String categoryId;
    private String subcategoryId;
    private BookmarkManager bookmarkManager;
    private ReadingProgressManager readingProgressManager;
    private SocialInteractionManager socialInteractionManager;
    
    public static ArticleDetailFragment newInstance(Article article) {
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ARTICLE, article);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ReadingProgressManager
        if (getContext() != null) {
            ReadingProgressManager.initialize(getContext());
            SocialInteractionManager.initialize(getContext());
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_article_detail, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            // Initialize BookmarkManager
            bookmarkManager = BookmarkManager.getInstance(requireContext());

            // Initialize ReadingProgressManager
            readingProgressManager = ReadingProgressManager.getInstance();

            // Initialize SocialInteractionManager
            socialInteractionManager = SocialInteractionManager.getInstance();

            // Initialize views
            articleImage = view.findViewById(R.id.articleImage);
            articleCategory = view.findViewById(R.id.articleCategory);
            articleSubcategory = view.findViewById(R.id.articleSubcategory);
            articleTitle = view.findViewById(R.id.articleTitle);
            authorImage = view.findViewById(R.id.authorImage);
            articleAuthor = view.findViewById(R.id.articleAuthor);
            articleDate = view.findViewById(R.id.articleDate);
            articleContent = view.findViewById(R.id.articleContent);
            bookmarkButton = view.findViewById(R.id.bookmarkButton);
            likeButton = view.findViewById(R.id.likeButton);
            shareButton = view.findViewById(R.id.shareButton);
            likeCounter = view.findViewById(R.id.likeCounter);
            shareCounter = view.findViewById(R.id.shareCounter);
            viewCounter = view.findViewById(R.id.viewCounter);
            commentsCount = view.findViewById(R.id.commentsCount);
            bookmarkButton2 = view.findViewById(R.id.bookmarkButton2);
            scrollView = (ScrollView) view; // The root view is the ScrollView

            // Set up swipe to refresh
            swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(this);
            }

            // Set up reading progress tracking
            setupReadingProgressTracking();
            
            // Set click listeners for category and subcategory
            articleCategory.setOnClickListener(this);
            articleSubcategory.setOnClickListener(this);
            
            // Set bookmark button click listener
            bookmarkButton.setOnClickListener(v -> toggleBookmark());
            bookmarkButton2.setOnClickListener(v -> toggleBookmark());

            // Set like button click listener
            likeButton.setOnClickListener(v -> toggleLike());

            // Set share button click listener
            shareButton.setOnClickListener(v -> shareArticle());
            
            // Get article from arguments
            if (getArguments() != null) {
                article = getArguments().getParcelable(ARG_ARTICLE);
                if (article != null) {
                    // Store category and subcategory IDs for navigation
                    categoryId = article.getCategoryId();
                    subcategoryId = article.getSubcategoryId();
                    // Sync bookmark status
                    bookmarkManager.syncArticleBookmarkStatus(article);

                    // Initialize social interaction state
                    socialInteractionManager.initializeArticleSocialState(article);

                    updateUI();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }
    
    // Simple method to get current article
    public Article getCurrentArticle() {
        return article;
    }
    
    // Safe method to update UI with null checks
    private void updateUI() {
        if (getView() == null || article == null) return;
        
        try {
            // Set title
            if (article.getTitle() != null) {
                articleTitle.setText(article.getTitle());
            }
            
            // Set content
            if (article.getContent() != null) {
                articleContent.setText(article.getContent());
            }
            
            // Set author name
            if (article.getAuthorName() != null) {
                articleAuthor.setText(article.getAuthorName());
            }
            
            // Set category
            if (article.getCategory() != null) {
                String category = article.getCategory();
                // Format category: first letter uppercase, rest lowercase
                String formattedCategory = category.substring(0, 1).toUpperCase() + 
                                         (category.length() > 1 ? category.substring(1).toLowerCase() : "");
                articleCategory.setText(formattedCategory);
                articleCategory.setVisibility(View.VISIBLE);
                articleCategory.setClickable(true);
                articleCategory.setTextColor(getResources().getColor(R.color.primary, null));
                articleCategory.setPaintFlags(articleCategory.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            } else {
                articleCategory.setVisibility(View.GONE);
            }
            
            // Set subcategory if available
            String subcategory = article.getSubcategoryDisplayName();
            if (subcategory != null && !subcategory.isEmpty()) {
                // Format subcategory: first letter uppercase, rest lowercase
                String formattedSubcategory = subcategory.substring(0, 1).toUpperCase() + 
                                           (subcategory.length() > 1 ? subcategory.substring(1).toLowerCase() : "");
                articleSubcategory.setText(formattedSubcategory);
                articleSubcategory.setVisibility(View.VISIBLE);
                articleSubcategory.setClickable(true);
                articleSubcategory.setTextColor(getResources().getColor(R.color.primary, null));
                articleSubcategory.setPaintFlags(articleSubcategory.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            } else {
                articleSubcategory.setVisibility(View.GONE);
            }
            
            // Set date
            if (article.getPublishDate() != null) {
                articleDate.setText(article.getFormattedDate());
            }
            
            // Load article image
            if (article.getImageUrl() != null && !article.getImageUrl().isEmpty()) {
                Glide.with(requireContext())
                    .load(article.getImageUrl())
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .into(articleImage);
            }
            
            // Load author image
            if (article.getAuthorImageUrl() != null && !article.getAuthorImageUrl().isEmpty()) {
                Glide.with(requireContext())
                    .load(article.getAuthorImageUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(authorImage);
            }
            
            // Update bookmark button icon
            updateBookmarkButton();

            // Update social interaction UI
            updateSocialInteractionUI();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void toggleBookmark() {
        if (article == null) return;
        
        bookmarkManager.toggleBookmark(article);
        updateBookmarkButton();
        
        // Optional: Show a toast message
        String message = article.isBookmarked() ? "Added to bookmarks" : "Removed from bookmarks";
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void updateBookmarkButton() {
        if (article == null || bookmarkButton == null) return;
        
        if (article.isBookmarked()) {
            bookmarkButton.setImageResource(R.drawable.ic_star_filled);
            bookmarkButton2.setImageResource(R.drawable.ic_star_filled);
        } else {
            bookmarkButton.setImageResource(R.drawable.ic_star_outline);
            bookmarkButton2.setImageResource(R.drawable.ic_star_outline);
        }
    }

    private void toggleLike() {
        if (article == null || socialInteractionManager == null) return;

        boolean isLikedNow = socialInteractionManager.toggleLike(article);
        updateSocialInteractionUI();

        // Show animation and feedback
        if (likeButton != null) {
            likeButton.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction(() -> {
                    likeButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .start();
                })
                .start();
        }

        // Optional: Show toast message
        String message = isLikedNow ? "Liked!" : "Like removed";
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void shareArticle() {
        if (article == null) return;

        // Record the share action
        socialInteractionManager.recordShare(article);

        // Show share options
        ShareUtils.shareArticle(requireContext(), article);

        // Update UI after sharing
        updateSocialInteractionUI();
    }

    private void updateSocialInteractionUI() {
        if (article == null) return;

        // Update like button
        if (likeButton != null) {
            if (article.isLikedByUser()) {
                likeButton.setImageResource(R.drawable.ic_thumb_up_filled);
                likeButton.setColorFilter(getResources().getColor(R.color.primary, null));
            } else {
                likeButton.setImageResource(R.drawable.ic_thumb_up_outline);
                likeButton.clearColorFilter();
            }
        }

        // Update bookmark button
        updateBookmarkButton();

        // Update counters
        if (likeCounter != null) {
            likeCounter.setText(String.valueOf(article.getLikeCount()));
        }

        if (shareCounter != null) {
            shareCounter.setText(String.valueOf(article.getShareCount()));
        }

        if (viewCounter != null) {
            String viewsText = getString(R.string.views_count, article.getViewCount());
            viewCounter.setText(viewsText);
        }

        if (commentsCount != null) {
            String commentsText = article.getCommentCount() > 0
                ? getString(R.string.likes_count, article.getCommentCount())
                : getString(R.string.no_comments);
            commentsCount.setText(commentsText);
        }
    }
    
    private void setupReadingProgressTracking() {
        if (scrollView == null || article == null) return;

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (scrollView.getChildCount() > 0) {
                View child = scrollView.getChildAt(0);
                int scrollY = scrollView.getScrollY();
                int height = child.getHeight() - scrollView.getHeight();

                if (height > 0) {
                    int progress = (int) ((float) scrollY / height * 100);
                    progress = Math.max(0, Math.min(100, progress));

                    // Save progress every 10% or when significant progress is made
                    if (progress % 10 == 0 || progress == 100) {
                        readingProgressManager.saveReadingProgress(article, progress);
                    }
                }
            }
        });
    }
    
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.articleCategory) {
            // Navigate to CategoryFragment
            if (categoryId != null && getActivity() != null) {
                Fragment categoryFragment = CategoryFragment.newInstance(categoryId);
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, categoryFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        } else if (v.getId() == R.id.articleSubcategory) {
            // Navigate to SubcategoryFragment
            if (categoryId != null && subcategoryId != null && getActivity() != null) {
                String categoryName = article != null ? article.getCategory() : "";
                Fragment subcategoryFragment = SubcategoryFragment.newInstance(categoryName, subcategoryId);
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, subcategoryFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        }
    }
    
    @Override
    public void onRefresh() {
        // Implement pull-to-refresh if needed
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        // Refresh article content if needed
        updateUI();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Save current reading progress when leaving the fragment
        if (readingProgressManager != null && article != null) {
            // Calculate current progress based on scroll position
            if (scrollView != null && scrollView.getChildCount() > 0) {
                View child = scrollView.getChildAt(0);
                int scrollY = scrollView.getScrollY();
                int height = child.getHeight() - scrollView.getHeight();

                if (height > 0) {
                    int progress = (int) ((float) scrollY / height * 100);
                    progress = Math.max(0, Math.min(100, progress));
                    readingProgressManager.saveReadingProgress(article, progress);
                }
            }
        }
    }
}
