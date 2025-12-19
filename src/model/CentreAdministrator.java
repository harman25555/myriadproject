package model;

public class CentreAdministrator extends User {

    public CentreAdministrator(String id,
                               String name,
                               String email,
                               String password) {
        super(id, name, email, password);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }
}