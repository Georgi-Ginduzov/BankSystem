package com.banksystem.repository;

import com.banksystem.model.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Integer> {
    List<Repayment> findByLoanIdOrderByMonthNumberAsc(Integer loanId);
    Optional<Repayment> findByLoanIdAndMonthNumber(Integer loanId, Integer monthNumber);
    long countByLoanIdAndStatus(Integer loanId, Repayment.RepaymentStatus status);
}
