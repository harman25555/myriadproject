package cli;

import exception.InvalidTreatmentStateException;
import exception.StorageException;
import exception.UserNotFoundException;
import model.CentreAdministrator;
import model.Patient;
import model.Treatment;
import model.TreatmentStatus;
import model.TreatmentType;
import service.PatientService;
import service.PromotionService;
import service.TreatmentService;
import model.Clinician;


import java.util.List;

public class AdminMenu {

    private final CentreAdministrator admin;
    private final PatientService patientService;
    private final TreatmentService treatmentService;
    private final PromotionService promotionService;
    private final InputHelper input;

    public AdminMenu(CentreAdministrator admin,
                     PatientService patientService,
                     TreatmentService treatmentService,
                     PromotionService promotionService,
                     InputHelper input) {
        this.admin = admin;
        this.patientService = patientService;
        this.treatmentService = treatmentService;
        this.promotionService = promotionService;
        this.input = input;
    }

    public void show() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = input.readInt("Choose an option: ", 1, 9);

            switch (choice) {
                case 1 -> registerWalkInPatient();
                case 2 -> allocatePatientToClinician();
                case 3 -> viewAndCostAssessedTreatments();
                case 4 -> addTreatmentType();
                case 5 -> removeTreatmentType();
                case 6 -> flagNonPayingPatient();
                case 7 -> sendPromotions();
                case 8 -> listAllTreatmentTypes();
                case 9 -> back = true;
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== Administrator Menu ===");
        System.out.println("1. Register walk-in patient");
        System.out.println("2. Allocate patient to clinician (create new treatment)");
        System.out.println("3. View assessed treatments and cost them");
        System.out.println("4. Add treatment type");
        System.out.println("5. Remove treatment type");
        System.out.println("6. Flag non-paying patient");
        System.out.println("7. Send promotions to registered patients (opt-in)");
        System.out.println("8. List all treatment types");
        System.out.println("9. Logout");
    }

    private void registerWalkInPatient() {
        System.out.println("\n--- Register Walk-in Patient ---");
        try {
            String name = input.readNonEmptyString("Full name: ");
            String email = input.readNonEmptyString("Email: ");
            // Walk-in: can be unregistered=false, but stored in system
            Patient patient = patientService.registerWalkInPatient(name, email);
            System.out.println("Walk-in patient recorded with ID: " + patient.getId());
        } catch (StorageException e) {
            System.out.println("Error registering walk-in patient: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid data: " + e.getMessage());
        }
    }

    private void allocatePatientToClinician() {
        System.out.println("\n--- Allocate Patient to Clinician ---");
        try {
            String patientId = input.readNonEmptyString("Patient ID: ");
            Patient patient = patientService.getPatientById(patientId);

            System.out.println("Patient found: " + patient.getName());

            String speciality = input.readNonEmptyString("Required clinician speciality: ");

            // Ask service to suggest clinicians with that speciality
            var clinicians = treatmentService.findCliniciansBySpeciality(speciality);

            if (clinicians.isEmpty()) {
                System.out.println("No clinician found with speciality: " + speciality);
                return;
            }

            System.out.println("Available clinicians:");
            for (int i = 0; i < clinicians.size(); i++) {
                System.out.println((i + 1) + ". " + clinicians.get(i).getName()
                        + " (" + clinicians.get(i).getSpeciality() + ")");
            }

            int choice = input.readInt("Select clinician: ", 1, clinicians.size());
            var clinician = clinicians.get(choice - 1);

            // Choose treatment types (for simplicity: single type)
            listAllTreatmentTypes();
            String treatmentTypeId = input.readNonEmptyString("Treatment Type ID: ");
            TreatmentType type = treatmentService.getTreatmentTypeById(treatmentTypeId);

            int quantity = input.readInt("Quantity (e.g., sessions): ", 1, 20);

            Treatment treatment = treatmentService.createNewTreatment(patient, clinician, type, quantity);
            System.out.println("Treatment created with ID: " + treatment.getId()
                    + " and status: " + treatment.getStatus());

        } catch (StorageException e) {
            System.out.println("Error during allocation: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // will catch "patient not found" or "treatment type not found"
            System.out.println("Invalid data: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }


    private void viewAndCostAssessedTreatments() {
        System.out.println("\n--- Assessed Treatments (Ready to Cost) ---");
        try {
            List<Treatment> assessed = treatmentService.getTreatmentsByStatus(TreatmentStatus.TREATMENT_ASSESSED);
            if (assessed.isEmpty()) {
                System.out.println("No assessed treatments to cost.");
                return;
            }

            for (int i = 0; i < assessed.size(); i++) {
                Treatment t = assessed.get(i);
                System.out.println((i + 1) + ". ID: " + t.getId()
                        + ", Patient: " + t.getPatient().getName()
                        + ", Clinician: " + t.getClinician().getName()
                        + ", Current status: " + t.getStatus());
            }

            int choice = input.readInt("Select treatment to cost (or 0 to cancel): ", 0, assessed.size());
            if (choice == 0) return;

            Treatment selected = assessed.get(choice - 1);
            treatmentService.costTreatment(selected.getId());
            Treatment updated = treatmentService.getTreatmentById(selected.getId());
            System.out.println("Total cost for treatment " + updated.getId() + ": " + updated.getTotalCost());

        } catch (StorageException e) {
            System.out.println("Error reading treatments: " + e.getMessage());
        } catch (InvalidTreatmentStateException e) {
            System.out.println("Cannot cost treatment: " + e.getMessage());
        }
    }

    private void addTreatmentType() {
        System.out.println("\n--- Add Treatment Type ---");
        try {
            String name = input.readNonEmptyString("Treatment name: ");
            double price = Double.parseDouble(input.readNonEmptyString("Base price: "));
            TreatmentType type = treatmentService.addTreatmentType(name, price);
            System.out.println("Treatment type added with ID: " + type.getId());
        } catch (StorageException e) {
            System.out.println("Error adding treatment type: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid price format.");
        }
    }

    private void removeTreatmentType() {
        System.out.println("\n--- Remove Treatment Type ---");
        try {
            listAllTreatmentTypes();
            String id = input.readNonEmptyString("Treatment Type ID to remove: ");
            treatmentService.removeTreatmentType(id);
            System.out.println("Treatment type removed.");
        } catch (StorageException e) {
            System.out.println("Error removing treatment type: " + e.getMessage());
        }
    }

    private void flagNonPayingPatient() {
        System.out.println("\n--- Flag Non-Paying Patient ---");
        try {
            String patientId = input.readNonEmptyString("Patient ID: ");
            patientService.flagNonPaying(patientId);
            System.out.println("Patient " + patientId + " flagged as non-paying.");
        } catch (StorageException e) {
            System.out.println("Error flagging patient: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid patient ID: " + e.getMessage());
        }
    }

    private void sendPromotions() {
        System.out.println("\n--- Send Promotions to Opt-in Patients ---");
        String title = input.readNonEmptyString("Promotion title: ");
        String message = input.readNonEmptyString("Promotion message: ");
        try {
            int count = promotionService.sendPromotionToOptInPatients(title, message);
            System.out.println("Promotion sen t to " + count + " patients.");
        } catch (StorageException e) {
            System.out.println("Error sending promotions: " + e.getMessage());
        }
    }

    private void listAllTreatmentTypes() {
        System.out.println("\n--- Treatment Types ---");
        try {
            List<TreatmentType> types = treatmentService.getAllTreatmentTypes();
            if (types.isEmpty()) {
                System.out.println("No treatment types configured.");
                return;
            }
            for (TreatmentType t : types) {
                System.out.println("ID: " + t.getId() + " | Name: " + t.getName()
                        + " | Price: " + t.getBasePrice());
            }
        } catch (StorageException e) {
            System.out.println("Error loading treatment types: " + e.getMessage());
        }
    }
}
