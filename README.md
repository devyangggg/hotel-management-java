# Hotel Reservation System

A small JavaFX desktop project for room search, reservations, cancellation and file exports. SQLite is the source of truth. The application is intentionally single-hotel and single-instance; there is no authentication, payment or network service.

## Requirements and commands

- **JDK 21** (not merely a JRE) and **Maven 3.9+** on `PATH`. macOS example: `brew install openjdk@21 maven`, then select JDK 21 with `JAVA_HOME` (or use your IDE's JDK 21 configuration). Check with `java -version` and `mvn -version`.
- Pinned runtime libraries in `pom.xml`: OpenJFX controls **21.0.6** (with its base/graphics modules) and SQLite JDBC **3.49.1.0**; JUnit Jupiter **5.11.4** for tests. JavaFX Maven plugin **0.0.8** downloads the platform-specific native libraries.

From the project root:

```sh
mvn test
mvn javafx:run
```

The first run downloads Maven dependencies and creates the database and four sample rooms. The database is at **`~/.hotel-reservation/hotel.db`**, outside the jar and in a writable user directory. The default relative path is read from `src/main/resources/application.properties`. Tests use isolated temporary databases and files, never your home database. `java.util.logging` writes technical messages to the console (not guest names or phones). Local exports go wherever you choose in the file picker; generated files in this repository are ignored by `.gitignore`.

## Sample walkthrough

1. Open **Rooms**; four Standard/Deluxe rooms appear. Choose check-in today or later, checkout after check-in, and optionally enter guests; click **Search available**. The room list and nightly rupee price update.
2. Select a room and click **Reserve selected room**. Enter a name, phone (optional `+` and **7–15 digits only**, no spaces), and number of guests within room capacity. The form shows nights and total. Click **Book room** once; the button is disabled while saving.
3. The confirmation shows the new reservation ID. Open **Reservations** to search by ID or guest name. Select the reservation and click **Cancel selected reservation**; confirm. It remains visible as CANCELLED and its dates become available again.
4. Select a reservation and use **Export text** to save human-readable UTF-8 details, or **Save snapshot** to serialize a detached example. **Read snapshot** displays a file in a read-only preview; it never changes the SQLite database. Only expected snapshot classes and basic required types are accepted on deserialization.

A stay occupies `[check-in, checkout)`: another guest may arrive on checkout day. Active stays overlap only when `existing.check_in < requested.check_out` **and** `existing.check_out > requested.check_in`. The DAO checks again in the same SQLite transaction as the INSERT. Dates are stored as ISO text; nightly prices are integer rupees and totals are long integers. A failed booking rolls back. Validation and technical failures show separate, non-stack-trace alerts.

## Limitations and verification

- Single-instance desktop design; do not assume multi-instance booking guarantees. Phone validation checks shape, not whether a number is real. Rooms are seeded once and not editable. No payments, login, email or network features.
- Tested with `mvn test` on a machine using JDK 26 (compiling for Java 21): 9 tests passed. That newer JDK prints native-access warnings from JavaFX/SQLite; JDK 21 is the documented target.
- `mvn javafx:run` launched and displayed the Rooms table with four sample rooms. Automated desktop interaction via macOS System Events was denied permission; booking, cancellation and file-picker **GUI walkthroughs remain manual verification steps**, although service/DAO/file behavior is tested without the GUI. To complete the manual check, follow the walkthrough plus invalid dates, too many guests, no matches, declined cancellation, bad snapshot file and storage-error feedback.
