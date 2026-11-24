package com.example.hotel.util;

import com.example.hotel.model.Reservation;
import com.example.hotel.model.ReservationSnapshot;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReservationFiles {
    private ReservationFiles() { }

    public static void exportText(Reservation reservation, Path destination) throws IOException {
        try (Writer writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writer.write(new ReservationSnapshot(reservation).display());
        }
    }

    public static void saveSnapshot(Reservation reservation, Path destination) throws IOException {
        try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(destination))) {
            stream.writeObject(new ReservationSnapshot(reservation));
        }
    }

    public static ReservationSnapshot readSnapshot(Path source) throws IOException {
        try (ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(source))) {
            // Only our detached value and basic string fields may be reconstructed.
            ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
                    "maxdepth=2;maxrefs=20;maxbytes=65536;maxarray=0;"
                            + "com.example.hotel.model.ReservationSnapshot;java.lang.String;!*");
            stream.setObjectInputFilter(filter);
            Object object;
            try {
                object = stream.readObject();
            } catch (ClassNotFoundException error) {
                throw new IOException("Unsupported snapshot format", error);
            }
            if (!(object instanceof ReservationSnapshot snapshot)) {
                throw new IOException("File is not a reservation snapshot");
            }
            return snapshot;
        }
    }
}
