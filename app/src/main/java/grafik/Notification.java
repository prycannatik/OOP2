package OOP2.PraktikumSBB;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

// Diese Klasse erzeugt eine Benachrichtigung, die für eine bestimmte Dauer angezeigt wird.
public class Notification {
    
    // Die Methode show erstellt und zeigt eine Benachrichtigung.
    public static void show(String title, String text, int durationSeconds) {
        // Platform.runLater wird verwendet, um sicherzustellen, dass die Benachrichtigung im JavaFX Application Thread erstellt und angezeigt wird.
        // Dies ist notwendig, da nur der JavaFX Application Thread die Benutzeroberfläche sicher ändern kann.
        Platform.runLater(() -> {
            // Erstellen Sie einen Notifications Builder, um die Benachrichtigung zu konfigurieren.
            Notifications notificationBuilder = Notifications.create()
                    // Setzen Sie den Titel der Benachrichtigung.
                    .title(title)
                    // Setzen Sie den Text der Benachrichtigung.
                    .text(text)
                    // Setzen Sie die Dauer, für die die Benachrichtigung angezeigt wird.
                    .hideAfter(Duration.seconds(durationSeconds))
                    // Setzen Sie die Position der Benachrichtigung.
                    .position(Pos.BOTTOM_RIGHT);

            // Erstellen und zeigen Sie die Benachrichtigung als Information.
            notificationBuilder.showInformation();
        });
    }
}
