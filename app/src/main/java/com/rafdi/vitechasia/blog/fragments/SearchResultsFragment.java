package com.rafdi.vitechasia.blog.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.rafdi.vitechasia.blog.R;
import com.rafdi.vitechasia.blog.activities.HomePage;
import com.rafdi.vitechasia.blog.adapters.ArticleVerticalAdapter;
import com.rafdi.vitechasia.blog.models.Article;
import com.rafdi.vitechasia.blog.models.Category;
import com.rafdi.vitechasia.blog.utils.DataHandler;
import com.rafdi.vitechasia.blog.utils.SearchHistoryManager;
import com.rafdi.vitechasia.blog.utils.CategoryManager;
import com.rafdi.vitechasia.blog.utils.PaginationUtils;
import com.rafdi.vitechasia.blog.utils.ErrorHandler;
import com.rafdi.vitechasia.blog.utils.RetryWithBackoff;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for displaying search results based on user queries.
 * Shows filtered articles with category-based search capabilities and search history.
 */
public class SearchResultsFragment extends Fragment implements ArticleVerticalAdapter.OnArticleClickListener {
    private static final String ARG_QUERY = "search_query";

    private String searchQuery;
    private String selectedCategory = null;
    private String selectedSubcategory = null;
    private SortBy currentSortBy = SortBy.RELEVANCE;
    private DateRange currentDateRange = DateRange.ALL_TIME;
    
    private enum SortBy {
        RELEVANCE,
        DATE_NEWEST,
        DATE_OLDEST,
        POPULARITY
    }
    
    private enum DateRange {
        ALL_TIME,
        LAST_24_HOURS,
        LAST_WEEK,
        LAST_MONTH,
        LAST_YEAR
    }
    private RecyclerView resultsRecyclerView;
    private ArticleVerticalAdapter verticalAdapter;
    private TextView noResultsText;
    private TextView searchQueryText;
    private ChipGroup chipGroupFilters;
    private LinearLayout searchHistoryLayout;
    private static final int ITEMS_PER_PAGE = 10;
    
    private List<Article> allSearchResults = new ArrayList<>();
    private SearchHistoryManager searchHistoryManager;
    private CategoryManager categoryManager;
    private TextInputLayout dateRangeLayout;
    private Button btnLoadMore;
    private ProgressBar progressBar;
    private PaginationUtils<Article> paginationUtils;

    public static SearchResultsFragment newInstance(String query) {
        SearchResultsFragment fragment = new SearchResultsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            searchQuery = getArguments().getString(ARG_QUERY);
        }

        // Initialize DataHandler to enable API-first functionality
        DataHandler.initialize(requireContext().getApplicationContext());

        // Initialize managers
        searchHistoryManager = SearchHistoryManager.getInstance(requireContext());
        categoryManager = CategoryManager.getInstance();

        // Add to search history
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            searchHistoryManager.addSearchQuery(searchQuery);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_results, container, false);

        // Initialize views
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView);
        noResultsText = view.findViewById(R.id.noResultsText);
        searchQueryText = view.findViewById(R.id.searchQueryText);
        chipGroupFilters = view.findViewById(R.id.chip_group_filters);
        searchHistoryLayout = view.findViewById(R.id.searchHistoryLayout);
        btnLoadMore = view.findViewById(R.id.btn_load_more);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // Set up load more button
        btnLoadMore.setOnClickListener(v -> loadNextPage());

        // Set search query text
        if (searchQueryText != null && searchQuery != null) {
            searchQueryText.setText(getString(R.string.search_results_for, searchQuery));
        }

        view.findViewById(R.id.btn_filter).setOnClickListener(v -> showFilterDialog());

        // Setup RecyclerView
        verticalAdapter = new ArticleVerticalAdapter(new ArrayList<>(), this);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultsRecyclerView.setAdapter(verticalAdapter);

        // Display search history if no query
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            displaySearchHistory();
        } else {
            // Perform initial search
            performSearch();
        }

        return view;
    }

    @Override
    public void onArticleClick(Article article) {
        if (getActivity() instanceof HomePage) {
            ((HomePage) getActivity()).loadArticleDetailFragment(article);
        }
    }

    private void displaySearchHistory() {
        if (searchHistoryLayout == null) return;

        searchHistoryLayout.removeAllViews();
        List<String> history = searchHistoryManager.getSearchHistory();

        if (history.isEmpty()) {
            noResultsText.setVisibility(View.VISIBLE);
            noResultsText.setText("No search history available");
            resultsRecyclerView.setVisibility(View.GONE);
            btnLoadMore.setVisibility(View.GONE);
            return;
        }

        noResultsText.setVisibility(View.GONE);
        resultsRecyclerView.setVisibility(View.GONE);

        // Add history items
        for (String query : history) {
            Chip chip = new Chip(requireContext());
            chip.setText(query);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> {
                // Navigate to new search results
                if (getActivity() instanceof HomePage) {
                    ((HomePage) getActivity()).performSearch(query);
                }
            });
            searchHistoryLayout.addView(chip);
        }
    }

    private void performSearch() {
        Log.d("SearchResultsFragment", "performSearch called with query: " + searchQuery);
        
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            Log.d("SearchResultsFragment", "Empty search query, showing search history");
            displaySearchHistory();
            return;
        }

        Log.d("SearchResultsFragment", "Fetching all articles to filter by query: " + searchQuery);
        
        // Show loading state
        showLoading(true);
        
        // Use RetryWithBackoff for the API call
        RetryWithBackoff<Object> retryWithBackoff = new RetryWithBackoff<>(requireContext());
        retryWithBackoff.execute(
            () -> {
                // This runs on a background thread
                final List<Article>[] result = new List[1];
                final Object lock = new Object();
                
                DataHandler.getInstance().getAllArticles(new DataHandler.DataLoadListener() {
                    @Override
                    public void onDataLoaded(List<Article> articles) {
                        synchronized (lock) {
                            result[0] = articles;
                            lock.notify();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        synchronized (lock) {
                            result[0] = null;
                            lock.notify();
                        }
                    }
                });
                
                // Wait for the callback to complete
                synchronized (lock) {
                    try {
                        lock.wait(10000); // 10 second timeout
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
                
                if (result[0] == null) {
                    throw new Exception("Failed to load articles");
                }
                
                return result[0];
            },
            new RetryWithBackoff.Callback<Object>() {
                @Override
                public void onSuccess(Object result) {
                    if (result instanceof List) {
                        handleSearchResults((List<Article>) result);
                    } else {
                        handleSearchError(new Exception("Unexpected result type"));
                    }
                }

                @Override
                public void onError(String errorMessage, boolean isNetworkError) {
                    handleSearchError(new Exception(errorMessage));
                }
            }
        );
    }

    private void handleSearchError(Throwable throwable) {
        Log.e("SearchResultsFragment", "Search failed", throwable);
        requireActivity().runOnUiThread(() -> {
            showLoading(false);
            
            // Use ErrorHandler to get a user-friendly error message
            int errorResId = ErrorHandler.handleError(requireContext(), throwable);
            String errorMessage = getString(errorResId);
            
            // Show error with retry option
            showErrorWithRetry(errorMessage);
        });
    }
    
    private void handleSearchResults(List<Article> articles) {
        Log.d("SearchResultsFragment", "Processing " + (articles != null ? articles.size() : 0) + " articles");
        
        try {
            // Filter articles based on search query
            allSearchResults = new ArrayList<>();
            int matchCount = 0;
            
            if (articles != null) {
                for (Article article : articles) {
                    if (matchesSearchQuery(article, searchQuery)) {
                        allSearchResults.add(article);
                        matchCount++;
                    }
                }
            }
            
            Log.d("SearchResultsFragment", "Found " + matchCount + " articles matching query");
            
            requireActivity().runOnUiThread(() -> {
                applyFilters();
                showLoading(false);
            });
        } catch (Exception e) {
            Log.e("SearchResultsFragment", "Error processing search results", e);
            handleSearchError(e);
        }
    }
    
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLoadMore.setVisibility(View.GONE);
            noResultsText.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }
    
    private void showErrorWithRetry(String errorMessage) {
        resultsRecyclerView.setVisibility(View.GONE);
        noResultsText.setVisibility(View.VISIBLE);
        noResultsText.setText(errorMessage);
        noResultsText.setOnClickListener(v -> performSearch());
        
        // Show a snackbar with the error message and retry button
        if (getView() != null) {
            Snackbar.make(
                getView(),
                errorMessage,
                Snackbar.LENGTH_INDEFINITE
            )
            .setAction(R.string.error_load_retry, v -> performSearch())
            .setActionTextColor(getResources().getColor(R.color.primary, null))
            .show();
        }
    }

    private boolean matchesSearchQuery(Article article, String query) {
        if (article == null || query == null) {
            Log.d("SearchResultsFragment", "matchesSearchQuery: article or query is null");
            return false;
        }

        String lowerQuery = query.toLowerCase();
        boolean matches = (article.getTitle() != null && article.getTitle().toLowerCase().contains(lowerQuery)) ||
               (article.getContent() != null && article.getContent().toLowerCase().contains(lowerQuery)) ||
               (article.getAuthorName() != null && article.getAuthorName().toLowerCase().contains(lowerQuery));
        
        if (matches) {
            Log.d("SearchResultsFragment", "Match found in article: " + article.getTitle());
            Log.d("SearchResultsFragment", "  Title: " + article.getTitle());
            Log.d("SearchResultsFragment", "  Content: " + (article.getContent() != null ? "[content available]" : "null"));
            Log.d("SearchResultsFragment", "  Author: " + article.getAuthorName());
        }
        
        return matches;
    }

    private void applyFilters() {
        if (allSearchResults == null || allSearchResults.isEmpty()) {
            showNoResults();
            return;
        }
        
        // Initialize or update pagination
        if (paginationUtils == null) {
            paginationUtils = new PaginationUtils<>(allSearchResults, ITEMS_PER_PAGE);
        } else {
            paginationUtils.updateData(allSearchResults);
        }

        List<Article> filteredResults = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long timeRange = getTimeRangeInMillis(currentDateRange);
        
        // Apply category and subcategory filters
        for (Article article : allSearchResults) {
            boolean matchesCategory = selectedCategory == null ||
                (article.getCategoryId() != null && article.getCategoryId().equalsIgnoreCase(selectedCategory));

            boolean matchesSubcategory = selectedSubcategory == null ||
                (article.getSubcategoryId() != null && article.getSubcategoryId().equalsIgnoreCase(selectedSubcategory));
                
            boolean matchesDateRange = currentDateRange == DateRange.ALL_TIME || 
                (article.getPublishDate() != null && 
                 (currentTime - article.getPublishDate().getTime()) <= timeRange);

            if (matchesCategory && matchesSubcategory && matchesDateRange) {
                filteredResults.add(article);
            }
        }
        
        // Apply sorting
        if (currentSortBy == SortBy.DATE_NEWEST) {
            filteredResults.sort((a1, a2) -> {
                if (a1.getPublishDate() == null || a2.getPublishDate() == null) return 0;
                return a2.getPublishDate().compareTo(a1.getPublishDate());
            });
        } else if (currentSortBy == SortBy.DATE_OLDEST) {
            filteredResults.sort((a1, a2) -> {
                if (a1.getPublishDate() == null || a2.getPublishDate() == null) return 0;
                return a1.getPublishDate().compareTo(a2.getPublishDate());
            });
        } else if (currentSortBy == SortBy.POPULARITY) {
            filteredResults.sort((a1, a2) -> Integer.compare(a2.getViewCount(), a1.getViewCount()));
        }
        
        // Update filter chips
        updateFilterChips();

        // Update UI
        requireActivity().runOnUiThread(() -> {
            if (filteredResults.isEmpty()) {
                showNoResults();
                btnLoadMore.setVisibility(View.GONE);
            } else {
                updateArticleList();
                updateLoadMoreButton();
            }
            updateFilterChips();
        });
    }

    private void showFilterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_filters, null);
        
        // Initialize views
        RadioGroup sortByGroup = dialogView.findViewById(R.id.sortByGroup);
        AutoCompleteTextView dateRangeSpinner = dialogView.findViewById(R.id.dateRangeSpinner);
        Button btnApply = dialogView.findViewById(R.id.btnApply);
        Button btnReset = dialogView.findViewById(R.id.btnReset);
        ChipGroup categoryChipGroup = dialogView.findViewById(R.id.category_chip_group);
        ChipGroup subcategoryChipGroup = dialogView.findViewById(R.id.subcategory_chip_group);
        View subcategoriesLabel = dialogView.findViewById(R.id.subcategories_label);
        dateRangeLayout = dialogView.findViewById(R.id.dateRangeLayout);

        // Set up sort by radio group
        switch (currentSortBy) {
            case DATE_NEWEST:
                sortByGroup.check(R.id.sortByDateNewest);
                break;
            case DATE_OLDEST:
                sortByGroup.check(R.id.sortByDateOldest);
                break;
            case POPULARITY:
                sortByGroup.check(R.id.sortByPopularity);
                break;
            default:
                sortByGroup.check(R.id.sortByRelevance);
        }
        
        // Set up categories
        List<Category> categories = DataHandler.getAllCategories();
        categoryChipGroup.removeAllViews();
        
        // Add "All Categories" option
        Chip allChip = new Chip(requireContext());
        allChip.setText("All Categories");
        allChip.setCheckable(true);
        allChip.setChecked(selectedCategory == null);
        allChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedCategory = null;
                selectedSubcategory = null;
                subcategoryChipGroup.setVisibility(View.GONE);
                subcategoriesLabel.setVisibility(View.GONE);
                
                // Uncheck other chips
                for (int i = 0; i < categoryChipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) categoryChipGroup.getChildAt(i);
                    if (chip != buttonView) {
                        chip.setChecked(false);
                    }
                }
            }
        });
        categoryChipGroup.addView(allChip);
        
        // Add category chips
        for (Category category : categories) {
            if (category == null) continue;
            
            Chip chip = new Chip(requireContext());
            chip.setText(category.getName());
            chip.setCheckable(true);
            chip.setChecked(category.getId().equals(selectedCategory));
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Uncheck "All Categories" and other chips
                    for (int i = 0; i < categoryChipGroup.getChildCount(); i++) {
                        Chip c = (Chip) categoryChipGroup.getChildAt(i);
                        if (c != buttonView) {
                            c.setChecked(false);
                        }
                    }
                    
                    selectedCategory = category.getId();
                    selectedSubcategory = null; // Reset subcategory when category changes
                    updateSubcategoryChips(subcategoryChipGroup, subcategoriesLabel, category.getId());
                } else if (selectedCategory != null && selectedCategory.equals(category.getId())) {
                    // If the currently selected category is unchecked, select "All Categories"
                    allChip.setChecked(true);
                    selectedCategory = null;
                    selectedSubcategory = null;
                    subcategoryChipGroup.setVisibility(View.GONE);
                    subcategoriesLabel.setVisibility(View.GONE);
                }
            });
            
            categoryChipGroup.addView(chip);
        }
        
        // Set up subcategories if a category is already selected
        if (selectedCategory != null) {
            updateSubcategoryChips(subcategoryChipGroup, subcategoriesLabel, selectedCategory);
        }
        
        // Set up date range spinner
        String[] dateRanges = getResources().getStringArray(R.array.date_ranges);
        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                dateRanges
        );
        dateRangeSpinner.setAdapter(dateAdapter);
        dateRangeSpinner.setText(dateRanges[currentDateRange.ordinal()], false);
        
        // Handle date range selection
        dateRangeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            currentDateRange = DateRange.values()[position];
            dateRangeSpinner.dismissDropDown();
        });
        
        // Set up dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        
        // Make the date range input clickable
        dateRangeSpinner.setFocusable(false);
        dateRangeSpinner.setClickable(true);
        dateRangeSpinner.setOnClickListener(v -> dateRangeSpinner.showDropDown());
        
        // Also make the entire TextInputLayout clickable
        dateRangeLayout.setEndIconOnClickListener(v -> dateRangeSpinner.showDropDown());
        dateRangeLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        
        btnApply.setOnClickListener(v -> {
            // Update sort by
            int selectedSortId = sortByGroup.getCheckedRadioButtonId();
            if (selectedSortId == R.id.sortByDateNewest) {
                currentSortBy = SortBy.DATE_NEWEST;
            } else if (selectedSortId == R.id.sortByDateOldest) {
                currentSortBy = SortBy.DATE_OLDEST;
            } else if (selectedSortId == R.id.sortByPopularity) {
                currentSortBy = SortBy.POPULARITY;
            } else {
                currentSortBy = SortBy.RELEVANCE;
            }
            
            applyFilters();
            dialog.dismiss();
        });
        
        btnReset.setOnClickListener(v -> {
            // Reset all filters
            currentSortBy = SortBy.RELEVANCE;
            currentDateRange = DateRange.ALL_TIME;
            selectedCategory = null;
            selectedSubcategory = null;
            
            // Reset UI
            sortByGroup.check(R.id.sortByRelevance);
            dateRangeSpinner.setText(dateRanges[0], false);
            
            // Reset chips
            for (int i = 0; i < categoryChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) categoryChipGroup.getChildAt(i);
                chip.setChecked(i == 0); // Only check "All Categories"
            }
            
            subcategoryChipGroup.setVisibility(View.GONE);
            subcategoriesLabel.setVisibility(View.GONE);
            
            applyFilters();
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void updateSubcategoryChips(ChipGroup subcategoryChipGroup, View subcategoriesLabel, String category) {
        if (subcategoryChipGroup == null || subcategoriesLabel == null) return;

        subcategoryChipGroup.removeAllViews();
        List<String> subcategories = DataHandler.getSubcategoriesForCategory(category);

        if (subcategories != null && !subcategories.isEmpty()) {
            subcategoriesLabel.setVisibility(View.VISIBLE);
            subcategoryChipGroup.setVisibility(View.VISIBLE);

            for (String subcategory : subcategories) {
                if (subcategory == null) continue;

                // Capitalize the first letter of the subcategory
                String displayName = subcategory.substring(0, 1).toUpperCase() +
                                  (subcategory.length() > 1 ? subcategory.substring(1) : "");

                Chip chip = new Chip(requireContext());
                chip.setText(displayName);
                chip.setCheckable(true);
                chip.setChecked(displayName.equals(selectedSubcategory));
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedSubcategory = displayName;
                    }
                });
                subcategoryChipGroup.addView(chip);
            }
        } else {
            subcategoriesLabel.setVisibility(View.GONE);
            subcategoryChipGroup.setVisibility(View.GONE);
        }
    }

    private long getTimeRangeInMillis(DateRange dateRange) {
        switch (dateRange) {
            case LAST_24_HOURS:
                return 24 * 60 * 60 * 1000L;
            case LAST_WEEK:
                return 7 * 24 * 60 * 60 * 1000L;
            case LAST_MONTH:
                return 30L * 24 * 60 * 60 * 1000L;
            case LAST_YEAR:
                return 365L * 24 * 60 * 60 * 1000L;
            case ALL_TIME:
            default:
                return Long.MAX_VALUE;
        }
    }
    
    private String getSortByText(SortBy sortBy) {
        switch (sortBy) {
            case DATE_NEWEST:
                return getString(R.string.sort_date_newest);
            case DATE_OLDEST:
                return getString(R.string.sort_date_oldest);
            case POPULARITY:
                return getString(R.string.sort_popularity);
            case RELEVANCE:
            default:
                return getString(R.string.sort_relevance);
        }
    }
    
    private String getDateRangeText(DateRange dateRange) {
        String[] dateRanges = getResources().getStringArray(R.array.date_ranges);
        return dateRanges[dateRange.ordinal()];
    }

    private void updateFilterChips() {
        if (chipGroupFilters == null) return;

        chipGroupFilters.removeAllViews();

        // Add sort filter chip if not default
        if (currentSortBy != SortBy.RELEVANCE) {
            addFilterChip(getSortByText(currentSortBy), v -> {
                currentSortBy = SortBy.RELEVANCE;
                applyFilters();
            });
        }
        
        // Add date range filter chip if not default
        if (currentDateRange != DateRange.ALL_TIME) {
            addFilterChip(getDateRangeText(currentDateRange), v -> {
                currentDateRange = DateRange.ALL_TIME;
                applyFilters();
            });
        }

        // Add category filter chip
        if (selectedCategory != null) {
            String displayName = getDisplayNameForCategory(selectedCategory);
            if (displayName != null) {
                addFilterChip(displayName, v -> {
                    selectedCategory = null;
                    selectedSubcategory = null;
                    applyFilters();
                });
            }
        }

        // Add subcategory filter chip
        if (selectedSubcategory != null) {
            addFilterChip(selectedSubcategory, v -> {
                selectedSubcategory = null;
                applyFilters();
            });
        }
    }

    private String getDisplayNameForCategory(String categoryId) {
        if (categoryId == null) return "";
        for (Category category : DataHandler.getAllCategories()) {
            if (categoryId.equalsIgnoreCase(category.getId())) {
                return category.getName();
            }
        }
        return categoryId;
    }

    private void addFilterChip(String text, View.OnClickListener onCloseClick) {
        if (chipGroupFilters == null || text == null) return;

        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.primary);
        chip.setTextColor(getResources().getColor(R.color.on_primary, requireContext().getTheme()));
        chip.setCloseIconTintResource(R.color.on_primary);

        chip.setOnCloseIconClickListener(v -> {
            if (onCloseClick != null) {
                onCloseClick.onClick(v);
            }
            chipGroupFilters.removeView(chip);
        });

        chipGroupFilters.addView(chip);
    }

    private void updateArticleList() {
        if (paginationUtils != null) {
            List<Article> currentPage = paginationUtils.getCurrentPageItems();
            if (verticalAdapter == null) {
                verticalAdapter = new ArticleVerticalAdapter(currentPage, this);
                resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                resultsRecyclerView.setAdapter(verticalAdapter);
            } else {
                verticalAdapter.setArticles(currentPage);
            }
            resultsRecyclerView.setVisibility(View.VISIBLE);
            noResultsText.setVisibility(View.GONE);
        }
    }
    
    private void updateLoadMoreButton() {
        if (paginationUtils != null) {
            btnLoadMore.setVisibility(paginationUtils.hasNextPage() ? View.VISIBLE : View.GONE);
        } else {
            btnLoadMore.setVisibility(View.GONE);
        }
    }
    
    private void loadNextPage() {
        if (paginationUtils != null && paginationUtils.hasNextPage()) {
            paginationUtils.loadNextPage();
            updateArticleList();
            updateLoadMoreButton();
        }
    }
    
    private void showResults(List<Article> results) {
        updateArticleList();
    }

    private void showNoResults() {
        Log.d("SearchResultsFragment", "showNoResults called");
        
        if (noResultsText == null) {
            Log.e("SearchResultsFragment", "Cannot show no results: noResultsText is null");
            return;
        }
        if (resultsRecyclerView == null) {
            Log.e("SearchResultsFragment", "resultsRecyclerView is null in showNoResults");
        }
        
        noResultsText.setVisibility(View.VISIBLE);
        if (resultsRecyclerView != null) {
            resultsRecyclerView.setVisibility(View.GONE);
        }
        
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String message = getString(R.string.no_search_results, searchQuery);
            Log.d("SearchResultsFragment", message);
            noResultsText.setText(message);
        } else {
            Log.d("SearchResultsFragment", "No search query provided");
            noResultsText.setText(R.string.no_articles_found);
        }
    }
}
