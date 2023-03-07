package com.r3.csde.dtos;

public class RegistrationRequestProgressDTO {

    private String registrationStatus;
    private String reason;


    public RegistrationRequestProgressDTO() {
    }


    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
