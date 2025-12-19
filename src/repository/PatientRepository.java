package repository;

import exception.StorageException;
import model.Patient;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CSV-based repository for Patient entities.
 */
public class PatientRepository implements Repository<Patient> {

    private final Path csvPath;

    public PatientRepository(String filePath) {
        this.csvPath = Paths.get(filePath);
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            if (Files.notExists(csvPath)) {
                Files.createDirectories(csvPath.getParent());
                Files.createFile(csvPath);
            }
        } catch (IOException e) {
            // Don't throw here – constructor should not throw checked exception.
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Patient> findById(String id) throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .filter(p -> p.getId().equals(id))
                    .findFirst();
        } catch (IOException e) {
            throw new StorageException("Error reading patient data", e);
        }
    }

    public Optional<Patient> findByEmail(String email) throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .filter(p -> p.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        } catch (IOException e) {
            throw new StorageException("Error reading patient data", e);
        }
    }

    @Override
    public List<Patient> findAll() throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Error reading patient data", e);
        }
    }

    @Override
    public void save(Patient patient) throws StorageException {
        try {
            List<Patient> patients = findAll();
            patients.removeIf(p -> p.getId().equals(patient.getId()));
            patients.add(patient);

            List<String> csvLines = patients.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());
            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error writing patient data", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        try {
            List<Patient> patients = findAll();
            patients.removeIf(p -> p.getId().equals(id));

            List<String> csvLines = patients.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());
            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error deleting patient data", e);
        }
    }

    private Patient fromCsv(String line) {
        String[] parts = line.split(",", -1); // keep empty strings
        String id = parts[0];
        String name = parts[1];
        String email = parts[2];
        String password = parts[3];
        boolean registered = Boolean.parseBoolean(parts[4]);
        boolean marketingOptIn = Boolean.parseBoolean(parts[5]);
        boolean flaggedNonPaying = Boolean.parseBoolean(parts[6]);

        Patient p = new Patient(id, name, email, password, registered, marketingOptIn);
        p.setFlaggedNonPaying(flaggedNonPaying);
        return p;
    }

    private String toCsv(Patient p) {
        return String.join(",",
                safe(p.getId()),
                safe(p.getName()),
                safe(p.getEmail()),
                safe(p.getPassword()),
                String.valueOf(p.isRegistered()),
                String.valueOf(p.isMarketingOptIn()),
                String.valueOf(p.isFlaggedNonPaying())
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " "); // crude CSV escaping
    }
}
