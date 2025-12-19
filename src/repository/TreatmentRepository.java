package repository;

import exception.StorageException;
import model.Treatment;
import model.TreatmentEntry;
import model.TreatmentStatus;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CSV-based repository for Treatment entities and their TreatmentEntry line items.
 */
public class TreatmentRepository implements Repository<Treatment> {

    private final Path treatmentsCsvPath;
    private final Path entriesCsvPath;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TreatmentRepository(String treatmentsFilePath, String entriesFilePath) {
        this.treatmentsCsvPath = Paths.get(treatmentsFilePath);
        this.entriesCsvPath = Paths.get(entriesFilePath);
        ensureFileExists(treatmentsCsvPath);
        ensureFileExists(entriesCsvPath);
    }

    private void ensureFileExists(Path path) {
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Treatment> findById(String id) throws StorageException {
        List<Treatment> all = findAll();
        return all.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
    }

    public List<Treatment> findByClinician(String clinicianId) throws StorageException {
        return findAll().stream()
                .filter(t -> t.getClinicianId().equals(clinicianId))
                .collect(Collectors.toList());
    }

    public List<Treatment> findByPatient(String patientId) throws StorageException {
        return findAll().stream()
                .filter(t -> t.getPatientId().equals(patientId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Treatment> findAll() throws StorageException {
        List<Treatment> treatments = readTreatments();
        Map<String, List<TreatmentEntry>> entriesByTreatment = readEntries()
                .stream()
                .collect(Collectors.groupingBy(TreatmentEntry::getTreatmentId));

        for (Treatment t : treatments) {
            List<TreatmentEntry> entries =
                    entriesByTreatment.getOrDefault(t.getId(), new ArrayList<>());
            t.setEntries(entries);
        }
        return treatments;
    }

    @Override
    public void save(Treatment treatment) throws StorageException {
        try {
            // save/update core treatment
            List<Treatment> allTreatments = readTreatments();
            allTreatments.removeIf(t -> t.getId().equals(treatment.getId()));
            allTreatments.add(treatment);
            writeTreatments(allTreatments);

            // ensure each entry has the treatmentId set
            for (TreatmentEntry entry : treatment.getEntries()) {
                entry.setTreatmentId(treatment.getId());
            }

            // save/update entries
            List<TreatmentEntry> allEntries = readEntries();
            allEntries.removeIf(e -> e.getTreatmentId().equals(treatment.getId()));
            allEntries.addAll(treatment.getEntries());
            writeEntries(allEntries);
        } catch (IOException e) {
            throw new StorageException("Error saving treatment data", e);
        }
    }


    @Override
    public void delete(String id) throws StorageException {
        try {
            List<Treatment> allTreatments = readTreatments();
            allTreatments.removeIf(t -> t.getId().equals(id));
            writeTreatments(allTreatments);

            List<TreatmentEntry> allEntries = readEntries();
            allEntries.removeIf(e -> e.getTreatmentId().equals(id));
            writeEntries(allEntries);
        } catch (IOException e) {
            throw new StorageException("Error deleting treatment data", e);
        }
    }

    private List<Treatment> readTreatments() throws StorageException {
        try (Stream<String> lines = Files.lines(treatmentsCsvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::treatmentFromCsv)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Error reading treatments file", e);
        }
    }

    private void writeTreatments(List<Treatment> treatments) throws IOException {
        List<String> lines = treatments.stream()
                .map(this::treatmentToCsv)
                .collect(Collectors.toList());
        Files.write(treatmentsCsvPath, lines);
    }

    private List<TreatmentEntry> readEntries() throws StorageException {
        try (Stream<String> lines = Files.lines(entriesCsvPath)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::entryFromCsv)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Error reading treatment entries file", e);
        }
    }

    private void writeEntries(List<TreatmentEntry> entries) throws IOException {
        List<String> lines = entries.stream()
                .map(this::entryToCsv)
                .collect(Collectors.toList());
        Files.write(entriesCsvPath, lines);
    }

    private Treatment treatmentFromCsv(String line) {
        String[] parts = line.split(",", -1);
        String id = parts[0];
        String patientId = parts[1];
        String clinicianId = parts[2];
        TreatmentStatus status = TreatmentStatus.valueOf(parts[3]);
        LocalDateTime createdAt = LocalDateTime.parse(parts[4], DATE_TIME_FORMATTER);
        double totalCost = Double.parseDouble(parts[5]);
        boolean paid = Boolean.parseBoolean(parts[6]);

        Treatment t = new Treatment(id, patientId, clinicianId);
        t.setStatus(status);
        t.setCreatedAt(createdAt);
        t.setTotalCost(totalCost);
        t.setPaid(paid);
        return t;
    }

    private String treatmentToCsv(Treatment t) {
        return String.join(",",
                safe(t.getId()),
                safe(t.getPatientId()),
                safe(t.getClinicianId()),
                t.getStatus().name(),
                t.getCreatedAt().format(DATE_TIME_FORMATTER),
                String.valueOf(t.getTotalCost()),
                String.valueOf(t.isPaid())
        );
    }

    private TreatmentEntry entryFromCsv(String line) {
        String[] parts = line.split(",", -1);
        String treatmentId = parts[0];
        String treatmentTypeId = parts[1];
        int quantity = Integer.parseInt(parts[2]);
        String notes = parts[3];
        double lineCost = Double.parseDouble(parts[4]);

        TreatmentEntry entry = new TreatmentEntry(treatmentId, treatmentTypeId, quantity, notes);
        entry.setLineCost(lineCost);
        return entry;
    }

    private String entryToCsv(TreatmentEntry e) {
        return String.join(",",
                safe(e.getTreatmentId()),
                safe(e.getTreatmentTypeId()),
                String.valueOf(e.getQuantity()),
                safe(e.getNotes()),
                String.valueOf(e.getLineCost())
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
}
