package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Treatment {

    private String id;
    private String patientId;
    private String clinicianId;
    private TreatmentStatus status;
    private LocalDateTime createdAt;
    private double totalCost;
    private boolean paid;
    private List<TreatmentEntry> entries = new ArrayList<>();
    private Patient patient;
    private Clinician clinician;

    public Patient getPatient() { return patient; }
    public void setPatient(Patient p) { this.patient = p; }

    public Clinician getClinician() { return clinician; }
    public void setClinician(Clinician c) { this.clinician = c; }
//    public Treatment(String id,
//                     String patientId,
//                     String clinicianId,
//                     TreatmentStatus status,
//                     LocalDateTime createdAt,
//                     double totalCost,
//                     boolean paid) {
//        this.id = Objects.requireNonNull(id);
//        this.patientId = Objects.requireNonNull(patientId);
//        this.clinicianId = Objects.requireNonNull(clinicianId);
//        this.status = Objects.requireNonNull(status);
//        this.createdAt = Objects.requireNonNull(createdAt);
//        this.totalCost = totalCost;
//        this.paid = paid;
//    }

    public Treatment() {
        this.entries = new ArrayList<>();
        this.status = TreatmentStatus.NEW_TREATMENT;
        this.createdAt = LocalDateTime.now();
        this.paid = false;
        this.totalCost = 0.0;
    }

    public Treatment(String id, String patientId, String clinicianId) {
        this();
        this.id = id;
        this.patientId = patientId;
        this.clinicianId = clinicianId;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getClinicianId() {
        return clinicianId;
    }

    public void setClinicianId(String clinicianId) {
        this.clinicianId = clinicianId;
    }

    public TreatmentStatus getStatus() {
        return status;
    }

    public void setStatus(TreatmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<TreatmentEntry> getEntries() {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        return entries;
    }

    public void setEntries(List<TreatmentEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }


    public double getTotalCost() {
        return totalCost;
    }

    public boolean isPaid() {
        return paid;
    }

    public void addEntry(TreatmentEntry entry) {
        entries.add(Objects.requireNonNull(entry));
        recalculateTotal();
    }

    public void clearEntries() {
        entries.clear();
        recalculateTotal();
    }


    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public void recalculateTotal() {
        this.totalCost = entries.stream()
                .mapToDouble(TreatmentEntry::getLineCost)
                .sum();
    }

    @Override
    public String toString() {
        return "Treatment{" +
                "id='" + id + '\'' +
                ", patientId='" + patientId + '\'' +
                ", clinicianId='" + clinicianId + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", totalCost=" + totalCost +
                ", paid=" + paid +
                ", entries=" + entries +
                '}';
    }
}