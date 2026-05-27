package com.banksystem.repository;

import com.banksystem.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Integer> {
    List<Loan> findByClient_Id(String clientId);
    List<Loan> findByClientId(String clientId, Loan.LoanStatus status);
    Optional<Loan> findByIdAndClientId(Integer id, String clientId);
}

