import { useEffect, useMemo, useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'
import { formatCurrency } from '../lib/format'

function AccountManagementPage() {
  const [query, setQuery] = useState('')
  const [accounts, setAccounts] = useState([])
  const [selectedAccountId, setSelectedAccountId] = useState('')
  const [balance, setBalance] = useState('')
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)

  const loadAccounts = async (clientId = '') => {
    const suffix = clientId.trim() ? `?clientId=${encodeURIComponent(clientId.trim())}` : ''
    const data = await requestJson(`/accounts${suffix}`)
    setAccounts(data)
    return data
  }

  useEffect(() => {
    let isMounted = true

    const run = async () => {
      try {
        const data = await loadAccounts()
        if (isMounted && data.length) {
          setSelectedAccountId(String(data[0].id))
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

  const selectedAccount = useMemo(
    () => accounts.find((account) => String(account.id) === String(selectedAccountId)) ?? null,
    [accounts, selectedAccountId],
  )

  const totalBalance = useMemo(
    () => accounts.reduce((sum, account) => sum + Number(account.balance ?? 0), 0),
    [accounts],
  )

  const activeAccounts = useMemo(
    () => accounts.filter((account) => account.status === 'ACTIVE').length,
    [accounts],
  )

  useEffect(() => {
    setBalance(selectedAccount ? String(Number(selectedAccount.balance ?? 0)) : '')
  }, [selectedAccount])

  const searchAccounts = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    try {
      setIsLoading(true)
      const data = await loadAccounts(query)
      setSelectedAccountId(data.length ? String(data[0].id) : '')
    } catch (error) {
      setAccounts([])
      setSelectedAccountId('')
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsLoading(false)
    }
  }

  const saveAccount = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!selectedAccount) {
      setStatus({ type: 'error', text: 'Select an account to modify.' })
      return
    }

    if (!balance.trim() || Number(balance) < 0) {
      setStatus({ type: 'error', text: 'Enter a valid non-negative balance.' })
      return
    }

    try {
      setIsSaving(true)
      const updated = await requestJson(`/accounts/${selectedAccount.id}`, {
        method: 'PATCH',
        body: JSON.stringify({ balance: Number(balance) }),
      })

      setAccounts((current) =>
        current.map((account) => (account.id === updated.id ? updated : account)),
      )
      setStatus({
        type: 'success',
        text: `Account #${updated.id} was updated. New balance: ${formatCurrency(updated.balance)}.`,
      })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Account management"
        title="Search and modify customer accounts"
        text="Bank employees can search accounts by client identifier and update the current account amount directly from this workspace."
      >
        <div className="status-panel" aria-label="Account management summary">
          <div>
            <span>Loaded accounts</span>
            <strong>{accounts.length}</strong>
          </div>
          <div>
            <span>Active accounts</span>
            <strong>{activeAccounts}</strong>
          </div>
          <div>
            <span>Loaded balance</span>
            <strong>{formatCurrency(totalBalance)}</strong>
          </div>
        </div>
      </PageIntro>

      <section className="single-workspace" aria-label="Employee account management workspace">
        {status.text && <StatusNotice type={status.type} text={status.text} />}

        <div className="account-management-grid">
          <section className="dashboard-form-card account-management-panel">
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Search</span>
              <h2>Find accounts</h2>
            </div>
            <form className="admin-search-form" onSubmit={searchAccounts}>
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Client ID or leave blank for all accounts"
              />
              <button type="submit" disabled={isLoading}>
                {isLoading ? 'Loading...' : 'Search'}
              </button>
            </form>
            <p className="account-management-panel__hint">
              Search by client identifier or keep the field empty to load every account available
              to your employee profile.
            </p>

            <div className="account-record-list">
              {accounts.length ? (
                accounts.map((account) => (
                  <button
                    key={account.id}
                    type="button"
                    className={
                      String(selectedAccountId) === String(account.id)
                        ? 'account-record-card account-record-card--selected'
                        : 'account-record-card'
                    }
                    onClick={() => setSelectedAccountId(String(account.id))}
                  >
                    <div className="account-record-card__top">
                      <span>{account.type}</span>
                      <small>{account.status}</small>
                    </div>
                    <strong>{account.iban}</strong>
                    <div className="account-record-card__meta">
                      <p>Client #{account.clientId}</p>
                      <p>Account #{account.id}</p>
                    </div>
                    <div className="account-record-card__balance">
                      <span>Current balance</span>
                      <strong>{formatCurrency(account.balance)}</strong>
                    </div>
                  </button>
                ))
              ) : (
                <div className="empty-card">No accounts matched the current search.</div>
              )}
            </div>
          </section>

          <form className="dashboard-form-card account-management-panel" onSubmit={saveAccount}>
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Modify</span>
              <h2>Edit account amount</h2>
            </div>
            {selectedAccount ? (
              <>
                <div className="account-management-summary">
                  <div>
                    <span>Account ID</span>
                    <strong>#{selectedAccount.id}</strong>
                  </div>
                  <div>
                    <span>Client</span>
                    <strong>{selectedAccount.clientId}</strong>
                  </div>
                  <div>
                    <span>Status</span>
                    <strong>{selectedAccount.status}</strong>
                  </div>
                  <div>
                    <span>Current balance</span>
                    <strong>{formatCurrency(selectedAccount.balance)}</strong>
                  </div>
                </div>
                <div className="field-grid account-management-fields">
                  <label className="field-grid__wide">
                    IBAN
                    <input value={selectedAccount.iban} disabled />
                  </label>
                  <label>
                    Account type
                    <input value={selectedAccount.type} disabled />
                  </label>
                  <label>
                    Balance (EUR)
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      value={balance}
                      onChange={(event) => setBalance(event.target.value)}
                    />
                  </label>
                </div>
                <p className="account-management-panel__hint">
                  Update the account balance only after verifying the cash movement or manual
                  correction that should be reflected in the ledger.
                </p>
                <div className="actions">
                  <button type="submit" disabled={isSaving}>
                    {isSaving ? 'Saving...' : 'Save account changes'}
                  </button>
                </div>
              </>
            ) : (
              <div className="empty-card">Select an account from the list to edit its amount.</div>
            )}
          </form>
        </div>
      </section>
    </main>
  )
}

export default AccountManagementPage
