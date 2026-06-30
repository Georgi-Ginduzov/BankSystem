package com.banksystem.repository;

import com.banksystem.model.Loan;
import com.banksystem.model.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Integer> {
    List<Repayment> findByLoanOrderByMonthNumberAsc(Loan loan);

    Optional<Repayment> findByLoanAndMonthNumber(Loan loan, Integer monthNumber);

    long countByLoanAndStatus(Loan loan, Repayment.RepaymentStatus status);

    void deleteByLoan(Loan loan);
}
