package service;

import exception.InvalidTreatmentStateException;
import exception.StorageException;
import exception.UserNotFoundException;
import model.Clinician;
import model.Patient;
import model.Treatment;
import model.TreatmentEntry;
import model.TreatmentStatus;
import model.TreatmentType;
import repository.ClinicianRepository;
import repository.PatientRepository;
import repository.TreatmentRepository;
import repository.TreatmentTypeRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final TreatmentTypeRepository treatmentTypeRepository;
    private final PatientRepository patientRepository;
    private final ClinicianRepository clinicianRepository;

    public TreatmentService(TreatmentRepository treatmentRepository,
                            TreatmentTypeRepository treatmentTypeRepository,
                            PatientRepository patientRepository,
                            ClinicianRepository clinicianRepository) {

        this.treatmentRepository = treatmentRepository;
        this.treatmentTypeRepository = treatmentTypeRepository;
        this.patientRepository = patientRepository;
        this.clinicianRepository = clinicianRepository;
    }

    // ADMIN: allocate by IDs (used elsewhere, throws UserNotFoundException)
    public Treatment allocateTreatment(String patientId,
                                       String clinicianId,
                                       List<String> treatmentTypeIds)
            throws StorageException, UserNotFoundException {

        // ensure patient & clinician exist
        if (patientRepository.findById(patientId).isEmpty()) {
            throw new UserNotFoundException("Patient with id " + patientId + " not found.");
        }
        if (clinicianRepository.findById(clinicianId).isEmpty()) {
            throw new UserNotFoundException("Clinician with id " + clinicianId + " not found.");
        }

        String treatmentId = UUID.randomUUID().toString();
        Treatment treatment = new Treatment();
        treatment.setId(treatmentId);
        treatment.setPatientId(patientId);
        treatment.setClinicianId(clinicianId);
        treatment.setStatus(TreatmentStatus.NEW_TREATMENT);
        treatment.setCreatedAt(LocalDateTime.now());
        treatment.setEntries(new ArrayList<>());
        treatment.setPaid(false);
        treatment.setTotalCost(0.0);

        // Optionally pre-fill entries with quantity=1 and no notes
        for (String typeId : treatmentTypeIds) {
            Optional<TreatmentType> typeOpt = treatmentTypeRepository.findById(typeId);
            typeOpt.ifPresent(type -> {
                TreatmentEntry entry = new TreatmentEntry();
                entry.setTreatmentId(treatmentId);
                entry.setTreatmentTypeId(type.getId());
                entry.setQuantity(1);
                entry.setNotes("");
                entry.setLineCost(0.0); // cost will be calculated later
                treatment.getEntries().add(entry);
            });
        }

        treatmentRepository.save(treatment);
        return treatment;
    }

    // ADMIN/PATIENT: Allocate using full objects (matches AdminMenu.createNewTreatment call)
    public Treatment createNewTreatment(Patient patient,
                                        Clinician clinician,
                                        TreatmentType type,
                                        int quantity) throws StorageException {

        String treatmentId = UUID.randomUUID().toString();

        Treatment treatment = new Treatment();
        treatment.setId(treatmentId);
        treatment.setPatientId(patient.getId());
        treatment.setClinicianId(clinician.getId());
        treatment.setStatus(TreatmentStatus.NEW_TREATMENT);
        treatment.setCreatedAt(LocalDateTime.now());
        treatment.setPaid(false);
        treatment.setTotalCost(0.0);

        TreatmentEntry entry = new TreatmentEntry();
        entry.setTreatmentId(treatmentId);
        entry.setTreatmentTypeId(type.getId());
        entry.setQuantity(quantity);
        entry.setNotes("");
        entry.setLineCost(0.0);

        List<TreatmentEntry> entries = new ArrayList<>();
        entries.add(entry);
        treatment.setEntries(entries);

        treatmentRepository.save(treatment);
        return treatment;
    }

    // CLINICIANS: find by speciality (used by AdminMenu + PatientMenu)
    public List<Clinician> findCliniciansBySpeciality(String speciality) throws StorageException {
        return clinicianRepository.findAll().stream()
                .filter(c -> c.getSpeciality() != null &&
                        c.getSpeciality().equalsIgnoreCase(speciality))
                .collect(Collectors.toList());
    }

    // CLINICIANS: view treatments
    public List<Treatment> getTreatmentsForClinician(String clinicianId) throws StorageException {
        return treatmentRepository.findByClinician(clinicianId);
    }

    public List<Treatment> getTreatmentsForClinicianByStatus(String clinicianId,
                                                             TreatmentStatus status)
            throws StorageException {
        return treatmentRepository.findByClinician(clinicianId).stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    // PATIENT: view treatments
    public List<Treatment> getTreatmentsForPatient(String patientId) throws StorageException {
        return treatmentRepository.findByPatient(patientId);
    }

    // CLINICIAN: assess treatment
    public void assessTreatment(String treatmentId, String notes)
            throws StorageException, InvalidTreatmentStateException {

        Optional<Treatment> opt = treatmentRepository.findById(treatmentId);
        if (opt.isEmpty()) {
            throw new InvalidTreatmentStateException("Treatment not found: " + treatmentId);
        }

        Treatment treatment = opt.get();
        if (treatment.getStatus() != TreatmentStatus.NEW_TREATMENT) {
            throw new InvalidTreatmentStateException(
                    "Treatment must be in NEW_TREATMENT state to assess.");
        }

        // For simplicity, just attach notes to first entry or create one dummy entry
        List<TreatmentEntry> entries = treatment.getEntries();
        if (entries.isEmpty()) {
            TreatmentEntry entry = new TreatmentEntry();
            entry.setTreatmentId(treatment.getId());
            entry.setTreatmentTypeId("ASSESSMENT");
            entry.setQuantity(1);
            entry.setNotes(notes);
            entry.setLineCost(0.0);
            entries.add(entry);
        } else {
            entries.get(0).setNotes(notes);
        }
        treatment.setEntries(entries);
        treatment.setStatus(TreatmentStatus.TREATMENT_ASSESSED);

        treatmentRepository.save(treatment);
    }

    public void recordAssessment(String treatmentId,
                                 List<TreatmentEntry> assessedEntries)
            throws StorageException, InvalidTreatmentStateException {

        Optional<Treatment> opt = treatmentRepository.findById(treatmentId);
        if (opt.isEmpty()) {
            throw new InvalidTreatmentStateException("Treatment not found: " + treatmentId);
        }

        Treatment treatment = opt.get();
        if (treatment.getStatus() != TreatmentStatus.NEW_TREATMENT) {
            throw new InvalidTreatmentStateException(
                    "Treatment must be in NEW_TREATMENT state to assess.");
        }

        treatment.setEntries(assessedEntries);
        treatment.setStatus(TreatmentStatus.TREATMENT_ASSESSED);

        treatmentRepository.save(treatment);
    }

    // ADMIN: cost treatment
    public double costTreatment(String treatmentId)
            throws StorageException, InvalidTreatmentStateException {

        Optional<Treatment> opt = treatmentRepository.findById(treatmentId);
        if (opt.isEmpty()) {
            throw new InvalidTreatmentStateException("Treatment not found: " + treatmentId);
        }

        Treatment treatment = opt.get();
        if (treatment.getStatus() != TreatmentStatus.TREATMENT_ASSESSED) {
            throw new InvalidTreatmentStateException(
                    "Treatment must be in TREATMENT_ASSESSED state before costing.");
        }

        double total = 0.0;
        for (TreatmentEntry entry : treatment.getEntries()) {
            Optional<TreatmentType> typeOpt = treatmentTypeRepository.findById(entry.getTreatmentTypeId());
            if (typeOpt.isEmpty()) {
                continue; // or throw
            }
            TreatmentType type = typeOpt.get();
            double lineCost = type.getBasePrice() * entry.getQuantity();
            entry.setLineCost(lineCost);
            total += lineCost;
        }

        treatment.setTotalCost(total);
        treatmentRepository.save(treatment);
        return total;
    }

    // ADMIN: mark paid
    public void markTreatmentAsPaid(String treatmentId)
            throws StorageException, InvalidTreatmentStateException {

        Optional<Treatment> opt = treatmentRepository.findById(treatmentId);
        if (opt.isEmpty()) {
            throw new InvalidTreatmentStateException("Treatment not found: " + treatmentId);
        }

        Treatment treatment = opt.get();
        if (treatment.getTotalCost() <= 0) {
            throw new InvalidTreatmentStateException(
                    "Cannot mark as paid when total cost is zero or not calculated.");
        }

        treatment.setPaid(true);
        treatmentRepository.save(treatment);
    }

    // ADMIN: get assessed, not yet costed
    public List<Treatment> getAssessedTreatmentsPendingCosting() throws StorageException {
        List<Treatment> result = new ArrayList<>();
        for (Treatment t : treatmentRepository.findAll()) {
            if (t.getStatus() == TreatmentStatus.TREATMENT_ASSESSED &&
                    t.getTotalCost() == 0.0) {
                result.add(t);
            }
        }
        return result;
    }

    // ADMIN: find by status (used by AdminMenu.getTreatmentsByStatus)
    public List<Treatment> getTreatmentsByStatus(TreatmentStatus status) throws StorageException {
        return treatmentRepository.findAll().stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    // ADMIN: get treatment by id (used by AdminMenu)
    public Treatment getTreatmentById(String id) throws StorageException {
        return treatmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + id));
    }

    // ADMIN: TreatmentType management
    public TreatmentType getTreatmentTypeById(String id) throws StorageException {
        return treatmentTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Treatment type not found: " + id));
    }

    public TreatmentType addTreatmentType(String name, double basePrice) throws StorageException {
        TreatmentType type = new TreatmentType(UUID.randomUUID().toString(), name, basePrice, true);
        treatmentTypeRepository.save(type);
        return type;
    }

    public void removeTreatmentType(String id) throws StorageException {
        treatmentTypeRepository.delete(id);
    }

    public List<TreatmentType> getAllTreatmentTypes() throws StorageException {
        return treatmentTypeRepository.findAll();
    }
}
