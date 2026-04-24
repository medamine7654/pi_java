package tn.piapp.service;

import tn.piapp.model.Category;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure, stateless service that suggests a category based on keyword matching.
 * No database access; no JavaFX imports.
 *
 * The keyword map covers both tool and service categories.
 * IMPORTANT: suggest() only scores against the candidates list passed in —
 * keyword map entries whose category name does not match any candidate are ignored.
 * This prevents a tool form from ever suggesting a service category.
 *
 * Confidence = round((matchedKeywordCount / totalKeywordsForCategory) * 100)
 * Matching is case-insensitive substring search.
 */
public class CategorySuggestionService {

    private static final Map<String, List<String>> KEYWORD_MAP = new LinkedHashMap<>();

    static {
        KEYWORD_MAP.put("Plumbing",           List.of("plumb", "pipe", "leak", "drain", "faucet", "toilet", "sink", "water"));
        KEYWORD_MAP.put("Electrical",         List.of("electric", "wire", "light", "outlet", "switch", "circuit", "power"));
        KEYWORD_MAP.put("Gardening",          List.of("garden", "lawn", "plant", "tree", "grass", "hedge", "landscape"));
        KEYWORD_MAP.put("Cleaning",           List.of("clean", "wash", "mop", "vacuum", "dust", "sanitize", "tidy"));
        KEYWORD_MAP.put("Painting",           List.of("paint", "brush", "wall", "color", "coat", "decor"));
        KEYWORD_MAP.put("Moving",             List.of("move", "transport", "carry", "relocate", "delivery", "haul"));
        KEYWORD_MAP.put("Tutoring",           List.of("tutor", "teach", "lesson", "study", "learn", "education", "homework"));
        KEYWORD_MAP.put("IT Support",         List.of("computer", "laptop", "software", "tech", "repair", "install", "network"));
        KEYWORD_MAP.put("Power Tools",        List.of("drill", "saw", "grinder", "sander", "electric tool"));
        KEYWORD_MAP.put("Hand Tools",         List.of("hammer", "screwdriver", "wrench", "pliers", "manual tool"));
        KEYWORD_MAP.put("Garden Tools",       List.of("mower", "trimmer", "rake", "shovel", "hoe", "pruner"));
        KEYWORD_MAP.put("Ladders",            List.of("ladder", "step", "scaffold", "height", "climb"));
        KEYWORD_MAP.put("Cleaning Equipment", List.of("vacuum", "pressure washer", "carpet cleaner", "steam"));
        KEYWORD_MAP.put("Measuring Tools",    List.of("measure", "level", "tape", "ruler", "laser"));
        KEYWORD_MAP.put("Outdoor Equipment",  List.of("tent", "camping", "bbq", "grill", "outdoor"));
        KEYWORD_MAP.put("Party Equipment",    List.of("party", "event", "chair", "table", "decoration", "tent"));
    }

    /**
     * Suggests the best-matching category from the candidates list.
     *
     * @param text       free text (name + description combined); may be null or empty
     * @param candidates the categories to score against (e.g. only tool or only service categories)
     * @return best match with confidence, or empty result if no keywords match
     */
    public CategorySuggestionResult suggest(String text, List<Category> candidates) {
        if (text == null || text.isBlank() || candidates == null || candidates.isEmpty()) {
            return new CategorySuggestionResult(null, 0);
        }

        String lower = text.toLowerCase();
        Category bestCategory   = null;
        int      bestConfidence = 0;

        for (Category candidate : candidates) {
            List<String> keywords = KEYWORD_MAP.get(candidate.getName());
            if (keywords == null) continue; // no keyword entry for this category name

            int matched = 0;
            for (String kw : keywords) {
                if (lower.contains(kw.toLowerCase())) matched++;
            }

            if (matched > 0) {
                int confidence = (int) Math.round((double) matched / keywords.size() * 100);
                if (confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestCategory   = candidate;
                }
            }
        }

        return new CategorySuggestionResult(bestCategory, bestConfidence);
    }
}
