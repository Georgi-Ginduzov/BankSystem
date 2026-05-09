package com.banksystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "merchant")
public class Merchant extends Client {

    @Id
    @Column(name = "eik_client_id", length = 13)
    private String id;

    @NotBlank(message = "Company name is required")
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @NotBlank(message = "Representative first name is required")
    @Column(name = "representative_first_name", nullable = false)
    private String representativeFirstName;

    @NotBlank(message = "Representative last name is required")
    @Column(name = "representative_last_name", nullable = false)
    private String representativeLastName;

    public Merchant() {}

    public Merchant(String eik, String companyName, String representativeFirstName, String representativeLastName) {
        super(eik);
        this.id = eik;
        this.companyName = companyName;
        this.representativeFirstName = representativeFirstName;
        this.representativeLastName = representativeLastName;
    }

    @Override
    public String getId() { return id; }
    @Override
    public void setId(String id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRepresentativeFirstName() { return representativeFirstName; }
    public void setRepresentativeFirstName(String representativeFirstName) { this.representativeFirstName = representativeFirstName; }

    public String getRepresentativeLastName() { return representativeLastName; }
    public void setRepresentativeLastName(String representativeLastName) { this.representativeLastName = representativeLastName; }
}