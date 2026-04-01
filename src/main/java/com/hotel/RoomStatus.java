package com.hotel;

// ── ENUMERATION ──────────────────────────────────────────────────────────────
// Replaces raw String status ("Available", "Booked", etc.) with a type-safe
// enum. Each constant carries its own display label and UI color hex so no
// switch-on-string logic is needed anywhere in the app.
public enum RoomStatus {

    AVAILABLE   ("Available",    "#16A34A"),
    BOOKED      ("Booked",       "#2563EB"),
    MAINTENANCE ("Maintenance",  "#D97706"),
    OUT_OF_ORDER("Out of Order", "#DC2626");

    private final String label;
    private final String colorHex;

    RoomStatus(String label, String colorHex) {
        this.label    = label;
        this.colorHex = colorHex;
    }

    public String getLabel()    { return label;    }
    public String getColorHex() { return colorHex; }

    // Parse from a persisted or user-entered string — robust to old data.
    // ── WRAPPER CLASS: uses Character.toUpperCase indirectly via equalsIgnoreCase
    public static RoomStatus fromLabel(String label) {
        if (label == null) return AVAILABLE;
        for (RoomStatus s : values()) {
            if (s.label.equalsIgnoreCase(label)) return s;
        }
        return AVAILABLE;
    }

    @Override
    public String toString() { return label; }
}