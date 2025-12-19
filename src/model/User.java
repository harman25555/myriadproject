package model;

import java.util.Objects;

public abstract class User {

    protected String id;
    protected String name;
    protected String email;
    protected String password;

    protected User(String id, String name, String email, String password) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    public abstract UserRole getRole();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public void setEmail(String email) {
        this.email = Objects.requireNonNull(email);
    }

    public void setPassword(String password) {
        this.password = Objects.requireNonNull(password);
    }

    @Override
    public String toString() {
        return getRole() + "{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}