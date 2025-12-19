package repository;

import exception.StorageException;
import model.Clinician;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClinicianRepository implements Repository<Clinician> {

    private final Path csvPath;

    public ClinicianRepository(String filePath) {
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
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Clinician> findById(String id) throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .filter(c -> c.getId().equals(id))
                    .findFirst();
        } catch (IOException e) {
            throw new StorageException("Error reading clinician data", e);
        }
    }

    public Optional<Clinician> findByEmail(String email) throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .filter(c -> c.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        } catch (IOException e) {
            throw new StorageException("Error reading clinician data", e);
        }
    }

    public List<Clinician> findBySpeciality(String speciality) throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .filter(c -> c.getSpeciality().equalsIgnoreCase(speciality))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Error reading clinician data", e);
        }
    }

    @Override
    public List<Clinician> findAll() throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Error reading clinician data", e);
        }
    }

    @Override
    public void save(Clinician clinician) throws StorageException {
        try {
            List<Clinician> clinicians = findAll();
            clinicians.removeIf(c -> c.getId().equals(clinician.getId()));
            clinicians.add(clinician);

            List<String> csvLines = clinicians.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());
            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error writing clinician data", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        try {
            List<Clinician> clinicians = findAll();
            clinicians.removeIf(c -> c.getId().equals(id));

            List<String> csvLines = clinicians.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());
            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error deleting clinician data", e);
        }
    }

    private Clinician fromCsv(String line) {
        String[] parts = line.split(",", -1);
        String id = parts[0];
        String name = parts[1];
        String email = parts[2];
        String password = parts[3];
        String speciality = parts[4];

        return new Clinician(id, name, email, password, speciality);
    }

    private String toCsv(Clinician c) {
        return String.join(",",
                safe(c.getId()),
                safe(c.getName()),
                safe(c.getEmail()),
                safe(c.getPassword()),
                safe(c.getSpeciality())
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
}
