package model;

public class TreatmentEntry {

    private String treatmentId;
    private String id;
    private String treatmentTypeId;
    private int quantity;
    private String notes;
    private double lineCost; // cached total for this line

    public TreatmentEntry() {
    }

    public TreatmentEntry(String treatmentId, String treatmentTypeId, int quantity, String notes) {
        this.treatmentId = treatmentId;
        this.treatmentTypeId = treatmentTypeId;
        this.quantity = quantity;
        this.notes = notes;
        this.lineCost = 0.0;
    }

    public TreatmentEntry(String treatmentId, String treatmentTypeId, int quantity,
                          String notes, double lineCost) {
        this.treatmentId = treatmentId;
        this.treatmentTypeId = treatmentTypeId;
        this.quantity = quantity;
        this.notes = notes;
        this.lineCost = lineCost;
    }

    public String getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(String treatmentId) {
        this.treatmentId = treatmentId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTreatmentTypeId() {
        return treatmentTypeId;
    }

    public void setTreatmentTypeId(String treatmentTypeId) {
        this.treatmentTypeId = treatmentTypeId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
    }

    public double getLineCost() {
        return lineCost;
    }

    public void setLineCost(double lineCost) {
        this.lineCost = lineCost;
    }

    @Override
    public String toString() {
        return "TreatmentEntry{" +
                "treatmentId='" + treatmentId + '\'' +
                ", id='" + id + '\'' +
                ", treatmentTypeId='" + treatmentTypeId + '\'' +
                ", quantity=" + quantity +
                ", notes='" + notes + '\'' +
                ", lineCost=" + lineCost +
                '}';
    }
}
