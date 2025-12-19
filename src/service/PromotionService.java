package service;

import exception.StorageException;
import model.Patient;
import repository.PatientRepository;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PromotionService {

    private final PatientRepository patientRepository;
    private final Path promotionLogPath;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PromotionService(PatientRepository patientRepository, String promotionLogCsvPath) {
        this.patientRepository = patientRepository;
        this.promotionLogPath = Paths.get(promotionLogCsvPath);
        ensureFileExists(promotionLogPath);
    }

    private void ensureFileExists(Path path) {
        try {
            if (Files.notExists(path)) {
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                Files.createFile(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int sendPromotionToOptInPatients(String title, String message) throws StorageException {
        List<Patient> patients = patientRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        for (Patient p : patients) {
            if (p.isMarketingOptIn()) {
                count++;
                String line = String.join(",",
                        now.format(FORMATTER),
                        safe(title),
                        safe(message),
                        safe(p.getId()),
                        safe(p.getEmail())
                );
                sb.append(line).append(System.lineSeparator());
            }
        }

        try {
            Files.writeString(promotionLogPath, sb.toString(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new StorageException("Error writing promotion log", e);
        }
        return count;
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
}
