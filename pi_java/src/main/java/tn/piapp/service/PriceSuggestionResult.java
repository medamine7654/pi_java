package tn.piapp.service;

import java.math.BigDecimal;

/**
 * Immutable result of a price suggestion computation.
 */
public class PriceSuggestionResult {

    private final BigDecimal suggested; // median, rounded to 2 dp
    private final BigDecimal median;
    private final BigDecimal mean;
    private final BigDecimal min;
    private final BigDecimal max;
    private final int count;

    public PriceSuggestionResult(BigDecimal suggested, BigDecimal median,
                                  BigDecimal mean, BigDecimal min,
                                  BigDecimal max, int count) {
        this.suggested = suggested;
        this.median    = median;
        this.mean      = mean;
        this.min       = min;
        this.max       = max;
        this.count     = count;
    }

    public BigDecimal getSuggested() { return suggested; }
    public BigDecimal getMedian()    { return median; }
    public BigDecimal getMean()      { return mean; }
    public BigDecimal getMin()       { return min; }
    public BigDecimal getMax()       { return max; }
    public int getCount()            { return count; }
}
