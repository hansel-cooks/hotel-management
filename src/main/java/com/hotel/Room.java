package com.hotel;

import java.io.Serializable;
import javafx.beans.property.*;

public class Room implements Serializable {
    private transient IntegerProperty roomId;
    private transient StringProperty type;
    private transient DoubleProperty price;
    private transient StringProperty status;

    public Room(int id, String type, double price, String status) {
        this.roomId = new SimpleIntegerProperty(id);
        this.type = new SimpleStringProperty(type);
        this.price = new SimpleDoubleProperty(price);
        this.status = new SimpleStringProperty(status);
    }

    public int getRoomId() { return roomId.get(); }
    public String getType() { return type.get(); }
    public double getPrice() { return price.get(); }
    public String getStatus() { return status.get(); }

    public void setStatus(String s) { status.set(s); }

    public IntegerProperty roomIdProperty() { return roomId; }
    public StringProperty typeProperty() { return type; }
    public DoubleProperty priceProperty() { return price; }
    public StringProperty statusProperty() { return status; }
}
