import cli.MainMenu;
import repository.AdminRepository;
import repository.ClinicianRepository;
import repository.PatientRepository;
import repository.TreatmentRepository;
import repository.TreatmentTypeRepository;
import service.AuthService;
import service.PatientService;
import service.TreatmentService;
import service.PromotionService;

public class Main {

    public static void main(String[] args) {

        String basePath = "data/"; // make sure folders exist
        String patientCsv = basePath + "patients.csv";
        String clinicianCsv = basePath + "clinicians.csv";
        String adminCsv = basePath + "admins.csv";
        String treatmentCsv = basePath + "treatments.csv";
        String treatmentEntriesCsv = basePath + "treatment_entries.csv";
        String treatmentTypeCsv = basePath + "treatment_types.csv";
        String promotionLogCsv = basePath + "promotion_log.csv";

        // --- Instantiate repositories ---
        PatientRepository patientRepository = new PatientRepository(patientCsv);
        ClinicianRepository clinicianRepository = new ClinicianRepository(clinicianCsv);
        AdminRepository adminRepository = new AdminRepository(adminCsv);
        // pass BOTH CSV paths
        TreatmentRepository treatmentRepository =
                new TreatmentRepository(treatmentCsv, treatmentEntriesCsv);
        TreatmentTypeRepository treatmentTypeRepository = new TreatmentTypeRepository(treatmentTypeCsv);

        // --- Instantiate services ---
        AuthService authService =
                new AuthService(patientRepository, clinicianRepository, adminRepository);
        PatientService patientService = new PatientService(patientRepository);
        TreatmentService treatmentService =
                new TreatmentService(treatmentRepository, treatmentTypeRepository,
                        patientRepository, clinicianRepository);
        PromotionService promotionService =
                new PromotionService(patientRepository, promotionLogCsv);

        // --- Start main menu ---
        MainMenu mainMenu = new MainMenu(authService, patientService, treatmentService, promotionService);
        mainMenu.start(); // loop until exit
    }
}
