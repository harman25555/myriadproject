package model;

import java.util.Objects;

public class TreatmentType {

    private String id;
    private String name;
    private double basePrice;
    private boolean active;

    public TreatmentType(String id, String name, double basePrice, boolean active) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.basePrice = basePrice;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public boolean isActive() {
        return active;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "TreatmentType{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", basePrice=" + basePrice +
                ", active=" + active +
                '}';
    }
}
