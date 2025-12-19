package cli;

import exception.AuthenticationException;
import exception.StorageException;
import model.CentreAdministrator;
import model.Clinician;
import model.Patient;
import model.User;
import model.UserRole;
import service.AuthService;
import service.PatientService;
import service.PromotionService;
import service.TreatmentService;

import java.util.Scanner;

public class MainMenu {

    private final AuthService authService;
    private final PatientService patientService;
    private final TreatmentService treatmentService;
    private final PromotionService promotionService;

    private final Scanner scanner;
    private final InputHelper input;

    public MainMenu(AuthService authService,
                    PatientService patientService,
                    TreatmentService treatmentService,
                    PromotionService promotionService) {
        this.authService = authService;
        this.patientService = patientService;
        this.treatmentService = treatmentService;
        this.promotionService = promotionService;
        this.scanner = new Scanner(System.in);
        this.input = new InputHelper(scanner);
    }

    public void start() {
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = input.readInt("Choose an option: ", 1, 5);

            switch (choice) {
                case 1 -> loginAsAdmin();
                case 2 -> loginAsClinician();
                case 3 -> loginAsPatient();
                case 4 -> registerNewPatient();
                case 5 -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
            }
        }
        scanner.close();
    }

    private void printMainMenu() {
        System.out.println("\n=== Myriad Medical Services (MMS) ===");
        System.out.println("1. Login as Administrator");
        System.out.println("2. Login as Clinician");
        System.out.println("3. Login as Patient");
        System.out.println("4. Register as new Patient");
        System.out.println("5. Exit");
    }

    private void loginAsAdmin() {
        System.out.println("\n--- Administrator Login ---");
        String email = input.readNonEmptyString("Email: ");
        String password = input.readNonEmptyString("Password: ");

        try {
            User user = authService.login(email, password);
            if (user.getRole() != UserRole.ADMIN) {
                System.out.println("Access denied: user is not an administrator.");
                return;
            }
            CentreAdministrator admin = (CentreAdministrator) user;
            System.out.println("Welcome, " + admin.getName() + "!");
            AdminMenu adminMenu = new AdminMenu(admin, patientService, treatmentService, promotionService, input);
            adminMenu.show();
        } catch (AuthenticationException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (StorageException e) {
            System.out.println("System error while logging in: " + e.getMessage());
        }
    }

    private void loginAsClinician() {
        System.out.println("\n--- Clinician Login ---");
        String email = input.readNonEmptyString("Email: ");
        String password = input.readNonEmptyString("Password: ");

        try {
            User user = authService.login(email, password);
            if (user.getRole() != UserRole.CLINICIAN) {
                System.out.println("Access denied: user is not a clinician.");
                return;
            }
            Clinician clinician = (Clinician) user;
            System.out.println("Welcome, Dr. " + clinician.getName() + "!");
            ClinicianMenu clinicianMenu = new ClinicianMenu(clinician, treatmentService, input);
            clinicianMenu.show();
        } catch (AuthenticationException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (StorageException e) {
            System.out.println("System error while logging in: " + e.getMessage());
        }
    }

    private void loginAsPatient() {
        System.out.println("\n--- Patient Login ---");
        String email = input.readNonEmptyString("Email: ");
        String password = input.readNonEmptyString("Password: ");

        try {
            User user = authService.login(email, password);
            if (user.getRole() != UserRole.PATIENT) {
                System.out.println("Access denied: user is not a patient.");
                return;
            }
            Patient patient = (Patient) user;
            System.out.println("Welcome, " + patient.getName() + "!");
            PatientMenu patientMenu = new PatientMenu(patient, patientService, treatmentService, promotionService, input);
            patientMenu.show();
        } catch (AuthenticationException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (StorageException e) {
            System.out.println("System error while logging in: " + e.getMessage());
        }
    }

    private void registerNewPatient() {
        System.out.println("\n--- Register New Patient ---");
        try {
            String name = input.readNonEmptyString("Full name: ");
            String email = input.readNonEmptyString("Email: ");
            String password = input.readNonEmptyString("Password: ");
            boolean marketing = input.readYesNo("Would you like to receive offers & promotions?");

            Patient patient = patientService.registerNewPatient(name, email, password, marketing);
            System.out.println("Patient registered successfully with ID: " + patient.getId());
        } catch (StorageException e) {
            System.out.println("Error registering patient: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid data: " + e.getMessage());
        }
    }
}
