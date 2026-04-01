package com.hotel;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App extends Application {

    private Stage primaryStage;

    static ObservableList<Room> rooms     = FXCollections.observableArrayList();
    static List<Booking>        bookings  = new ArrayList<>();
    static Map<Integer, Room>   roomIndex = new HashMap<>();

    // ── MULTITHREADING ────────────────────────────────────────────────────────
    private final ScheduledThreadPoolExecutor scheduler =
        new ScheduledThreadPoolExecutor(2, r -> {
            Thread t = new Thread(r, "maintenance-worker");
            t.setDaemon(true); return t;
        });

    private final ExecutorService serviceExecutor =
        Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "room-service-worker");
            t.setDaemon(true); return t;
        });

    private final AtomicInteger maintenanceCount = new AtomicInteger(0);

    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> pendingMaintenance =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Future<?>> activeRoomServices =
        new ConcurrentHashMap<>();

    // ── REGEX ─────────────────────────────────────────────────────────────────
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[0-9]{10}$");
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    // ── CLASSY DARK PALETTE ───────────────────────────────────────────────────
    private static final String BG        = "#0D1117";   // deep black-navy
    private static final String SURFACE   = "#161B22";   // card surface
    private static final String SURFACE2  = "#21262D";   // elevated / input bg
    private static final String ACCENT    = "#C9A84C";   // warm gold
    private static final String ACCENT_DIM= "#A8863A";   // gold hover
    private static final String DANGER    = "#F85149";   // red
    private static final String SUCCESS   = "#3FB950";   // green
    private static final String MUTED     = "#8B949E";   // grey text
    private static final String BORDER    = "#30363D";   // border
    private static final String TEXT      = "#E6EDF3";   // near white
    private static final String TEXT_SEC  = "#B1BAC4";   // secondary
    private static final String SERVICE   = "#A371F7";   // purple

    private static final String FONT =
        "-fx-font-family:'Segoe UI','Helvetica Neue',Arial,sans-serif;";

    // ── Style helpers ──────────────────────────────────────────────────────────
    private String cardStyle() {
        return "-fx-background-color:" + SURFACE + ";-fx-background-radius:14;" +
               "-fx-border-color:" + BORDER + ";-fx-border-radius:14;-fx-border-width:1;" +
               "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),20,0,0,4);";
    }
    private String primaryBtn() {
        return FONT + "-fx-background-color:" + ACCENT + ";-fx-text-fill:#0D1117;" +
               "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:10 20;" +
               "-fx-background-radius:8;-fx-cursor:hand;";
    }
    private String dangerBtn() {
        return FONT + "-fx-background-color:" + DANGER + ";-fx-text-fill:white;" +
               "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:10 20;" +
               "-fx-background-radius:8;-fx-cursor:hand;";
    }
    private String successBtn() {
        return FONT + "-fx-background-color:" + SUCCESS + ";-fx-text-fill:#0D1117;" +
               "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:10 20;" +
               "-fx-background-radius:8;-fx-cursor:hand;";
    }
    private String ghostBtn() {
        return FONT + "-fx-background-color:transparent;-fx-text-fill:" + TEXT_SEC + ";" +
               "-fx-font-size:13px;-fx-padding:10 18;-fx-background-radius:8;" +
               "-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-border-width:1;" +
               "-fx-cursor:hand;";
    }
    private String serviceBtnStyle() {
        return FONT + "-fx-background-color:" + SERVICE + ";-fx-text-fill:white;" +
               "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:10 20;" +
               "-fx-background-radius:8;-fx-cursor:hand;";
    }
    private String fieldStyle() {
        return FONT + "-fx-background-color:" + SURFACE2 + ";-fx-border-color:" + BORDER + ";" +
               "-fx-border-radius:8;-fx-background-radius:8;-fx-border-width:1.5;" +
               "-fx-padding:9 13;-fx-font-size:13px;-fx-text-fill:" + TEXT + ";" +
               "-fx-prompt-text-fill:" + MUTED + ";";
    }
    private String tableStyle() {
        return FONT + "-fx-background-color:" + SURFACE + ";-fx-border-color:" + BORDER + ";" +
               "-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;" +
               "-fx-text-fill:" + TEXT + ";";
    }
    private Label heading(String t) {
        Label l = new Label(t);
        l.setStyle(FONT + "-fx-font-size:22px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");
        return l;
    }
    private Label sectionLabel(String t) {
        Label l = new Label(t);
        l.setStyle(FONT + "-fx-font-size:10px;-fx-font-weight:700;" +
                   "-fx-text-fill:" + MUTED + ";-fx-letter-spacing:1.2px;");
        return l;
    }
    private Label chip(String text, String color) {
        Label l = new Label(text);
        l.setStyle(FONT + "-fx-font-size:12px;-fx-font-weight:600;-fx-text-fill:" + color +
                   ";-fx-background-color:" + color + "22;" +
                   "-fx-background-radius:20;-fx-padding:4 12;" +
                   "-fx-border-color:" + color + "55;-fx-border-radius:20;-fx-border-width:1;");
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

    @Override
    public void stop() {
        scheduler.shutdownNow();
        serviceExecutor.shutdownNow();
    }

    private void seedRooms() {
        List.of(
            new Room(101, "Single",  1200, RoomStatus.AVAILABLE.getLabel()),
            new Room(102, "Single",  1200, RoomStatus.AVAILABLE.getLabel()),
            new Room(201, "Double",  2500, RoomStatus.AVAILABLE.getLabel()),
            new Room(202, "Double",  2500, RoomStatus.AVAILABLE.getLabel()),
            new Room(301, "Deluxe",  4000, RoomStatus.AVAILABLE.getLabel()),
            new Room(401, "Suite",   7500, RoomStatus.AVAILABLE.getLabel())
        ).forEach(r -> { rooms.add(r); roomIndex.put(r.getRoomId(), r); });
        saveData();
    }

    // ── HOME ──────────────────────────────────────────────────────────────────
    private void showHome() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");

        VBox hero = new VBox(22);
        hero.setAlignment(Pos.CENTER);
        hero.setMaxWidth(440);
        hero.setStyle(cardStyle() + "-fx-padding:54 52;");

        Label logo = new Label("\uD83C\uDFE8");
        logo.setStyle("-fx-font-size:52px;");

        Label title = new Label("HOTEL MANAGER");
        title.setStyle(FONT + "-fx-font-size:26px;-fx-font-weight:700;" +
                       "-fx-text-fill:" + ACCENT + ";-fx-letter-spacing:4px;");

        Label divider = new Label("───────────────────");
        divider.setStyle("-fx-text-fill:" + BORDER + ";");

        Label sub = new Label("Premium Hospitality Management");
        sub.setStyle(FONT + "-fx-font-size:13px;-fx-text-fill:" + MUTED + ";");

        long avail  = rooms.stream().filter(r -> r.getCurrentStatus() == RoomStatus.AVAILABLE).count();
        long booked = rooms.stream().filter(r -> r.getCurrentStatus() == RoomStatus.BOOKED).count();

        HBox chips = new HBox(12);
        chips.setAlignment(Pos.CENTER);
        chips.getChildren().addAll(
            chip("✅  " + avail + " Available", SUCCESS),
            chip("📋  " + booked + " Booked",   ACCENT)
        );

        Button bookingBtn = new Button("Guest Booking");
        Button adminBtn   = new Button("Admin Panel");
        bookingBtn.setPrefWidth(300); bookingBtn.setStyle(primaryBtn());
        adminBtn.setPrefWidth(300);   adminBtn.setStyle(ghostBtn());

        bookingBtn.setOnAction(e -> showBooking());
        adminBtn.setOnAction(e -> showAdminLogin());

        hero.getChildren().addAll(logo, title, divider, sub, chips, bookingBtn, adminBtn);
        root.setCenter(new StackPane(hero));

        primaryStage.setScene(new Scene(root, 980, 680));
        primaryStage.setTitle("Hotel Manager");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // ── NAVBAR ────────────────────────────────────────────────────────────────
    private HBox navbar(String pageTitle, Runnable onBack) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:" + SURFACE + ";" +
                     "-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;" +
                     "-fx-padding:14 28;");
        Button back = new Button("← Back");
        back.setStyle(ghostBtn());
        back.setOnAction(e -> onBack.run());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label t = new Label("🏨  " + pageTitle);
        t.setStyle(FONT + "-fx-font-size:14px;-fx-font-weight:700;" +
                   "-fx-text-fill:" + ACCENT + ";-fx-letter-spacing:1px;");
        bar.getChildren().addAll(back, sp, t);
        return bar;
    }

    // ── STAT CARD ─────────────────────────────────────────────────────────────
    private VBox statCard(String icon, String value, String label, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(cardStyle() + "-fx-padding:16 20;");
        card.setPrefWidth(168);
        Label ico = new Label(icon); ico.setStyle("-fx-font-size:20px;");
        Label val = new Label(value);
        val.setStyle(FONT + "-fx-font-size:22px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle(FONT + "-fx-font-size:11px;-fx-text-fill:" + MUTED + ";");
        card.getChildren().addAll(ico, val, lbl);
        return card;
    }

    // ── ROOM TABLE ────────────────────────────────────────────────────────────
    // FIX: SortedList + comparatorProperty binding — column-header sort works.
    private TableView<Room> createRoomTable(ObservableList<Room> source) {
        SortedList<Room> sorted = new SortedList<>(source);
        TableView<Room>  table  = new TableView<>(sorted);
        sorted.comparatorProperty().bind(table.comparatorProperty());

        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(36);

        // Style alternating rows + selection via CSS
        table.setStyle(tableStyle() +
            "-fx-selection-bar:" + ACCENT + "33;" +
            "-fx-selection-bar-non-focused:" + ACCENT + "1A;");

        TableColumn<Room, Integer> id = col("Room No.", "roomId", 85);
        TableColumn<Room, String>  ty = col("Type",     "type",  110);

        TableColumn<Room, Double> pr = new TableColumn<>("Price / Night");
        pr.setCellValueFactory(new PropertyValueFactory<>("price"));
        pr.setPrefWidth(130);
        pr.setSortable(true);
        pr.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : "Rs." + String.format("%,.0f", p));
                setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-weight:600;" + FONT);
            }
        });

        TableColumn<Room, String> st = new TableColumn<>("Status");
        st.setCellValueFactory(new PropertyValueFactory<>("status"));
        st.setPrefWidth(120);
        st.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                RoomStatus rs = RoomStatus.fromLabel(s);
                setText(s);
                setStyle("-fx-text-fill:" + rs.getColorHex() +
                         ";-fx-font-weight:700;" + FONT);
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

        // ── Left: room table ──────────────────────────────────────────────────
        VBox left = new VBox(12);
        left.setStyle("-fx-padding:22 14 22 24;");
        left.setPrefWidth(510);

        FilteredList<Room> filteredRooms = new FilteredList<>(rooms, r -> true);
        TableView<Room> table = createRoomTable(filteredRooms);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Filter buttons
        HBox filterRow = new HBox(8);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Button showAll   = new Button("All Rooms");
        Button showAvail = new Button("Available");
        showAll.setStyle(primaryBtn()); showAvail.setStyle(ghostBtn());

        showAll.setOnAction(e -> {
            filteredRooms.setPredicate(r -> true);
            styleToggle(showAll, showAvail);
        });
        showAvail.setOnAction(e -> {
            filteredRooms.setPredicate(r -> r.getCurrentStatus() == RoomStatus.AVAILABLE);
            styleToggle(showAvail, showAll);
        });

        // Sort buttons — price col at index 2
        @SuppressWarnings("unchecked")
        TableColumn<Room, Double> priceCol =
            (TableColumn<Room, Double>) table.getColumns().get(2);

        Button sortAsc   = new Button("₹ Low → High");
        Button sortDesc  = new Button("₹ High → Low");
        Button sortReset = new Button("Reset");
        sortAsc.setStyle(ghostBtn()); sortDesc.setStyle(ghostBtn()); sortReset.setStyle(ghostBtn());

        sortAsc.setOnAction(e -> {
            priceCol.setSortType(TableColumn.SortType.ASCENDING);
            table.getSortOrder().setAll(priceCol);
            styleToggle(sortAsc, sortDesc); sortReset.setStyle(ghostBtn());
        });
        sortDesc.setOnAction(e -> {
            priceCol.setSortType(TableColumn.SortType.DESCENDING);
            table.getSortOrder().setAll(priceCol);
            styleToggle(sortDesc, sortAsc); sortReset.setStyle(ghostBtn());
        });
        sortReset.setOnAction(e -> {
            table.getSortOrder().clear();
            sortAsc.setStyle(ghostBtn()); sortDesc.setStyle(ghostBtn());
        });

        HBox sortRow = new HBox(8, sectionLabel("SORT:"), sortAsc, sortDesc, sortReset);
        sortRow.setAlignment(Pos.CENTER_LEFT);

        filterRow.getChildren().addAll(showAll, showAvail);
        left.getChildren().addAll(heading("Rooms"), filterRow, sortRow, table);

        // ── Right: booking form wrapped in ScrollPane ─────────────────────────
        // FIX: ScrollPane prevents room-service section being cut off
        VBox formInner = new VBox(12);
        formInner.setStyle(cardStyle() + "-fx-padding:24;");
        formInner.setPrefWidth(360);

        Label formTitle = new Label("New Booking");
        formTitle.setStyle(FONT + "-fx-font-size:16px;-fx-font-weight:700;" +
                           "-fx-text-fill:" + ACCENT + ";");

        TextField nameF  = styledField("Full Name");
        TextField phoneF = styledField("Phone (10 digits)");
        TextField emailF = styledField("Email Address");
        DatePicker checkIn  = styledDate();
        DatePicker checkOut = styledDate();

        Label msgLabel = new Label();
        msgLabel.setStyle(FONT + "-fx-font-size:12px;");
        msgLabel.setWrapText(true);

        Button bookBtn     = new Button("Confirm Booking");
        Button checkoutBtn = new Button("Checkout & Pay");
        bookBtn.setPrefWidth(Double.MAX_VALUE);
        checkoutBtn.setPrefWidth(Double.MAX_VALUE);
        bookBtn.setStyle(primaryBtn());
        checkoutBtn.setStyle(ghostBtn());

        // ── Room Service section ───────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + BORDER + ";");

        Label serviceHeading = new Label("🛎  Room Service");
        serviceHeading.setStyle(FONT + "-fx-font-size:14px;-fx-font-weight:700;" +
                                "-fx-text-fill:" + SERVICE + ";");

        Label serviceMsg = new Label("Select a booked room to request service.");
        serviceMsg.setStyle(FONT + "-fx-font-size:12px;-fx-text-fill:" + MUTED + ";");
        serviceMsg.setWrapText(true);

        Button serviceBtn = new Button("Call Room Service  (15s)");
        serviceBtn.setPrefWidth(Double.MAX_VALUE);
        serviceBtn.setStyle(serviceBtnStyle());
        serviceBtn.setDisable(true);

        ProgressBar serviceBar = new ProgressBar(0);
        serviceBar.setPrefWidth(Double.MAX_VALUE);
        serviceBar.setPrefHeight(8);
        serviceBar.setStyle("-fx-accent:" + SERVICE + ";-fx-background-color:" + SURFACE2 + ";" +
                            "-fx-background-radius:4;-fx-border-radius:4;");
        serviceBar.setVisible(false);

        Label countdownLbl = new Label();
        countdownLbl.setStyle(FONT + "-fx-font-size:12px;-fx-font-weight:600;" +
                              "-fx-text-fill:" + SERVICE + ";");

        // Update service button on selection change
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) {
                serviceBtn.setDisable(true);
                serviceMsg.setText("Select a booked room to request service.");
                return;
            }
            boolean isBooked   = sel.getCurrentStatus() == RoomStatus.BOOKED;
            boolean inProgress = activeRoomServices.containsKey(sel.getRoomId());
            serviceBtn.setDisable(!isBooked || inProgress);
            if (inProgress)
                serviceMsg.setText("⏳  Service in progress for Room " + sel.getRoomId());
            else if (isBooked)
                serviceMsg.setText("Room " + sel.getRoomId() + " — ready for service.");
            else
                serviceMsg.setText("Room is not booked — service unavailable.");
        });

        // ── BOOK ──────────────────────────────────────────────────────────────
        bookBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            try {
                validateBooking(r, nameF.getText(), phoneF.getText(),
                                emailF.getText(), checkIn.getValue(), checkOut.getValue());
                Booking b = new Booking(bookings.size() + 1, r.getRoomId(), r.getType(),
                        nameF.getText().trim(), phoneF.getText().trim(),
                        emailF.getText().trim(), checkIn.getValue(),
                        checkOut.getValue(), r.getPrice());
                bookings.add(b);
                r.setStatusEnum(RoomStatus.BOOKED);
                roomIndex.put(r.getRoomId(), r);
                table.refresh();
                saveData();
                nameF.clear(); phoneF.clear(); emailF.clear();
                checkIn.setValue(null); checkOut.setValue(null);
                showMsg(msgLabel, "✓  Booking confirmed! Room " + r.getRoomId(), true);
                serviceBtn.setDisable(false);
                serviceMsg.setText("Room " + r.getRoomId() + " — ready for service.");
            } catch (InvalidBookingException ex) {
                showMsg(msgLabel, ex.getMessage(), false);
            }
        });

        // ── CHECKOUT ──────────────────────────────────────────────────────────
        checkoutBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { showMsg(msgLabel, "⚠  Select a booked room", false); return; }
            if (r.getCurrentStatus() != RoomStatus.BOOKED)
                { showMsg(msgLabel, "⚠  Room is not booked", false); return; }
            Optional<Booking> opt = bookings.stream()
                    .filter(x -> x.getRoomId() == r.getRoomId() && x.isActive())
                    .findFirst();
            if (opt.isEmpty()) { showMsg(msgLabel, "⚠  No active booking found", false); return; }
            showBillDialog(opt.get(), r, () -> {
                table.refresh();
                saveData();
                showMsg(msgLabel, "✓  Payment complete. Room entering maintenance.", true);
                serviceBtn.setDisable(true);
            });
        });

        // ── ROOM SERVICE: MULTITHREADING + SYNCHRONIZATION ────────────────────
        serviceBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null || r.getCurrentStatus() != RoomStatus.BOOKED) {
                showMsg(msgLabel, "⚠  Select a booked room first", false);
                return;
            }
            requestRoomService(r, serviceBtn, serviceBar, countdownLbl, serviceMsg, table);
        });

        formInner.getChildren().addAll(
            formTitle,
            sectionLabel("GUEST INFORMATION"),
            nameF, phoneF, emailF,
            sectionLabel("CHECK-IN"),  checkIn,
            sectionLabel("CHECK-OUT"), checkOut,
            bookBtn, checkoutBtn,
            msgLabel,
            sep,
            serviceHeading, serviceMsg,
            serviceBtn, serviceBar, countdownLbl
        );

        // FIX: wrap in ScrollPane so room service section is never hidden
        ScrollPane scroll = new ScrollPane(formInner);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;-fx-padding:22 22 22 8;");
        scroll.setPrefWidth(410);

        HBox body = new HBox(left, scroll);
        HBox.setHgrow(left, Priority.ALWAYS);
        root.setCenter(body);
        primaryStage.getScene().setRoot(root);
    }

    // ── ROOM SERVICE: synchronized entry point ────────────────────────────────
    private synchronized void requestRoomService(
            Room room, Button serviceBtn, ProgressBar serviceBar,
            Label countdownLbl, Label serviceMsg, TableView<Room> table) {

        Integer key = room.getRoomId();
        if (activeRoomServices.containsKey(key)) return;

        room.setStatusEnum(RoomStatus.ROOM_SERVICE);
        serviceBtn.setDisable(true);
        serviceBar.setVisible(true);
        serviceBar.setProgress(0);
        table.refresh();

        Future<?> future = serviceExecutor.submit(() -> {
            try {
                final int total = 15;
                for (int sec = 0; sec <= total; sec++) {
                    final int elapsed = sec;
                    Platform.runLater(() -> {
                        int remaining = total - elapsed;
                        serviceBar.setProgress((double) elapsed / total);
                        countdownLbl.setText("🛎  Service in progress… " + remaining + "s remaining");
                        serviceMsg.setText("Room service active for Room " + room.getRoomId());
                    });
                    if (sec < total) Thread.sleep(1_000);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                finishRoomService(key, room, serviceBtn, serviceBar, countdownLbl, serviceMsg, table);
            }
        });

        activeRoomServices.put(key, future);
    }

    // ── ROOM SERVICE: synchronized cleanup ────────────────────────────────────
    private synchronized void finishRoomService(
            Integer key, Room room, Button serviceBtn, ProgressBar serviceBar,
            Label countdownLbl, Label serviceMsg, TableView<Room> table) {

        activeRoomServices.remove(key);
        Platform.runLater(() -> {
            room.setStatusEnum(RoomStatus.BOOKED);
            serviceBar.setProgress(1.0);
            serviceBar.setVisible(false);
            countdownLbl.setText("");
            serviceMsg.setText("✓  Service complete for Room " + room.getRoomId());
            serviceBtn.setDisable(false);
            table.refresh();
            saveData();
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Service Complete");
            a.setHeaderText("Room " + room.getRoomId() + " — Service Done");
            a.setContentText("Room service has been completed successfully.");
            a.show();
        });
    }

    // ── VALIDATION ────────────────────────────────────────────────────────────
    private void validateBooking(Room r, String name, String phone,
                                  String email, LocalDate checkIn, LocalDate checkOut)
            throws InvalidBookingException {
        if (r == null)
            throw new InvalidBookingException("⚠  Select a room first");
        if (r.getCurrentStatus() != RoomStatus.AVAILABLE)
            throw new InvalidBookingException("⚠  Room " + r.getRoomId() + " is not available");
        if (name == null || name.trim().isEmpty())
            throw new InvalidBookingException("⚠  Enter guest name");
        if (phone == null || phone.trim().isEmpty())
            throw new InvalidBookingException("⚠  Enter phone number");
        Matcher phoneMatcher = PHONE_PATTERN.matcher(phone.trim());
        if (!phoneMatcher.matches())
            throw new InvalidBookingException("⚠  Phone must be exactly 10 digits");
        if (email == null || email.trim().isEmpty())
            throw new InvalidBookingException("⚠  Enter email address");
        Matcher emailMatcher = EMAIL_PATTERN.matcher(email.trim());
        if (!emailMatcher.matches())
            throw new InvalidBookingException("⚠  Enter a valid email (e.g. name@domain.com)");
        if (checkIn == null || checkOut == null)
            throw new InvalidBookingException("⚠  Select check-in and check-out dates");
        if (!checkOut.isAfter(checkIn))
            throw new InvalidBookingException("⚠  Check-out must be after check-in");
    }

    // ── BILL DIALOG ───────────────────────────────────────────────────────────
    // FIX: buttons had explicit min/pref widths — text was clipped to "..." because
    // the HBox was trying to shrink buttons below their text length.
    // Solution: give each button an explicit setPrefWidth() and use
    // setMinWidth(Button.USE_PREF_SIZE) so JavaFX never clips them.
    private void showBillDialog(Booking b, Room r, Runnable onPaid) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Invoice — Room " + r.getRoomId());

        VBox root = new VBox(16);
        root.setStyle("-fx-background-color:" + BG + ";-fx-padding:32;");
        root.setPrefWidth(460);

        // Header
        Label title = new Label("INVOICE");
        title.setStyle(FONT + "-fx-font-size:26px;-fx-font-weight:700;" +
                       "-fx-text-fill:" + ACCENT + ";-fx-letter-spacing:4px;");
        Label hotel = new Label("Hotel Manager  ·  Premium Hospitality");
        hotel.setStyle(FONT + "-fx-font-size:12px;-fx-text-fill:" + MUTED + ";");

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color:" + BORDER + ";");

        // Bill details card
        VBox details = new VBox(10);
        details.setStyle(cardStyle() + "-fx-padding:20;");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        details.getChildren().addAll(
            billRow("Guest",       b.getName()),
            billRow("Phone",       b.getPhone()),
            billRow("Email",       b.getEmail()),
            billRow("Room",        "No. " + b.getRoomId() + "  (" + b.getRoomType() + ")"),
            billRow("Check-in",    b.getCheckIn().format(fmt)),
            billRow("Check-out",   b.getCheckOut().format(fmt)),
            billRow("Nights",      Long.toString(b.getDays())),
            billRow("Rate / Night","Rs." + String.format("%,.0f", r.getPrice()))
        );

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color:" + BORDER + ";");

        // Total
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle(cardStyle() + "-fx-padding:16 20;" +
                          "-fx-border-color:" + ACCENT + "44;");
        Label tl = new Label("TOTAL AMOUNT");
        tl.setStyle(FONT + "-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:" + TEXT_SEC + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label tv = new Label("Rs." + String.format("%,.0f", b.getTotalBill()));
        tv.setStyle(FONT + "-fx-font-weight:700;-fx-text-fill:" + ACCENT + ";-fx-font-size:24px;");
        totalRow.getChildren().addAll(tl, sp, tv);

        // ── BILL BUTTON FIX ───────────────────────────────────────────────────
        // Each button gets an explicit prefWidth AND minWidth = USE_PREF_SIZE.
        // This stops JavaFX from shrinking them and clipping text to "...".
        // The three buttons share the full width via an HBox with equal grow.
        Button cancelBtn   = new Button("Cancel");
        Button downloadBtn = new Button("Download Bill");
        Button payBtn      = new Button("Confirm Payment");

        for (Button btn : new Button[]{cancelBtn, downloadBtn, payBtn}) {
            btn.setPrefWidth(130);
            btn.setMinWidth(Button.USE_PREF_SIZE);  // never shrink below pref
            HBox.setHgrow(btn, Priority.ALWAYS);    // share available space equally
        }

        cancelBtn.setStyle(ghostBtn());
        downloadBtn.setStyle(successBtn());
        payBtn.setStyle(primaryBtn());

        Label billMsg = new Label();
        billMsg.setStyle(FONT + "-fx-font-size:12px;");
        billMsg.setWrapText(true);

        HBox btnRow = new HBox(10, cancelBtn, downloadBtn, payBtn);
        btnRow.setAlignment(Pos.CENTER);

        cancelBtn.setOnAction(e -> dialog.close());

        // DOWNLOAD — I/O Streams
        downloadBtn.setOnAction(e -> {
            String path = exportBill(b, r);
            if (path != null) {
                showMsg(billMsg, "✓  Bill saved to: " + path, true);
            } else {
                showMsg(billMsg, "✕  Export failed — check console", false);
            }
        });

        payBtn.setOnAction(e -> {
            b.markPaid();
            r.setStatusEnum(RoomStatus.MAINTENANCE);
            exportBill(b, r);
            scheduleMaintenance(r);
            dialog.close();
            onPaid.run();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Payment Successful");
            alert.setHeaderText("✓  Thank you, " + b.getName() + "!");
            alert.setContentText("Rs." + String.format("%,.0f", b.getTotalBill())
                    + " received.\nBill exported.\nRoom available after maintenance (10s).");
            alert.showAndWait();
        });

        root.getChildren().addAll(title, hotel, sep1, details, sep2, totalRow, billMsg, btnRow);
        dialog.setScene(new Scene(root));
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private HBox billRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setStyle(FONT + "-fx-font-size:13px;-fx-text-fill:" + MUTED + ";");
        l.setPrefWidth(115);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle(FONT + "-fx-font-size:13px;-fx-text-fill:" + TEXT + ";-fx-font-weight:600;");
        v.setWrapText(true); v.setMaxWidth(230);
        row.getChildren().addAll(l, sp, v);
        return row;
    }

    // ── ADMIN LOGIN ───────────────────────────────────────────────────────────
    private void showAdminLogin() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(navbar("Admin Login", this::showHome));

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(380);
        card.setStyle(cardStyle() + "-fx-padding:44;");

        Label ico   = new Label("🔐"); ico.setStyle("-fx-font-size:40px;");
        Label title = new Label("ADMIN ACCESS");
        title.setStyle(FONT + "-fx-font-size:18px;-fx-font-weight:700;" +
                       "-fx-text-fill:" + ACCENT + ";-fx-letter-spacing:3px;");

        PasswordField pass = new PasswordField();
        pass.setPromptText("Enter admin password");
        pass.setStyle(fieldStyle()); pass.setPrefWidth(Double.MAX_VALUE);

        Label msg = new Label();
        msg.setStyle(FONT + "-fx-font-size:13px;-fx-text-fill:" + DANGER + ";");

        Button login = new Button("Login");
        login.setPrefWidth(Double.MAX_VALUE); login.setStyle(primaryBtn());

        login.setOnAction(e -> {
            if (pass.getText().equals("admin123")) showAdminPanel();
            else { msg.setText("✕  Incorrect password"); pass.clear(); }
        });
        pass.setOnAction(e -> login.fire());

        card.getChildren().addAll(ico, title, pass, login, msg);
        root.setCenter(new StackPane(card));
        primaryStage.getScene().setRoot(root);
    }

    // ── ADMIN PANEL ───────────────────────────────────────────────────────────
    private void showAdminPanel() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(navbar("Admin Panel", this::showHome));
        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color:" + BG + ";-fx-tab-min-width:130;");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            new Tab("Dashboard", dashboardTab()),
            new Tab("Rooms",     roomsAdminTab())
        );
        root.setCenter(tabs);
        primaryStage.getScene().setRoot(root);
    }

    // ── DASHBOARD TAB ─────────────────────────────────────────────────────────
    private VBox dashboardTab() {
        VBox v = new VBox(22);
        v.setStyle("-fx-padding:26;-fx-background-color:" + BG + ";");

        long available = rooms.stream()
            .filter(r -> r.getCurrentStatus() == RoomStatus.AVAILABLE).count();
        long booked    = rooms.stream()
            .filter(r -> r.getCurrentStatus() == RoomStatus.BOOKED).count();
        long maint     = rooms.stream()
            .filter(r -> r.getCurrentStatus() == RoomStatus.MAINTENANCE).count();
        double revenue = bookings.stream()
            .filter(b -> !b.isActive() && b.isPaid())
            .mapToDouble(Booking::getTotalBill).sum();

        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            statCard("🏠", String.valueOf(rooms.size()),  "Total Rooms",  TEXT),
            statCard("✅", String.valueOf(available),      "Available",    SUCCESS),
            statCard("📋", String.valueOf(booked),         "Booked",       ACCENT),
            statCard("🔧", String.valueOf(maint),          "Maintenance",  DANGER),
            statCard("💰", "Rs." + String.format("%,.0f", revenue), "Revenue", ACCENT)
        );

        Label allLabel = new Label("All Bookings");
        allLabel.setStyle(FONT + "-fx-font-size:16px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField search = styledField("Search by name, room, status…");
        search.setPrefWidth(320);
        Button searchBtn = new Button("Search");
        searchBtn.setStyle(primaryBtn());
        searchRow.getChildren().addAll(search, searchBtn);

        ListView<String> allList = new ListView<>();
        allList.setStyle(tableStyle() + "-fx-padding:4;" +
                         "-fx-control-inner-background:" + SURFACE + ";");
        VBox.setVgrow(allList, Priority.ALWAYS);

        Runnable refreshList = () -> {
            allList.getItems().clear();
            String q = search.getText().trim().toLowerCase();
            Iterator<Booking> it = bookings.iterator();
            List<Booking> filtered = new ArrayList<>();
            while (it.hasNext()) {
                Booking b = it.next();
                if (q.isEmpty() ||
                    b.getName().toLowerCase().contains(q) ||
                    String.valueOf(b.getRoomId()).contains(q) ||
                    (b.isPaid() ? "paid" : b.isActive() ? "active" : "checked out").contains(q))
                    filtered.add(b);
            }
            filtered.stream()
                .sorted(Comparator.comparingInt(Booking::getBookingId).reversed())
                .forEach(b -> allList.getItems().add(b.getSummary()));
            if (allList.getItems().isEmpty())
                allList.getItems().add("No bookings found.");
        };
        refreshList.run();
        searchBtn.setOnAction(e -> refreshList.run());
        search.setOnAction(e -> refreshList.run());

        v.getChildren().addAll(heading("Dashboard"), stats, allLabel, searchRow, allList);
        return v;
    }

    // ── ROOMS ADMIN TAB ───────────────────────────────────────────────────────
    private VBox roomsAdminTab() {
        VBox v = new VBox(16);
        v.setStyle("-fx-padding:26;-fx-background-color:" + BG + ";");

        TableView<Room> table = createRoomTable(rooms);
        table.setPrefHeight(250);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox formCard = new VBox(12);
        formCard.setStyle(cardStyle() + "-fx-padding:22;");
        formCard.setMaxWidth(660);

        Label formTitle = new Label("Add New Room");
        formTitle.setStyle(FONT + "-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:" + ACCENT + ";");

        HBox fields = new HBox(10);
        TextField idF    = styledField("Room No.");
        TextField typeF  = styledField("Type");
        TextField priceF = styledField("Price / Night");
        idF.setPrefWidth(90); typeF.setPrefWidth(160); priceF.setPrefWidth(140);
        Button addBtn = new Button("Add Room"); addBtn.setStyle(primaryBtn());
        Label  addMsg = new Label(); addMsg.setStyle(FONT + "-fx-font-size:12px;");
        fields.getChildren().addAll(idF, typeF, priceF, addBtn);
        fields.setAlignment(Pos.CENTER_LEFT);
        formCard.getChildren().addAll(formTitle, fields, addMsg);

        addBtn.setOnAction(e -> {
            try {
                int    rid    = Integer.parseInt(idF.getText().trim());
                String rtype  = typeF.getText().trim();
                double rprice = Double.parseDouble(priceF.getText().trim());
                if (rtype.isEmpty()) { showMsg(addMsg, "⚠  Enter room type", false); return; }
                if (roomIndex.containsKey(rid)) { showMsg(addMsg, "⚠  Room ID already exists", false); return; }
                Room newRoom = new Room(rid, rtype, rprice, RoomStatus.AVAILABLE.getLabel());
                rooms.add(newRoom); roomIndex.put(rid, newRoom);
                saveData(); table.refresh();
                idF.clear(); typeF.clear(); priceF.clear();
                showMsg(addMsg, "✓  Room " + rid + " added", true);
            } catch (NumberFormatException ex) {
                showMsg(addMsg, "⚠  Invalid Room No. or Price", false);
            }
        });

        HBox editRow = new HBox(10);
        editRow.setAlignment(Pos.CENTER_LEFT);
        TextField newPriceF = styledField("New price"); newPriceF.setPrefWidth(130);
        Button editBtn = new Button("Update Price"); editBtn.setStyle(ghostBtn());
        editBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { showMsg(addMsg, "⚠  Select a room", false); return; }
            try {
                r.setPrice(Double.parseDouble(newPriceF.getText().trim()));
                saveData(); table.refresh();
                showMsg(addMsg, "✓  Price updated", true);
            } catch (NumberFormatException ex) {
                showMsg(addMsg, "⚠  Invalid price", false);
            }
        });
        editRow.getChildren().addAll(newPriceF, editBtn);

        Button delBtn = new Button("Delete Selected Room"); delBtn.setStyle(dangerBtn());
        delBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { showMsg(addMsg, "⚠  Select a room to delete", false); return; }
            if (r.getCurrentStatus() == RoomStatus.BOOKED)
                { showMsg(addMsg, "⚠  Cannot delete a booked room", false); return; }
            if (r.getCurrentStatus() == RoomStatus.MAINTENANCE) {
                ScheduledFuture<?> f = pendingMaintenance.remove(r.getRoomId());
                if (f != null) { f.cancel(false); maintenanceCount.decrementAndGet(); }
            }
            rooms.remove(r); roomIndex.remove(r.getRoomId());
            saveData(); table.refresh();
            showMsg(addMsg, "✓  Room deleted", true);
        });

        v.getChildren().addAll(heading("Room Management"), table, formCard, editRow, delBtn);
        return v;
    }

    // ── MAINTENANCE ───────────────────────────────────────────────────────────
    private void scheduleMaintenance(Room r) {
        maintenanceCount.incrementAndGet();
        ScheduledFuture<?> future = scheduler.schedule(() ->
            Platform.runLater(() -> {
                r.setStatusEnum(RoomStatus.AVAILABLE);
                maintenanceCount.decrementAndGet();
                pendingMaintenance.remove(r.getRoomId());
                saveData();
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Ready");
                a.setHeaderText("Room " + r.getRoomId() + " is Available");
                a.setContentText("Maintenance complete. Room ready for new bookings.");
                a.show();
            }),
        10, TimeUnit.SECONDS);
        pendingMaintenance.put(r.getRoomId(), future);
    }

    // ── I/O STREAMS: export bill ───────────────────────────────────────────────
    private String exportBill(Booking b, Room r) {
        String safeName = b.getName().replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = "Bill_" + b.getBookingId() + "_" + safeName + ".txt";

        try (FileOutputStream   fos = new FileOutputStream(fileName);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter      bw  = new BufferedWriter(osw);
             PrintWriter         pw  = new PrintWriter(bw)) {

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            DateTimeFormatter nowFmt  = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

            StringBuilder sep = new StringBuilder();
            for (int i = 0; i < 52; i++) sep.append("═");
            String line = sep.toString();

            pw.println(line);
            pw.println("           HOTEL MANAGER — INVOICE");
            pw.println(line);
            pw.printf("  Printed   : %s%n",  LocalDateTime.now().format(nowFmt));
            pw.printf("  Booking # : #%d%n", b.getBookingId());
            pw.println(line);
            pw.printf("  Guest     : %s%n",  b.getName());
            pw.printf("  Phone     : %s%n",  b.getPhone());
            pw.printf("  Email     : %s%n",  b.getEmail());
            pw.println(line);
            pw.printf("  Room      : %d  (%s)%n", r.getRoomId(), r.getType());
            pw.printf("  Floor     : %d%n",        r.getFloor());
            pw.printf("  Check-in  : %s%n",        b.getCheckIn().format(dateFmt));
            pw.printf("  Check-out : %s%n",        b.getCheckOut().format(dateFmt));
            pw.printf("  Nights    : %d%n",        b.getDays());
            pw.printf("  Rate/Night: Rs.%,.0f%n",  r.getPrice());
            pw.println(line);
            pw.printf("  TOTAL     : Rs.%,.0f%n",  b.getTotalBill());
            pw.println("  Status    : " + (b.isPaid() ? "PAID ✓" : "PENDING"));
            pw.println(line);
            pw.println("  Thank you for choosing Hotel Manager!");
            pw.println(line);

        } catch (IOException ex) {
            System.err.println("[BillExporter] Failed: " + ex.getMessage());
            return null;
        }
        return fileName;
    }

    // ── PERSISTENCE ───────────────────────────────────────────────────────────
    private void saveData() {
        try (ObjectOutputStream o = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream("data.dat")))) {
            o.writeObject(new ArrayList<>(rooms));
            o.writeObject(bookings);
        } catch (Exception e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try (ObjectInputStream o = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream("data.dat")))) {
            List<Room> loadedRooms = (ArrayList<Room>) o.readObject();
            rooms.setAll(loadedRooms);
            loadedRooms.forEach(r -> roomIndex.put(r.getRoomId(), r));
            bookings = (ArrayList<Booking>) o.readObject();
        } catch (Exception e) {
            System.out.println("Starting fresh.");
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt); f.setStyle(fieldStyle());
        return f;
    }
    private DatePicker styledDate() {
        DatePicker dp = new DatePicker();
        dp.setStyle(fieldStyle()); dp.setPrefWidth(Double.MAX_VALUE);
        return dp;
    }
    private void showMsg(Label l, String text, boolean success) {
        l.setText(text);
        l.setStyle(FONT + "-fx-font-size:12px;-fx-text-fill:" + (success ? SUCCESS : DANGER) + ";");
    }
    private void styleToggle(Button active, Button inactive) {
        active.setStyle(primaryBtn()); inactive.setStyle(ghostBtn());
    }
}