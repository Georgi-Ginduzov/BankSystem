package com.banksystem.repository;

import com.banksystem.model.Loan;
import com.banksystem.model.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Integer> {
    List<Loan> findByClientId(String clientId);
    List<Loan> findByClientIdAndStatus(String clientId, Loan.LoanStatus status);
    Optional<Loan> findByIdAndClientId(Integer id, String clientId);
}

