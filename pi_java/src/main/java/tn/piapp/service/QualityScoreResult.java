package tn.piapp.service;

import java.util.List;

/**
 * Immutable result of a quality score computation.
 * Score is 0–100; percentage equals score (already out of 100).
 */
public class QualityScoreResult {

    private final int score;
    private final int percentage;
    private final String rating;           // "Excellent" | "Good" | "Needs Improvement"
    private final List<String> suggestions; // improvement hints for unmet criteria
    private final List<String> checklist;  // "✓ Has image" / "✗ No image" etc.

    public QualityScoreResult(int score, String rating,
                               List<String> suggestions, List<String> checklist) {
        this.score = score;
        this.percentage = score; // score is already out of 100
        this.rating = rating;
        this.suggestions = List.copyOf(suggestions);
        this.checklist = List.copyOf(checklist);
    }

    public int getScore()               { return score; }
    public int getPercentage()          { return percentage; }
    public String getRating()           { return rating; }
    public List<String> getSuggestions(){ return suggestions; }
    public List<String> getChecklist()  { return checklist; }
}
