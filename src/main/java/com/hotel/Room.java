package com.hotel;

import java.io.Serializable;
import javafx.beans.property.*;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient IntegerProperty roomId;
    private transient StringProperty type;
    private transient DoubleProperty price;
    private transient StringProperty status;

    // For serialization
    private int _roomId;
    private String _type;
    private double _price;
    private String _status;

    public Room(int id, String type, double price, String status) {
        this._roomId = id;
        this._type = type;
        this._price = price;
        this._status = status;
        initProperties();
    }

    private void initProperties() {
        this.roomId = new SimpleIntegerProperty(_roomId);
        this.type = new SimpleStringProperty(_type);
        this.price = new SimpleDoubleProperty(_price);
        this.status = new SimpleStringProperty(_status);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        initProperties();
    }

    public int getRoomId() { return roomId.get(); }
    public String getType() { return type.get(); }
    public double getPrice() { return price.get(); }
    public String getStatus() { return status.get(); }

    public void setStatus(String s) {
        status.set(s);
        _status = s;
    }

    public void setPrice(double p) {
        price.set(p);
        _price = p;
    }

    public IntegerProperty roomIdProperty() { return roomId; }
    public StringProperty typeProperty() { return type; }
    public DoubleProperty priceProperty() { return price; }
    public StringProperty statusProperty() { return status; }
}