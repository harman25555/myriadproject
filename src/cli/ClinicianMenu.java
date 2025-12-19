package cli;

import exception.InvalidTreatmentStateException;
import exception.StorageException;
import model.Clinician;
import model.Treatment;
import model.TreatmentStatus;
import service.TreatmentService;

import java.util.List;

public class ClinicianMenu {

    private final Clinician clinician;
    private final TreatmentService treatmentService;
    private final InputHelper input;

    public ClinicianMenu(Clinician clinician,
                         TreatmentService treatmentService,
                         InputHelper input) {
        this.clinician = clinician;
        this.treatmentService = treatmentService;
        this.input = input;
    }

    public void show() {
        boolean back = false;
        while (!back) {
            printMenu();
            int choice = input.readInt("Choose an option: ", 1, 4);

            switch (choice) {
                case 1 -> viewMyTreatments();
                case 2 -> assessTreatment();
                case 3 -> viewAssessedTreatments();
                case 4 -> back = true;
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== Clinician Menu ===");
        System.out.println("1. View my assigned treatments");
        System.out.println("2. Assess treatment (record & mark as assessed)");
        System.out.println("3. View my assessed treatments");
        System.out.println("4. Logout");
    }

    private void viewMyTreatments() {
        System.out.println("\n--- My Treatments ---");
        try {
            List<Treatment> treatments = treatmentService.getTreatmentsForClinician(clinician.getId());
            if (treatments.isEmpty()) {
                System.out.println("No treatments assigned.");
                return;
            }
            for (Treatment t : treatments) {
                System.out.println("ID: " + t.getId()
                        + " | Patient: " + t.getPatient().getName()
                        + " | Status: " + t.getStatus());
            }
        } catch (StorageException e) {
            System.out.println("Error loading treatments: " + e.getMessage());
        }
    }

    private void viewAssessedTreatments() {
        System.out.println("\n--- My Assessed Treatments ---");
        try {
            List<Treatment> treatments = treatmentService.getTreatmentsForClinicianByStatus(
                    clinician.getId(), TreatmentStatus.TREATMENT_ASSESSED);

            if (treatments.isEmpty()) {
                System.out.println("No assessed treatments.");
                return;
            }
            for (Treatment t : treatments) {
                System.out.println("ID: " + t.getId()
                        + " | Patient: " + t.getPatient().getName()
                        + " | Status: " + t.getStatus());
            }
        } catch (StorageException e) {
            System.out.println("Error loading assessed treatments: " + e.getMessage());
        }
    }

    private void assessTreatment() {
        System.out.println("\n--- Assess Treatment ---");
        try {
            List<Treatment> treatments = treatmentService.getTreatmentsForClinicianByStatus(
                    clinician.getId(), TreatmentStatus.NEW_TREATMENT);

            if (treatments.isEmpty()) {
                System.out.println("No new treatments to assess.");
                return;
            }

            for (int i = 0; i < treatments.size(); i++) {
                Treatment t = treatments.get(i);
                System.out.println((i + 1) + ". ID: " + t.getId()
                        + " | Patient: " + t.getPatient().getName());
            }

            int choice = input.readInt("Select treatment to assess (or 0 to cancel): ", 0, treatments.size());
            if (choice == 0) return;

            Treatment selected = treatments.get(choice - 1);
            String notes = input.readNonEmptyString("Enter assessment notes: ");
            // For simplicity, this example just adds generic assessment notes.
            treatmentService.assessTreatment(selected.getId(), notes);
            System.out.println("Treatment " + selected.getId() + " marked as ASSESSED.");
        } catch (StorageException e) {
            System.out.println("Error during assessment: " + e.getMessage());
        } catch (InvalidTreatmentStateException e) {
            System.out.println("Cannot assess treatment: " + e.getMessage());
        }
    }
}
