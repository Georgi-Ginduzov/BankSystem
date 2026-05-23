package com.banksystem.repository;

import com.banksystem.model.LoanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanTypeRepository extends JpaRepository<LoanType, Integer>
{
    Optional<LoanType> findByName(String name);
}
