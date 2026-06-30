import { useEffect, useMemo, useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'
import { formatCurrency } from '../lib/format'

const initialAccountForm = {
  accountType: 'CHECKING',
  initialDeposit: 0,
}

function CustomerAccountsPage({ session }) {
  const [allAccounts, setAllAccounts] = useState([])
  const [form, setForm] = useState(initialAccountForm)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [action, setAction] = useState({ type: '', text: '' })
  const [isOpeningAccount, setIsOpeningAccount] = useState(false)

  const loadAccounts = async () => {
    const data = await requestJson(`/clients/${session.ucn}/accounts`)
    setAllAccounts(data)
  }

  useEffect(() => {
    let isMounted = true

    const run = async () => {
      if (!session.ucn) {
        setStatus({ type: 'error', text: 'Customer account is missing a client identifier.' })
        return
      }

      try {
        await loadAccounts()
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

  const mainAccount = useMemo(
    () =>
      accounts.find((account) => account.type === 'CHECKING') ??
      accounts.find((account) => account.type === 'SAVINGS') ??
      accounts[0] ??
      null,
    [accounts],
  )

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const submitAccountOpening = async (event) => {
    event.preventDefault()
    setAction({ type: '', text: '' })

    try {
      setIsOpeningAccount(true)
      const data = await requestJson('/accounts', {
        method: 'POST',
        body: JSON.stringify({
          clientId: session.ucn,
          accountType: form.accountType,
          initialDeposit: Number(form.initialDeposit),
        }),
      })

      setAction({
        type: 'success',
        text: `Account ${data.iban ?? data.id ?? ''} was opened successfully.`,
      })
      setForm(initialAccountForm)
      await loadAccounts()
    } catch (error) {
      setAction({ type: 'error', text: error.message })
    } finally {
      setIsOpeningAccount(false)
    }
  }

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Accounts"
        title="Manage your bank accounts"
        text="Open new accounts and review balances across your main and additional accounts."
      >
        <div className="status-panel" aria-label="Account summary">
          <div>
            <span>Total accounts</span>
            <strong>{accounts.length}</strong>
          </div>
          <div>
            <span>Main account</span>
            <strong>{mainAccount?.type ?? 'Not opened yet'}</strong>
          </div>
          <div>
            <span>Total balance</span>
            <strong>{formatCurrency(totalBalance)}</strong>
          </div>
        </div>
      </PageIntro>

      <section className="single-workspace" aria-label="Customer accounts workspace">
        {status.text && <StatusNotice type={status.type} text={status.text} />}

        <div className="dashboard-form-grid">
          <form className="dashboard-form-card" onSubmit={submitAccountOpening}>
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Open account</span>
              <h2>Create a new account</h2>
            </div>
            <div className="field-grid">
              <label>
                Account type
                <select name="accountType" value={form.accountType} onChange={updateField}>
                  <option value="CHECKING">Checking</option>
                  <option value="SAVINGS">Savings</option>
                  <option value="BUSINESS">Business</option>
                </select>
              </label>
              <label>
                Initial deposit
                <input
                  name="initialDeposit"
                  type="number"
                  min="0"
                  step="1"
                  value={form.initialDeposit}
                  onChange={updateField}
                />
              </label>
            </div>
            {action.text && <StatusNotice type={action.type} text={action.text} />}
            <div className="actions">
              <button type="submit" disabled={isOpeningAccount}>
                {isOpeningAccount ? 'Opening...' : 'Open account'}
              </button>
            </div>
          </form>

          <section className="dashboard-form-card">
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Main account</span>
              <h2>Primary balance</h2>
            </div>
            {mainAccount ? (
              <article className="account-summary-card account-summary-card--primary">
                <span>{mainAccount.type}</span>
                <strong>{formatCurrency(mainAccount.balance)}</strong>
                <p>{mainAccount.iban}</p>
                <small>Status: {mainAccount.status}</small>
              </article>
            ) : (
              <div className="empty-card">
                You do not have an account yet. Open your first one from this page.
              </div>
            )}
          </section>
        </div>

        <div className="dashboard-section dashboard-section--flush" aria-labelledby="all-accounts-title">
          <div className="section-heading section-heading--compact">
            <span className="eyebrow">Portfolio</span>
            <h2 id="all-accounts-title">All accounts</h2>
          </div>
          <div className="dashboard-data-grid">
            {accounts.length ? (
              accounts.map((account) => (
                <article key={account.id} className="data-card">
                  <span>{account.type}</span>
                  <strong>{account.iban}</strong>
                  <p>Balance: {formatCurrency(account.balance)}</p>
                  <small>Status: {account.status}</small>
                </article>
              ))
            ) : (
              <div className="empty-card">No accounts are available for this customer profile yet.</div>
            )}
          </div>
        </div>
      </section>
    </main>
  )
}

export default CustomerAccountsPage
