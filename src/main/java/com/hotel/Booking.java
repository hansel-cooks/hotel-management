package com.hotel;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

// ── SERIALIZATION ─────────────────────────────────────────────────────────────
// Booking implements Serializable so the entire object graph is persisted via
// ObjectOutputStream. LocalDate is serializable; no special handling needed.
// serialVersionUID declared explicitly so class evolution doesn't silently
// break existing data files.
public class Booking implements Serializable {

    private static final long serialVersionUID = 2L;

    private final int       bookingId;
    private final int       roomId;
    private final String    roomType;
    private final String    guestName;
    private final String    phone;
    private final String    email;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private boolean         active;
    private final double    totalBill;
    private boolean         paid;

    public Booking(int id, int roomId, String roomType, String guestName,
                   String phone, String email,
                   LocalDate checkIn, LocalDate checkOut, double pricePerNight) {
        this.bookingId  = id;
        this.roomId     = roomId;
        this.roomType   = roomType;
        this.guestName  = guestName;
        this.phone      = phone;
        this.email      = email;
        this.checkIn    = checkIn;
        this.checkOut   = checkOut;
        this.active     = true;
        this.paid       = false;
        // ── WRAPPER CLASS: Long.max — explicit Long wrapper static method ──────
        long nights    = Long.max(ChronoUnit.DAYS.between(checkIn, checkOut), 1L);
        this.totalBill = nights * pricePerNight;
    }

    public int       getBookingId() { return bookingId; }
    public int       getRoomId()    { return roomId;    }
    public String    getRoomType()  { return roomType;  }
    public String    getName()      { return guestName; }
    public String    getPhone()     { return phone;     }
    public String    getEmail()     { return email;     }
    public LocalDate getCheckIn()   { return checkIn;   }
    public LocalDate getCheckOut()  { return checkOut;  }
    public boolean   isActive()     { return active;    }
    public double    getTotalBill() { return totalBill; }
    public boolean   isPaid()       { return paid;      }

    public long getDays() {
        return Long.max(ChronoUnit.DAYS.between(checkIn, checkOut), 1L);
    }

    public void markPaid() {
        this.paid   = true;
        this.active = false;
    }

    // ── STRING BUILDER ────────────────────────────────────────────────────────
    // getSummary builds the booking's display string using StringBuilder instead
    // of + concatenation. Each + creates a new String object in memory;
    // StringBuilder reuses a single buffer and only allocates once at toString().
    // For a list that may show 100+ bookings, this matters.
    public String getSummary() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        // ── WRAPPER CLASS: String.valueOf(int) for safe int to String ─────────
        String status = paid ? "PAID" : active ? "ACTIVE" : "CHECKED-OUT";

        StringBuilder sb = new StringBuilder();
        sb.append("#").append(String.valueOf(bookingId))
          .append("  Room ").append(String.valueOf(roomId))
          .append("  ").append(roomType)
          .append("  ").append(guestName)
          .append("  ").append(checkIn.format(fmt))
          .append(" -> ").append(checkOut.format(fmt))
          .append("  Rs.").append(String.format("%,.0f", totalBill))
          .append("  [").append(status).append("]");

        return sb.toString();
    }
}