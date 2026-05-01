package com.finpulse.util;

import org.springframework.stereotype.Component;

@Component
public class CurrencyUtils {
    public static String getCurrencySymbol(String currencyCode) {
        if (currencyCode == null) return "$";
        return switch (currencyCode) {
            case "USD" -> "$";
            case "GBP" -> "£";
            case "EUR" -> "€";
            case "PKR" -> "₨";
            case "AED" -> "AED";
            case "SAR" -> "SAR";
            default    -> "$";
        };
    }
}
