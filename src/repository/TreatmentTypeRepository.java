package repository;

import exception.StorageException;
import model.TreatmentType;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TreatmentTypeRepository implements Repository<TreatmentType> {

    private final Path csvPath;

    public TreatmentTypeRepository(String filePath) {
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
    public Optional<TreatmentType> findById(String id) throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .filter(tt -> tt.getId().equals(id))
                    .findFirst();
        } catch (IOException e) {
            throw new StorageException("Error reading treatment type data", e);
        }
    }

    @Override
    public List<TreatmentType> findAll() throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Error reading treatment type data", e);
        }
    }

    @Override
    public void save(TreatmentType type) throws StorageException {
        try {
            List<TreatmentType> types = findAll();
            types.removeIf(t -> t.getId().equals(type.getId()));
            types.add(type);

            List<String> csvLines = types.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());
            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error writing treatment type data", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        try {
            List<TreatmentType> types = findAll();
            types.removeIf(t -> t.getId().equals(id));

            List<String> csvLines = types.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());
            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error deleting treatment type data", e);
        }
    }

    private TreatmentType fromCsv(String line) {
        String[] parts = line.split(",", -1);
        String id = parts[0];
        String name = parts[1];
        double basePrice = Double.parseDouble(parts[2]);
        boolean active = Boolean.parseBoolean(parts[3]);

        return new TreatmentType(id, name, basePrice, active);
    }

    private String toCsv(TreatmentType t) {
        return String.join(",",
                safe(t.getId()),
                safe(t.getName()),
                String.valueOf(t.getBasePrice()),
                String.valueOf(t.isActive())
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
}
