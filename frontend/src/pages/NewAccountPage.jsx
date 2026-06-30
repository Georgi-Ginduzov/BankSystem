import { useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'
import { formatCurrency } from '../lib/format'

const initialAccountForm = {
  clientId: '',
  accountType: 'CHECKING',
  initialDeposit: 0,
}

const initialDepositForm = {
  accountId: '',
  amount: 0,
}

function NewAccountPage({ onNavigate }) {
  const [form, setForm] = useState(initialAccountForm)
  const [depositForm, setDepositForm] = useState(initialDepositForm)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [depositStatus, setDepositStatus] = useState({ type: '', text: '' })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isDepositing, setIsDepositing] = useState(false)

  const updateField = (event) => {
    const { name, type, checked, value } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
  }

  const updateDepositField = (event) => {
    const { name, value } = event.target
    setDepositForm((current) => ({ ...current, [name]: value }))
  }

  const openAccount = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!form.clientId.trim()) {
      setStatus({ type: 'error', text: 'Въведете клиентски номер.' })
      return
    }

    const payload = {
      ...form,
      initialDeposit: Number(form.initialDeposit),
    }

    try {
      setIsSubmitting(true)
      const data = await requestJson('/accounts', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setStatus({
        type: 'success',
        text: `Сметката е открита успешно. IBAN: ${data.iban ?? data.id ?? 'нов'}.`,
      })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsSubmitting(false)
    }
  }

  const depositToAccount = async (event) => {
    event.preventDefault()
    setDepositStatus({ type: '', text: '' })

    if (!depositForm.accountId.trim()) {
      setDepositStatus({ type: 'error', text: 'Въведете номер на сметка.' })
      return
    }

    if (!Number(depositForm.amount) || Number(depositForm.amount) <= 0) {
      setDepositStatus({ type: 'error', text: 'Въведете валидна сума за захранване.' })
      return
    }

    try {
      setIsDepositing(true)
      const data = await requestJson(`/accounts/${depositForm.accountId.trim()}/deposit`, {
        method: 'POST',
        body: JSON.stringify({ amount: Number(depositForm.amount) }),
      })
      setDepositStatus({
        type: 'success',
        text: `Сметка #${data.id} е захранена успешно. Нов баланс: ${formatCurrency(
          data.balance,
        )}.`,
      })
      setDepositForm(initialDepositForm)
    } catch (error) {
      setDepositStatus({ type: 'error', text: error.message })
    } finally {
      setIsDepositing(false)
    }
  }

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Account service"
        title="Откриване на нова сметка"
        text="Служителят може да открива нови сметки и единствено оттук да захранва клиентски сметки с наличност."
      />

      <section className="dashboard-section">
        <div className="dashboard-form-card dashboard-form-card--highlight">
          <div className="section-heading section-heading--compact">
            <span className="eyebrow">Баланс по сметки</span>
            <h2>Търсите страницата за редакция на наличност?</h2>
          </div>
          <p className="dashboard-form-card__text">
            Отворете модула за управление на сметки, за да намерите клиентска сметка и да
            промените текущия й баланс.
          </p>
          <div className="actions">
            <button type="button" onClick={() => onNavigate?.('account-management')}>
              Към управление на сметки
            </button>
          </div>
        </div>
      </section>

      <section className="dual-workspace">
        <form className="loan-form" onSubmit={openAccount}>
          <div className="section-heading">
            <span className="eyebrow">Нова сметка</span>
            <h2>Данни за сметката</h2>
          </div>
          <div className="field-grid">
            <label>
              Клиентски номер
              <input
                name="clientId"
                value={form.clientId}
                onChange={updateField}
                placeholder="Напр. 1024"
              />
            </label>
            <label>
              Тип сметка
              <select name="accountType" value={form.accountType} onChange={updateField}>
                <option value="CHECKING">Разплащателна</option>
                <option value="SAVINGS">Спестовна</option>
                <option value="BUSINESS">Бизнес</option>
              </select>
            </label>
            <label>
              Начално салдо
              <input
                name="initialDeposit"
                type="number"
                min="0"
                step="10"
                value={form.initialDeposit}
                onChange={updateField}
              />
            </label>
          </div>
          {status.text && <StatusNotice type={status.type} text={status.text} />}
          <div className="actions">
            <button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Откриване...' : 'Открий сметка'}
            </button>
          </div>
        </form>

        <form className="loan-form" onSubmit={depositToAccount}>
          <div className="section-heading">
            <span className="eyebrow">Захранване</span>
            <h2>Добавяне на пари по сметка</h2>
          </div>
          <div className="field-grid">
            <label>
              Номер на сметка
              <input
                name="accountId"
                value={depositForm.accountId}
                onChange={updateDepositField}
                placeholder="Напр. 12"
              />
            </label>
            <label>
              Сума (EUR)
              <input
                name="amount"
                type="number"
                min="0.01"
                step="0.01"
                value={depositForm.amount}
                onChange={updateDepositField}
              />
            </label>
          </div>
          {depositStatus.text && <StatusNotice type={depositStatus.type} text={depositStatus.text} />}
          <div className="actions">
            <button type="submit" disabled={isDepositing}>
              {isDepositing ? 'Захранване...' : 'Добави пари'}
            </button>
          </div>
        </form>
      </section>
    </main>
  )
}

export default NewAccountPage
