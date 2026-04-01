package com.hotel;

import java.io.Serializable;
import javafx.beans.property.*;

// ── SERIALIZATION ─────────────────────────────────────────────────────────────
// JavaFX Property objects are NOT serializable → marked transient.
// We persist plain primitives and reconstruct Properties in readObject().
//
// ── COMPARABLE ────────────────────────────────────────────────────────────────
// Implementing Comparable<Room> lets rooms be sorted naturally by price using
// Collections.sort() or sorted streams without needing an external Comparator.
public class Room implements Serializable, Comparable<Room> {

    private static final long serialVersionUID = 1L;

    private transient IntegerProperty roomIdProp;
    private transient StringProperty  typeProp;
    private transient DoubleProperty  priceProp;
    private transient StringProperty  statusProp;

    // Serialization backing fields
    private int        _roomId;
    private String     _type;
    private double     _price;
    private RoomStatus _status;

    // floor derived via integer division
    private final int floor;

    public Room(int id, String type, double price, String status) {
        this._roomId = id;
        this._type   = type;
        this._price  = price;
        this._status = RoomStatus.fromLabel(status);
        this.floor   = Integer.valueOf(id) / 100;
        initProperties();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        initProperties();
    }

    private void initProperties() {
        this.roomIdProp = new SimpleIntegerProperty(_roomId);
        this.typeProp   = new SimpleStringProperty(_type);
        this.priceProp  = new SimpleDoubleProperty(_price);
        this.statusProp = new SimpleStringProperty(_status.getLabel());
    }

    // ── COMPARABLE ────────────────────────────────────────────────────────────
    // Double.compare handles floating-point correctly — avoids the subtract-cast bug.
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

    public RoomStatus getCurrentStatus() { return _status; }

    // Setters keep backing field + Property in sync
    public void setStatus(String s) {
        _status = RoomStatus.fromLabel(s);
        statusProp.set(_status.getLabel());
    }

    // ── Overload accepting the enum directly ──────────────────────────────────
    // Used by RoomService and maintenance logic — avoids raw string calls.
    public void setStatusEnum(RoomStatus s) {
        _status = s;
        statusProp.set(s.getLabel());
    }

    public void setPrice(double p) {
        if (Double.compare(p, 0.0) > 0) { _price = p; priceProp.set(p); }
    }

    // JavaFX property accessors
    public IntegerProperty roomIdProperty() { return roomIdProp; }
    public StringProperty  typeProperty()   { return typeProp;   }
    public DoubleProperty  priceProperty()  { return priceProp;  }
    public StringProperty  statusProperty() { return statusProp; }
}