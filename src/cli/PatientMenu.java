package cli;

import exception.StorageException;
import model.Clinician;
import model.Patient;
import model.Treatment;
import model.TreatmentType;
import service.PatientService;
import service.PromotionService;
import service.TreatmentService;

import java.util.List;

public class PatientMenu {

    private Patient patient; // mutable: may be upgraded
    private final PatientService patientService;
    private final TreatmentService treatmentService;
    private final PromotionService promotionService;
    private final InputHelper input;

    public PatientMenu(Patient patient,
                       PatientService patientService,
                       TreatmentService treatmentService,
                       PromotionService promotionService,
                       InputHelper input) {
        this.patient = patient;
        this.patientService = patientService;
        this.treatmentService = treatmentService;
        this.promotionService = promotionService;
        this.input = input;
    }

    public void show() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = input.readInt("Choose an option: ", 1, 6);

            switch (choice) {
                case 1 -> bookTreatment();
                case 2 -> upgradeToRegistered();
                case 3 -> manageNotifications();
                case 4 -> viewMyTreatments();
                case 5 -> viewBills();  // optional/simple
                case 6 -> back = true;
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== Patient Menu ===");
        System.out.println("Logged in as: " + patient.getName()
                + " (registered: " + patient.isRegistered() + ")");
        System.out.println("1. Book treatment");
        System.out.println("2. Upgrade to registered");
        System.out.println("3. Manage notification preferences");
        System.out.println("4. View my treatments");
        System.out.println("5. View my bills");
        System.out.println("6. Logout");
    }

    private void bookTreatment() {
        System.out.println("\n--- Book Treatment ---");
        try {
            if (!patient.isRegistered()) {
                System.out.println("Warning: you are currently not a registered patient.");
                boolean proceed = input.readYesNo("Continue booking as unregistered?");
                if (!proceed) return;
            }

            // Choose treatment type
            List<TreatmentType> types = treatmentService.getAllTreatmentTypes();
            if (types.isEmpty()) {
                System.out.println("No treatments available at the moment.");
                return;
            }
            System.out.println("Available treatment types:");
            for (int i = 0; i < types.size(); i++) {
                TreatmentType t = types.get(i);
                System.out.println((i + 1) + ". ID: " + t.getId()
                        + " | Name: " + t.getName()
                        + " | Price: " + t.getBasePrice());
            }

            int typeIdx = input.readInt("Select treatment type: ", 1, types.size());
            TreatmentType selectedType = types.get(typeIdx - 1);

            int quantity = input.readInt("Quantity (e.g., sessions): ", 1, 20);

            // For simplicity: let the system auto-select a clinician with matching speciality
            List<Clinician> clinicians = treatmentService.findCliniciansBySpeciality(selectedType.getName());
            if (clinicians.isEmpty()) {
                System.out.println("No clinician available for this treatment right now.");
                return;
            }
            Clinician clinician = clinicians.get(0);
            System.out.println("Treatment will be assigned to clinician: " + clinician.getName());

            Treatment treatment = treatmentService.createNewTreatment(patient, clinician, selectedType, quantity);
            System.out.println("Treatment booked with ID: " + treatment.getId()
                    + " and status: " + treatment.getStatus());

        } catch (StorageException e) {
            System.out.println("Error while booking treatment: " + e.getMessage());
        }
    }

    private void upgradeToRegistered() {
        System.out.println("\n--- Upgrade to Registered Patient ---");
        if (patient.isRegistered()) {
            System.out.println("You are already a registered patient.");
            return;
        }
        try {
            patient = patientService.upgradeToRegistered(patient.getId());
            System.out.println("You are now a registered patient!");
        } catch (StorageException e) {
            System.out.println("Error upgrading patient: " + e.getMessage());
        }
    }

    private void manageNotifications() {
        System.out.println("\n--- Manage Notification Preferences ---");
        System.out.println("Current status: " + (patient.isMarketingOptIn() ? "Opted in" : "Opted out"));
        boolean optIn = input.readYesNo("Would you like to receive offers & promotions?");
        try {
            patient = patientService.updateMarketingPreference(patient.getId(), optIn);
            System.out.println("Preference updated. You are now "
                    + (patient.isMarketingOptIn() ? "opted in." : "opted out."));
        } catch (StorageException e) {
            System.out.println("Error updating preference: " + e.getMessage());
        }
    }

    private void viewMyTreatments() {
        System.out.println("\n--- My Treatments ---");
        try {
            List<Treatment> treatments = treatmentService.getTreatmentsForPatient(patient.getId());
            if (treatments.isEmpty()) {
                System.out.println("You have no treatments.");
                return;
            }
            for (Treatment t : treatments) {
                System.out.println("ID: " + t.getId()
                        + " | Clinician: " + t.getClinician().getName()
                        + " | Status: " + t.getStatus()
                        + " | Total (if costed): " + t.getTotalCost());
            }
        } catch (StorageException e) {
            System.out.println("Error loading your treatments: " + e.getMessage());
        }
    }

    private void viewBills() {
        System.out.println("\n--- My Bills ---");
        try {
            List<Treatment> treatments = treatmentService.getTreatmentsForPatient(patient.getId());
            double totalOutstanding = 0.0;
            for (Treatment t : treatments) {
                if (t.isPaid()) continue;
                if (t.getTotalCost() > 0) {
                    System.out.println("Treatment ID: " + t.getId()
                            + " | Status: " + t.getStatus()
                            + " | Amount due: " + t.getTotalCost());
                    totalOutstanding += t.getTotalCost();
                }
            }
            System.out.println("Total outstanding: " + totalOutstanding);
        } catch (StorageException e) {
            System.out.println("Error loading bills: " + e.getMessage());
        }
    }
}
