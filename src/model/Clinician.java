package model;

public class Clinician extends User {

    private String speciality;

    public Clinician(String id,
                     String name,
                     String email,
                     String password,
                     String speciality) {
        super(id, name, email, password);
        this.speciality = speciality;
    }

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
    }

    @Override
    public UserRole getRole() {
        return UserRole.CLINICIAN;
    }
}
