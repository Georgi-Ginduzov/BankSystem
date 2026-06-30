import { useEffect, useMemo, useState } from 'react'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'
import { formatCurrency } from '../lib/format'

const employeeActions = [
  {
    page: 'account-management',
    title: 'Account balances',
    detail: 'Search accounts and update account balances from the employee workspace.',
  },
  {
    page: 'loan-management',
    title: 'Loan reviews',
    detail: 'Approve, reject, and modify customer loans from one workspace.',
    roles: ['LOAN_OFFICER', 'MANAGER'],
  },
  {
    page: 'client-profiles',
    title: 'Client profiles',
    detail: 'Load, create, and maintain customer records from one place.',
  },
  {
    page: 'new-account',
    title: 'New account',
    detail: 'Open checking, savings, or business accounts for clients.',
  },
]

function HomePage({ onNavigate, session }) {
  if (session.role === 'CUSTOMER') {
    return <CustomerDashboard onNavigate={onNavigate} session={session} />
  }

  return <EmployeeDashboard onNavigate={onNavigate} session={session} />
}

function EmployeeDashboard({ onNavigate, session }) {
  const [overview, setOverview] = useState(null)
  const [status, setStatus] = useState({ type: '', text: '' })

  useEffect(() => {
    let isMounted = true

    const loadOverview = async () => {
      try {
        const data = await requestJson('/analytics/overview')
        if (isMounted) {
          setOverview(data)
        }
      } catch (error) {
        if (isMounted) {
          setStatus({ type: 'error', text: error.message })
        }
      }
    }

    loadOverview()
    return () => {
      isMounted = false
    }
  }, [])

  const metricCards = overview
    ? [
        { label: 'Active accounts', value: overview.activeAccounts },
        { label: 'Pending loans', value: overview.pendingLoans },
        { label: 'Active loans', value: overview.activeLoans },
        { label: 'Paid off loans', value: overview.paidOffLoans },
      ]
    : []

  return (
    <main className="home-page">
      <section className="dashboard-hero" aria-labelledby="dashboard-title">
        <div className="dashboard-hero__content">
          <span className="eyebrow">Employee analytics</span>
          <h1 id="dashboard-title">Operational snapshot for the lending team.</h1>
          <p>
            Signed in as {session.email}. Track portfolio health, review today&apos;s workload,
            and jump straight into the bank&apos;s service modules.
          </p>
        </div>
        <div className="spotlight-card" aria-label="Portfolio totals">
          <span>Outstanding balance</span>
          <strong>{formatCurrency(overview?.outstandingLoanBalance ?? 0)}</strong>
          <p>Total originations: {formatCurrency(overview?.totalLoanOriginations ?? 0)}</p>
          <dl className="spotlight-card__stats">
            <div>
              <dt>Total accounts</dt>
              <dd>{overview?.totalAccounts ?? 0}</dd>
            </div>
            <div>
              <dt>Active accounts</dt>
              <dd>{overview?.activeAccounts ?? 0}</dd>
            </div>
            <div>
              <dt>Active loans</dt>
              <dd>{overview?.activeLoans ?? 0}</dd>
            </div>
          </dl>
        </div>
      </section>

      {status.text && (
        <section className="dashboard-section">
          <StatusNotice type={status.type} text={status.text} />
        </section>
      )}

      <section className="dashboard-section" aria-labelledby="metrics-title">
        <div className="section-heading section-heading--compact">
          <span className="eyebrow">Overview</span>
          <h2 id="metrics-title">Core lending metrics</h2>
        </div>
        <div className="dashboard-metric-grid">
          {metricCards.map((card) => (
            <article key={card.label} className="dashboard-metric-card">
              <span>{card.label}</span>
              <strong>{card.value}</strong>
            </article>
          ))}
        </div>
      </section>

      <section className="dashboard-section" aria-labelledby="actions-title">
        <div className="section-heading section-heading--compact">
          <span className="eyebrow">Workspace</span>
          <h2 id="actions-title">Service modules</h2>
        </div>
        <div className="loan-service-grid__cards">
          {employeeActions
            .filter((action) => !action.roles || action.roles.includes(session.role))
            .map((action) => (
            <button
              key={action.page}
              type="button"
              className="service-card service-card--interactive"
              onClick={() => onNavigate(action.page)}
            >
              <span>{action.title}</span>
              <strong>Open</strong>
              <small>{action.detail}</small>
            </button>
            ))}
        </div>
      </section>
    </main>
  )
}

function CustomerDashboard({ onNavigate, session }) {
  const [allAccounts, setAllAccounts] = useState([])
  const [loans, setLoans] = useState([])
  const [status, setStatus] = useState({ type: '', text: '' })

  const loadDashboard = async () => {
    const [accountData, loanData] = await Promise.all([
      requestJson(`/clients/${session.ucn}/accounts`),
      requestJson(`/clients/${session.ucn}/loans`),
    ])

    setAllAccounts(accountData)
    setLoans(loanData)
  }

  useEffect(() => {
    let isMounted = true

    const run = async () => {
      if (!session.ucn) {
        setStatus({ type: 'error', text: 'Customer account is missing a client identifier.' })
        return
      }

      try {
        await loadDashboard()
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

  const totalBalance = useMemo(
    () => accounts.reduce((sum, account) => sum + Number(account.balance ?? 0), 0),
    [accounts],
  )

  const activeLoanBalance = useMemo(
    () =>
      loans
        .filter((loan) => loan.status === 'ACTIVE')
        .reduce((sum, loan) => sum + Number(loan.remainingAmount ?? 0), 0),
    [loans],
  )

  const overviewLoans = useMemo(
    () => loans.filter((loan) => loan.status !== 'REJECTED'),
    [loans],
  )

  const mainAccount = useMemo(
    () =>
      accounts.find((account) => account.type === 'CHECKING') ??
      accounts.find((account) => account.type === 'SAVINGS') ??
      accounts[0] ??
      null,
    [accounts],
  )

  const otherAccounts = useMemo(
    () => accounts.filter((account) => account.id !== mainAccount?.id),
    [accounts, mainAccount],
  )

  const recentLoans = useMemo(() => overviewLoans.slice(0, 3), [overviewLoans])
  const hasNoProducts = accounts.length === 0 && overviewLoans.length === 0
  const heroLabel = mainAccount
    ? 'Main account balance'
    : overviewLoans.length
      ? 'Outstanding credit balance'
      : 'Main account balance'
  const heroValue = mainAccount ? formatCurrency(mainAccount.balance) : formatCurrency(activeLoanBalance)

  return (
    <main className="home-page">
      <section className="dashboard-hero" aria-labelledby="customer-title">
        <div className="dashboard-hero__content">
          <span className="eyebrow">Customer dashboard</span>
          <h1 id="customer-title">See your balances and credit snapshot at a glance.</h1>
          <p>
            Signed in as {session.email}. Your dashboard highlights the balance on your main
            account, other accounts you hold, and a quick summary of your credits.
          </p>
        </div>
        <div className="spotlight-card" aria-label="Customer totals">
          <span>{heroLabel}</span>
          <strong>{heroValue}</strong>
          <p>
            {mainAccount
              ? `${mainAccount.type} account ${mainAccount.iban}`
              : overviewLoans.length
                ? `You currently have ${overviewLoans.length} credit${overviewLoans.length === 1 ? '' : 's'} in the system.`
              : 'Open your first account to start using the platform.'}
          </p>
          <dl className="spotlight-card__stats">
            <div>
              <dt>Total balance</dt>
              <dd>{formatCurrency(totalBalance)}</dd>
            </div>
            <div>
              <dt>Other accounts</dt>
              <dd>{otherAccounts.length}</dd>
            </div>
            <div>
              <dt>Credits</dt>
              <dd>{overviewLoans.length}</dd>
            </div>
          </dl>
        </div>
      </section>

      {status.text && (
        <section className="dashboard-section">
          <StatusNotice type={status.type} text={status.text} />
        </section>
      )}

      {hasNoProducts && (
        <section className="dashboard-section" aria-labelledby="starter-title">
          <div className="dashboard-form-card dashboard-form-card--highlight">
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">First step</span>
              <h2 id="starter-title">You do not have an account or a loan yet.</h2>
            </div>
            <p className="dashboard-form-card__text">
              Start by opening your first account. Once you have an account, you can track balances
              here and continue to the loans area when you need financing.
            </p>
            <div className="actions">
              <button type="button" onClick={() => onNavigate('accounts')}>
                Open your first account
              </button>
            </div>
          </div>
        </section>
      )}

      <section className="dashboard-section" aria-labelledby="overview-title">
        <div className="section-heading section-heading--compact">
          <span className="eyebrow">Overview</span>
          <h2 id="overview-title">Your banking snapshot</h2>
        </div>
        <div className="dashboard-form-grid">
          <section className="dashboard-form-card">
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Accounts</span>
              <h2>Main and other accounts</h2>
            </div>
            {mainAccount ? (
              <div className="account-summary-list">
                <article className="account-summary-card account-summary-card--primary">
                  <span>Main account</span>
                  <strong>{formatCurrency(mainAccount.balance)}</strong>
                  <p>{mainAccount.iban}</p>
                  <small>
                    {mainAccount.type} • {mainAccount.status}
                  </small>
                </article>
                {otherAccounts.length ? (
                  otherAccounts.map((account) => (
                    <article key={account.id} className="account-summary-card">
                      <span>{account.type}</span>
                      <strong>{formatCurrency(account.balance)}</strong>
                      <p>{account.iban}</p>
                      <small>{account.status}</small>
                    </article>
                  ))
                ) : (
                  <div className="empty-card">
                    No additional accounts yet. You can open more from the Accounts page.
                  </div>
                )}
              </div>
            ) : (
              <div className="empty-card">
                No accounts found yet. Open one to start tracking your balance.
              </div>
            )}
            <div className="actions">
              <button type="button" onClick={() => onNavigate('accounts')}>
                Go to accounts
              </button>
            </div>
          </section>

          <section className="dashboard-form-card">
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Loans</span>
              <h2>Credit summary</h2>
            </div>
            {recentLoans.length ? (
              <div className="credit-summary-list">
                {recentLoans.map((loan) => (
                  <article key={loan.id} className="credit-summary-card">
                    <span>{loan.loanTypeName}</span>
                    <strong>{formatCurrency(loan.remainingAmount)}</strong>
                    <p>Monthly payment: {formatCurrency(loan.monthlyPayment)}</p>
                    <small>
                      {loan.status} • {loan.termMonths} months
                    </small>
                  </article>
                ))}
              </div>
            ) : (
              <div className="empty-card">
                No credits yet. When you apply for one, a quick status summary will appear here.
              </div>
            )}
            <div className="actions">
              <button type="button" onClick={() => onNavigate('loans')}>
                Go to loans
              </button>
            </div>
          </section>
        </div>
      </section>
    </main>
  )
}

export default HomePage
