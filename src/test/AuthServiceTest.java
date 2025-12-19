package test;

import exception.AuthenticationException;
import exception.StorageException;
import model.CentreAdministrator;
import model.Clinician;
import model.Patient;
import org.junit.jupiter.api.*;
import repository.AdminRepository;
import repository.ClinicianRepository;
import repository.PatientRepository;
import service.AuthService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private Path tempDir;
    private Path patientCsv;
    private Path clinicianCsv;
    private Path adminCsv;

    private PatientRepository patientRepository;
    private ClinicianRepository clinicianRepository;
    private AdminRepository adminRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() throws IOException, StorageException {
        tempDir = Files.createTempDirectory("mms-test-auth");

        patientCsv = tempDir.resolve("patients.csv");
        clinicianCsv = tempDir.resolve("clinicians.csv");
        adminCsv = tempDir.resolve("admins.csv");

        Files.createFile(patientCsv);
        Files.createFile(clinicianCsv);
        Files.createFile(adminCsv);

        patientRepository = new PatientRepository(patientCsv.toString());
        clinicianRepository = new ClinicianRepository(clinicianCsv.toString());
        adminRepository = new AdminRepository(adminCsv.toString());

        authService = new AuthService(patientRepository, clinicianRepository, adminRepository);

        // Seed one of each
        Patient p = new Patient("p1", "Pat Test", "pat@test.com", "pwd", true, false);
        patientRepository.save(p);

        Clinician c = new Clinician("c1", "Doc Test", "doc@test.com", "pwd", "Cardiology");
        clinicianRepository.save(c);

        CentreAdministrator a = new CentreAdministrator("a1", "Admin Test", "admin@test.com", "pwd");
        adminRepository.save(a);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); }
                    catch (IOException ignored) {}
                });
    }
    // VALID LOGIN TESTS
    @Test
    void loginPatient_validCredentials_returnsPatient() throws Exception {
        Patient p = authService.loginPatient("pat@test.com", "pwd");
        assertEquals("Pat Test", p.getName());
    }

    @Test
    void loginClinician_validCredentials_returnsClinician() throws Exception {
        Clinician c = authService.loginClinician("doc@test.com", "pwd");
        assertEquals("Doc Test", c.getName());
    }

    @Test
    void loginAdmin_validCredentials_returnsAdmin() throws Exception {
        CentreAdministrator a = authService.loginAdmin("admin@test.com", "pwd");
        assertEquals("Admin Test", a.getName());
    }
    // INVALID PASSWORD / WRONG USER TESTS
    @Test
    void loginPatient_invalidPassword_throwsException() {
        assertThrows(AuthenticationException.class,
                () -> authService.loginPatient("pat@test.com", "wrong"));
    }

    @Test
    void loginClinician_invalidPassword_throwsException() {
        assertThrows(AuthenticationException.class,
                () -> authService.loginClinician("doc@test.com", "123"));
    }

    @Test
    void loginAdmin_invalidPassword_throwsException() {
        assertThrows(AuthenticationException.class,
                () -> authService.loginAdmin("admin@test.com", "incorrect"));
    }
    // NON-EXISTENT USER TESTS
    @Test
    void loginPatient_unknownEmail_throwsException() {
        assertThrows(AuthenticationException.class,
                () -> authService.loginPatient("unknown@test.com", "pwd"));
    }

    @Test
    void login_unknownEmail_throwsException() {
        assertThrows(AuthenticationException.class,
                () -> authService.login("noone@test.com", "pwd"));
    }
    // WRONG ROLE TESTS
    @Test
    void loginPatient_usingClinicianEmail_throwsException() {
        assertThrows(AuthenticationException.class,
                () -> authService.loginPatient("doc@test.com", "pwd"));
    }

    @Test
    void loginClinician_usingAdminEmail_throwsException() {
        assertThrows(AuthenticationException.class,
                () -> authService.loginClinician("admin@test.com", "pwd"));
    }
    // CASE INSENSITIVE EMAIL MATCHING
    @Test
    void loginPatient_emailCaseInsensitive_success() throws Exception {
        Patient p = authService.loginPatient("PAT@TEST.COM", "pwd");
        assertNotNull(p);
        assertEquals("Pat Test", p.getName());
    }
    // NULL / EMPTY INPUT TESTS
    @Test
    void login_nullEmail_throwsException() {
        assertThrows(AuthenticationException.class,
                () -> authService.login(null, "pwd"));
    }

    @Test
    void login_emptyEmail_throwsException() {
        assertThrows(AuthenticationException.class,
                () -> authService.login("", "pwd"));
    }

}
