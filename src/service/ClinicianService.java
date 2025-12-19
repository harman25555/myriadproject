package service;

import exception.StorageException;
import exception.UserNotFoundException;
import model.Clinician;
import repository.ClinicianRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClinicianService {

    private final ClinicianRepository clinicianRepository;

    public ClinicianService(ClinicianRepository clinicianRepository) {
        this.clinicianRepository = clinicianRepository;
    }

    public Clinician getClinicianById(String id) throws StorageException, UserNotFoundException {
        Optional<Clinician> opt = clinicianRepository.findById(id);
        if (opt.isEmpty()) {
            throw new UserNotFoundException("Clinician with id " + id + " not found.");
        }
        return opt.get();
    }

    public List<Clinician> getCliniciansBySpeciality(String speciality) throws StorageException {
        return clinicianRepository.findAll().stream()
                .filter(c -> c.getSpeciality().equalsIgnoreCase(speciality))
                .collect(Collectors.toList());
    }

    public List<Clinician> getAllClinicians() throws StorageException {
        return clinicianRepository.findAll();
    }
}
