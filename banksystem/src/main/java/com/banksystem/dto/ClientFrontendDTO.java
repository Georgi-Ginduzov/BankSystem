package com.banksystem.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ClientFrontendDTO {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String segment; // RETAIL, BUSINESS, PREMIUM
}