package com.example.petlife.dto.pet;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PetCareRecordForm {

    private String careType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate administeredOn;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate nextDueOn;

    private String memo;

    public String getCareType() {
        return careType;
    }

    public void setCareType(String careType) {
        this.careType = careType;
    }

    public LocalDate getAdministeredOn() {
        return administeredOn;
    }

    public void setAdministeredOn(LocalDate administeredOn) {
        this.administeredOn = administeredOn;
    }

    public LocalDate getNextDueOn() {
        return nextDueOn;
    }

    public void setNextDueOn(LocalDate nextDueOn) {
        this.nextDueOn = nextDueOn;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
