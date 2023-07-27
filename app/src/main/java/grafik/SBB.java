package OOP2.PraktikumSBB;

// Importieren Sie die benötigten Pakete und Klassen.
import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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

// Hauptklasse, die von der JavaFX Application Klasse erbt.
public class SBB extends Application {
    // URL der API, von der wir Daten abrufen.
    private static final String API_URL = "https://data.sbb.ch/api/records/1.0/search/?dataset=rail-traffic-information&sort=record_timestamp&pretty_print=true&rows=100";

    // Ein TableView ist ein JavaFX UI-Control, das eine Tabelle für die Anzeige von
    // Daten in tabellarischer Form anzeigt.
    private TableView<Record> table = new TableView<>();

    // ObservableList ist eine Liste, die Listener über Änderungen benachrichtigt.
    private ObservableList<Record> data = FXCollections.observableArrayList();

    // Ein Set enthält keine doppelten Elemente. Dies wird verwendet, um alte
    // Datensätze zu speichern.
    private Set<Record> oldRecords = new HashSet<>();

    // Ein Scheduler ermöglicht die Planung von Aufgaben, die in Zukunft ausgeführt
    // werden.
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Spinner wird verwendet, um eine nummerische Wertauswahl zu ermöglichen.
    private Spinner<Integer> rowSpinner = new Spinner<>();

    // Der Haupteinstiegspunkt für alle JavaFX-Anwendungen.
    public static void main(String[] args) {
        // Die launch-Methode startet eine eigenständige Anwendung.
        launch(args);
    }

    // Die start-Methode ist die Hauptmethode einer JavaFX-Anwendung.
    @Override
    public void start(Stage stage) {
        // Setzen Sie die Breite und Höhe des Fensters.
        stage.setWidth(550);
        stage.setHeight(400);

        // Eine TableColumn stellt eine einzelne Spalte in einer TableView dar.
        TableColumn<Record, String> infoCol = new TableColumn<>("");
        // Setzen Sie den CellValueFactory, der bestimmt, wie die Daten in der Zelle
        // dargestellt werden.
        infoCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullInfo()));
        // Die Spaltenbreite ist 90% der Tabellenbreite.
        infoCol.prefWidthProperty().bind(table.widthProperty().multiply(0.9));

        // Erstellen Sie eine zweite TableColumn für die Info-Taste.
        TableColumn<Record, Void> buttonCol = new TableColumn<>("");
        // Erstellen Sie eine benutzerdefinierte Zelle mit einer Schaltfläche.
        buttonCol.setCellFactory(col -> new TableCell<Record, Void>() {
            private final Button infoButton = new Button();

            {
                // Setzen Sie das Symbol der Schaltfläche und das Verhalten beim Klicken.
                infoButton.setGraphic(new ImageView(new Image("D:\\Downloads\\infoicon.png", 16, 16, true, true)));
                infoButton.setOnAction(e -> {
                    Record record = getTableRow().getItem();
                    Dialog<String> dialog = new Dialog<>();
                    dialog.initOwner(stage); // Setzen Sie das Hauptfenster als Owner.
                    dialog.setTitle("Detailierte Informationen");
                    dialog.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
                    dialog.setContentText(record.fields.description);
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
                    dialog.showAndWait();
                });
            }

            // Zeigt den Info Button nur an wenn die Zelle nicht leer ist
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(infoButton);
                }
            }
        });
        buttonCol.prefWidthProperty().bind(table.widthProperty().multiply(0.075)); // 7.5% width

        // Fügen Sie die Spalten zur Tabelle hinzu und setzen Sie die Daten.
        table.getColumns().addAll(infoCol, buttonCol);
        table.setItems(data);

        // Erstellen Sie einen DatePicker zum Auswählen eines Datums.
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Filter per Datum");
        datePicker.setOnAction(e -> {
            if (datePicker.getValue() != null) {
                fetchAndDisplayData(datePicker.getValue());
            } else {
                fetchAndDisplayData(null);
            }
        });

        // Erstellen Sie eine Aktualisierungsschaltfläche.
        Button refreshButton = new Button("Aktualisieren");
        refreshButton.setOnAction(e -> fetchAndDisplayData(null));

        // Konfigurieren Sie den Spinner.
        rowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 100));
        rowSpinner.setEditable(true);
        rowSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue < 1 || newValue > 9999) {
                rowSpinner.getValueFactory().setValue(100);
            } else {
                fetchAndDisplayData(null);
            }
        });

        rowSpinner.setEditable(true);
        rowSpinner.valueProperty().addListener((obs, oldValue, newValue) -> fetchAndDisplayData(null));

        // Erstellen Sie ein Layout und fügen Sie alle Elemente hinzu.
        HBox topBox = new HBox(datePicker, rowSpinner);
        VBox vbox = new VBox(topBox, table, refreshButton);
        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();
        Platform.runLater(() -> vbox.requestFocus());

        // Planen Sie eine regelmäßige Aktualisierung alle 5 Minuten.
        scheduler.scheduleAtFixedRate(() -> Platform.runLater(() -> fetchAndDisplayData(null)), 0, 5, TimeUnit.MINUTES);
        // Stoppen Sie den Scheduler, wenn die Anwendung geschlossen wird.
        stage.setOnCloseRequest(e -> scheduler.shutdownNow());
    }

    // Diese Methode lädt die Daten von der API und aktualisiert die TableView.
    private void fetchAndDisplayData(LocalDate filterDate) {
        try {
            String apiUrl = API_URL.replace("rows=100", "rows=" + rowSpinner.getValue());

            URL url = new URL(apiUrl);
            URLConnection request = url.openConnection();
            request.connect();
            InputStreamReader reader = new InputStreamReader((InputStream) request.getContent());

            // Verwenden Sie Gson, um die Daten in Java-Objekte zu konvertieren.
            Gson gson = new Gson();
            ApiResponse response = gson.fromJson(reader, ApiResponse.class);
            Record[] records = response.records;

            // Anzeigen der Daten.
            displayData(filterDate, records);

            // Filtern Sie neue Datensätze und fügen Sie sie zur Liste hinzu.
            Arrays.stream(records)
                    .filter(record -> !oldRecords.contains(record))
                    .forEach(record -> {
                        data.add(0, record); // Add new record at the top
                        Notification.show("Neue Meldung", record.getFullInfo(), 5);
                    });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Diese Methode filtert und zeigt die Daten in der TableView.
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

        // Aktualisieren Sie die alten Datensätze.
        oldRecords.clear();
        oldRecords.addAll(Arrays.asList(records));
    }
}
