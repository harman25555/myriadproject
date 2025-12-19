package test;

import exception.InvalidTreatmentStateException;
import exception.StorageException;
import exception.UserNotFoundException;
import model.Clinician;
import model.Patient;
import model.Treatment;
import model.TreatmentEntry;
import model.TreatmentStatus;
import model.TreatmentType;
import org.junit.jupiter.api.*;
import repository.ClinicianRepository;
import repository.PatientRepository;
import repository.TreatmentRepository;
import repository.TreatmentTypeRepository;
import service.PatientService;
import service.TreatmentService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreatmentServiceTest {

    private Path tempDir;
    private Path patientCsv;
    private Path clinicianCsv;
    private Path treatmentCsv;
    private Path treatmentEntriesCsv;
    private Path treatmentTypeCsv;

    private PatientRepository patientRepository;
    private ClinicianRepository clinicianRepository;
    private TreatmentRepository treatmentRepository;
    private TreatmentTypeRepository treatmentTypeRepository;

    private PatientService patientService;
    private TreatmentService treatmentService;

    private Patient testPatient;
    private Clinician testClinician;
    private TreatmentType testType;

    @BeforeEach
    void setUp() throws IOException, StorageException {
        tempDir = Files.createTempDirectory("mms-test-treatment");

        patientCsv = tempDir.resolve("patients.csv");
        clinicianCsv = tempDir.resolve("clinicians.csv");
        treatmentCsv = tempDir.resolve("treatments.csv");
        treatmentEntriesCsv = tempDir.resolve("treatment_entries.csv");
        treatmentTypeCsv = tempDir.resolve("treatment_types.csv");

        Files.createFile(patientCsv);
        Files.createFile(clinicianCsv);
        Files.createFile(treatmentCsv);
        Files.createFile(treatmentEntriesCsv);
        Files.createFile(treatmentTypeCsv);

        patientRepository = new PatientRepository(patientCsv.toString());
        clinicianRepository = new ClinicianRepository(clinicianCsv.toString());
        treatmentRepository = new TreatmentRepository(treatmentCsv.toString(), treatmentEntriesCsv.toString());
        treatmentTypeRepository = new TreatmentTypeRepository(treatmentTypeCsv.toString());

        patientService = new PatientService(patientRepository);
        treatmentService = new TreatmentService(treatmentRepository, treatmentTypeRepository,
                patientRepository, clinicianRepository);

        // seed one patient
        testPatient = patientService.registerNewPatient("Test Patient", "tp@test.com", "pwd", false);

        // seed one clinician
        testClinician = new Clinician(
                "cln-1",
                "Dr. Test",
                "cln@test.com",
                "pwd",
                "Physiotherapy"
        );
        clinicianRepository.save(testClinician);

        // seed one treatment type (id, name, price, active)
        testType = new TreatmentType("tt-1", "Physiotherapy", 100.0, true);
        treatmentTypeRepository.save(testType);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {}
                });
    }

    @Test
    void createNewTreatment_createsCorrectTreatment() throws StorageException {
        Treatment t = treatmentService.createNewTreatment(testPatient, testClinician, testType, 3);

        assertNotNull(t.getId());
        assertEquals(testPatient.getId(), t.getPatientId());
        assertEquals(testClinician.getId(), t.getClinicianId());
        assertEquals(TreatmentStatus.NEW_TREATMENT, t.getStatus());
        assertEquals(1, t.getEntries().size());
        assertEquals(3, t.getEntries().get(0).getQuantity());

        // load again from repo
        Treatment loaded = treatmentService.getTreatmentById(t.getId());
        assertEquals(TreatmentStatus.NEW_TREATMENT, loaded.getStatus());
    }

    @Test
    void costTreatment_onNewTreatment_throwsInvalidTreatmentStateException()
            throws StorageException {

        Treatment t = treatmentService.createNewTreatment(testPatient, testClinician, testType, 1);

        // try costing before clinician has assessed it -> should fail
        assertThrows(InvalidTreatmentStateException.class, () ->
                treatmentService.costTreatment(t.getId())
        );
    }

    @Test
    void costTreatment_onAssessedTreatment_calculatesTotal() throws StorageException, InvalidTreatmentStateException {
        Treatment t = treatmentService.createNewTreatment(testPatient, testClinician, testType, 2);

        treatmentRepository.save(markAssessed(t));

        double total = treatmentService.costTreatment(t.getId());
        assertEquals(2 * 100.0, total, 0.0001);

        Treatment loaded = treatmentService.getTreatmentById(t.getId());
        assertEquals(total, loaded.getTotalCost(), 0.0001);
        assertEquals(TreatmentStatus.TREATMENT_ASSESSED, loaded.getStatus());
    }

    private Treatment markAssessed(Treatment t) throws StorageException {
        t.setStatus(TreatmentStatus.TREATMENT_ASSESSED);
        treatmentRepository.save(t);
        return t;
    }

    @Test
    void getTreatmentsForClinician_returnsOnlyThatCliniciansTreatments() throws StorageException {
        Treatment t1 = treatmentService.createNewTreatment(testPatient, testClinician, testType, 1);

        Clinician other = new Clinician("cln-2", "Other Doc", "other@test.com", "pwd", "Physiotherapy");
        clinicianRepository.save(other);
        Treatment t2 = treatmentService.createNewTreatment(testPatient, other, testType, 1);

        List<Treatment> forTestClinician = treatmentService.getTreatmentsForClinician(testClinician.getId());
        assertTrue(forTestClinician.stream().anyMatch(t -> t.getId().equals(t1.getId())));
        assertFalse(forTestClinician.stream().anyMatch(t -> t.getId().equals(t2.getId())));
    }

    @Test
    void assessTreatment_attachesNotesAndSetsStatus() throws StorageException, InvalidTreatmentStateException {
        Treatment t = treatmentService.createNewTreatment(testPatient, testClinician, testType, 1);
        treatmentService.assessTreatment(t.getId(), "Patient requires gentle mobilization.");

        Treatment loaded = treatmentService.getTreatmentById(t.getId());
        assertEquals(TreatmentStatus.TREATMENT_ASSESSED, loaded.getStatus());
        assertFalse(loaded.getEntries().isEmpty());
        assertTrue(loaded.getEntries().get(0).getNotes().contains("gentle mobilization"));
    }

    @Test
    void recordAssessment_withEntries_overwritesEntriesAndSetsStatus() throws StorageException, InvalidTreatmentStateException {
        Treatment t = treatmentService.createNewTreatment(testPatient, testClinician, testType, 1);

        // create a detailed assessment entry
        TreatmentEntry e1 = new TreatmentEntry(t.getId(), testType.getId(), 2, "Two sessions recommended");
        e1.setLineCost(0.0);

        treatmentService.recordAssessment(t.getId(), List.of(e1));

        Treatment loaded = treatmentService.getTreatmentById(t.getId());
        assertEquals(TreatmentStatus.TREATMENT_ASSESSED, loaded.getStatus());
        assertEquals(1, loaded.getEntries().size());
        assertEquals("Two sessions recommended", loaded.getEntries().get(0).getNotes());
        assertEquals(testType.getId(), loaded.getEntries().get(0).getTreatmentTypeId());
    }

    @Test
    void markTreatmentAsPaid_success_whenCosted() throws StorageException, InvalidTreatmentStateException {
        Treatment t = treatmentService.createNewTreatment(testPatient, testClinician, testType, 2);

        // mark assessed and cost it
        treatmentRepository.save(markAssessed(t));
        double total = treatmentService.costTreatment(t.getId());
        assertTrue(total > 0);

        treatmentService.markTreatmentAsPaid(t.getId());

        Treatment loaded = treatmentService.getTreatmentById(t.getId());
        assertTrue(loaded.isPaid());
    }

    @Test
    void markTreatmentAsPaid_beforeCost_throwsInvalidTreatmentStateException() throws StorageException {
        Treatment t = treatmentService.createNewTreatment(testPatient, testClinician, testType, 1);

        // mark as assessed but do NOT cost
        treatmentRepository.save(markAssessed(t));

        assertThrows(InvalidTreatmentStateException.class, () ->
                treatmentService.markTreatmentAsPaid(t.getId())
        );
    }

    @Test
    void allocateTreatment_byIds_createsTreatment() throws StorageException, UserNotFoundException {
        Treatment created = treatmentService.allocateTreatment(testPatient.getId(), testClinician.getId(), List.of(testType.getId()));
        assertNotNull(created.getId());
        assertEquals(testPatient.getId(), created.getPatientId());
        assertEquals(testClinician.getId(), created.getClinicianId());
        assertEquals(TreatmentStatus.NEW_TREATMENT, created.getStatus());
        assertFalse(created.getEntries().isEmpty());
        assertEquals(testType.getId(), created.getEntries().get(0).getTreatmentTypeId());
    }

    @Test
    void allocateTreatment_invalidPatient_throwsUserNotFoundException() {
        assertThrows(UserNotFoundException.class, () ->
                treatmentService.allocateTreatment("non-existent-patient", testClinician.getId(), List.of(testType.getId()))
        );
    }

    @Test
    void getTreatmentsByStatus_returnsCorrectSubset() throws StorageException {
        Treatment t1 = treatmentService.createNewTreatment(testPatient, testClinician, testType, 1);
        Treatment t2 = treatmentService.createNewTreatment(testPatient, testClinician, testType, 1);

        // make t2 assessed
        treatmentRepository.save(markAssessed(t2));

        List<Treatment> newTreatments = treatmentService.getTreatmentsByStatus(TreatmentStatus.NEW_TREATMENT);
        List<Treatment> assessedTreatments = treatmentService.getTreatmentsByStatus(TreatmentStatus.TREATMENT_ASSESSED);

        assertTrue(newTreatments.stream().anyMatch(t -> t.getId().equals(t1.getId())));
        assertFalse(newTreatments.stream().anyMatch(t -> t.getId().equals(t2.getId())));

        assertTrue(assessedTreatments.stream().anyMatch(t -> t.getId().equals(t2.getId())));
    }
}
