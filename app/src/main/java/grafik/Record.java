package OOP2.PraktikumSBB;

import com.google.gson.annotations.SerializedName;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

// Diese Klasse repräsentiert einen "Datensatz" oder "Record"
public class Record {
    // Die Annotation @SerializedName wird verwendet, um anzugeben, dass das Feld "timestamp" 
    // im JSON die Bezeichnung "record_timestamp" hat
    @SerializedName("record_timestamp") String timestamp;
    // "fields" repräsentiert zusätzliche Daten im Datensatz
    public Fields fields;

    // Diese Methode wandelt das Timestamp-Format in eine lesbare Form um
    public String getTimestamp() {
        ZonedDateTime dateTime = ZonedDateTime.parse(timestamp);
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm 'Uhr' yyyy-MM-dd"));
    }

    // Diese Methode gibt die vollständige Information des Datensatzes zurück
    public String getFullInfo() {
        return getTimestamp() + "     " + fields.title;  // Add extra spaces for separation
    }

    // Überschreibt die equals-Methode, um die Gleichheit von Datensätzen zu bestimmen
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Record record = (Record) obj;
        return Objects.equals(timestamp, record.timestamp) &&
                Objects.equals(fields.title, record.fields.title);
    }

    // Überschreibt die hashCode-Methode, um den eindeutigen Hash eines Datensatzes zu bestimmen
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, fields.title);
    }
}
