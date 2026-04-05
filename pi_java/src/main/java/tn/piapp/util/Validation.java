package tn.piapp.util;

import tn.piapp.model.Service;
import tn.piapp.model.Tool;

import java.math.BigDecimal;

public class Validation {

    private Validation() {}

    public static String validateService(Service s) {
        if (s.getName() == null || s.getName().isBlank()) {
            return "Name is required.";
        }
        if (s.getBasePrice() == null || s.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            return "Base price must be 0 or greater.";
        }
        if (s.getDurationMinutes() <= 0) {
            return "Duration must be greater than 0.";
        }
        return null;
    }

    public static String validateTool(Tool t) {
        if (t.getName() == null || t.getName().isBlank()) {
            return "Name is required.";
        }
        if (t.getPricePerDay() == null || t.getPricePerDay().compareTo(BigDecimal.ZERO) < 0) {
            return "Price per day must be 0 or greater.";
        }
        if (t.getStockQuantity() < 0) {
            return "Stock quantity must be 0 or greater.";
        }
        return null;
    }
}
