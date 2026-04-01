package com.hotel;

import java.io.Serializable;
import javafx.beans.property.*;

// ── SERIALIZATION ─────────────────────────────────────────────────────────────
// JavaFX Property objects are NOT serializable → marked transient.
// We persist plain primitives (_roomId, _type, _price, _status) and reconstruct
// the Properties in readObject() — standard pattern for Observable + Serialization.
//
// ── COMPARABLE ────────────────────────────────────────────────────────────────
// Implementing Comparable<Room> lets rooms be sorted naturally by price using
// Collections.sort() or sorted streams without needing an external Comparator.
public class Room implements Serializable, Comparable<Room> {

    private static final long serialVersionUID = 1L;

    // Transient: excluded from the serialized byte stream.
    private transient IntegerProperty roomIdProp;
    private transient StringProperty  typeProp;
    private transient DoubleProperty  priceProp;
    private transient StringProperty  statusProp;

    // ── SERIALIZATION backing fields (survive writeObject / readObject) ───────
    private int        _roomId;
    private String     _type;
    private double     _price;
    // ── ENUM stored directly — enums are Serializable constants ──────────────
    private RoomStatus _status;

    // ── WRAPPER CLASS ─────────────────────────────────────────────────────────
    // floor derived via Integer division; roomId boxed then unboxed on return.
    private final int floor;

    public Room(int id, String type, double price, String status) {
        this._roomId = id;
        this._type   = type;
        this._price  = price;
        // ── ENUM: convert raw String input to type-safe enum ─────────────────
        this._status = RoomStatus.fromLabel(status);
        // ── WRAPPER CLASS: Integer.valueOf then divide to get floor ───────────
        this.floor   = Integer.valueOf(id) / 100;
        initProperties();
    }

    // ── SERIALIZATION ─────────────────────────────────────────────────────────
    // Called by ObjectInputStream after the byte stream is read.
    // Transient fields are null at this point — we rebuild them here.
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();   // restores all non-transient fields
        initProperties();         // rebuilds the transient JavaFX Properties
    }

    private void initProperties() {
        this.roomIdProp = new SimpleIntegerProperty(_roomId);
        this.typeProp   = new SimpleStringProperty(_type);
        this.priceProp  = new SimpleDoubleProperty(_price);
        // ── ENUM: write its label string into the observable StringProperty ──
        this.statusProp = new SimpleStringProperty(_status.getLabel());
    }

    // ── COMPARABLE ────────────────────────────────────────────────────────────
    // compareTo sorts rooms by price ascending.
    // Double.compare handles floating-point comparison correctly — avoids the
    // classic bug of subtracting doubles and casting to int.
    @Override
    public int compareTo(Room other) {
        return Double.compare(this._price, other._price);
    }

    // Getters — used by PropertyValueFactory in TableView
    public int    getRoomId() { return roomIdProp.get(); }
    public String getType()   { return typeProp.get();   }
    public double getPrice()  { return priceProp.get();  }
    public String getStatus() { return statusProp.get(); }
    public int    getFloor()  { return floor;            }

    // ── ENUM convenience ──────────────────────────────────────────────────────
    public RoomStatus getCurrentStatus() { return _status; }

    // Setters keep backing field + Property in sync
    public void setStatus(String s) {
        _status = RoomStatus.fromLabel(s);
        statusProp.set(_status.getLabel());
    }

    public void setStatusEnum(RoomStatus s) {
        _status = s;
        statusProp.set(s.getLabel());
    }

    public void setPrice(double p) {
        // ── WRAPPER CLASS: Double.compare — explicit wrapper API ──────────────
        if (Double.compare(p, 0.0) > 0) { _price = p; priceProp.set(p); }
    }

    // JavaFX property accessors for PropertyValueFactory / bindings
    public IntegerProperty roomIdProperty() { return roomIdProp; }
    public StringProperty  typeProperty()   { return typeProp;   }
    public DoubleProperty  priceProperty()  { return priceProp;  }
    public StringProperty  statusProperty() { return statusProp; }
}