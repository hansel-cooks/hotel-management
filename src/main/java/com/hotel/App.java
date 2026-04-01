package com.hotel;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
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
import java.util.stream.Collectors;

public class App extends Application {

    private Stage primaryStage;

    // ── GENERICS + COLLECTIONS ────────────────────────────────────────────────
    static ObservableList<Room> rooms    = FXCollections.observableArrayList();
    static List<Booking>        bookings = new ArrayList<>();
    static Map<Integer, Room>   roomIndex = new HashMap<>();

    // ── MULTITHREADING ────────────────────────────────────────────────────────
    private final ScheduledThreadPoolExecutor scheduler =
        new ScheduledThreadPoolExecutor(2, r -> {
            Thread t = new Thread(r, "maintenance-worker");
            t.setDaemon(true);
            return t;
        });

    // ── MULTITHREADING: AtomicInteger for thread-safe maintenance count ───────
    private final AtomicInteger maintenanceCount = new AtomicInteger(0);

    // ── GENERICS + MULTITHREADING: ConcurrentHashMap for cancellable tasks ────
    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> pendingMaintenance =
        new ConcurrentHashMap<>();

    // ── REGEX ─────────────────────────────────────────────────────────────────
    // Compiled once as static constants — Pattern.compile is expensive,
    // reusing the Pattern object is the correct practice.
    // PHONE: exactly 10 digits
    // EMAIL: standard user@domain.ext format
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[0-9]{10}$");
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    // ── Design tokens — Dark, classy palette ──────────────────────────────────
    private static final String BG        = "#0F1923";   // deep navy
    private static final String SURFACE   = "#1A2535";   // card surface
    private static final String SURFACE2  = "#243044";   // elevated card
    private static final String ACCENT    = "#C9A84C";   // gold
    private static final String ACCENT2   = "#A8863A";   // dark gold hover
    private static final String DANGER    = "#E05252";   // red
    private static final String SUCCESS   = "#4CAF82";   // emerald
    private static final String MUTED     = "#8A9BB5";   // blue-grey muted
    private static final String BORDER    = "#2E3F58";   // border
    private static final String TEXT      = "#EDF2F7";   // near white
    private static final String TEXT_SEC  = "#A0B0C8";   // secondary text

    private static final String FONT = "-fx-font-family:'Segoe UI','Helvetica Neue',Arial,sans-serif;";

    // ── Styles ────────────────────────────────────────────────────────────────
    private String cardStyle() {
        return "-fx-background-color:" + SURFACE + ";-fx-background-radius:14;" +
               "-fx-border-color:" + BORDER + ";-fx-border-radius:14;-fx-border-width:1;" +
               "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),18,0,0,4);";
    }
    private String primaryBtn() {
        return FONT + "-fx-background-color:" + ACCENT + ";-fx-text-fill:#0F1923;" +
               "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:10 24;" +
               "-fx-background-radius:8;-fx-cursor:hand;";
    }
    private String dangerBtn() {
        return FONT + "-fx-background-color:" + DANGER + ";-fx-text-fill:white;" +
               "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:10 24;" +
               "-fx-background-radius:8;-fx-cursor:hand;";
    }
    private String successBtn() {
        return FONT + "-fx-background-color:" + SUCCESS + ";-fx-text-fill:#0F1923;" +
               "-fx-font-weight:700;-fx-font-size:13px;-fx-padding:10 24;" +
               "-fx-background-radius:8;-fx-cursor:hand;";
    }
    private String ghostBtn() {
        return FONT + "-fx-background-color:" + SURFACE2 + ";-fx-text-fill:" + TEXT_SEC + ";" +
               "-fx-font-size:13px;-fx-padding:10 18;-fx-background-radius:8;" +
               "-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-border-width:1;" +
               "-fx-cursor:hand;";
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
        l.setStyle(FONT + "-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:" + MUTED +
                   ";-fx-letter-spacing:1.5px;");
        return l;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    public static void main(String[] args) { launch(); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        loadData();
        if (rooms.isEmpty()) seedRooms();
        showHome();
    }

    // ── MULTITHREADING: shutdown executor cleanly on app close ────────────────
    @Override
    public void stop() { scheduler.shutdownNow(); }

    private void seedRooms() {
        // ── ENUM: use RoomStatus constants instead of raw string literals ──────
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
        hero.setStyle(cardStyle() + "-fx-padding:52 50;");

        Label logo = new Label("\uD83C\uDFE8");
        logo.setStyle("-fx-font-size:48px;");

        Label title = new Label("HOTEL MANAGER");
        title.setStyle(FONT + "-fx-font-size:26px;-fx-font-weight:700;" +
                       "-fx-text-fill:" + ACCENT + ";-fx-letter-spacing:3px;");

        Label divider = new Label("─────────────────────");
        divider.setStyle("-fx-text-fill:" + BORDER + ";");

        Label sub = new Label("Premium Hospitality Management");
        sub.setStyle(FONT + "-fx-font-size:13px;-fx-text-fill:" + MUTED + ";");

        // ── GENERICS: typed stream on ObservableList<Room> ────────────────────
        long avail  = rooms.stream()
                          .filter(r -> r.getCurrentStatus() == RoomStatus.AVAILABLE).count();
        long booked = rooms.stream()
                          .filter(r -> r.getCurrentStatus() == RoomStatus.BOOKED).count();

        HBox badges = new HBox(14);
        badges.setAlignment(Pos.CENTER);
        badges.getChildren().addAll(
            badge("✅  " + avail + " Available",  SUCCESS),
            badge("📋  " + booked + " Booked",    ACCENT)
        );

        Button bookingBtn = new Button("Guest Booking");
        Button adminBtn   = new Button("Admin Panel");
        bookingBtn.setPrefWidth(300); adminBtn.setPrefWidth(300);
        bookingBtn.setStyle(primaryBtn()); adminBtn.setStyle(ghostBtn());
        bookingBtn.setOnAction(e -> showBooking());
        adminBtn.setOnAction(e -> showAdminLogin());

        hero.getChildren().addAll(logo, title, divider, sub, badges, bookingBtn, adminBtn);
        root.setCenter(new StackPane(hero));

        primaryStage.setScene(new Scene(root, 960, 650));
        primaryStage.setTitle("Hotel Manager");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private Label badge(String text, String color) {
        Label l = new Label(text);
        l.setStyle(FONT + "-fx-font-size:12px;-fx-font-weight:600;-fx-text-fill:" + color +
                   ";-fx-background-color:" + color + "28;" +
                   "-fx-background-radius:20;-fx-padding:5 14;" +
                   "-fx-border-color:" + color + "55;-fx-border-radius:20;-fx-border-width:1;");
        return l;
    }

    // ── NAV BAR ───────────────────────────────────────────────────────────────
    private HBox navbar(String pageTitle, Runnable onBack) {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:" + SURFACE + ";" +
                     "-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;" +
                     "-fx-padding:15 28;");
        Button back = new Button("← Back");
        back.setStyle(ghostBtn());
        back.setOnAction(e -> onBack.run());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label t = new Label("🏨   " + pageTitle);
        t.setStyle(FONT + "-fx-font-size:14px;-fx-font-weight:700;" +
                   "-fx-text-fill:" + ACCENT + ";-fx-letter-spacing:1px;");
        bar.getChildren().addAll(back, sp, t);
        return bar;
    }

    // ── STAT CARD ─────────────────────────────────────────────────────────────
    private VBox statCard(String icon, String value, String label, String accentColor) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(cardStyle() + "-fx-padding:18 22;" +
                      "-fx-border-color:" + accentColor + "44;" +
                      "-fx-border-left-width:3;");
        card.setPrefWidth(175);
        Label ico = new Label(icon); ico.setStyle("-fx-font-size:20px;");
        Label val = new Label(value);
        val.setStyle(FONT + "-fx-font-size:22px;-fx-font-weight:700;" +
                     "-fx-text-fill:" + accentColor + ";");
        Label lbl = new Label(label);
        lbl.setStyle(FONT + "-fx-font-size:11px;-fx-text-fill:" + MUTED + ";");
        card.getChildren().addAll(ico, val, lbl);
        return card;
    }

    // ── ROOM TABLE ────────────────────────────────────────────────────────────
    private TableView<Room> createRoomTable() {
        TableView<Room> table = new TableView<>(rooms);
        table.setStyle(tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Dark row highlight
        table.setStyle(tableStyle() +
            "-fx-selection-bar:" + ACCENT + "33;" +
            "-fx-selection-bar-non-focused:" + ACCENT + "1A;");

        TableColumn<Room, Integer> id = col("Room No.", "roomId", 90);
        TableColumn<Room, String>  ty = col("Type",    "type",   120);

        TableColumn<Room, Double> pr = new TableColumn<>("Price / Night");
        pr.setCellValueFactory(new PropertyValueFactory<>("price"));
        pr.setPrefWidth(130);
        pr.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double p, boolean empty) {
                super.updateItem(p, empty);
                // ── WRAPPER CLASS: null-safe unboxing before format ───────────
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
                // ── ENUM: color fetched directly from enum constant ───────────
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

        VBox left = new VBox(14);
        left.setStyle("-fx-padding:24 16 24 24;");
        left.setPrefWidth(490);

        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Button showAll   = new Button("All");
        Button showAvail = new Button("Available");
        Button sortPrice = new Button("Sort by Price");
        showAll.setStyle(primaryBtn());
        showAvail.setStyle(ghostBtn());
        sortPrice.setStyle(ghostBtn());

        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.setStyle(fieldStyle() + "-fx-padding:7 10;");
        typeFilter.getItems().addAll("All Types", "Single", "Double", "Deluxe", "Suite");
        typeFilter.setValue("All Types");

        filterRow.getChildren().addAll(showAll, showAvail, sortPrice, typeFilter);

        TableView<Room> table = createRoomTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // ── GENERICS: Predicate<Room> lambda with ObservableList.filtered ─────
        Runnable applyFilter = () -> {
            boolean availOnly = ghostBtn().equals(showAll.getStyle());
            String  typeVal   = typeFilter.getValue();
            table.setItems(rooms.filtered(r -> {
                boolean statusOk = !availOnly || r.getCurrentStatus() == RoomStatus.AVAILABLE;
                boolean typeOk   = "All Types".equals(typeVal) || r.getType().equals(typeVal);
                return statusOk && typeOk;
            }));
        };

        showAll.setOnAction(e   -> { styleToggle(showAll, showAvail); applyFilter.run(); });
        showAvail.setOnAction(e -> { styleToggle(showAvail, showAll); applyFilter.run(); });
        typeFilter.setOnAction(e -> applyFilter.run());

        // ── COMPARABLE ────────────────────────────────────────────────────────
        // Collections.sort uses Room.compareTo() which sorts by price ascending.
        // This works because Room implements Comparable<Room>.
        sortPrice.setOnAction(e -> {
            List<Room> sorted = new ArrayList<>(rooms);
            Collections.sort(sorted);    // uses Room.compareTo() — Comparable
            rooms.setAll(sorted);
            table.setItems(rooms);
        });

        left.getChildren().addAll(heading("Rooms"), filterRow, table);

        // Right: booking form
        VBox right = new VBox(16);
        right.setStyle("-fx-padding:24 24 24 8;");
        right.setPrefWidth(390);

        VBox formCard = new VBox(13);
        formCard.setStyle(cardStyle() + "-fx-padding:26;");

        Label formTitle = new Label("New Booking");
        formTitle.setStyle(FONT + "-fx-font-size:16px;-fx-font-weight:700;" +
                           "-fx-text-fill:" + ACCENT + ";");

        TextField nameF  = styledField("Full Name");
        TextField phoneF = styledField("Phone (10 digits)");
        TextField emailF = styledField("Email Address");
        Label ciLabel    = sectionLabel("CHECK-IN");
        DatePicker checkIn  = styledDate();
        Label coLabel    = sectionLabel("CHECK-OUT");
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

        // BOOK action
        bookBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            try {
                // ── CUSTOM EXCEPTION: validateBooking throws InvalidBookingException
                validateBooking(r, nameF.getText(), phoneF.getText(),
                                emailF.getText(), checkIn.getValue(), checkOut.getValue());

                Booking b = new Booking(bookings.size() + 1, r.getRoomId(), r.getType(),
                        nameF.getText().trim(), phoneF.getText().trim(),
                        emailF.getText().trim(), checkIn.getValue(),
                        checkOut.getValue(), r.getPrice());
                bookings.add(b);
                // ── ENUM ──────────────────────────────────────────────────────
                r.setStatusEnum(RoomStatus.BOOKED);
                roomIndex.put(r.getRoomId(), r);
                table.refresh();
                saveData();
                nameF.clear(); phoneF.clear(); emailF.clear();
                checkIn.setValue(null); checkOut.setValue(null);
                showMsg(msgLabel, "Booking confirmed! Room " + r.getRoomId(), true);

            } catch (InvalidBookingException ex) {
                // ── CUSTOM EXCEPTION: caught here, message shown in UI ─────────
                showMsg(msgLabel, ex.getMessage(), false);
            }
        });

        // CHECKOUT action
        checkoutBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { showMsg(msgLabel, "Select a booked room", false); return; }
            if (r.getCurrentStatus() != RoomStatus.BOOKED) {
                showMsg(msgLabel, "Room is not booked", false); return;
            }
            // ── GENERICS: Optional<Booking> from typed stream ─────────────────
            Optional<Booking> opt = bookings.stream()
                    .filter(x -> x.getRoomId() == r.getRoomId() && x.isActive())
                    .findFirst();
            if (opt.isEmpty()) { showMsg(msgLabel, "No active booking found", false); return; }

            showBillDialog(opt.get(), r, () -> {
                table.refresh();
                saveData();
                showMsg(msgLabel, "Payment complete. Room entering maintenance.", true);
            });
        });

        formCard.getChildren().addAll(
            formTitle, sectionLabel("GUEST INFORMATION"),
            nameF, phoneF, emailF,
            ciLabel, checkIn, coLabel, checkOut,
            bookBtn, checkoutBtn, msgLabel
        );
        right.getChildren().add(formCard);
        VBox.setVgrow(formCard, Priority.ALWAYS);

        HBox body = new HBox(left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        root.setCenter(body);
        primaryStage.getScene().setRoot(root);
    }

    // ── CUSTOM EXCEPTION + REGEX ──────────────────────────────────────────────
    // All booking validation is centralised here.
    // Throws InvalidBookingException with a specific message for each failure.
    // ── REGEX: Matcher used to validate phone and email format ────────────────
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

        // ── REGEX: Matcher.matches() checks the full string against the pattern
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
    private void showBillDialog(Booking b, Room r, Runnable onPaid) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Bill Summary");

        VBox root = new VBox(18);
        root.setStyle("-fx-background-color:" + BG + ";-fx-padding:32;");
        root.setPrefWidth(480);

        Label title = new Label("INVOICE");
        title.setStyle(FONT + "-fx-font-size:24px;-fx-font-weight:700;" +
                       "-fx-text-fill:" + ACCENT + ";-fx-letter-spacing:3px;");
        Label hotel = new Label("Hotel Manager");
        hotel.setStyle(FONT + "-fx-font-size:13px;-fx-text-fill:" + MUTED + ";");

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color:" + BORDER + ";");

        VBox details = new VBox(10);
        details.setStyle(cardStyle() + "-fx-padding:20;");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        details.getChildren().addAll(
            billRow("Guest",      b.getName()),
            billRow("Phone",      b.getPhone()),
            billRow("Email",      b.getEmail()),
            billRow("Room",       "No. " + b.getRoomId() + "  (" + b.getRoomType() + ")"),
            billRow("Check-in",   b.getCheckIn().format(fmt)),
            billRow("Check-out",  b.getCheckOut().format(fmt)),
            // ── WRAPPER CLASS: Long.toString ───────────────────────────────────
            billRow("Nights",     Long.toString(b.getDays())),
            billRow("Rate/Night", "Rs." + String.format("%,.0f", r.getPrice()))
        );

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color:" + BORDER + ";");

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setStyle(cardStyle() + "-fx-padding:14 20;-fx-border-color:" + ACCENT + "55;");
        Label tl = new Label("TOTAL AMOUNT");
        tl.setStyle(FONT + "-fx-font-weight:700;-fx-text-fill:" + TEXT + ";-fx-font-size:13px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label tv = new Label("Rs." + String.format("%,.0f", b.getTotalBill()));
        tv.setStyle(FONT + "-fx-font-weight:700;-fx-text-fill:" + ACCENT + ";-fx-font-size:22px;");
        totalRow.getChildren().addAll(tl, sp, tv);

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER);
        Button cancelBtn   = new Button("Cancel");
        Button downloadBtn = new Button("Download");
        Button payBtn      = new Button("Pay  Rs." + String.format("%,.0f", b.getTotalBill()));
        cancelBtn.setStyle(ghostBtn());
        downloadBtn.setStyle(successBtn());
        payBtn.setStyle(primaryBtn());

        cancelBtn.setOnAction(e -> dialog.close());

        // ── I/O STREAMS: export bill to text file ─────────────────────────────
        downloadBtn.setOnAction(e -> {
            String path = exportBill(b, r);
            Alert a = new Alert(path != null ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            a.setTitle(path != null ? "Saved" : "Failed");
            a.setHeaderText(path != null ? "Bill saved!" : "Export failed");
            a.setContentText(path != null ? "File: " + path : "Check console.");
            a.showAndWait();
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
            alert.setHeaderText("Thank you, " + b.getName() + "!");
            alert.setContentText("Rs." + String.format("%,.0f", b.getTotalBill()) +
                    " received.\nBill saved.\nRoom available after maintenance (10s).");
            alert.showAndWait();
        });

        btnRow.getChildren().addAll(cancelBtn, downloadBtn, payBtn);
        root.getChildren().addAll(title, hotel, sep1, details, sep2, totalRow, btnRow);
        dialog.setScene(new Scene(root));
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private HBox billRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setStyle(FONT + "-fx-font-size:13px;-fx-text-fill:" + MUTED + ";");
        l.setPrefWidth(110);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle(FONT + "-fx-font-size:13px;-fx-text-fill:" + TEXT + ";-fx-font-weight:600;");
        v.setWrapText(true);
        v.setMaxWidth(230);
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

        Label ico   = new Label("🔐"); ico.setStyle("-fx-font-size:38px;");
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
            else { msg.setText("Incorrect password"); pass.clear(); }
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
        VBox v = new VBox(24);
        v.setStyle("-fx-padding:28;-fx-background-color:" + BG + ";");

        // ── GENERICS: typed stream operations ─────────────────────────────────
        long available = rooms.stream()
            .filter(r -> r.getCurrentStatus() == RoomStatus.AVAILABLE).count();
        long booked    = rooms.stream()
            .filter(r -> r.getCurrentStatus() == RoomStatus.BOOKED).count();
        long maint     = rooms.stream()
            .filter(r -> r.getCurrentStatus() == RoomStatus.MAINTENANCE).count();
        double revenue = bookings.stream()
            .filter(b -> !b.isActive() && b.isPaid())
            .mapToDouble(Booking::getTotalBill).sum();

        HBox stats = new HBox(14);
        stats.getChildren().addAll(
            statCard("🏠", String.valueOf(rooms.size()),             "Total Rooms",   TEXT),
            statCard("✅", String.valueOf(available),                "Available",     SUCCESS),
            statCard("📋", String.valueOf(booked),                   "Booked",        ACCENT),
            // ── WRAPPER CLASS: Integer.toString on AtomicInteger ──────────────
            statCard("🔧", Integer.toString(maintenanceCount.get()), "Maintenance",   DANGER),
            statCard("💰", "Rs." + String.format("%,.0f", revenue),  "Revenue",       ACCENT)
        );

        Label allLabel = new Label("All Bookings");
        allLabel.setStyle(FONT + "-fx-font-size:16px;-fx-font-weight:700;-fx-text-fill:" + TEXT + ";");

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField search = styledField("Search by name, room, status...");
        search.setPrefWidth(320);
        Button searchBtn = new Button("Search");
        searchBtn.setStyle(primaryBtn());
        searchRow.getChildren().addAll(search, searchBtn);

        ListView<String> allList = new ListView<>();
        allList.setStyle(tableStyle() + "-fx-padding:4;-fx-control-inner-background:" + SURFACE + ";");
        VBox.setVgrow(allList, Priority.ALWAYS);

        Runnable refreshList = () -> {
            allList.getItems().clear();
            String q = search.getText().trim().toLowerCase();

            // ── ITERATOR ──────────────────────────────────────────────────────
            // Using Iterator explicitly instead of for-each to demonstrate the
            // Iterator pattern. Iterator gives manual control — useful when you
            // need to remove items during traversal (remove() method).
            Iterator<Booking> it = bookings.iterator();
            List<Booking> filtered = new ArrayList<>();
            while (it.hasNext()) {
                Booking b = it.next();
                if (q.isEmpty() ||
                    b.getName().toLowerCase().contains(q) ||
                    // ── WRAPPER CLASS: String.valueOf(int) ────────────────────
                    String.valueOf(b.getRoomId()).contains(q) ||
                    (b.isPaid() ? "paid" : b.isActive() ? "active" : "checked out").contains(q)) {
                    filtered.add(b);
                }
            }

            filtered.stream()
                .sorted(Comparator.comparingInt(Booking::getBookingId).reversed())
                // ── STRING BUILDER: getSummary() uses StringBuilder internally ─
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
        VBox v = new VBox(18);
        v.setStyle("-fx-padding:28;-fx-background-color:" + BG + ";");

        TableView<Room> table = createRoomTable();
        table.setPrefHeight(260);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox formCard = new VBox(14);
        formCard.setStyle(cardStyle() + "-fx-padding:22;");
        formCard.setMaxWidth(660);

        Label formTitle = new Label("Add New Room");
        formTitle.setStyle(FONT + "-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:" + ACCENT + ";");

        HBox fields = new HBox(12);
        TextField idF    = styledField("Room No.");
        TextField typeF  = styledField("Type");
        TextField priceF = styledField("Price / Night");
        idF.setPrefWidth(100); typeF.setPrefWidth(170); priceF.setPrefWidth(140);
        Button addBtn = new Button("Add Room"); addBtn.setStyle(primaryBtn());
        Label addMsg  = new Label();
        addMsg.setStyle(FONT + "-fx-font-size:13px;");
        fields.getChildren().addAll(idF, typeF, priceF, addBtn);
        fields.setAlignment(Pos.CENTER_LEFT);
        formCard.getChildren().addAll(formTitle, fields, addMsg);

        addBtn.setOnAction(e -> {
            try {
                // ── WRAPPER CLASS: Integer.parseInt, Double.parseDouble ────────
                int    rid    = Integer.parseInt(idF.getText().trim());
                String rtype  = typeF.getText().trim();
                double rprice = Double.parseDouble(priceF.getText().trim());
                if (rtype.isEmpty()) { showMsg(addMsg, "Enter room type", false); return; }
                if (roomIndex.containsKey(Integer.valueOf(rid))) {
                    showMsg(addMsg, "Room ID already exists", false); return;
                }
                Room newRoom = new Room(rid, rtype, rprice, RoomStatus.AVAILABLE.getLabel());
                rooms.add(newRoom);
                roomIndex.put(rid, newRoom);
                saveData(); table.refresh();
                idF.clear(); typeF.clear(); priceF.clear();
                showMsg(addMsg, "Room " + rid + " added", true);
            } catch (NumberFormatException ex) {
                showMsg(addMsg, "Invalid Room No. or Price", false);
            }
        });

        HBox editRow = new HBox(10);
        editRow.setAlignment(Pos.CENTER_LEFT);
        TextField newPriceF = styledField("New price");
        newPriceF.setPrefWidth(130);
        Button editBtn = new Button("Update Price"); editBtn.setStyle(ghostBtn());
        editBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { showMsg(addMsg, "Select a room", false); return; }
            try {
                r.setPrice(Double.parseDouble(newPriceF.getText().trim()));
                saveData(); table.refresh();
                showMsg(addMsg, "Price updated for Room " + r.getRoomId(), true);
            } catch (NumberFormatException ex) {
                showMsg(addMsg, "Invalid price", false);
            }
        });
        editRow.getChildren().addAll(newPriceF, editBtn);

        Button delBtn = new Button("Delete Selected Room"); delBtn.setStyle(dangerBtn());
        delBtn.setOnAction(e -> {
            Room r = table.getSelectionModel().getSelectedItem();
            if (r == null) { showMsg(addMsg, "Select a room to delete", false); return; }
            if (r.getCurrentStatus() == RoomStatus.BOOKED) {
                showMsg(addMsg, "Cannot delete a booked room", false); return;
            }
            // ── MULTITHREADING: cancel pending task if room deleted in maintenance
            if (r.getCurrentStatus() == RoomStatus.MAINTENANCE) {
                ScheduledFuture<?> f = pendingMaintenance.remove(r.getRoomId());
                if (f != null) { f.cancel(false); maintenanceCount.decrementAndGet(); }
            }
            rooms.remove(r);
            roomIndex.remove(r.getRoomId());
            saveData(); table.refresh();
            showMsg(addMsg, "Room deleted", true);
        });

        v.getChildren().addAll(heading("Room Management"), table, formCard, editRow, delBtn);
        return v;
    }

    // ── MULTITHREADING ────────────────────────────────────────────────────────
    // ScheduledThreadPoolExecutor instead of raw Thread + sleep.
    // Platform.runLater marshals the Room mutation back to the JavaFX thread.
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

    // ── I/O STREAMS ───────────────────────────────────────────────────────────
    // FileOutputStream -> OutputStreamWriter(UTF-8) -> BufferedWriter -> PrintWriter
    // ── STRING BUILDER: used to build the separator line ──────────────────────
    private String exportBill(Booking b, Room r) {
        String safeName = b.getName().replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = "Bill_" + String.valueOf(b.getBookingId()) + "_" + safeName + ".txt";

        try (FileOutputStream   fos = new FileOutputStream(fileName);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter     bw  = new BufferedWriter(osw);
             PrintWriter        pw  = new PrintWriter(bw)) {

            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            DateTimeFormatter nowFmt  = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

            // ── STRING BUILDER: build the separator line once, reuse it ────────
            StringBuilder sep = new StringBuilder();
            for (int i = 0; i < 52; i++) sep.append("=");
            String line = sep.toString();

            pw.println(line);
            pw.println("          HOTEL MANAGER - INVOICE");
            pw.println(line);
            pw.printf("  Printed on : %s%n",  LocalDateTime.now().format(nowFmt));
            pw.printf("  Booking ID : #%d%n", b.getBookingId());
            pw.println(line);
            pw.println("  GUEST DETAILS");
            pw.println("  " + "-".repeat(48));
            pw.printf("  Name       : %s%n", b.getName());
            pw.printf("  Phone      : %s%n", b.getPhone());
            pw.printf("  Email      : %s%n", b.getEmail());
            pw.println();
            pw.println("  STAY DETAILS");
            pw.println("  " + "-".repeat(48));
            pw.printf("  Room No.   : %d  (%s)%n", r.getRoomId(), r.getType());
            pw.printf("  Floor      : %d%n",        r.getFloor());
            pw.printf("  Check-in   : %s%n",        b.getCheckIn().format(dateFmt));
            pw.printf("  Check-out  : %s%n",        b.getCheckOut().format(dateFmt));
            pw.printf("  Nights     : %d%n",        b.getDays());
            pw.printf("  Rate/Night : Rs.%,.0f%n",  r.getPrice());
            pw.println(line);
            pw.printf("  TOTAL BILL : Rs.%,.0f%n",  b.getTotalBill());
            pw.println(line);
            pw.println("  Status     : " + (b.isPaid() ? "PAID" : "PENDING"));
            pw.println(line);
            pw.println("  Thank you for staying with us!");
            pw.println(line);

        } catch (IOException ex) {
            System.err.println("[BillExporter] Failed: " + ex.getMessage());
            return null;
        }
        return fileName;
    }

    // ── PERSISTENCE (I/O STREAMS + SERIALIZATION) ─────────────────────────────
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