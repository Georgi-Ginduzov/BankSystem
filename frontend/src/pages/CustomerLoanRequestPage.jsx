import { useEffect, useMemo, useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { loanTypes } from '../data/loanTypes'
import { requestJson } from '../lib/api'
import { formatCurrency } from '../lib/format'

const initialLoanRequestForm = {
  phone: '',
  repaymentAccountId: '',
  loanType: loanTypes[0].value,
  amount: 10000,
  periodMonths: 24,
  monthlyIncome: 2500,
  purpose: '',
}

function CustomerLoanRequestPage({ onNavigate, session }) {
  const [form, setForm] = useState(initialLoanRequestForm)
  const [accounts, setAccounts] = useState([])
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [application, setApplication] = useState(null)

  const selectedType = useMemo(
    () => loanTypes.find((type) => type.value === form.loanType) ?? loanTypes[0],
    [form.loanType],
  )

  useEffect(() => {
    let isMounted = true

    const run = async () => {
      if (!session.ucn?.trim()) {
        return
      }

      try {
        const data = await requestJson(`/clients/${session.ucn}/accounts`)
        const eligibleAccounts = data.filter(
          (account) => account.status === 'ACTIVE' && account.type !== 'LOAN',
        )

        if (isMounted) {
          setAccounts(eligibleAccounts)
          setForm((current) => ({
            ...current,
            repaymentAccountId:
              current.repaymentAccountId || String(eligibleAccounts[0]?.id ?? ''),
          }))
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
  }, [session.ucn])

  const validationError = useMemo(() => {
    const amount = Number(form.amount)
    const months = Number(form.periodMonths)
    const income = Number(form.monthlyIncome)

    if (!session.ucn?.trim()) return 'Customer account is missing a client identifier.'
    if (!form.repaymentAccountId) return 'Select the active account that will be used for repayments.'
    if (!form.phone.trim()) return 'Phone number is required.'
    if (amount < selectedType.minAmount || amount > selectedType.maxAmount) {
      return `The amount for ${selectedType.label.toLowerCase()} must be between ${formatCurrency(
        selectedType.minAmount,
      )} and ${formatCurrency(selectedType.maxAmount)}.`
    }
    if (months < selectedType.minMonths || months > selectedType.maxMonths) {
      return `The term must be between ${selectedType.minMonths} and ${selectedType.maxMonths} months.`
    }
    if (!income || income <= 0) return 'Enter a valid monthly income.'
    if (!form.purpose.trim()) return 'Describe the purpose of the loan request.'
    return ''
  }, [form, selectedType, session.ucn])

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => {
      if (name !== 'loanType') {
        return { ...current, [name]: value }
      }

      const nextType = loanTypes.find((type) => type.value === value) ?? loanTypes[0]
      return {
        ...current,
        loanType: value,
        amount: Math.min(Math.max(Number(current.amount), nextType.minAmount), nextType.maxAmount),
        periodMonths: Math.min(
          Math.max(Number(current.periodMonths), nextType.minMonths),
          nextType.maxMonths,
        ),
      }
    })
  }

  const submitApplication = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (validationError) {
      setStatus({ type: 'error', text: validationError })
      return
    }

    try {
      setIsSubmitting(true)
      const payload = {
        clientId: session.ucn,
        phone: form.phone.trim(),
        loanType: form.loanType,
        amount: Number(form.amount),
        repaymentAccountId: Number(form.repaymentAccountId),
        periodMonths: Number(form.periodMonths),
        monthlyIncome: Number(form.monthlyIncome),
        purpose: form.purpose.trim(),
      }

      const data = await requestJson('/loans/apply', {
        method: 'POST',
        body: JSON.stringify(payload),
      })

      setApplication({
        id: data.id ?? data.applicationId ?? '',
        status: data.status ?? 'PENDING',
        amount: data.amount ?? payload.amount,
        loanType: data.loanType ?? payload.loanType,
        periodMonths: data.periodMonths ?? payload.periodMonths,
      })
      setForm({
        ...initialLoanRequestForm,
        repaymentAccountId: String(accounts[0]?.id ?? ''),
      })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsSubmitting(false)
    }
  }

  const typeLabel =
    loanTypes.find((type) => type.value === application?.loanType)?.label ?? application?.loanType

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Customer loan request"
        title="Request a new loan"
        text="Submit a new loan request from your customer workspace. After submission, the bank team can review it and continue the approval flow."
      >
        <div className="status-panel" aria-label="Loan request limits">
          <div>
            <span>Loan type</span>
            <strong>{selectedType.label}</strong>
          </div>
          <div>
            <span>Amount range</span>
            <strong>
              {formatCurrency(selectedType.minAmount)} - {formatCurrency(selectedType.maxAmount)}
            </strong>
          </div>
          <div>
            <span>Term range</span>
            <strong>
              {selectedType.minMonths} - {selectedType.maxMonths} months
            </strong>
          </div>
        </div>
      </PageIntro>

      <section className="single-workspace" aria-label="Customer loan request form">
        <div className="form-shell">
          {application ? (
            <div className="accepted-view" role="status" aria-live="polite">
              <span className="accepted-mark" aria-hidden="true">
                ✓
              </span>
              <span className="eyebrow">Request submitted</span>
              <h2>Your loan request has been recorded.</h2>
              <p>
                The bank team will review the request and update the status in your loan history.
              </p>
              <dl className="application-summary">
                <div>
                  <dt>Request</dt>
                  <dd>#{application.id || 'Pending'}</dd>
                </div>
                <div>
                  <dt>Status</dt>
                  <dd>{formatStatusLabel(application.status)}</dd>
                </div>
                <div>
                  <dt>Type</dt>
                  <dd>{typeLabel}</dd>
                </div>
                <div>
                  <dt>Amount</dt>
                  <dd>{formatCurrency(application.amount)}</dd>
                </div>
                <div>
                  <dt>Term</dt>
                  <dd>{application.periodMonths} months</dd>
                </div>
              </dl>
              <div className="actions actions--split">
                <button type="button" className="button-secondary" onClick={() => setApplication(null)}>
                  Submit another request
                </button>
                <button type="button" onClick={() => onNavigate('loans')}>
                  Back to loans
                </button>
              </div>
            </div>
          ) : (
            <form className="loan-form" onSubmit={submitApplication}>
              <div className="section-heading">
                <span className="eyebrow">Loan form</span>
                <h2>Application details</h2>
              </div>

              <div className="field-grid">
                <label>
                  Repayment account
                  <select name="repaymentAccountId" value={form.repaymentAccountId} onChange={updateField}>
                    <option value="">Select an active account</option>
                    {accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {account.type} • {account.iban} • {formatCurrency(account.balance)}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Phone
                  <input
                    name="phone"
                    value={form.phone}
                    onChange={updateField}
                    placeholder="+359 88 000 0000"
                    autoComplete="tel"
                  />
                </label>
                <label>
                  Loan type
                  <select name="loanType" value={form.loanType} onChange={updateField}>
                    {loanTypes.map((type) => (
                      <option key={type.value} value={type.value}>
                        {type.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Amount (EUR)
                  <input
                    name="amount"
                    type="number"
                    min={selectedType.minAmount}
                    max={selectedType.maxAmount}
                    step="100"
                    value={form.amount}
                    onChange={updateField}
                  />
                </label>
                <label>
                  Term in months
                  <input
                    name="periodMonths"
                    type="number"
                    min={selectedType.minMonths}
                    max={selectedType.maxMonths}
                    value={form.periodMonths}
                    onChange={updateField}
                  />
                </label>
                <label>
                  Monthly income (EUR)
                  <input
                    name="monthlyIncome"
                    type="number"
                    min="0"
                    step="1"
                    value={form.monthlyIncome}
                    onChange={updateField}
                  />
                </label>
                <label className="field-grid__wide">
                  Purpose
                  <textarea
                    name="purpose"
                    value={form.purpose}
                    onChange={updateField}
                    rows="4"
                    placeholder="Briefly describe what the financing will be used for"
                  />
                </label>
              </div>

              <div className="limits-bar">
                <div>
                  <span>Amount in EUR</span>
                  <strong>
                    {formatCurrency(selectedType.minAmount)} - {formatCurrency(selectedType.maxAmount)}
                  </strong>
                </div>
                <div>
                  <span>Repayment account</span>
                  <strong>{accounts.length ? `${accounts.length} active account(s)` : 'No active account'}</strong>
                </div>
                <div>
                  <span>Term</span>
                  <strong>
                    {selectedType.minMonths} - {selectedType.maxMonths} months
                  </strong>
                </div>
                <div>
                  <span>Indicative rate</span>
                  <strong>{selectedType.rate}</strong>
                </div>
              </div>

              {status.text && <StatusNotice type={status.type} text={status.text} />}

              <div className="actions actions--split">
                <button type="button" className="button-secondary" onClick={() => onNavigate('loans')}>
                  Back
                </button>
                <button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? 'Submitting...' : 'Send request'}
                </button>
              </div>
            </form>
          )}
        </div>
      </section>
    </main>
  )
}

function formatStatusLabel(status) {
  return String(status ?? 'N/A')
    .toLowerCase()
    .replace(/_/g, ' ')
}

export default CustomerLoanRequestPage
