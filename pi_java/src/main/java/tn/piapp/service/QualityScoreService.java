package tn.piapp.service;

import tn.piapp.model.Service;
import tn.piapp.model.Tool;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure, stateless service that computes a quality score (0–100) for a Tool or Service.
 * No database access; no JavaFX imports.
 *
 * Scoring rubric (same for both types, price threshold differs):
 *   Has non-blank imageName          → 30 pts
 *   Description ≥ 100 chars          → 30 pts  (≥ 50 chars → 15 pts)
 *   Non-null categoryId              → 15 pts
 *   Non-blank location               → 10 pts
 *   Price > 0 and ≤ threshold        → 10 pts  (Tool: 500, Service: 1000)
 *   createdAt within 30 days         →  5 pts  (intentional recency bonus)
 *
 * Rating thresholds:
 *   ≥ 81 → Excellent
 *   ≥ 51 → Good
 *    < 51 → Needs Improvement
 */
public class QualityScoreService {

    // ── Tool scoring ───────────────────────────────────────────────────────────

    public QualityScoreResult score(Tool tool) {
        int pts = 0;
        List<String> suggestions = new ArrayList<>();
        List<String> checklist   = new ArrayList<>();

        // 1. Image (30 pts)
        if (tool.getImageName() != null && !tool.getImageName().isBlank()) {
            pts += 30;
            checklist.add("✓ Has professional image");
        } else {
            suggestions.add("Add an image to make your listing more attractive");
            checklist.add("✗ Missing image");
        }

        // 2. Description (30 pts / 15 pts)
        String desc = tool.getDescription();
        if (desc != null && desc.length() >= 100) {
            pts += 30;
            checklist.add("✓ Detailed description provided");
        } else if (desc != null && desc.length() >= 50) {
            pts += 15;
            suggestions.add("Add more details to your description (at least 100 characters)");
            checklist.add("✗ Description could be more detailed");
        } else {
            suggestions.add("Write a detailed description (at least 100 characters)");
            checklist.add("✗ Description is too short");
        }

        // 3. Category (15 pts)
        if (tool.getCategoryId() != null) {
            pts += 15;
            checklist.add("✓ Category assigned");
        } else {
            suggestions.add("Assign a category to help users find your tool");
            checklist.add("✗ No category assigned");
        }

        // 4. Location (10 pts)
        if (tool.getLocation() != null && !tool.getLocation().isBlank()) {
            pts += 10;
            checklist.add("✓ Location specified");
        } else {
            suggestions.add("Add your location to attract local renters");
            checklist.add("✗ Location not specified");
        }

        // 5. Price (10 pts) — threshold 500 for tools
        BigDecimal price = tool.getPricePerDay();
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0
                && price.compareTo(new BigDecimal("500")) <= 0) {
            pts += 10;
            checklist.add("✓ Price is reasonable");
        } else if (price != null && price.compareTo(new BigDecimal("500")) > 0) {
            pts += 5;
            suggestions.add("Consider if your price is competitive");
            checklist.add("✗ Price might be too high");
        } else {
            suggestions.add("Set a reasonable price for your tool");
            checklist.add("✗ Price not set");
        }

        // 6. Recency (5 pts) — intentional gamification bonus
        if (tool.getCreatedAt() != null
                && ChronoUnit.DAYS.between(tool.getCreatedAt(), LocalDateTime.now()) <= 30) {
            pts += 5;
            checklist.add("✓ Recently added (recency bonus)");
        }

        return new QualityScoreResult(pts, deriveRating(pts), suggestions, checklist);
    }

    // ── Service scoring ────────────────────────────────────────────────────────

    public QualityScoreResult score(Service service) {
        int pts = 0;
        List<String> suggestions = new ArrayList<>();
        List<String> checklist   = new ArrayList<>();

        // 1. Image (30 pts)
        if (service.getImageName() != null && !service.getImageName().isBlank()) {
            pts += 30;
            checklist.add("✓ Has professional image");
        } else {
            suggestions.add("Add an image to make your listing more attractive");
            checklist.add("✗ Missing image");
        }

        // 2. Description (30 pts / 15 pts)
        String desc = service.getDescription();
        if (desc != null && desc.length() >= 100) {
            pts += 30;
            checklist.add("✓ Detailed description provided");
        } else if (desc != null && desc.length() >= 50) {
            pts += 15;
            suggestions.add("Add more details to your description (at least 100 characters)");
            checklist.add("✗ Description could be more detailed");
        } else {
            suggestions.add("Write a detailed description (at least 100 characters)");
            checklist.add("✗ Description is too short");
        }

        // 3. Category (15 pts)
        if (service.getCategoryId() != null) {
            pts += 15;
            checklist.add("✓ Category assigned");
        } else {
            suggestions.add("Assign a category to help users find your service");
            checklist.add("✗ No category assigned");
        }

        // 4. Location (10 pts)
        if (service.getLocation() != null && !service.getLocation().isBlank()) {
            pts += 10;
            checklist.add("✓ Location specified");
        } else {
            suggestions.add("Add your location to attract local customers");
            checklist.add("✗ Location not specified");
        }

        // 5. Price (10 pts) — threshold 1000 for services
        BigDecimal price = service.getBasePrice();
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0
                && price.compareTo(new BigDecimal("1000")) <= 0) {
            pts += 10;
            checklist.add("✓ Price is reasonable");
        } else if (price != null && price.compareTo(new BigDecimal("1000")) > 0) {
            pts += 5;
            suggestions.add("Consider if your price is competitive");
            checklist.add("✗ Price might be too high");
        } else {
            suggestions.add("Set a reasonable price for your service");
            checklist.add("✗ Price not set");
        }

        // 6. Recency (5 pts) — intentional gamification bonus
        if (service.getCreatedAt() != null
                && ChronoUnit.DAYS.between(service.getCreatedAt(), LocalDateTime.now()) <= 30) {
            pts += 5;
            checklist.add("✓ Recently added (recency bonus)");
        }

        return new QualityScoreResult(pts, deriveRating(pts), suggestions, checklist);
    }

    // ── Rating helper ──────────────────────────────────────────────────────────

    private String deriveRating(int score) {
        if (score >= 81) return "Excellent";
        if (score >= 51) return "Good";
        return "Needs Improvement";
    }
}
