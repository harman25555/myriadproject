package model;

import java.time.LocalDate;

public class Promotion {

    private String id;
    private String title;
    private String message;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;

    public Promotion(String id,
                     String title,
                     String message,
                     LocalDate startDate,
                     LocalDate endDate,
                     boolean active) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    public Promotion() {

    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setId(String id) {
        this.id = id;
    }
}
