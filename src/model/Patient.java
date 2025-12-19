package model;

public class Patient extends User {

    private boolean registered;
    private boolean marketingOptIn;
    private boolean flaggedNonPaying;

    public Patient(String id,
                   String name,
                   String email,
                   String password,
                   boolean registered,
                   boolean marketingOptIn) {
        super(id, name, email, password);
        this.registered = registered;
        this.marketingOptIn = marketingOptIn;
        this.flaggedNonPaying = false;
    }

    @Override
    public UserRole getRole() {
        return UserRole.PATIENT;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean isMarketingOptIn() {
        return marketingOptIn;
    }

    public void setMarketingOptIn(boolean marketingOptIn) {
        this.marketingOptIn = marketingOptIn;
    }

    public boolean isFlaggedNonPaying() {
        return flaggedNonPaying;
    }

    public void setFlaggedNonPaying(boolean flaggedNonPaying) {
        this.flaggedNonPaying = flaggedNonPaying;
    }
}