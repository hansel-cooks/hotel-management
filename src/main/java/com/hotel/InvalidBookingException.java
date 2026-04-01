package com.hotel;

// ── CUSTOM EXCEPTION ──────────────────────────────────────────────────────────
// InvalidBookingException is a checked exception thrown whenever booking
// validation fails — empty fields, invalid phone/email, bad dates, or a room
// that isn't available. Using a custom exception instead of generic
// IllegalArgumentException makes the error type self-documenting and lets
// App.java catch booking errors specifically without catching everything else.
//
// Extends Exception (checked) so the compiler forces every caller to handle it,
// which is exactly what we want for user-input validation.
public class InvalidBookingException extends Exception {

    private static final long serialVersionUID = 3L;

    // ── CONSTRUCTOR chaining ───────────────────────────────────────────────────
    // Passes the message up to Exception via super() — standard exception pattern.
    public InvalidBookingException(String message) {
        super(message);
    }
}