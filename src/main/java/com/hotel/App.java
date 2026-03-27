package com.hotel;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.*;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class App extends Application {

    private Stage primaryStage;

    static ObservableList<Room> rooms = FXCollections.observableArrayList();
    static List<Booking> bookings = new ArrayList<>();

    // ── Design tokens ──────────────────────────────────────────────────────────
    private static final String BG       = "#F7F8FA";
    private static final String SURFACE  = "#FFFFFF";
    private static final String ACCENT   = "#2563EB";
    private static final String ACCENT2  = "#1D4ED8";
    private static final String DANGER   = "#DC2626";
    private static final String SUCCESS  = "#16A34A";
    private static final String MUTED    = "#6B7280";
    private static final String BORDER   = "#E5E7EB";
    private static final String TEXT     = "#111827";
    private static final String TEXT_SEC = "#374151";

    private static final String FONT_NORMAL = "-fx-font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;";

    // ── Styles ─────────────────────────────────────────────────────────────────
    private String cardStyle() {
        return "-fx-background-color:" + SURFACE + ";" +
               "-fx-background-radius:12;" +
               "-fx-border-color:" + BORDER + ";" +
               "-fx-border-radius:12;" +
               "-fx-border-width:1;" +
               "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2);";
    }

    private String primaryBtn() {
        return FONT_NORMAL +
               "-fx-background-color:" + ACCENT + ";" +
               "-fx-text-fill:white;" +
               "-fx-font-weight:600;" +
               "-fx-font-size:13px;" +
               "-fx-padding:9 22;" +
               "-fx-background-radius:8;" +
               "-fx-cursor:hand;";
    }

    private String dangerBtn() {
        return FONT_NORMAL +
               "-fx-background-color:" + DANGER + ";" +
               "-fx-text-fill:white;" +
               "-fx-font-weight:600;" +
               "-fx-font-size:13px;" +
               "-fx-padding:9 22;" +
               "-fx-background-radius:8;" +
               "-fx-cursor:hand;";
    }

    private String ghostBtn() {
        return FONT_NORMAL +
               "-fx-background-color:transparent;" +
               "-fx-text-fill:" + TEXT_SEC + ";" +
               "-fx-font-size:13px;" +
               "-fx-padding:9 16;" +
               "-fx-background-radius:8;" +
               "-fx-border-color:" + BORDER + ";" +
               "-fx-border-radius:8;" +
               "-fx-border-width:1;" +
               "-fx-cursor:hand;";
    }

    private String fieldStyle() {
        return FONT_NORMAL +
               "-fx-background-color:white;" +
               "-fx-border-color:" + BORDER + ";" +
               "-fx-border-radius:8;" +
               "-fx-background-radius:8;" +
               "-fx-border-width:1.5;" +
               "-fx-padding:8 12;" +
               "-fx-font-size:13px;" +
               "-fx-text-fill:" + TEXT + ";";
    }

    private String tableStyle() {
        return FONT_NORMAL +
               "-fx-background-color:white;" +
               "-fx-border-color:" + BORDER + ";" +
               "-fx-border-radius:10;" +
               "-fx-background-radius:10;" +
               "-fx-font-size:13px;";
    }

    private Label heading(String text) {
        Label l = new Label(text);
        l.setStyle(FONT_NORMAL + "-fx-font-size:22px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");
        return l;
    }

    private Label subheading(String text) {
        Label l = new Label(text);
        l.setStyle(FONT_NORMAL + "-fx-font-size:13px;-fx-text-fill:" + MUTED + ";");
        return l;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle(FONT_NORMAL + "-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:" + MUTED +
                   ";-fx-letter-spacing:1px;");
        return l;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    public static void main(String[] args) { launch(); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        loadData();
        if (rooms.isEmpty()) seedRooms();
        showHome();
    }

    private void seedRooms() {
        rooms.addAll(
            new Room(101, "Single",  1200, "Available"),
            new Room(102, "Single",  1200, "Available"),
            new Room(201, "Double",  2500, "Available"),
            new Room(202, "Double",  2500, "Available"),
            new Room(301, "Deluxe",  4000, "Available"),
            new Room(401, "Suite",   7500, "Available")
        );
        saveData();
    }

    // ── HOME ──────────────────────────────────────────────────────────────────
    private void showHome() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");

        VBox hero = new VBox(20);
        hero.setAlignment(Pos.CENTER);
        hero.setMaxWidth(420);
        hero.setStyle(cardStyle() + "-fx-padding:50 48;");

        Label logo = new Label("\uD83C\uDFE8");
        logo.setStyle("-fx-font-size:44px;");

        Label title = new Label("Hotel Manager");
        title.setStyle(FONT_NORMAL + "-fx-font-size:28px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");

        Label sub = new Label("Streamlined hospitality management");
        sub.setStyle(FONT_NORMAL + "-fx-font-size:14px;-fx-text-fill:" + MUTED + ";");

        Button bookingBtn = new Button("Guest Booking");
        Button adminBtn   = new Button("Admin Panel");

        bookingBtn.setPrefWidth(280);
        adminBtn.setPrefWidth(280);
        bookingBtn.setStyle(primaryBtn());
        adminBtn.setStyle(ghostBtn());

        bookingBtn.setOnAction(e -> showBooking());
        adminBtn.setOnAction(e -> showAdminLogin());

        hero.getChildren().addAll(logo, title, sub, bookingBtn, adminBtn);

        StackPane center = new StackPane(hero);
        root.setCenter(center);

        primaryStage.setScene(new Scene(root, 900, 620));
        primaryStage.setTitle("Hotel Manager");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // ── NAV BAR ───────────────────────────────────────────────────────────────
    private HBox navbar(String pageTitle, Runnable onBack) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:" + SURFACE + ";" +
                     "-fx-border-color:" + BORDER + ";" +
                     "-fx-border-width:0 0 1 0;" +
                     "-fx-padding:14 28;");

        Button back = new Button("← Back");
        back.setStyle(ghostBtn());
        back.setOnAction(e -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label title = new Label("🏨  " + pageTitle);
        title.setStyle(FONT_NORMAL + "-fx-font-size:14px;-fx-font-weight:600;-fx-text-fill:" + TEXT + ";");

        bar.getChildren().addAll(back, spacer, title);
        return bar;
    }

    // ── STAT CARD ─────────────────────────────────────────────────────────────
    private VBox statCard(String icon, String value, String label) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(cardStyle() + "-fx-padding:18 22;");
        card.setPrefWidth(185);

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size:20px;");

        Label val = new Label(value);
        val.setStyle(FONT_NORMAL + "-fx-font-size:24px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");

        Label lbl = new Label(label);
        lbl.setStyle(FONT_NORMAL + "-fx-font-size:12px;-fx-text-fill:" + MUTED + ";");

        card.getChildren().addAll(ico, val, lbl);
        return card;
    }

    // ── ROOM TABLE ────────────────────────────────────────────────────────────
    private TableView<Room> createRoomTable() {
        TableView<Room> table = new TableView<>(rooms);
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Room, Integer> id = col("Room No.", "roomId", 90);
        TableColumn<Room, String>  ty = col("Type",    "type",   120);
        TableColumn<Room, Double>  pr = new TableColumn<>("Price / Night");
        pr.setCellValueFactory(new PropertyValueFactory<>("price"));
        pr.setPrefWidth(130);
        pr.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : "₹" + String.format("%,.0f", p));
            }
        });

        TableColumn<Room, String> st = new TableColumn<>("Status");
        st.setCellValueFactory(new PropertyValueFactory<>("status"));
        st.setPrefWidth(120);
        st.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                String color = switch (s) {
                    case "Available"   -> SUCCESS;
                    case "Booked"      -> ACCENT;
                    case "Maintenance" -> "#D97706";
                    default            -> MUTED;
                };
                setStyle("-fx-text-fill:" + color + ";-fx-font-weight:600;" + FONT_NORMAL);
            }
        });

        table.getColumns().addAll(id, ty, pr, st);
        return table;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<Room, T> col(String title, String prop, double w) {
        TableColumn<Room, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    // ── BOOKING UI ────────────────────────────────────────────────────────────
    private void showBooking() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(navbar("Guest Booking", this::showHome));

        // Left: room table + filters
        VBox left = new VBox(14);
        left.setStyle("-fx-padding:24 16 24 24;");
        left.setPrefWidth(480);

        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        Button showAll   = new Button("All Rooms");
        Button showAvail = new Button("Available Only");
        showAll.setStyle(primaryBtn());
        showAvail.setStyle(ghostBtn());

        filterRow.getChildren().addAll(showAll, showAvail);

        TableView<Room> table = createRoomTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        showAll.setOnAction(e -> { table.setItems(rooms); styleToggle(showAll, showAvail); });
        showAvail.setOnAction(e -> {
            table.setItems(rooms.filtered(r -> r.getStatus().equals("Available")));
            styleToggle(showAvail, showAll);
        });

        left.getChildren().addAll(heading("Rooms"), filterRow, table);

        // Right: booking form
        VBox right = new VBox(16);
        right.setStyle("-fx-padding:24 24 24 8;");
        right.setPrefWidth(380);

        VBox formCard = new VBox(14);
        formCard.setStyle(cardStyle() + "-fx-padding:24;");

        Label formTitle = new Label("New Booking");
        formTitle.setStyle(FONT_NORMAL + "-fx-font-size:16px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");

        TextField nameF  = styledField("Full Name");
        TextField phoneF = styledField("Phone Number");
        TextField emailF = styledField("Email Address");

        Label ciLabel = sectionLabel("CHECK-IN");
        DatePicker checkIn  = styledDate();
        Label coLabel = sectionLabel("CHECK-OUT");
        DatePicker checkOut = styledDate();

        Label msgLabel = new Label();
        msgLabel.setStyle(FONT_NORMAL + "-fx-font-size:13px;");
        msgLabel.setWrapText(true);

        Button bookBtn     = new Button("Confirm Booking");
        Button checkoutBtn = new Button("Checkout & Pay Bill");

        bookBtn.setPrefWidth(Double.MAX_VALUE);
        checkoutBtn.setPrefWidth(Double.MAX_VALUE);
        bookBtn.setStyle(primaryBtn());
        checkoutBtn.setStyle(ghostBtn());

        // BOOK
        bookBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { showMsg(msgLabel, "⚠  Select a room first", false); return; }
            if (!r.getStatus().equals("Available")) { showMsg(msgLabel, "⚠  Room not available", false); return; }

            String n = nameF.getText().trim(), p = phoneF.getText().trim(), em = emailF.getText().trim();
            if (n.isEmpty() || p.isEmpty() || em.isEmpty() ||
                checkIn.getValue() == null || checkOut.getValue() == null) {
                showMsg(msgLabel, "⚠  Please fill all fields", false); return;
            }
            if (!checkOut.getValue().isAfter(checkIn.getValue())) {
                showMsg(msgLabel, "⚠  Check-out must be after check-in", false); return;
            }

            Booking b = new Booking(bookings.size() + 1, r.getRoomId(), r.getType(),
                    n, p, em, checkIn.getValue(), checkOut.getValue(), r.getPrice());
            bookings.add(b);
            r.setStatus("Booked");
            table.refresh();
            saveData();

            nameF.clear(); phoneF.clear(); emailF.clear();
            checkIn.setValue(null); checkOut.setValue(null);

            showMsg(msgLabel, "✓  Booking confirmed! Room " + r.getRoomId(), true);
        });

        // CHECKOUT & PAY
        checkoutBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { showMsg(msgLabel, "⚠  Select a booked room", false); return; }
            if (!r.getStatus().equals("Booked")) { showMsg(msgLabel, "⚠  Room is not booked", false); return; }

            Booking b = bookings.stream()
                    .filter(x -> x.getRoomId() == r.getRoomId() && x.isActive())
                    .findFirst().orElse(null);

            if (b == null) { showMsg(msgLabel, "⚠  No active booking found", false); return; }

            showBillDialog(b, r, () -> {
                table.refresh();
                saveData();
                showMsg(msgLabel, "✓  Payment complete. Room entering maintenance.", true);
            });
        });

        formCard.getChildren().addAll(
            formTitle,
            sectionLabel("GUEST INFORMATION"),
            nameF, phoneF, emailF,
            ciLabel, checkIn,
            coLabel, checkOut,
            bookBtn, checkoutBtn, msgLabel
        );

        right.getChildren().add(formCard);
        VBox.setVgrow(formCard, Priority.ALWAYS);

        HBox body = new HBox(left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        root.setCenter(body);

        primaryStage.getScene().setRoot(root);
    }

    // ── BILL DIALOG ───────────────────────────────────────────────────────────
    private void showBillDialog(Booking b, Room r, Runnable onPaid) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Bill Summary");

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color:" + BG + ";-fx-padding:32;");
        root.setPrefWidth(380);

        Label title = new Label("Invoice");
        title.setStyle(FONT_NORMAL + "-fx-font-size:22px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");

        Label hotel = new Label("Hotel Manager");
        hotel.setStyle(FONT_NORMAL + "-fx-font-size:13px;-fx-text-fill:" + MUTED + ";");

        Separator sep1 = new Separator();

        VBox details = new VBox(8);
        details.setStyle(cardStyle() + "-fx-padding:18;");
        details.getChildren().addAll(
            billRow("Guest",        b.getName()),
            billRow("Phone",        b.getPhone()),
            billRow("Email",        b.getEmail()),
            billRow("Room",         "No. " + b.getRoomId() + " (" + b.getRoomType() + ")"),
            billRow("Check-in",     b.getCheckIn().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))),
            billRow("Check-out",    b.getCheckOut().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))),
            billRow("Nights",       String.valueOf(b.getDays())),
            billRow("Rate/Night",   "₹" + String.format("%,.0f", r.getPrice()))
        );

        Separator sep2 = new Separator();

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalLbl = new Label("TOTAL AMOUNT");
        totalLbl.setStyle(FONT_NORMAL + "-fx-font-weight:700;-fx-text-fill:" + TEXT + ";-fx-font-size:14px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label totalVal = new Label("₹" + String.format("%,.0f", b.getTotalBill()));
        totalVal.setStyle(FONT_NORMAL + "-fx-font-weight:700;-fx-text-fill:" + ACCENT + ";-fx-font-size:20px;");
        totalRow.getChildren().addAll(totalLbl, sp, totalVal);

        HBox btnRow = new HBox(12);
        Button cancelBtn = new Button("Cancel");
        Button payBtn    = new Button("Confirm Payment  ₹" + String.format("%,.0f", b.getTotalBill()));
        cancelBtn.setStyle(ghostBtn());
        payBtn.setStyle(primaryBtn());
        payBtn.setPrefWidth(Double.MAX_VALUE);
        HBox.setHgrow(payBtn, Priority.ALWAYS);

        cancelBtn.setOnAction(e -> dialog.close());
        payBtn.setOnAction(e -> {
            b.markPaid();
            r.setStatus("Maintenance");
            startMaintenance(r);
            dialog.close();
            onPaid.run();
            showPaymentSuccess(b);
        });

        btnRow.getChildren().addAll(cancelBtn, payBtn);

        root.getChildren().addAll(title, hotel, sep1, details, sep2, totalRow, btnRow);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private HBox billRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setStyle(FONT_NORMAL + "-fx-font-size:13px;-fx-text-fill:" + MUTED + ";");
        l.setPrefWidth(110);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle(FONT_NORMAL + "-fx-font-size:13px;-fx-text-fill:" + TEXT + ";-fx-font-weight:600;");
        row.getChildren().addAll(l, sp, v);
        return row;
    }

    private void showPaymentSuccess(Booking b) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payment Successful");
        alert.setHeaderText("✓  Thank you, " + b.getName() + "!");
        alert.setContentText("Payment of ₹" + String.format("%,.0f", b.getTotalBill()) +
                " received.\nRoom will be available after maintenance.");
        alert.showAndWait();
    }

    // ── ADMIN LOGIN ───────────────────────────────────────────────────────────
    private void showAdminLogin() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(navbar("Admin Login", this::showHome));

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(360);
        card.setStyle(cardStyle() + "-fx-padding:40;");

        Label ico = new Label("🔐");
        ico.setStyle("-fx-font-size:36px;");

        Label title = new Label("Admin Access");
        title.setStyle(FONT_NORMAL + "-fx-font-size:20px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");

        PasswordField pass = new PasswordField();
        pass.setPromptText("Enter admin password");
        pass.setStyle(fieldStyle());
        pass.setPrefWidth(Double.MAX_VALUE);

        Label msg = new Label();
        msg.setStyle(FONT_NORMAL + "-fx-font-size:13px;-fx-text-fill:" + DANGER + ";");

        Button login = new Button("Login to Admin Panel");
        login.setPrefWidth(Double.MAX_VALUE);
        login.setStyle(primaryBtn());

        login.setOnAction(e -> {
            if (pass.getText().equals("admin123")) {
                showAdminPanel();
            } else {
                msg.setText("✕  Incorrect password");
                pass.clear();
            }
        });
        pass.setOnAction(e -> login.fire());

        card.getChildren().addAll(ico, title, pass, login, msg);

        StackPane center = new StackPane(card);
        root.setCenter(center);
        primaryStage.getScene().setRoot(root);
    }

    // ── ADMIN PANEL ───────────────────────────────────────────────────────────
    private void showAdminPanel() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(navbar("Admin Panel", this::showHome));

        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color:" + BG + ";-fx-tab-min-width:120;");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dashTab  = new Tab("Dashboard", dashboardTab());
        Tab roomsTab = new Tab("Rooms",     roomsAdminTab());

        tabs.getTabs().addAll(dashTab, roomsTab);
        root.setCenter(tabs);
        primaryStage.getScene().setRoot(root);
    }

    // ── DASHBOARD TAB ─────────────────────────────────────────────────────────
    private VBox dashboardTab() {
        VBox v = new VBox(24);
        v.setStyle("-fx-padding:28;-fx-background-color:" + BG + ";");

        long available = rooms.stream().filter(r -> r.getStatus().equals("Available")).count();
        long booked    = rooms.stream().filter(r -> r.getStatus().equals("Booked")).count();
        double revenue = bookings.stream().filter(b -> !b.isActive() && b.isPaid())
                                 .mapToDouble(Booking::getTotalBill).sum();

        HBox stats = new HBox(16);
        stats.getChildren().addAll(
            statCard("🏠", String.valueOf(rooms.size()),  "Total Rooms"),
            statCard("✅", String.valueOf(available),     "Available"),
            statCard("📋", String.valueOf(booked),        "Booked"),
            statCard("💰", "₹" + String.format("%,.0f", revenue), "Revenue Collected")
        );

        // All bookings with search
        Label allLabel = new Label("All Bookings");
        allLabel.setStyle(FONT_NORMAL + "-fx-font-size:16px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField search = styledField("Search by name, room, or status...");
        search.setPrefWidth(320);
        Button searchBtn = new Button("Search");
        searchBtn.setStyle(primaryBtn());
        searchRow.getChildren().addAll(search, searchBtn);

        ListView<String> allList = new ListView<>();
        allList.setStyle(tableStyle() + "-fx-padding:4;");
        VBox.setVgrow(allList, Priority.ALWAYS);

        Runnable refreshList = () -> {
            allList.getItems().clear();
            String q = search.getText().trim().toLowerCase();
            bookings.stream()
                .filter(b -> q.isEmpty() ||
                    b.getName().toLowerCase().contains(q) ||
                    String.valueOf(b.getRoomId()).contains(q) ||
                    (b.isPaid() ? "paid" : b.isActive() ? "active" : "checked out").contains(q))
                .sorted(Comparator.comparingInt(Booking::getBookingId).reversed())
                .forEach(b -> {
                    String status = b.isPaid()
                        ? "PAID  ₹" + String.format("%,.0f", b.getTotalBill())
                        : b.isActive() ? "ACTIVE" : "CHECKED OUT";
                    allList.getItems().add(String.format(
                        "#%-4d  Room %-4d  %-10s  %-20s  %-13s  %s → %s   %s",
                        b.getBookingId(), b.getRoomId(), b.getRoomType(), b.getName(), b.getPhone(),
                        b.getCheckIn().format(DateTimeFormatter.ofPattern("dd MMM")),
                        b.getCheckOut().format(DateTimeFormatter.ofPattern("dd MMM")),
                        status));
                });
            if (allList.getItems().isEmpty()) allList.getItems().add("No bookings found.");
        };

        refreshList.run();
        searchBtn.setOnAction(e -> refreshList.run());
        search.setOnAction(e -> refreshList.run());

        v.getChildren().addAll(heading("Dashboard"), stats, allLabel, searchRow, allList);
        return v;
    }

    // ── ROOMS ADMIN TAB ───────────────────────────────────────────────────────
    private VBox roomsAdminTab() {
        VBox v = new VBox(18);
        v.setStyle("-fx-padding:28;-fx-background-color:" + BG + ";");

        TableView<Room> table = createRoomTable();
        table.setPrefHeight(280);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Add room form
        VBox formCard = new VBox(14);
        formCard.setStyle(cardStyle() + "-fx-padding:22;");
        formCard.setMaxWidth(600);

        Label formTitle = new Label("Add New Room");
        formTitle.setStyle(FONT_NORMAL + "-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");

        HBox fields = new HBox(12);
        TextField idF    = styledField("Room No.");
        TextField typeF  = styledField("Type (Single/Double...)");
        TextField priceF = styledField("Price / Night");
        idF.setPrefWidth(100);
        typeF.setPrefWidth(180);
        priceF.setPrefWidth(140);

        Button addBtn = new Button("Add Room");
        addBtn.setStyle(primaryBtn());

        Label addMsg = new Label();
        addMsg.setStyle(FONT_NORMAL + "-fx-font-size:13px;");

        fields.getChildren().addAll(idF, typeF, priceF, addBtn);
        fields.setAlignment(Pos.CENTER_LEFT);
        formCard.getChildren().addAll(formTitle, fields, addMsg);

        addBtn.setOnAction(e -> {
            try {
                int rid = Integer.parseInt(idF.getText().trim());
                String rtype = typeF.getText().trim();
                double rprice = Double.parseDouble(priceF.getText().trim());

                if (rtype.isEmpty()) { showMsg(addMsg, "⚠  Enter room type", false); return; }
                boolean dup = rooms.stream().anyMatch(r -> r.getRoomId() == rid);
                if (dup) { showMsg(addMsg, "⚠  Room ID already exists", false); return; }

                rooms.add(new Room(rid, rtype, rprice, "Available"));
                saveData();
                table.refresh();
                idF.clear(); typeF.clear(); priceF.clear();
                showMsg(addMsg, "✓  Room " + rid + " added", true);
            } catch (NumberFormatException ex) {
                showMsg(addMsg, "⚠  Invalid Room No. or Price", false);
            }
        });

        // Delete selected room
        Button delBtn = new Button("Delete Selected Room");
        delBtn.setStyle(dangerBtn());
        delBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { showMsg(addMsg, "⚠  Select a room to delete", false); return; }
            if (r.getStatus().equals("Booked")) { showMsg(addMsg, "⚠  Cannot delete a booked room", false); return; }
            rooms.remove(r);
            saveData();
            table.refresh();
            showMsg(addMsg, "✓  Room deleted", true);
        });

        HBox actionRow = new HBox(10);
        actionRow.getChildren().add(delBtn);

        v.getChildren().addAll(heading("Room Management"), table, formCard, actionRow);
        return v;
    }

    // ── MAINTENANCE THREAD ────────────────────────────────────────────────────
    private void startMaintenance(Room r) {
        new Thread(() -> {
            try {
                Thread.sleep(10_000); // 10 seconds
                Platform.runLater(() -> {
                    r.setStatus("Available");
                    saveData();
                });
            } catch (InterruptedException ignored) {}
        }, "maintenance-" + r.getRoomId()).start();
    }

    // ── PERSISTENCE ───────────────────────────────────────────────────────────
    private void saveData() {
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream("data.dat"))) {
            o.writeObject(new ArrayList<>(rooms));
            o.writeObject(bookings);
        } catch (Exception e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try (ObjectInputStream o = new ObjectInputStream(new FileInputStream("data.dat"))) {
            rooms    = FXCollections.observableArrayList((ArrayList<Room>) o.readObject());
            bookings = (ArrayList<Booking>) o.readObject();
        } catch (Exception e) {
            System.out.println("Starting fresh.");
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(fieldStyle());
        return f;
    }

    private DatePicker styledDate() {
        DatePicker dp = new DatePicker();
        dp.setStyle(fieldStyle());
        dp.setPrefWidth(Double.MAX_VALUE);
        return dp;
    }

    private void showMsg(Label l, String text, boolean success) {
        l.setText(text);
        l.setStyle(FONT_NORMAL + "-fx-font-size:13px;-fx-text-fill:" +
                   (success ? SUCCESS : DANGER) + ";");
    }

    private void styleToggle(Button active, Button inactive) {
        active.setStyle(primaryBtn());
        inactive.setStyle(ghostBtn());
    }
}