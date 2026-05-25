package com.finpulse.dto.request;

public record PreferencesRequest(
        String currency,
        String dateFormat
) {
}
