package com.banksystem.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ClientRequestDTO {

    @NotBlank(message = "Client type is required")
    private String clientType; // "CUSTOMER" or "MERCHANT"

    // For Customer (Individual)
    @Pattern(regexp = "^[0-9]{10}$", message = "UCN must be exactly 10 digits")
    private String ucn;

    private String firstName;
    private String lastName;

    // For Merchant (Corporate)
    @Pattern(regexp = "^[0-9]{9,13}$", message = "EIK must be between 9 and 13 digits")
    private String eik;

    private String companyName;
    private String representativeFirstName;
    private String representativeLastName;

    // Getters and Setters
    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }

    public String getUcn() { return ucn; }
    public void setUcn(String ucn) { this.ucn = ucn; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEik() { return eik; }
    public void setEik(String eik) { this.eik = eik; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRepresentativeFirstName() { return representativeFirstName; }
    public void setRepresentativeFirstName(String representativeFirstName) { this.representativeFirstName = representativeFirstName; }

    public String getRepresentativeLastName() { return representativeLastName; }
    public void setRepresentativeLastName(String representativeLastName) { this.representativeLastName = representativeLastName; }
}