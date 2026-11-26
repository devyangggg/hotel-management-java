package com.example.hotel.ui;

import com.example.hotel.dao.Database;
import com.example.hotel.dao.JdbcReservationDao;
import com.example.hotel.model.Reservation;
import com.example.hotel.model.ReservationStatus;
import com.example.hotel.model.Room;
import com.example.hotel.service.ReservationService;
import com.example.hotel.service.ValidationException;
import com.example.hotel.service.RoomUnavailableException;
import com.example.hotel.util.AppConfig;
import com.example.hotel.util.ReservationFiles;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

public final class HotelApplication extends Application {
    private static final Logger LOG = Logger.getLogger(HotelApplication.class.getName());
    private ReservationService service;
    private final TableView<Room> roomTable = new TableView<>();
    private final TableView<Reservation> reservationTable = new TableView<>();
    private final DatePicker searchIn = new DatePicker(LocalDate.now());
    private final DatePicker searchOut = new DatePicker(LocalDate.now().plusDays(1));
    private final TextField minimumGuests = new TextField();
    private final DatePicker bookingIn = new DatePicker(LocalDate.now());
    private final DatePicker bookingOut = new DatePicker(LocalDate.now().plusDays(1));
    private final TextField guestName = new TextField();
    private final TextField phone = new TextField();
    private final TextField guests = new TextField("1");
    private final TextField reservationSearch = new TextField();
    private final TextArea snapshotPreview = new TextArea();
    private final Label roomChoice = new Label("Select a room from Rooms");
    private final Label estimate = new Label("Select a room to see the total");
    private final Label roomMessage = new Label("Loading rooms…");
    private final Label reservationMessage = new Label("Loading reservations…");
    private final Button bookButton = new Button("Book room");
    private Room selectedRoom;
    private boolean bookingInProgress;
    private TabPane tabs;

    @Override
    public void init() {
        try (var settings = getClass().getResourceAsStream("/logging.properties")) {
            if (settings != null) LogManager.getLogManager().readConfiguration(settings);
        } catch (IOException error) {
            LOG.log(Level.WARNING, "Could not load logging settings", error);
        }
        LOG.info("Application starting");
    }

    @Override
    public void start(Stage stage) {
        tabs = new TabPane(roomsTab(), bookingTab(), reservationsTab());
        stage.setTitle("Hotel Reservation System");
        stage.setScene(new Scene(tabs, 940, 650));
        stage.show();
        runTask(this::createService, loaded -> {
            service = loaded;
            refreshRooms();
            refreshReservations();
        }, null);
    }

    @Override
    public void stop() {
        LOG.info("Application stopped");
    }

    private ReservationService createService() throws IOException, SQLException {
        Database database = new Database(AppConfig.databasePath());
        database.initialize();
        return new ReservationService(new JdbcReservationDao(database), Clock.systemDefaultZone());
    }

    private void configureRoomTable() {
        roomTable.setPlaceholder(new Label("No rooms found for these dates"));
        TableColumn<Room, String> number = new TableColumn<>("Room");
        number.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getNumber()));
        TableColumn<Room, String> description = new TableColumn<>("Type / amenities");
        description.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().description()));
        TableColumn<Room, Integer> capacity = new TableColumn<>("Capacity");
        capacity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCapacity()));
        TableColumn<Room, Integer> price = new TableColumn<>("₹ / night");
        price.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getNightlyPriceRupees()));
        roomTable.getColumns().addAll(number, description, capacity, price);
        roomTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        roomTable.getSelectionModel().selectedItemProperty().addListener((observable, old, room) -> {
            selectedRoom = room;
            updateEstimate();
        });
    }

    private Tab roomsTab() {
        configureRoomTable();
        minimumGuests.setPromptText("Any");
        minimumGuests.setPrefWidth(75);
        Button showAll = new Button("All rooms");
        showAll.setOnAction(event -> refreshRooms());
        Button search = new Button("Search available");
        search.setOnAction(event -> searchAvailableRooms());
        Button choose = new Button("Reserve selected room");
        choose.setOnAction(event -> chooseSelectedRoom());
        HBox filters = new HBox(10, new Label("Check-in"), searchIn, new Label("Check-out"),
                searchOut, new Label("Guests"), minimumGuests, search, showAll);
        VBox top = new VBox(10, filters, roomMessage);
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(15));
        pane.setTop(top);
        pane.setCenter(roomTable);
        pane.setBottom(choose);
        return tab("Rooms", pane);
    }

    private Tab bookingTab() {
        guestName.setPromptText("Full name");
        phone.setPromptText("7–15 digits, optional leading +");
        bookingIn.valueProperty().addListener((observable, old, value) -> updateEstimate());
        bookingOut.valueProperty().addListener((observable, old, value) -> updateEstimate());
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Room"), roomChoice);
        form.addRow(1, new Label("Check-in"), bookingIn);
        form.addRow(2, new Label("Check-out"), bookingOut);
        form.addRow(3, new Label("Guest name"), guestName);
        form.addRow(4, new Label("Phone"), phone);
        form.addRow(5, new Label("Number of guests"), guests);
        form.addRow(6, new Label("Estimate"), estimate);
        bookButton.setOnAction(event -> submitBooking());
        bookButton.setDisable(true);
        VBox pane = new VBox(20, new Label("Select a room in the Rooms tab, then enter guest details."),
                form, bookButton);
        pane.setPadding(new Insets(20));
        return tab("New Reservation", pane);
    }

    private void configureReservationTable() {
        reservationTable.setPlaceholder(new Label("No reservations found"));
        TableColumn<Reservation, Long> id = new TableColumn<>("ID");
        id.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        TableColumn<Reservation, String> name = new TableColumn<>("Guest");
        name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getGuest().getName()));
        TableColumn<Reservation, String> room = new TableColumn<>("Room");
        room.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRoom().getNumber()));
        TableColumn<Reservation, LocalDate> checkIn = new TableColumn<>("Check-in");
        checkIn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCheckIn()));
        TableColumn<Reservation, LocalDate> checkOut = new TableColumn<>("Check-out");
        checkOut.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCheckOut()));
        TableColumn<Reservation, Long> total = new TableColumn<>("Total ₹");
        total.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getTotalRupees()));
        TableColumn<Reservation, ReservationStatus> status = new TableColumn<>("Status");
        status.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getStatus()));
        reservationTable.getColumns().addAll(id, name, room, checkIn, checkOut, total, status);
        reservationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private Tab reservationsTab() {
        configureReservationTable();
        reservationSearch.setPromptText("Reservation ID or guest name");
        Button search = new Button("Search");
        search.setOnAction(event -> refreshReservations());
        Button cancel = new Button("Cancel selected reservation");
        cancel.setOnAction(event -> cancelSelected());
        Button export = new Button("Export text");
        export.setOnAction(event -> saveSelected(false));
        Button save = new Button("Save snapshot");
        save.setOnAction(event -> saveSelected(true));
        Button load = new Button("Read snapshot");
        load.setOnAction(event -> loadSnapshot());
        snapshotPreview.setEditable(false);
        snapshotPreview.setPrefRowCount(5);
        snapshotPreview.setPromptText("A loaded snapshot is displayed here; SQLite is never modified.");
        HBox filters = new HBox(10, reservationSearch, search, reservationMessage);
        HBox actions = new HBox(10, cancel, export, save, load);
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(15));
        pane.setTop(filters);
        pane.setCenter(reservationTable);
        pane.setBottom(new VBox(10, actions, new Label("Read-only snapshot preview"), snapshotPreview));
        return tab("Reservations", pane);
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private void showRooms(List<Room> rooms, String emptyMessage) {
        selectedRoom = null;
        roomTable.getItems().setAll(rooms);
        roomMessage.setText(rooms.isEmpty() ? emptyMessage : rooms.size() + " room(s) shown");
        updateEstimate();
    }

    private void searchAvailableRooms() {
        int count;
        try {
            count = minimumGuests.getText().isBlank() ? 0 : Integer.parseInt(minimumGuests.getText().trim());
        } catch (NumberFormatException error) {
            alert(Alert.AlertType.WARNING, "Guests must be a whole number");
            return;
        }
        if (!ready()) return;
        LocalDate in = searchIn.getValue();
        LocalDate out = searchOut.getValue();
        runTask(() -> service.searchAvailable(in, out, count),
                rooms -> showRooms(rooms, "No available rooms match this search"), null);
    }

    private void chooseSelectedRoom() {
        if (selectedRoom == null) {
            alert(Alert.AlertType.INFORMATION, "Select a room first");
            return;
        }
        bookingIn.setValue(searchIn.getValue());
        bookingOut.setValue(searchOut.getValue());
        tabs.getSelectionModel().select(1);
        updateEstimate();
    }

    private void refreshRooms() {
        if (ready()) runTask(service::rooms, rooms -> showRooms(rooms, "No rooms configured"), null);
    }

    private void refreshReservations() {
        if (!ready()) return;
        String search = reservationSearch.getText();
        runTask(() -> service.reservations(search), reservations -> {
            reservationTable.getItems().setAll(reservations);
            reservationMessage.setText(reservations.size() + " reservation(s)");
        }, null);
    }

    private void updateEstimate() {
        if (selectedRoom == null) {
            roomChoice.setText("Select a room from Rooms");
            estimate.setText("Select a room to see the total");
            bookButton.setDisable(true);
            return;
        }
        roomChoice.setText(selectedRoom.toString());
        LocalDate in = bookingIn.getValue();
        LocalDate out = bookingOut.getValue();
        if (in == null || out == null || !out.isAfter(in)) {
            estimate.setText("Choose a check-out after check-in");
        } else {
            long nights = ChronoUnit.DAYS.between(in, out);
            try {
                estimate.setText(nights + " night(s) × ₹" + selectedRoom.getNightlyPriceRupees()
                        + " = ₹" + Math.multiplyExact(nights, selectedRoom.getNightlyPriceRupees()));
            } catch (ArithmeticException error) {
                estimate.setText("Stay is too long");
            }
        }
        bookButton.setDisable(bookingInProgress);
    }

    private void submitBooking() {
        if (!ready() || selectedRoom == null || bookButton.isDisabled()) return;
        int count;
        try {
            count = Integer.parseInt(guests.getText().trim());
        } catch (NumberFormatException error) {
            alert(Alert.AlertType.WARNING, "Number of guests must be a whole number");
            return;
        }
        Room room = selectedRoom;
        LocalDate in = bookingIn.getValue();
        LocalDate out = bookingOut.getValue();
        String name = guestName.getText();
        String number = phone.getText();
        bookingInProgress = true;
        bookButton.setDisable(true);
        runTask(() -> service.book(room, name, number, count, in, out),
                this::showBookingConfirmation, this::finishBooking);
    }

    private void showBookingConfirmation(Reservation reservation) {
        LOG.info("Reservation booked: ID " + reservation.getId());
        alert(Alert.AlertType.INFORMATION, "Booked! Reservation ID " + reservation.getId()
                + ", " + reservation.nights() + " night(s), ₹" + reservation.getTotalRupees());
        refreshRooms();
        reservationSearch.clear();
        refreshReservations();
        tabs.getSelectionModel().select(2);
    }

    private void finishBooking() {
        bookingInProgress = false;
        updateEstimate();
    }

    private void cancelSelected() {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.INFORMATION, "Select a reservation first");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel reservation " + selected.getId() + "? The record will be retained.",
                ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        runTask(() -> { service.cancel(selected.getId()); return selected.getId(); }, id -> {
            LOG.info("Reservation cancelled: ID " + id);
            alert(Alert.AlertType.INFORMATION, "Reservation " + id + " cancelled");
            refreshRooms();
            refreshReservations();
        }, null);
    }

    private void saveSelected(boolean snapshot) {
        Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.INFORMATION, "Select a reservation first");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(snapshot ? "Save reservation snapshot" : "Export reservation text");
        chooser.setInitialFileName("reservation-" + selected.getId() + (snapshot ? ".ser" : ".txt"));
        File file = chooser.showSaveDialog(tabs.getScene().getWindow());
        if (file == null) return;
        runTask(() -> {
            if (snapshot) ReservationFiles.saveSnapshot(selected, file.toPath());
            else ReservationFiles.exportText(selected, file.toPath());
            return file.getName();
        }, name -> {
            LOG.info("Reservation file saved");
            alert(Alert.AlertType.INFORMATION, "Saved " + name);
        }, null);
    }

    private void loadSnapshot() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Read a reservation snapshot (does not change the database)");
        File file = chooser.showOpenDialog(tabs.getScene().getWindow());
        if (file == null) return;
        runTask(() -> ReservationFiles.readSnapshot(file.toPath()),
                snapshot -> {
                    snapshotPreview.setText(snapshot.display());
                    LOG.info("Reservation snapshot read for preview");
                }, null);
    }

    private boolean ready() {
        if (service != null) return true;
        alert(Alert.AlertType.WARNING, "Database is not ready. Check the configured database location.");
        return false;
    }

    private void alert(Alert.AlertType type, String message) {
        new Alert(type, message, ButtonType.OK).showAndWait();
    }

    private <T> void runTask(Work<T> work, Consumer<T> success, Runnable finished) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.run(); }
        };
        task.setOnSucceeded(event -> {
            if (finished != null) finished.run();
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            if (finished != null) finished.run();
            showTaskError(task.getException());
        });
        Thread worker = new Thread(task, "hotel-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void showTaskError(Throwable error) {
        if (error instanceof ValidationException || error instanceof RoomUnavailableException) {
            alert(Alert.AlertType.WARNING, error.getMessage());
        } else if (error instanceof IOException) {
            LOG.log(Level.WARNING, "File operation failed", error);
            alert(Alert.AlertType.ERROR, "File operation failed. Check path, permissions or snapshot format.");
        } else {
            LOG.log(Level.SEVERE, "Storage or file operation failed", error);
            alert(Alert.AlertType.ERROR, "Operation failed. Check the database location and application log.");
        }
    }

    @FunctionalInterface
    private interface Work<T> { T run() throws Exception; }

    public static void main(String[] args) { launch(args); }
}
