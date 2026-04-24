package tn.piapp.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure, stateless service that computes price statistics from a list of prices.
 * No database access; no JavaFX imports.
 *
 * Median: middle element (odd count) or average of two middle elements (even count)
 *         after sorting in ascending order.
 * Mean:   arithmetic mean, rounded to 2 decimal places.
 */
public class PriceSuggestionService {

    /**
     * Computes price statistics for a non-empty list of prices.
     *
     * @param prices non-null, non-empty list of prices
     * @return PriceSuggestionResult with suggested (median), median, mean, min, max, count
     * @throws IllegalArgumentException if prices is null or empty
     */
    public PriceSuggestionResult suggest(List<BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("Price list must not be null or empty");
        }

        List<BigDecimal> sorted = new ArrayList<>(prices);
        Collections.sort(sorted);

        int count = sorted.size();

        // Median
        BigDecimal median;
        int mid = count / 2;
        if (count % 2 == 0) {
            median = sorted.get(mid - 1).add(sorted.get(mid))
                           .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            median = sorted.get(mid).setScale(2, RoundingMode.HALF_UP);
        }

        // Mean
        BigDecimal sum = sorted.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        BigDecimal min = sorted.get(0);
        BigDecimal max = sorted.get(count - 1);

        return new PriceSuggestionResult(median, median, mean, min, max, count);
    }
}
