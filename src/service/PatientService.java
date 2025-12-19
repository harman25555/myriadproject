package service;

import exception.StorageException;
import exception.UserNotFoundException;
import model.Patient;
import repository.PatientRepository;

import java.util.List;
import java.util.Optional;

public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient registerWalkInPatient(String name, String email) throws StorageException {
        String id = java.util.UUID.randomUUID().toString();
        Patient patient = new Patient(id, name, email, "", false, false);
        patientRepository.save(patient);
        return patient;
    }

    public Patient registerNewPatient(String name,
                                      String email,
                                      String password,
                                      boolean marketingOptIn) throws StorageException {

        boolean exists = patientRepository.findAll().stream()
                .anyMatch(p -> p.getEmail().equalsIgnoreCase(email));

        if (exists) {
            throw new IllegalArgumentException("A patient with this email already exists.");
        }

        String id = java.util.UUID.randomUUID().toString();
        Patient patient = new Patient(id, name, email, password, true, marketingOptIn);
        patientRepository.save(patient);
        return patient;
    }

    public Patient getPatientById(String id) throws StorageException {
        Optional<Patient> opt = patientRepository.findById(id);
        if (opt.isEmpty()) {
            // this is unchecked – AdminMenu already catches IllegalArgumentException
            throw new IllegalArgumentException("Patient with id " + id + " not found.");
        }
        return opt.get();
    }

    public List<Patient> getAllPatients() throws StorageException {
        return patientRepository.findAll();
    }

    public void flagNonPaying(String patientId) throws StorageException {
        Patient patient = getPatientById(patientId);
        patient.setFlaggedNonPaying(true);
        patientRepository.save(patient);
    }

    public Patient updateMarketingPreference(String patientId, boolean optIn) throws StorageException {
        Patient p = getPatientById(patientId);
        p.setMarketingOptIn(optIn);
        patientRepository.save(p);
        return p;
    }

    public Patient upgradeToRegistered(String patientId) throws StorageException {
        Patient p = getPatientById(patientId);
        p.setRegistered(true);
        patientRepository.save(p);
        return p;
    }

    public Patient updateMarketingPreferences(String patientId, boolean optIn)
            throws StorageException {

        Patient p = getPatientById(patientId);
        p.setMarketingOptIn(optIn);
        patientRepository.save(p);
        return p;
    }

    public Patient upgradePatientToRegistered(String patientId, String password)
            throws StorageException, UserNotFoundException {

        Optional<Patient> opt = patientRepository.findById(patientId);
        if (opt.isEmpty()) {
            throw new UserNotFoundException("Patient with id " + patientId + " not found.");
        }

        Patient p = opt.get();
        p.setRegistered(true);
        p.setPassword(password);

        patientRepository.save(p);
        return p;
    }
}
