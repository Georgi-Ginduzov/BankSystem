import { useEffect, useMemo, useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'
import { formatCurrency } from '../lib/format'

function CustomerLoansPage({ onNavigate, session }) {
  const [allAccounts, setAllAccounts] = useState([])
  const [loans, setLoans] = useState([])
  const [status, setStatus] = useState({ type: '', text: '' })

  useEffect(() => {
    let isMounted = true

    const run = async () => {
      if (!session.ucn) {
        setStatus({ type: 'error', text: 'Customer account is missing a client identifier.' })
        return
      }

      try {
        const [accountData, loanData] = await Promise.all([
          requestJson(`/clients/${session.ucn}/accounts`),
          requestJson(`/clients/${session.ucn}/loans`),
        ])

        if (isMounted) {
          setAllAccounts(accountData)
          setLoans(loanData)
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

  const accounts = useMemo(
    () => allAccounts.filter((account) => account.type !== 'LOAN'),
    [allAccounts],
  )

  const activeLoanBalance = useMemo(
    () =>
      loans
        .filter((loan) => loan.status === 'ACTIVE')
        .reduce((sum, loan) => sum + Number(loan.remainingAmount ?? 0), 0),
    [loans],
  )

  const paidLoanCount = useMemo(
    () => loans.filter((loan) => loan.status === 'PAID_OFF').length,
    [loans],
  )

  const hasAccounts = accounts.length > 0

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Loans"
        title="Review your loans and request a new one"
        text="See every credit in one place, open the detailed repayment plan, and start a new loan request when needed."
      >
        <div className="status-panel" aria-label="Loan summary">
          <div>
            <span>Total loans</span>
            <strong>{loans.length}</strong>
          </div>
          <div>
            <span>Outstanding balance</span>
            <strong>{formatCurrency(activeLoanBalance)}</strong>
          </div>
          <div>
            <span>Paid off</span>
            <strong>{paidLoanCount}</strong>
          </div>
        </div>
      </PageIntro>

      <section className="single-workspace" aria-label="Customer loans workspace">
        {status.text && <StatusNotice type={status.type} text={status.text} />}

        {!hasAccounts && (
          <div className="dashboard-form-card dashboard-form-card--highlight">
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Account first</span>
              <h2>Open an account to get started.</h2>
            </div>
            <p className="dashboard-form-card__text">
              You do not have any account yet. Create one from the Accounts page so your dashboard
              can start tracking balances and activity.
            </p>
            <div className="actions">
              <button type="button" onClick={() => onNavigate('accounts')}>
                Go to accounts
              </button>
            </div>
          </div>
        )}

        <div className="dashboard-form-card">
          <div className="section-heading section-heading--compact">
            <span className="eyebrow">New request</span>
            <h2>Apply for financing</h2>
          </div>
          <p className="dashboard-form-card__text">
            Open the dedicated request flow to enter the amount, term, income, and purpose of the
            loan.
          </p>
          <div className="actions">
            <button type="button" onClick={() => onNavigate('customer-loan-request')}>
              Request a loan
            </button>
          </div>
        </div>

        <div className="dashboard-section dashboard-section--flush" aria-labelledby="customer-loans-title">
          <div className="section-heading section-heading--compact">
            <span className="eyebrow">Loan history</span>
            <h2 id="customer-loans-title">Your credits</h2>
          </div>
          <div className="customer-loan-list">
            {loans.length ? (
              loans.map((loan) => (
                <button
                  key={loan.id}
                  type="button"
                  className="customer-loan-item"
                  onClick={() => onNavigate('customer-loan-details', { loanId: loan.id })}
                >
                  <span>{loan.loanTypeName}</span>
                  <strong>{formatCurrency(loan.initialAmount)}</strong>
                  <p>
                    Remaining: {formatCurrency(loan.remainingAmount)} • Monthly payment:{' '}
                    {formatCurrency(loan.monthlyPayment)}
                  </p>
                  <small>
                    {loan.status} • {loan.termMonths} months • Started {formatDate(loan.startDate)}
                  </small>
                </button>
              ))
            ) : (
              <div className="empty-card">No loan history is available for this customer profile yet.</div>
            )}
          </div>
        </div>
      </section>
    </main>
  )
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

export default CustomerLoansPage
