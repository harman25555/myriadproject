package repository;

import exception.StorageException;
import model.Promotion;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CSV-based repository for Promotion entities.
 */
public class PromotionRepository implements Repository<Promotion> {

    private final Path csvPath;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PromotionRepository(String filePath) {
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
    public Optional<Promotion> findById(String id) throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .filter(p -> p.getId().equals(id))
                    .findFirst();
        } catch (IOException e) {
            throw new StorageException("Error reading promotion data", e);
        }
    }

    @Override
    public List<Promotion> findAll() throws StorageException {
        try (Stream<String> lines = Files.lines(csvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::fromCsv)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Error reading promotion data", e);
        }
    }

    @Override
    public void save(Promotion promotion) throws StorageException {
        try {
            List<Promotion> promotions = findAll();
            promotions.removeIf(p -> p.getId().equals(promotion.getId()));
            promotions.add(promotion);

            List<String> csvLines = promotions.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());
            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error writing promotion data", e);
        }
    }

    @Override
    public void delete(String id) throws StorageException {
        try {
            List<Promotion> promotions = findAll();
            promotions.removeIf(p -> p.getId().equals(id));

            List<String> csvLines = promotions.stream()
                    .map(this::toCsv)
                    .collect(Collectors.toList());
            Files.write(csvPath, csvLines);
        } catch (IOException e) {
            throw new StorageException("Error deleting promotion data", e);
        }
    }

    private Promotion fromCsv(String line) {
        String[] parts = line.split(",", -1);
        String id = parts[0];
        String title = parts[1];
        String message = parts[2];
        LocalDate startDate = LocalDate.parse(parts[3], DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(parts[4], DATE_FORMATTER);
        boolean active = Boolean.parseBoolean(parts[5]);

        Promotion p = new Promotion(id, title, message, startDate, endDate, active);
        p.setActive(active);
        return p;
    }

    private String toCsv(Promotion p) {
        return String.join(",",
                safe(p.getId()),
                safe(p.getTitle()),
                safe(p.getMessage()),
                p.getStartDate().format(DATE_FORMATTER),
                p.getEndDate().format(DATE_FORMATTER),
                String.valueOf(p.isActive())
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
}
