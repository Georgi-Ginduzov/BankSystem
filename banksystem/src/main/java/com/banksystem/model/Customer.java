package com.banksystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "customer")
public class Customer extends Client {

    @Id
    @Column(name = "ucn_client_id", length = 10)
    private String id;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    public Customer() {}

    public Customer(String ucn, String firstName, String lastName) {
        super(ucn);
        this.id = ucn;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String getId() { return id; }
    @Override
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}