package com.hotel;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Booking implements Serializable {
    private static final long serialVersionUID = 2L;

    private int bookingId;
    private int roomId;
    private String roomType;
    private String name;
    private String phone;
    private String email;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private boolean active;
    private double totalBill;
    private boolean paid;

    public Booking(int id, int roomId, String roomType, String name, String phone, String email,
                   LocalDate checkIn, LocalDate checkOut, double pricePerNight) {
        this.bookingId = id;
        this.roomId = roomId;
        this.roomType = roomType;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.active = true;
        this.paid = false;
        this.totalBill = getDays() * pricePerNight;
    }

    public int getBookingId() { return bookingId; }
    public int getRoomId() { return roomId; }
    public String getRoomType() { return roomType; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public boolean isActive() { return active; }
    public double getTotalBill() { return totalBill; }
    public boolean isPaid() { return paid; }

    public long getDays() {
        long d = ChronoUnit.DAYS.between(checkIn, checkOut);
        return d <= 0 ? 1 : d;
    }

    public void markPaid() {
        this.paid = true;
        this.active = false;
    }
}