package test;

import exception.StorageException;
import exception.UserNotFoundException;
import model.Patient;
import org.junit.jupiter.api.*;
import repository.PatientRepository;
import service.PatientService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PatientServiceTest {

    private Path tempDir;
    private Path patientCsv;
    private PatientRepository patientRepository;
    private PatientService patientService;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("mms-test-patient");
        patientCsv = tempDir.resolve("patients.csv");
        Files.createFile(patientCsv);

        patientRepository = new PatientRepository(patientCsv.toString());
        patientService = new PatientService(patientRepository);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
    }

    @Test
    void registerNewPatient_createsPatientAndPersists() throws StorageException {
        Patient p = patientService.registerNewPatient(
                "Alice Test", "alice@test.com", "secret", true);

        assertNotNull(p.getId());
        assertEquals("Alice Test", p.getName());
        assertTrue(p.isRegistered());

        Patient loaded = patientRepository.findById(p.getId())
                .orElseThrow();
        assertEquals("alice@test.com", loaded.getEmail());
    }

    @Test
    void registerNewPatient_duplicateEmail_throwsIllegalArgumentException() throws StorageException {
        patientService.registerNewPatient("Bob", "bob@test.com", "pwd", false);

        assertThrows(IllegalArgumentException.class, () ->
                patientService.registerNewPatient("Bob2", "bob@test.com", "pwd2", true)
        );
    }

    @Test
    void getPatientById_unknownId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                patientService.getPatientById("non-existent-id")
        );
    }

    @Test
    void flagNonPaying_setsFlagTrue() throws StorageException {
        Patient p = patientService.registerNewPatient("Carl", "carl@test.com", "pwd", false);

        patientService.flagNonPaying(p.getId());

        Patient loaded = patientService.getPatientById(p.getId());
        assertTrue(loaded.isFlaggedNonPaying());
    }

    @Test
    void upgradePatientToRegistered_successfullyUpgrades() throws Exception {
        // create walk-in patient
        Patient p = patientService.registerWalkInPatient("Walk In", "walk@test.com");

        assertFalse(p.isRegistered());

        // upgrade
        patientService.upgradePatientToRegistered(p.getId(), "newpwd");

        Patient updated = patientService.getPatientById(p.getId());
        assertTrue(updated.isRegistered());
        assertEquals("newpwd", updated.getPassword());
    }

    @Test
    void upgradePatientToRegistered_invalidId_throwsException() {
        assertThrows(UserNotFoundException.class, () ->
                patientService.upgradePatientToRegistered("not-here", "pwd")
        );
    }

    @Test
    void updateMarketingPreferences_updatesCorrectly() throws Exception {
        Patient p = patientService.registerNewPatient("Dave", "dave@test.com", "pwd", false);

        // update marketing preference
        patientService.updateMarketingPreferences(p.getId(), true);

        Patient loaded = patientService.getPatientById(p.getId());
        assertTrue(loaded.isMarketingOptIn());
    }

    @Test
    void registerWalkInPatient_createsUnregisteredPatient() throws StorageException {
        Patient p = patientService.registerWalkInPatient("Walky", "walky@test.com");

        assertNotNull(p.getId());
        assertFalse(p.isRegistered());
        assertFalse(p.isMarketingOptIn());
    }


    @Test
    void flagNonPaying_isIdempotent() throws StorageException {
        Patient p = patientService.registerNewPatient("Henry", "hen@test.com", "pwd", false);

        patientService.flagNonPaying(p.getId());
        patientService.flagNonPaying(p.getId());

        Patient loaded = patientService.getPatientById(p.getId());
        assertTrue(loaded.isFlaggedNonPaying());
    }

    @Test
    void multipleUpdates_persistCorrectly() throws StorageException {
        Patient p = patientService.registerNewPatient("Multi", "multi@test.com", "pwd", true);

        // flag and update preferences
        patientService.flagNonPaying(p.getId());
        patientService.updateMarketingPreferences(p.getId(), false);

        Patient loaded = patientService.getPatientById(p.getId());
        assertTrue(loaded.isFlaggedNonPaying());
        assertFalse(loaded.isMarketingOptIn());
    }

    @Test
    void findByEmail_returnsCorrectPatient() throws Exception {
        patientService.registerNewPatient("Lookup", "lookup@test.com", "pwd", true);

        Patient found = patientRepository.findByEmail("lookup@test.com")
                .orElseThrow(() -> new AssertionError("Patient not found"));

        assertEquals("Lookup", found.getName());
    }
}
