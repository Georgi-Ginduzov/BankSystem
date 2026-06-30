package com.banksystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanReviewRequestDTO {
    @NotBlank(message = "Action is required")
    private String action;
}
