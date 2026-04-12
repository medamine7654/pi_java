package tn.piapp.util;

import org.junit.jupiter.api.Test;
import tn.piapp.model.Service;
import tn.piapp.model.Tool;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    // ─── Helper builders ────────────────────────────────────────────────────

    /** Returns a fully valid Service — tweak individual fields per test. */
    private Service validService() {
        Service s = new Service();
        s.setName("Cleaning Service");
        s.setBasePrice(new BigDecimal("50.00"));
        s.setDurationMinutes(60);
        return s;
    }

    /** Returns a fully valid Tool — tweak individual fields per test. */
    private Tool validTool() {
        Tool t = new Tool();
        t.setName("Drill");
        t.setPricePerDay(new BigDecimal("15.00"));
        t.setStockQuantity(5);
        return t;
    }

    // ─── Service tests ───────────────────────────────────────────────────────

    @Test
    void service_blankName_returnsError() {
        Service s = validService();
        s.setName("");
        assertNotNull(Validation.validateService(s), "Expected error for blank name");
    }

    @Test
    void service_spacesOnlyName_returnsError() {
        Service s = validService();
        s.setName("   ");
        assertNotNull(Validation.validateService(s), "Expected error for whitespace-only name");
    }

    @Test
    void service_nullName_returnsError() {
        Service s = validService();
        s.setName(null);
        assertNotNull(Validation.validateService(s), "Expected error for null name");
    }

    @Test
    void service_negativeBasePrice_returnsError() {
        Service s = validService();
        s.setBasePrice(new BigDecimal("-1.00"));
        assertNotNull(Validation.validateService(s), "Expected error for negative base price");
    }

    @Test
    void service_zeroDurationMinutes_returnsError() {
        Service s = validService();
        s.setDurationMinutes(0);
        assertNotNull(Validation.validateService(s), "Expected error for duration = 0");
    }

    @Test
    void service_negativeDurationMinutes_returnsError() {
        Service s = validService();
        s.setDurationMinutes(-10);
        assertNotNull(Validation.validateService(s), "Expected error for negative duration");
    }

    @Test
    void service_valid_returnsNull() {
        Service s = validService();
        assertNull(Validation.validateService(s), "Expected no error for valid service");
    }

    @Test
    void service_zeroPriceIsValid() {
        Service s = validService();
        s.setBasePrice(BigDecimal.ZERO);
        assertNull(Validation.validateService(s), "Base price of 0 should be valid");
    }

    @Test
    void service_durationOneIsValid() {
        Service s = validService();
        s.setDurationMinutes(1);
        assertNull(Validation.validateService(s), "Duration of 1 should be valid");
    }

    // ─── Tool tests ──────────────────────────────────────────────────────────

    @Test
    void tool_blankName_returnsError() {
        Tool t = validTool();
        t.setName("");
        assertNotNull(Validation.validateTool(t), "Expected error for blank name");
    }

    @Test
    void tool_spacesOnlyName_returnsError() {
        Tool t = validTool();
        t.setName("   ");
        assertNotNull(Validation.validateTool(t), "Expected error for whitespace-only name");
    }

    @Test
    void tool_nullName_returnsError() {
        Tool t = validTool();
        t.setName(null);
        assertNotNull(Validation.validateTool(t), "Expected error for null name");
    }

    @Test
    void tool_negativePricePerDay_returnsError() {
        Tool t = validTool();
        t.setPricePerDay(new BigDecimal("-5.00"));
        assertNotNull(Validation.validateTool(t), "Expected error for negative price per day");
    }

    @Test
    void tool_negativeStockQuantity_returnsError() {
        Tool t = validTool();
        t.setStockQuantity(-1);
        assertNotNull(Validation.validateTool(t), "Expected error for negative stock quantity");
    }

    @Test
    void tool_valid_returnsNull() {
        Tool t = validTool();
        assertNull(Validation.validateTool(t), "Expected no error for valid tool");
    }

    @Test
    void tool_zeroPriceIsValid() {
        Tool t = validTool();
        t.setPricePerDay(BigDecimal.ZERO);
        assertNull(Validation.validateTool(t), "Price per day of 0 should be valid");
    }

    @Test
    void tool_zeroStockIsValid() {
        Tool t = validTool();
        t.setStockQuantity(0);
        assertNull(Validation.validateTool(t), "Stock quantity of 0 should be valid");
    }

    // ─── Error message content tests ─────────────────────────────────────────

    @Test
    void service_blankName_errorMessageIsCorrect() {
        Service s = validService();
        s.setName("");
        assertEquals("Name is required.", Validation.validateService(s));
    }

    @Test
    void service_negativePrice_errorMessageIsCorrect() {
        Service s = validService();
        s.setBasePrice(new BigDecimal("-1"));
        assertEquals("Base price must be 0 or greater.", Validation.validateService(s));
    }

    @Test
    void service_zeroDuration_errorMessageIsCorrect() {
        Service s = validService();
        s.setDurationMinutes(0);
        assertEquals("Duration must be greater than 0.", Validation.validateService(s));
    }

    @Test
    void tool_blankName_errorMessageIsCorrect() {
        Tool t = validTool();
        t.setName("");
        assertEquals("Name is required.", Validation.validateTool(t));
    }

    @Test
    void tool_negativePrice_errorMessageIsCorrect() {
        Tool t = validTool();
        t.setPricePerDay(new BigDecimal("-1"));
        assertEquals("Price per day must be 0 or greater.", Validation.validateTool(t));
    }

    @Test
    void tool_negativeStock_errorMessageIsCorrect() {
        Tool t = validTool();
        t.setStockQuantity(-1);
        assertEquals("Stock quantity must be 0 or greater.", Validation.validateTool(t));
    }
}
