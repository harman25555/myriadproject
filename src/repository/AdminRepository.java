package repository;
import exception.StorageException;
import model.CentreAdministrator;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdminRepository implements Repository<CentreAdministrator> {

    private final Path csvPath;

    public AdminRepository(String filePath) {
        this.csvPath = Paths.get(filePath);
        ensureFileExists();
    }

    private void ensureFileExists() {
        try {
            if (Files.notExists(csvPath)) {
                Files.createDirectories(csvPath.getParent());
                Files.createFile(csvPath);

                // SEED - Create a default admin so project works immediately
                String defaultAdmin = "admin001,Administrator,admin@mms.com,admin123";
                Files.write(csvPath, List.of(defaultAdmin));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<CentreAdministrator> findById(String id) throws StorageException {
        return findAll().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst();
    }

    public Optional<CentreAdministrator> findByEmail(String email) throws StorageException {
        return findAll().stream()
                .filter(a -> a.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public List<CentreAdministrator> findAll() throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(l -> !l.isBlank())
                    .map(this::fromCsv)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Error reading admin CSV", e);
        }
    }

    @Override
    public void save(CentreAdministrator admin) throws StorageException {
        try {
            List<CentreAdministrator> admins = findAll();
            admins.removeIf(a -> a.getId().equals(admin.getId()));
            admins.add(admin);

            List<String> csvLines = admins.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());

            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error saving admin CSV", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        try {
            List<CentreAdministrator> admins = findAll();
            admins.removeIf(a -> a.getId().equals(id));

            List<String> csvLines = admins.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());

            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error deleting admin", e);
        }
    }

    private CentreAdministrator fromCsv(String line) {
        String[] parts = line.split(",", -1);
        String id = parts[0];
        String name = parts[1];
        String email = parts[2];
        String password = parts[3];

        return new CentreAdministrator(id, name, email, password);
    }

    private String toCsv(CentreAdministrator admin) {
        return String.join(",",
                safe(admin.getId()),
                safe(admin.getName()),
                safe(admin.getEmail()),
                safe(admin.getPassword())
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
}
