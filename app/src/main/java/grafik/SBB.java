package OOP2.PraktikumSBB;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Diese Hauptklasse startet die Anwendung und verwaltet den Hauptbildschirm
public class SBB extends Application {
    // URL der API
    private static final String API_URL = "https://data.sbb.ch/api/records/1.0/search/?dataset=rail-traffic-information&sort=record_timestamp&pretty_print=true&rows=100";
    // TableView, um die Datensätze anzuzeigen
    private TableView<Record> table = new TableView<>();
    // Liste der Daten, die im TableView angezeigt werden
    private ObservableList<Record> data = FXCollections.observableArrayList();
    // Set der alten Datensätze, um neue Datensätze zu erkennen
    private Set<Record> oldRecords = new HashSet<>();
    // Scheduler für regelmäßige Aktualisierungen
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // Setze die Breite und Höhe des Fensters
        stage.setWidth(500);
        stage.setHeight(400);

        // Erstelle eine TableColumn, um die Informationen eines Datensatzes anzuzeigen
        TableColumn<Record, String> infoCol = new TableColumn<>("");
        infoCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullInfo()));
        infoCol.prefWidthProperty().bind(table.widthProperty());

        // Füge die TableColumn zum TableView hinzu und setze die Daten
        table.getColumns().add(infoCol);
        table.setItems(data);

        // Erstelle einen DatePicker für das Filtern nach Datum
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Filter by Date");
        datePicker.setOnAction(e -> {
            // Wenn der Wert des DatePicker nicht null ist, hole und zeige die Daten für das ausgewählte Datum an.
            if (datePicker.getValue() != null) {
                fetchAndDisplayData(datePicker.getValue());
            } 
            // Wenn der Wert des DatePicker null ist (d.h., das Datum wurde gelöscht), hole und zeige alle Daten an.
            else {
                fetchAndDisplayData(null);
            }
        });

        // Erstelle einen Refresh-Button, um die Daten manuell zu aktualisieren
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> fetchAndDisplayData(null));

        // Erstelle ein VBox-Layout und füge den DatePicker, TableView und Refresh-Button hinzu
        VBox vbox = new VBox(datePicker, table, refreshButton);

        // Erstelle eine Szene mit dem VBox-Layout und setze sie auf der Bühne
        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();

        // Unfokus DatePicker
        Platform.runLater(() -> vbox.requestFocus());

        // Planen Sie eine regelmäßige Aktualisierung alle 5 Minuten
        scheduler.scheduleAtFixedRate(() -> Platform.runLater(() -> fetchAndDisplayData(null)), 0, 5, TimeUnit.MINUTES);

        // Stoppen Sie den Scheduler, wenn die Anwendung geschlossen wird
        stage.setOnCloseRequest(e -> scheduler.shutdownNow());
    }

    // Diese Methode lädt die Daten von der API und aktualisiert die TableView
    private void fetchAndDisplayData(LocalDate filterDate) {
        try {
            // Verbinde zur API und lade die Daten
            URL url = new URL(API_URL);
            URLConnection request = url.openConnection();
            request.connect();
            InputStreamReader reader = new InputStreamReader((InputStream) request.getContent());

            // Verwende Gson, um die Daten in Java-Objekte zu konvertieren
            Gson gson = new Gson();
            ApiResponse response = gson.fromJson(reader, ApiResponse.class);
            Record[] records = response.records;

            // Zeige die Daten an
            displayData(filterDate, records);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Diese Methode filtert und zeigt die Daten in der TableView
    private void displayData(LocalDate filterDate, Record[] records) {
        data.clear();
        if (filterDate != null) {
            for (Record record : records) {
                ZonedDateTime dateTime = ZonedDateTime.parse(record.timestamp);
                if (dateTime.toLocalDate().isEqual(filterDate)) {
                    data.add(record);
                }
            }
        } else {
            data.addAll(records);
        }

        // Zeige Neuigkeiten
        int newRecordsCount = (int) Arrays.stream(records).filter(record -> !oldRecords.contains(record)).count();
        oldRecords.clear();
        oldRecords.addAll(Arrays.asList(records));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Neue Meldungen");
        alert.setHeaderText(null);
        alert.setContentText("Es gibt " + newRecordsCount + " neue Meldungen.");
        alert.showAndWait();
    }
}
