package com.rafdi.vitechasia.blog.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rafdi.vitechasia.blog.databinding.ItemCategoryFilterBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying category filter options in a RecyclerView.
 */
public class CategoryFilterAdapter extends RecyclerView.Adapter<CategoryFilterAdapter.ViewHolder> {
    private final List<String> categories = new ArrayList<>();
    private final List<String> selectedCategories = new ArrayList<>();
    private OnCategorySelectListener listener;

    public interface OnCategorySelectListener {
        void onCategorySelected(String category, boolean isSelected);
    }

    public CategoryFilterAdapter(OnCategorySelectListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<String> categories) {
        this.categories.clear();
        if (categories != null) {
            this.categories.addAll(categories);
        }
        notifyDataSetChanged();
    }

    public void setSelectedCategories(List<String> selectedCategories) {
        this.selectedCategories.clear();
        if (selectedCategories != null) {
            this.selectedCategories.addAll(selectedCategories);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCategoryFilterBinding binding = ItemCategoryFilterBinding.inflate(
                inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = categories.get(position);
        boolean isSelected = selectedCategories.contains(category);
        holder.bind(category, isSelected);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategoryFilterBinding binding;

        ViewHolder(ItemCategoryFilterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String category, boolean isSelected) {
            binding.chipCategory.setText(category);
            binding.chipCategory.setChecked(isSelected);
            
            binding.chipCategory.setOnCheckedChangeListener((buttonView, checked) -> {
                if (listener != null) {
                    listener.onCategorySelected(category, checked);
                }
            });
        }
    }
}