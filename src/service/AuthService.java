package service;

import exception.AuthenticationException;
import exception.StorageException;
import model.*;
import repository.*;

import java.util.Optional;

public class AuthService {

    private final PatientRepository patientRepo;
    private final ClinicianRepository clinicianRepo;
    private final AdminRepository adminRepo;

    public AuthService(PatientRepository patientRepo,
                       ClinicianRepository clinicianRepo,
                       AdminRepository adminRepo) {
        this.patientRepo = patientRepo;
        this.clinicianRepo = clinicianRepo;
        this.adminRepo = adminRepo;
    }

    /**
     * Login a user by email/password.
     * Tries: Admin → Clinician → Patient
     */
    public User login(String email, String password)
            throws AuthenticationException, StorageException {

        // 1. Admin
        Optional<CentreAdministrator> adminOpt =
                adminRepo.findByEmail(email);
        if (adminOpt.isPresent() &&
                adminOpt.get().getPassword().equals(password)) {
            return adminOpt.get();
        }

        // 2. Clinician
        Optional<Clinician> clinicianOpt =
                clinicianRepo.findByEmail(email);
        if (clinicianOpt.isPresent() &&
                clinicianOpt.get().getPassword().equals(password)) {
            return clinicianOpt.get();
        }

        // 3. Patient
        Optional<Patient> patientOpt =
                patientRepo.findByEmail(email);
        if (patientOpt.isPresent() &&
                patientOpt.get().getPassword().equals(password)) {
            return patientOpt.get();
        }

        throw new AuthenticationException("Invalid email or password.");
    }

    /**
     * Convenience: login as Patient (throws AuthenticationException if credentials wrong or not a patient)
     */
    public Patient loginPatient(String email, String password) throws AuthenticationException, StorageException {
        User u = login(email, password);
        if (u instanceof Patient) {
            return (Patient) u;
        }
        throw new AuthenticationException("User is not a patient.");
    }

    /**
     * Convenience: login as Clinician
     */
    public Clinician loginClinician(String email, String password) throws AuthenticationException, StorageException {
        User u = login(email, password);
        if (u instanceof Clinician) {
            return (Clinician) u;
        }
        throw new AuthenticationException("User is not a clinician.");
    }

    /**
     * Convenience: login as Admin
     */
    public CentreAdministrator loginAdmin(String email, String password) throws AuthenticationException, StorageException {
        User u = login(email, password);
        if (u instanceof CentreAdministrator) {
            return (CentreAdministrator) u;
        }
        throw new AuthenticationException("User is not an administrator.");
    }
}
