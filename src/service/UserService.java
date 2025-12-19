package service;

import exception.AuthenticationException;
import exception.StorageException;
import exception.UserNotFoundException;
import model.CentreAdministrator;
import model.Clinician;
import model.Patient;
import model.User;
import repository.AdminRepository;
import repository.ClinicianRepository;
import repository.PatientRepository;

import java.util.Optional;

public class UserService {

    private final PatientRepository patientRepository;
    private final ClinicianRepository clinicianRepository;
    private final AdminRepository adminRepository;

    public UserService(PatientRepository patientRepository,
                       ClinicianRepository clinicianRepository,
                       AdminRepository adminRepository) {
        this.patientRepository = patientRepository;
        this.clinicianRepository = clinicianRepository;
        this.adminRepository = adminRepository;
    }

    /**
     * Authenticate by email/password.
     */
    public User authenticate(String email, String password)
            throws AuthenticationException, StorageException {

        // 1. Admin login
        Optional<CentreAdministrator> adminOpt = adminRepository.findAll().stream()
                .filter(a -> a.getEmail().equalsIgnoreCase(email)
                        && a.getPassword().equals(password))
                .findFirst();

        if (adminOpt.isPresent()) {
            return adminOpt.get();
        }

        // 2. Clinician login
        Optional<Clinician> clinicianOpt = clinicianRepository.findAll().stream()
                .filter(c -> c.getEmail().equalsIgnoreCase(email)
                        && c.getPassword().equals(password))
                .findFirst();

        if (clinicianOpt.isPresent()) {
            return clinicianOpt.get();
        }

        // 3. Patient login
        Optional<Patient> patientOpt = patientRepository.findAll().stream()
                .filter(p -> p.getEmail().equalsIgnoreCase(email)
                        && p.getPassword().equals(password))
                .findFirst();

        if (patientOpt.isPresent()) {
            return patientOpt.get();
        }

        throw new AuthenticationException("Invalid email or password.");
    }

    /**
     * Register a fully registered patient.
     */
    public Patient registerNewPatient(String name,
                                      String email,
                                      String password,
                                      boolean marketingOptIn)
            throws StorageException {

        boolean exists = patientRepository.findAll().stream()
                .anyMatch(p -> p.getEmail().equalsIgnoreCase(email));

        if (exists) {
            throw new IllegalArgumentException("A patient with this email already exists.");
        }

        String id = java.util.UUID.randomUUID().toString();

        Patient patient = new Patient(
                id,
                name,
                email,
                password,
                true,            // registered
                marketingOptIn
        );

        patientRepository.save(patient);
        return patient;
    }

    /**
     * Upgrade walk-in patient to registered.
     */
    public Patient upgradePatientToRegistered(String patientId, String password)
            throws StorageException, UserNotFoundException {

        Optional<Patient> opt = patientRepository.findById(patientId);

        if (opt.isEmpty()) {
            throw new UserNotFoundException("Patient with id " + patientId + " not found.");
        }

        Patient patient = opt.get();
        patient.setRegistered(true);
        patient.setPassword(password);

        patientRepository.save(patient);
        return patient;
    }

    /**
     * Update patient marketing preferences.
     */
    public void updateMarketingPreferences(String patientId, boolean optIn)
            throws StorageException, UserNotFoundException {

        Optional<Patient> opt = patientRepository.findById(patientId);

        if (opt.isEmpty()) {
            throw new UserNotFoundException("Patient with id " + patientId + " not found.");
        }

        Patient patient = opt.get();
        patient.setMarketingOptIn(optIn);

        patientRepository.save(patient);
    }
}
