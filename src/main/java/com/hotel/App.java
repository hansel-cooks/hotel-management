package com.hotel;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

public class App extends Application {

    private Stage primaryStage;

    static ObservableList<Room> rooms = FXCollections.observableArrayList();
    static List<Booking> bookings = new ArrayList<>();

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        loadData();

        if (rooms == null || rooms.size() == 0) {
            rooms = FXCollections.observableArrayList();
            rooms.addAll(
                new Room(1, "Single", 1000, "Available"),
                new Room(2, "Double", 2000, "Available"),
                new Room(3, "Deluxe", 3000, "Available"),
                new Room(4, "Suite", 5000, "Available")
            );
            saveData();
        }

        showHome();
    }

    // ---------------- HOME ----------------
    private void showHome() {
        VBox home = new VBox(20);
        home.setStyle("-fx-background-color: #f4f4f4; -fx-alignment: center;");

        Label title = new Label("Hotel Management System");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button bookingBtn = new Button("Booking");
        Button adminBtn = new Button("Admin");

        bookingBtn.setPrefWidth(150);
        adminBtn.setPrefWidth(150);

        bookingBtn.setOnAction(e -> primaryStage.setScene(new Scene(bookingUI(), 800, 500)));
        adminBtn.setOnAction(e -> primaryStage.setScene(new Scene(adminLoginUI(), 800, 500)));

        home.getChildren().addAll(title, bookingBtn, adminBtn);

        primaryStage.setScene(new Scene(home, 800, 500));
        primaryStage.setTitle("Hotel System");
        primaryStage.show();
    }

    // ---------------- TABLE ----------------
    private TableView<Room> createTable() {
        TableView<Room> table = new TableView<>(rooms);

        TableColumn<Room, Integer> id = new TableColumn<>("Room ID");
        id.setCellValueFactory(new PropertyValueFactory<>("roomId"));

        TableColumn<Room, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Room, Double> price = new TableColumn<>("Price");
        price.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Room, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(id, type, price, status);
        return table;
    }

    // ---------------- BOOKING ----------------
    private VBox bookingUI() {
        VBox v = new VBox(10);
        v.setStyle("-fx-padding: 20; -fx-background-color: white;");

        Button back = new Button("Back");
        back.setOnAction(e -> showHome());

        TableView<Room> table = createTable();

        TextField name = new TextField();
        name.setPromptText("Name");

        TextField phone = new TextField();
        phone.setPromptText("Phone");

        TextField email = new TextField();
        email.setPromptText("Email");

        DatePicker checkIn = new DatePicker();
        DatePicker checkOut = new DatePicker();

        Label msg = new Label();

        Button book = new Button("Book");
        Button checkout = new Button("Checkout");
        Button showAvailable = new Button("Show Available");
        Button showAll = new Button("Show All");

        // BOOK
        book.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();

            if (r == null) {
                msg.setText("Select a room");
                return;
            }

            if (!r.getStatus().equals("Available")) {
                msg.setText("Room not available");
                return;
            }

            if (name.getText().isEmpty() ||
                phone.getText().isEmpty() ||
                email.getText().isEmpty() ||
                checkIn.getValue() == null ||
                checkOut.getValue() == null) {

                msg.setText("Fill all fields");
                return;
            }

            Booking b = new Booking(
                bookings.size() + 1,
                r.getRoomId(),
                name.getText(),
                phone.getText(),
                email.getText(),
                checkIn.getValue(),
                checkOut.getValue()
            );

            bookings.add(b);
            r.setStatus("Booked");

            table.refresh();
            saveData();

            msg.setText("Booked successfully");
        });

        // CHECKOUT
        checkout.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();

            if (r != null && r.getStatus().equals("Booked")) {

                Booking b = bookings.stream()
                        .filter(x -> x.getRoomId() == r.getRoomId() && x.isActive())
                        .findFirst().orElse(null);

                if (b != null) {
                    double bill = b.getDays() * r.getPrice();
                    msg.setText("Bill Paid: ₹" + bill);
                    b.checkout();
                }

                r.setStatus("Maintenance");
                saveData();
                startMaintenance(r);
                table.refresh();
            }
        });

        showAvailable.setOnAction(e ->
            table.setItems(rooms.filtered(r -> r.getStatus().equals("Available")))
        );

        showAll.setOnAction(e ->
            table.setItems(rooms)
        );

        v.getChildren().addAll(back, table, name, phone, email, checkIn, checkOut,
                book, checkout, showAvailable, showAll, msg);

        return v;
    }

    // ---------------- ADMIN LOGIN ----------------
    private VBox adminLoginUI() {
        VBox v = new VBox(10);
        v.setStyle("-fx-padding: 20; -fx-background-color: white;");

        Button back = new Button("Back");
        back.setOnAction(e -> showHome());

        PasswordField pass = new PasswordField();
        pass.setPromptText("Password");

        Label msg = new Label();

        Button login = new Button("Login");

        login.setOnAction(e -> {
            if (pass.getText().equals("admin123")) {
                primaryStage.setScene(new Scene(adminPanel(), 800, 500));
            } else {
                msg.setText("Wrong password");
            }
        });

        v.getChildren().addAll(back, pass, login, msg);
        return v;
    }

    // ---------------- ADMIN PANEL ----------------
    private VBox adminPanel() {
        VBox v = new VBox(10);
        v.setStyle("-fx-padding: 20; -fx-background-color: white;");

        Button back = new Button("Back");
        back.setOnAction(e -> showHome());

        TableView<Room> table = createTable();

        TextField id = new TextField();
        id.setPromptText("Room ID");

        TextField type = new TextField();
        type.setPromptText("Type");

        TextField price = new TextField();
        price.setPromptText("Price");

        Button add = new Button("Add Room");

        ListView<String> bookingList = new ListView<>();

        // refresh booking list
        Runnable refreshBookings = () -> {
            bookingList.getItems().clear();
            for (Booking b : bookings) {
                bookingList.getItems().add(
                    "Room " + b.getRoomId() +
                    " | " + b.getName() +
                    " | " + b.getCheckIn() + " to " + b.getCheckOut() +
                    " | " + (b.isActive() ? "ACTIVE" : "CHECKED OUT")
                );
            }
        };

        refreshBookings.run();

        add.setOnAction(e -> {
            try {
                Room newRoom = new Room(
                    Integer.parseInt(id.getText()),
                    type.getText(),
                    Double.parseDouble(price.getText()),
                    "Available"
                );

                rooms.add(newRoom);
                saveData();

                table.refresh();
                id.clear();
                type.clear();
                price.clear();

            } catch (Exception ex) {
                System.out.println("Invalid input");
            }
        });

        v.getChildren().addAll(back, table, id, type, price, add, bookingList);
        return v;
    }

    // ---------------- THREAD ----------------
    private void startMaintenance(Room r) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    r.setStatus("Available");
                    saveData();
                });
            } catch (Exception e) {}
        }).start();
    }

    // ---------------- FILE ----------------
    private void saveData() {
        try {
            ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream("data.dat"));
            o.writeObject(new ArrayList<>(rooms));
            o.writeObject(bookings);
            o.close();
        } catch (Exception e) {}
    }

    private void loadData() {
        try {
            ObjectInputStream o = new ObjectInputStream(new FileInputStream("data.dat"));
            rooms = FXCollections.observableArrayList((ArrayList<Room>) o.readObject());
            bookings = (ArrayList<Booking>) o.readObject();
            o.close();
        } catch (Exception e) {
            System.out.println("No previous data");
        }
    }
}