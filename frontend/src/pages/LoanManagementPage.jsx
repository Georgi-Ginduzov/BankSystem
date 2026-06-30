import { useEffect, useMemo, useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'
import { formatCurrency } from '../lib/format'

const initialModifyLoanForm = {
  loanId: '',
  initialAmount: '',
  remainingAmount: '',
  termMonths: '',
  status: 'PENDING',
}

function LoanManagementPage() {
  const [loans, setLoans] = useState([])
  const [loanDetails, setLoanDetails] = useState(null)
  const [selectedLoanId, setSelectedLoanId] = useState('')
  const [portfolioStatusFilter, setPortfolioStatusFilter] = useState('ALL')
  const [modifyLoanForm, setModifyLoanForm] = useState(initialModifyLoanForm)
  const [paymentForms, setPaymentForms] = useState({})
  const [installmentEditForms, setInstallmentEditForms] = useState({})
  const [activeInstallmentEditor, setActiveInstallmentEditor] = useState(null)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isLoading, setIsLoading] = useState(true)
  const [isModifyingLoan, setIsModifyingLoan] = useState(false)
  const [isReviewingId, setIsReviewingId] = useState(null)
  const [isPayingMonth, setIsPayingMonth] = useState(null)
  const [isSavingInstallmentMonth, setIsSavingInstallmentMonth] = useState(null)

  const loadLoans = async () => {
    const data = await requestJson('/loans')
    setLoans(data)
    return data
  }

  const loadSelectedLoanDetails = async (loanRecord) => {
    if (!loanRecord?.id || !loanRecord.clientId) {
      setLoanDetails(null)
      return null
    }

    const data = await requestJson(`/clients/${loanRecord.clientId}/loans/${loanRecord.id}`)
    setLoanDetails(data)
    return data
  }

  const refreshSelectedLoan = async (loanId = selectedLoanId) => {
    const refreshedLoans = await loadLoans()
    const refreshedSelectedLoan =
      refreshedLoans.find((loan) => String(loan.id) === String(loanId)) ?? null

    if (refreshedSelectedLoan) {
      setSelectedLoanId(String(refreshedSelectedLoan.id))
      await loadSelectedLoanDetails(refreshedSelectedLoan)
    } else {
      setLoanDetails(null)
    }

    return refreshedSelectedLoan
  }

  useEffect(() => {
    let isMounted = true

    const run = async () => {
      try {
        const data = await loadLoans()
        if (isMounted && !selectedLoanId && data.length) {
          setSelectedLoanId(String(data[0].id))
        }
      } catch (error) {
        if (isMounted) {
          setStatus({ type: 'error', text: error.message })
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    run()
    return () => {
      isMounted = false
    }
  }, [])

  const pendingLoans = useMemo(
    () => loans.filter((loan) => loan.status === 'PENDING'),
    [loans],
  )

  const visibleLoans = useMemo(() => {
    if (portfolioStatusFilter === 'ALL') {
      return loans
    }

    return loans.filter((loan) => loan.status === portfolioStatusFilter)
  }, [loans, portfolioStatusFilter])

  useEffect(() => {
    if (!visibleLoans.length) {
      setSelectedLoanId('')
      setLoanDetails(null)
      return
    }

    if (!visibleLoans.some((loan) => String(loan.id) === String(selectedLoanId))) {
      setSelectedLoanId(String(visibleLoans[0].id))
    }
  }, [selectedLoanId, visibleLoans])

  const selectedLoan = useMemo(
    () => loans.find((loan) => String(loan.id) === String(selectedLoanId)) ?? null,
    [loans, selectedLoanId],
  )

  const nextPayableMonth = useMemo(
    () =>
      loanDetails?.repaymentPlan?.find((item) => item.status === 'PENDING' || item.status === 'OVERDUE')
        ?.monthNumber ?? null,
    [loanDetails],
  )

  useEffect(() => {
    let isMounted = true

    const loadDetails = async () => {
      if (!selectedLoan?.id || !selectedLoan.clientId) {
        setLoanDetails(null)
        return
      }

      try {
        const data = await requestJson(`/clients/${selectedLoan.clientId}/loans/${selectedLoan.id}`)
        if (isMounted) {
          setLoanDetails(data)
        }
      } catch (error) {
        if (isMounted) {
          setLoanDetails(null)
          setStatus({ type: 'error', text: error.message })
        }
      }
    }

    loadDetails()
    return () => {
      isMounted = false
    }
  }, [selectedLoan?.clientId, selectedLoan?.id])

  useEffect(() => {
    const nextPaymentForms = {}
    const nextInstallmentEditForms = {}

    ;(loanDetails?.repaymentPlan ?? []).forEach((item) => {
      nextPaymentForms[item.monthNumber] = createDefaultPaymentForm(item.expectedPaymentAmount)
      nextInstallmentEditForms[item.monthNumber] = {
        dueDate: formatDateInput(item.dueDate),
        expectedPaymentAmount: String(Number(item.expectedPaymentAmount ?? 0)),
      }
    })

    setPaymentForms(nextPaymentForms)
    setInstallmentEditForms(nextInstallmentEditForms)
    setActiveInstallmentEditor(null)
  }, [loanDetails])

  useEffect(() => {
    if (!selectedLoan) {
      setModifyLoanForm(initialModifyLoanForm)
      return
    }

    setModifyLoanForm({
      loanId: String(selectedLoan.id),
      initialAmount: String(Number(selectedLoan.initialAmount ?? 0)),
      remainingAmount: String(Number(selectedLoan.remainingAmount ?? 0)),
      termMonths: String(Number(selectedLoan.termMonths ?? 0)),
      status: selectedLoan.status ?? 'PENDING',
    })
  }, [selectedLoan])

  const updateModifyLoanField = (event) => {
    const { name, value } = event.target
    setModifyLoanForm((current) => ({ ...current, [name]: value }))
  }

  const updatePaymentForm = (monthNumber, field, value) => {
    setPaymentForms((current) => ({
      ...current,
      [monthNumber]: {
        ...(current[monthNumber] ?? createDefaultPaymentForm(0)),
        [field]: value,
      },
    }))
  }

  const updateInstallmentEditForm = (monthNumber, field, value) => {
    setInstallmentEditForms((current) => ({
      ...current,
      [monthNumber]: {
        ...(current[monthNumber] ?? { dueDate: '', expectedPaymentAmount: '' }),
        [field]: value,
      },
    }))
  }

  const reviewLoan = async (loanId, action) => {
    setStatus({ type: '', text: '' })

    try {
      setIsReviewingId(loanId)
      const data = await requestJson(`/loans/${loanId}/review`, {
        method: 'PATCH',
        body: JSON.stringify({ action }),
      })
      await refreshSelectedLoan(data.id ?? loanId)
      setStatus({
        type: 'success',
        text: `Loan #${loanId} was ${action === 'APPROVE' ? 'approved' : 'rejected'} successfully.`,
      })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsReviewingId(null)
    }
  }

  const modifyLoan = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!modifyLoanForm.loanId.trim()) {
      setStatus({ type: 'error', text: 'Select a loan to modify.' })
      return
    }

    const payload = {
      initialAmount: Number(modifyLoanForm.initialAmount),
      remainingAmount: Number(modifyLoanForm.remainingAmount),
      termMonths: Number(modifyLoanForm.termMonths),
      status: modifyLoanForm.status,
    }

    if (!payload.initialAmount || payload.initialAmount <= 0) {
      setStatus({ type: 'error', text: 'Initial amount must be positive.' })
      return
    }

    if (payload.remainingAmount < 0) {
      setStatus({ type: 'error', text: 'Remaining amount cannot be negative.' })
      return
    }

    if (!payload.termMonths || payload.termMonths <= 0) {
      setStatus({ type: 'error', text: 'Term months must be positive.' })
      return
    }

    try {
      setIsModifyingLoan(true)
      const data = await requestJson(`/loans/${modifyLoanForm.loanId.trim()}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      })
      await refreshSelectedLoan(data.id ?? modifyLoanForm.loanId.trim())
      setStatus({ type: 'success', text: `Loan #${modifyLoanForm.loanId.trim()} was updated.` })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsModifyingLoan(false)
    }
  }

  const markInstallmentPaid = async (repaymentItem, useCustomAmount) => {
    if (!selectedLoan) {
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
        text: 'The recorded payment amount must be at least as high as the scheduled installment.',
      })
      return
    }

    setStatus({ type: '', text: '' })

    try {
      setIsPayingMonth(repaymentItem.monthNumber)
      await requestJson('/loans/payments', {
        method: 'POST',
        body: JSON.stringify({
          loanId: selectedLoan.id,
          clientId: selectedLoan.clientId,
          monthNumber: repaymentItem.monthNumber,
          paymentAmount,
          overpaymentStrategy:
            paymentAmount > Number(repaymentItem.expectedPaymentAmount)
              ? paymentForm.overpaymentStrategy
              : null,
        }),
      })

      await refreshSelectedLoan(selectedLoan.id)
      setStatus({
        type: 'success',
        text:
          paymentAmount > Number(repaymentItem.expectedPaymentAmount)
            ? `Installment ${repaymentItem.monthNumber} was recorded with an overpayment and the remaining schedule was updated.`
            : `Installment ${repaymentItem.monthNumber} for loan #${selectedLoan.id} was marked as paid.`,
      })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsPayingMonth(null)
    }
  }

  const saveInstallmentChanges = async (repaymentItem) => {
    if (!selectedLoan) {
      return
    }

    const form = installmentEditForms[repaymentItem.monthNumber]
    const paymentAmount = Number(form?.expectedPaymentAmount)

    if (!form?.dueDate) {
      setStatus({ type: 'error', text: 'Due date is required for installment updates.' })
      return
    }

    if (!paymentAmount || paymentAmount <= 0) {
      setStatus({ type: 'error', text: 'Expected payment amount must be positive.' })
      return
    }

    setStatus({ type: '', text: '' })

    try {
      setIsSavingInstallmentMonth(repaymentItem.monthNumber)
      await requestJson(`/loans/${selectedLoan.id}/installments/${repaymentItem.monthNumber}`, {
        method: 'PATCH',
        body: JSON.stringify({
          dueDate: form.dueDate,
          expectedPaymentAmount: paymentAmount,
        }),
      })

      await refreshSelectedLoan(selectedLoan.id)
      setStatus({
        type: 'success',
        text: `Installment ${repaymentItem.monthNumber} was updated successfully.`,
      })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsSavingInstallmentMonth(null)
    }
  }

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Loan operations"
        title="Approve, reject, and modify loans"
        text="Employees can review pending customer loan requests, adjust unpaid installments, and record scheduled or higher loan repayments."
      >
        <div className="status-panel" aria-label="Loan operations summary">
          <div>
            <span>Pending reviews</span>
            <strong>{pendingLoans.length}</strong>
          </div>
          <div>
            <span>Total loans</span>
            <strong>{loans.length}</strong>
          </div>
          <div>
            <span>Portfolio filter</span>
            <strong>{portfolioStatusFilter === 'ALL' ? 'All statuses' : portfolioStatusFilter}</strong>
          </div>
        </div>
      </PageIntro>

      <section className="single-workspace" aria-label="Loan management workspace">
        {status.text && <StatusNotice type={status.type} text={status.text} />}

        <div className="dashboard-form-grid">
          <section className="dashboard-form-card">
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Review queue</span>
              <h2>Pending customer requests</h2>
            </div>
            <div className="loan-record-list">
              {pendingLoans.length ? (
                pendingLoans.map((loan) => (
                  <article key={loan.id} className="loan-record-card">
                    <span>{loan.loanTypeName}</span>
                    <strong>Loan #{loan.id}</strong>
                    <p>
                      Client: {loan.clientId} • Amount: {formatCurrency(loan.initialAmount)}
                    </p>
                    <small>
                      Term: {loan.termMonths} months • Status: {loan.status}
                    </small>
                    <div className="loan-record-card__actions">
                      <button
                        type="button"
                        className="button-secondary"
                        onClick={() => setSelectedLoanId(String(loan.id))}
                      >
                        Review details
                      </button>
                      <button
                        type="button"
                        onClick={() => reviewLoan(loan.id, 'APPROVE')}
                        disabled={isReviewingId === loan.id}
                      >
                        {isReviewingId === loan.id ? 'Working...' : 'Approve'}
                      </button>
                      <button
                        type="button"
                        onClick={() => reviewLoan(loan.id, 'REJECT')}
                        disabled={isReviewingId === loan.id}
                      >
                        {isReviewingId === loan.id ? 'Working...' : 'Reject'}
                      </button>
                    </div>
                  </article>
                ))
              ) : (
                <div className="empty-card">There are no pending loan requests right now.</div>
              )}
            </div>
          </section>

          <form className="dashboard-form-card" onSubmit={modifyLoan}>
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Modify</span>
              <h2>Edit an existing loan</h2>
            </div>
            <label>
              Loan
              <select
                name="loanId"
                value={modifyLoanForm.loanId}
                onChange={(event) => setSelectedLoanId(event.target.value)}
              >
                <option value="">Select a loan</option>
                {loans.map((loan) => (
                  <option key={loan.id} value={loan.id}>
                    #{loan.id} • {loan.clientId} • {loan.status}
                  </option>
                ))}
              </select>
            </label>
            {selectedLoan ? (
              <>
                <div className="field-grid">
                  <label>
                    Initial amount (EUR)
                    <input
                      name="initialAmount"
                      type="number"
                      min="1"
                      step="1"
                      value={modifyLoanForm.initialAmount}
                      onChange={updateModifyLoanField}
                    />
                  </label>
                  <label>
                    Remaining amount (EUR)
                    <input
                      name="remainingAmount"
                      type="number"
                      min="0"
                      step="1"
                      value={modifyLoanForm.remainingAmount}
                      onChange={updateModifyLoanField}
                    />
                  </label>
                  <label>
                    Term in months
                    <input
                      name="termMonths"
                      type="number"
                      min="1"
                      value={modifyLoanForm.termMonths}
                      onChange={updateModifyLoanField}
                    />
                  </label>
                  <label>
                    Status
                    <select name="status" value={modifyLoanForm.status} onChange={updateModifyLoanField}>
                      <option value="PENDING">Pending</option>
                      <option value="ACTIVE">Active</option>
                      <option value="REJECTED">Rejected</option>
                      <option value="PAID_OFF">Paid off</option>
                    </select>
                  </label>
                </div>
                <div className="limits-bar">
                  <div>
                    <span>Client</span>
                    <strong>{selectedLoan.clientId}</strong>
                  </div>
                  <div>
                    <span>Monthly payment</span>
                    <strong>{formatCurrency(selectedLoan.monthlyPayment)}</strong>
                  </div>
                  <div>
                    <span>Reviewed by</span>
                    <strong>{selectedLoan.employeeId ?? 'Not reviewed yet'}</strong>
                  </div>
                </div>
              </>
            ) : (
              <div className="empty-card">Select a loan to modify its status or financial terms.</div>
            )}
            <div className="actions">
              <button type="submit" disabled={isModifyingLoan || !selectedLoan}>
                {isModifyingLoan ? 'Saving...' : 'Save changes'}
              </button>
            </div>
          </form>
        </div>

        <section className="dashboard-section dashboard-section--flush" aria-labelledby="all-loans-title">
          <div className="section-heading section-heading--compact">
            <span className="eyebrow">Portfolio</span>
            <h2 id="all-loans-title">All customer loans</h2>
          </div>
          <div className="portfolio-filter-row">
            <label>
              Search by status
              <select
                value={portfolioStatusFilter}
                onChange={(event) => setPortfolioStatusFilter(event.target.value)}
              >
                <option value="ALL">All statuses</option>
                <option value="PENDING">Pending</option>
                <option value="ACTIVE">Active</option>
                <option value="REJECTED">Rejected</option>
                <option value="PAID_OFF">Paid off</option>
              </select>
            </label>
          </div>
          {isLoading ? (
            <div className="empty-card">Loading loans...</div>
          ) : (
            <div className="loan-record-list">
              {visibleLoans.length ? (
                visibleLoans.map((loan) => (
                  <button
                    key={loan.id}
                    type="button"
                    className={
                      String(selectedLoanId) === String(loan.id)
                        ? 'loan-record-card loan-record-card--selected'
                        : 'loan-record-card'
                    }
                    onClick={() => setSelectedLoanId(String(loan.id))}
                  >
                    <span>{loan.loanTypeName}</span>
                    <strong>Loan #{loan.id}</strong>
                    <p>
                      Client: {loan.clientId} • Remaining: {formatCurrency(loan.remainingAmount)}
                    </p>
                    <small>
                      {loan.status} • {loan.termMonths} months • Monthly payment:{' '}
                      {formatCurrency(loan.monthlyPayment)}
                    </small>
                  </button>
                ))
              ) : (
                <div className="empty-card">No loans matched the selected status.</div>
              )}
            </div>
          )}
        </section>

        <section className="dashboard-section dashboard-section--flush" aria-labelledby="repayment-management-title">
          <div className="section-heading section-heading--compact">
            <span className="eyebrow">Repayments</span>
            <h2 id="repayment-management-title">Manage installments and paid amounts</h2>
          </div>
          {!selectedLoan ? (
            <div className="empty-card">Select a loan to view its repayment schedule.</div>
          ) : selectedLoan.status === 'REJECTED' ? (
            <div className="empty-card">Rejected loans do not have a repayment plan.</div>
          ) : selectedLoan.status !== 'ACTIVE' && selectedLoan.status !== 'PAID_OFF' ? (
            <div className="empty-card">Repayment tracking becomes available after the loan is approved.</div>
          ) : !loanDetails ? (
            <div className="empty-card">Loading repayment plan...</div>
          ) : loanDetails.repaymentPlan?.length ? (
            <div className="repayment-plan">
              {loanDetails.repaymentPlan.map((item) => {
                const paymentForm =
                  paymentForms[item.monthNumber] ?? createDefaultPaymentForm(item.expectedPaymentAmount)
                const editForm = installmentEditForms[item.monthNumber] ?? {
                  dueDate: '',
                  expectedPaymentAmount: '',
                }
                const isNextPayable = nextPayableMonth === item.monthNumber
                const isEditing = activeInstallmentEditor === item.monthNumber
                const isEditableInstallment = !isPastInstallment(item.dueDate)

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

                    {(item.status === 'PENDING' || item.status === 'OVERDUE') && (
                      <>
                        {isEditableInstallment ? (
                          <div className="loan-record-card__actions">
                            <button
                              type="button"
                              className="button-secondary"
                              onClick={() =>
                                setActiveInstallmentEditor((current) =>
                                  current === item.monthNumber ? null : item.monthNumber,
                                )
                              }
                            >
                              {isEditing ? 'Close installment editor' : 'Edit installment'}
                            </button>
                          </div>
                        ) : (
                          <small className="repayment-plan__hint">
                            Previous installments are locked and cannot be modified.
                          </small>
                        )}

                        {isEditing && isEditableInstallment && (
                          <div className="repayment-plan__inline-form repayment-plan__inline-form--stacked">
                            <label>
                              Due date
                              <input
                                type="date"
                                value={editForm.dueDate}
                                onChange={(event) =>
                                  updateInstallmentEditForm(
                                    item.monthNumber,
                                    'dueDate',
                                    event.target.value,
                                  )
                                }
                              />
                            </label>
                            <label>
                              Expected payment amount (EUR)
                              <input
                                type="number"
                                min="0.01"
                                step="0.01"
                                value={editForm.expectedPaymentAmount}
                                onChange={(event) =>
                                  updateInstallmentEditForm(
                                    item.monthNumber,
                                    'expectedPaymentAmount',
                                    event.target.value,
                                  )
                                }
                              />
                            </label>
                            <button
                              type="button"
                              onClick={() => saveInstallmentChanges(item)}
                              disabled={isSavingInstallmentMonth === item.monthNumber}
                            >
                              {isSavingInstallmentMonth === item.monthNumber
                                ? 'Saving...'
                                : 'Save installment changes'}
                            </button>
                            <small className="repayment-plan__hint">
                              Future unpaid installments will be recalculated automatically to keep
                              the plan consistent.
                            </small>
                          </div>
                        )}

                        {isNextPayable ? (
                          <div className="repayment-plan__actions">
                            <button
                              type="button"
                              onClick={() => markInstallmentPaid(item, false)}
                              disabled={isPayingMonth === item.monthNumber}
                            >
                              {isPayingMonth === item.monthNumber
                                ? 'Recording...'
                                : 'Mark scheduled payment'}
                            </button>
                            <div className="repayment-plan__inline-form">
                              <label>
                                Record a higher payment (EUR)
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
                                Use extra amount to
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
                                  <option value="REDUCE_INSTALLMENT">
                                    Reduce next installment cost
                                  </option>
                                </select>
                              </label>
                              <button
                                type="button"
                                className="button-secondary"
                                onClick={() => markInstallmentPaid(item, true)}
                                disabled={isPayingMonth === item.monthNumber}
                              >
                                {isPayingMonth === item.monthNumber
                                  ? 'Updating...'
                                  : 'Record higher payment'}
                              </button>
                            </div>
                          </div>
                        ) : (
                          <small className="repayment-plan__hint">
                            Earlier installments must be settled before this one can be marked as
                            paid.
                          </small>
                        )}
                      </>
                    )}
                  </article>
                )
              })}
            </div>
          ) : (
            <div className="empty-card">No repayment plan is available for this loan yet.</div>
          )}
        </section>
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

function formatDateInput(value) {
  if (!value) {
    return ''
  }

  return String(value).slice(0, 10)
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

function isPastInstallment(value) {
  if (!value) {
    return false
  }

  const dueDate = new Date(value)
  const today = new Date()
  dueDate.setHours(0, 0, 0, 0)
  today.setHours(0, 0, 0, 0)
  return dueDate < today
}

function formatStatusLabel(status) {
  return String(status ?? 'N/A')
    .toLowerCase()
    .replace(/_/g, ' ')
}

export default LoanManagementPage
