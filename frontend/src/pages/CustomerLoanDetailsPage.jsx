import { useEffect, useMemo, useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'
import { formatCurrency } from '../lib/format'

function CustomerLoanDetailsPage({ loanId, onNavigate, session }) {
  const [loan, setLoan] = useState(null)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isPayingMonth, setIsPayingMonth] = useState(null)
  const [paymentForms, setPaymentForms] = useState({})

  useEffect(() => {
    let isMounted = true

    const run = async () => {
      try {
        const data = await loadLoanDetails(session.ucn, loanId)
        if (isMounted) {
          setLoan(data)
        }
      } catch (error) {
        if (isMounted) {
          setStatus({ type: 'error', text: error.message })
        }
      }
    }

    run()

    return () => {
      isMounted = false
    }
  }, [loanId, session.ucn])

  useEffect(() => {
    const nextForms = {}
    ;(loan?.repaymentPlan ?? []).forEach((item) => {
      nextForms[item.monthNumber] = createDefaultPaymentForm(item.expectedPaymentAmount)
    })
    setPaymentForms(nextForms)
  }, [loan])

  const repaymentStats = useMemo(() => {
    const plan = loan?.repaymentPlan ?? []
    return {
      total: plan.length,
      paid: plan.filter((item) => item.status === 'PAID').length,
      overdue: plan.filter((item) => item.status === 'OVERDUE').length,
    }
  }, [loan])

  const nextPayableMonth = useMemo(
    () =>
      loan?.repaymentPlan?.find((item) => item.status === 'PENDING' || item.status === 'OVERDUE')
        ?.monthNumber ?? null,
    [loan],
  )

  const updatePaymentForm = (monthNumber, field, value) => {
    setPaymentForms((current) => ({
      ...current,
      [monthNumber]: {
        ...(current[monthNumber] ?? createDefaultPaymentForm(0)),
        [field]: value,
      },
    }))
  }

  const markInstallmentPaid = async (repaymentItem, useCustomAmount) => {
    if (!loan) {
      return
    }

    const paymentForm = paymentForms[repaymentItem.monthNumber] ?? createDefaultPaymentForm(
      repaymentItem.expectedPaymentAmount,
    )
    const paymentAmount = useCustomAmount
      ? Number(paymentForm.customAmount)
      : Number(repaymentItem.expectedPaymentAmount)

    if (!paymentAmount || paymentAmount < Number(repaymentItem.expectedPaymentAmount)) {
      setStatus({
        type: 'error',
        text: 'The custom payment amount must be at least as high as the scheduled installment.',
      })
      return
    }

    setStatus({ type: '', text: '' })

    try {
      setIsPayingMonth(repaymentItem.monthNumber)
      await requestJson('/loans/payments', {
        method: 'POST',
        body: JSON.stringify({
          loanId: loan.id,
          clientId: session.ucn,
          monthNumber: repaymentItem.monthNumber,
          paymentAmount,
          overpaymentStrategy:
            paymentAmount > Number(repaymentItem.expectedPaymentAmount)
              ? paymentForm.overpaymentStrategy
              : null,
        }),
      })

      const refreshedLoan = await loadLoanDetails(session.ucn, loan.id)
      setLoan(refreshedLoan)
      setStatus({
        type: 'success',
        text:
          paymentAmount > Number(repaymentItem.expectedPaymentAmount)
            ? `Installment ${repaymentItem.monthNumber} was paid with an extra amount and the schedule was updated.`
            : `Installment ${repaymentItem.monthNumber} was marked as paid successfully.`,
      })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsPayingMonth(null)
    }
  }

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Loan details"
        title={loan ? `${loan.loanTypeName} loan #${loan.id}` : 'Loan details'}
        text="Review the current status of the loan and the repayment plan with paid and pending installments."
      >
        <div className="status-panel" aria-label="Loan summary">
          <div>
            <span>Status</span>
            <strong>{formatStatusLabel(loan?.status)}</strong>
          </div>
          <div>
            <span>Paid installments</span>
            <strong>
              {loan?.paidInstallments ?? 0} / {loan?.termMonths ?? 0}
            </strong>
          </div>
          <div>
            <span>Remaining amount</span>
            <strong>{formatCurrency(loan?.remainingAmount ?? 0)}</strong>
          </div>
        </div>
      </PageIntro>

      <section className="single-workspace" aria-label="Customer loan details">
        {status.text && <StatusNotice type={status.type} text={status.text} />}

        {loan && (
          <>
            <div className="loan-detail-summary">
              <article className="data-card">
                <span>Initial amount</span>
                <strong>{formatCurrency(loan.initialAmount)}</strong>
                <p>Start date: {formatDate(loan.startDate)}</p>
              </article>
              <article className="data-card">
                <span>Monthly payment</span>
                <strong>{formatCurrency(loan.monthlyPayment)}</strong>
                <p>Loan term: {loan.termMonths} months</p>
              </article>
              <article className="data-card">
                <span>Repayment status</span>
                <strong>{formatStatusLabel(loan.status)}</strong>
                <p>
                  Paid: {repaymentStats.paid} • Overdue: {repaymentStats.overdue} • Total:{' '}
                  {repaymentStats.total}
                </p>
              </article>
              <article className="data-card">
                <span>Repayment account</span>
                <strong>{loan.settlementAccountIban ?? 'Not linked yet'}</strong>
                <p>
                  {loan.settlementAccountId
                    ? `Installments are paid from account #${loan.settlementAccountId}.`
                    : 'Ask a bank employee to link an active account before paying installments.'}
                </p>
              </article>
            </div>

            <div className="dashboard-section dashboard-section--flush">
              <div className="section-heading section-heading--compact">
                <span className="eyebrow">Repayment plan</span>
                <h2>Installment schedule</h2>
              </div>
              <div className="repayment-plan">
                {loan.status === 'REJECTED' ? (
                  <div className="empty-card">
                    This request was rejected, so no repayment plan was created.
                  </div>
                ) : loan.repaymentPlan?.length ? (
                  loan.repaymentPlan.map((item) => {
                    const paymentForm =
                      paymentForms[item.monthNumber] ?? createDefaultPaymentForm(item.expectedPaymentAmount)
                    const isNextPayable = nextPayableMonth === item.monthNumber

                    return (
                      <article
                        key={item.monthNumber}
                        className={`repayment-plan__item repayment-plan__item--${item.status}`}
                      >
                        <div className="repayment-plan__header">
                          <strong>Installment {item.monthNumber}</strong>
                          <span>{formatStatusLabel(item.status)}</span>
                        </div>
                        <p>Due date: {formatDate(item.dueDate)}</p>
                        <p>Expected payment: {formatCurrency(item.expectedPaymentAmount)}</p>
                        <p>Principal: {formatCurrency(item.expectedPrincipalAmount)}</p>
                        <p>Interest: {formatCurrency(item.expectedInterestAmount)}</p>
                        <p>Remaining after payment: {formatCurrency(item.expectedRemainingToPay)}</p>
                        <small>
                          {item.paymentDate
                            ? `Paid on ${formatDateTime(item.paymentDate)} for ${formatCurrency(
                                item.actualPaymentAmount ?? 0,
                              )}`
                            : 'No payment has been recorded yet.'}
                        </small>
                        {loan.status === 'ACTIVE' &&
                          (item.status === 'PENDING' || item.status === 'OVERDUE') &&
                          (isNextPayable ? (
                            <div className="repayment-plan__actions">
                              <button
                                type="button"
                                onClick={() => markInstallmentPaid(item, false)}
                                disabled={isPayingMonth === item.monthNumber}
                              >
                                {isPayingMonth === item.monthNumber ? 'Paying...' : 'Mark scheduled payment'}
                              </button>
                              <div className="repayment-plan__inline-form">
                                <label>
                                  Pay a higher amount
                                  <input
                                    type="number"
                                    min={Number(item.expectedPaymentAmount)}
                                    step="0.01"
                                    value={paymentForm.customAmount}
                                    onChange={(event) =>
                                      updatePaymentForm(item.monthNumber, 'customAmount', event.target.value)
                                    }
                                  />
                                </label>
                                <label>
                                  Apply extra amount to
                                  <select
                                    value={paymentForm.overpaymentStrategy}
                                    onChange={(event) =>
                                      updatePaymentForm(
                                        item.monthNumber,
                                        'overpaymentStrategy',
                                        event.target.value,
                                      )
                                    }
                                  >
                                    <option value="REDUCE_TERM">Reduce next installment count</option>
                                    <option value="REDUCE_INSTALLMENT">Reduce next installment cost</option>
                                  </select>
                                </label>
                                <button
                                  type="button"
                                  className="button-secondary"
                                  onClick={() => markInstallmentPaid(item, true)}
                                  disabled={isPayingMonth === item.monthNumber}
                                >
                                  {isPayingMonth === item.monthNumber ? 'Updating...' : 'Pay higher amount'}
                                </button>
                              </div>
                            </div>
                          ) : (
                            <small className="repayment-plan__hint">
                              Earlier installments must be paid first before this one can be recorded.
                            </small>
                          ))}
                      </article>
                    )
                  })
                ) : (
                  <div className="empty-card">No repayment plan is available for this loan yet.</div>
                )}
              </div>
            </div>
          </>
        )}

        <div className="actions">
          <button type="button" className="button-secondary" onClick={() => onNavigate('loans')}>
            Back to loans
          </button>
        </div>
      </section>
    </main>
  )
}

function createDefaultPaymentForm(expectedAmount) {
  return {
    customAmount: String(Number(expectedAmount ?? 0)),
    overpaymentStrategy: 'REDUCE_TERM',
  }
}

function formatDate(value) {
  if (!value) {
    return 'N/A'
  }

  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value))
}

function formatDateTime(value) {
  if (!value) {
    return 'N/A'
  }

  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatStatusLabel(status) {
  return String(status ?? 'N/A')
    .toLowerCase()
    .replace(/_/g, ' ')
}

async function loadLoanDetails(clientId, loanId) {
  if (!clientId) {
    throw new Error('Customer account is missing a client identifier.')
  }

  if (!loanId) {
    throw new Error('Loan identifier is missing.')
  }

  return requestJson(`/clients/${clientId}/loans/${loanId}`)
}

export default CustomerLoanDetailsPage
