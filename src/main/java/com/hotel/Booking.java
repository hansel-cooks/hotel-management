package com.hotel;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Booking implements Serializable {
    private int bookingId;
    private int roomId;
    private String name;
    private String phone;
    private String email;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private boolean active;

    public Booking(int id, int roomId, String name, String phone, String email,
                   LocalDate checkIn, LocalDate checkOut) {
        this.bookingId = id;
        this.roomId = roomId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.active = true;
    }

    public int getRoomId() { return roomId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public boolean isActive() {
         return active;}

    public long getDays() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public void checkout() {
        active = false;
    }
}
