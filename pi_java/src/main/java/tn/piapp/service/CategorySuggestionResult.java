package tn.piapp.service;

import tn.piapp.model.Category;

/**
 * Immutable result of a category suggestion computation.
 * category is null and confidence is 0 when no match was found.
 */
public class CategorySuggestionResult {

    private final Category category;  // null if no match
    private final int confidence;     // 0–100; 0 means no match

    public CategorySuggestionResult(Category category, int confidence) {
        this.category   = category;
        this.confidence = confidence;
    }

    public Category getCategory()  { return category; }
    public int getConfidence()     { return confidence; }
    public boolean hasMatch()      { return category != null && confidence > 0; }
}
