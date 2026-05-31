import { useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'

const initialAccountForm = {
  customerId: '',
  accountType: 'CHECKING',
  currency: 'BGN',
  initialDeposit: 0,
  branch: '',
  overdraftEnabled: false,
}

function NewAccountPage() {
  const [form, setForm] = useState(initialAccountForm)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (event) => {
    const { name, type, checked, value } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
  }

  const openAccount = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!form.customerId.trim()) {
      setStatus({ type: 'error', text: 'Въведете клиентски номер.' })
      return
    }

    const payload = {
      ...form,
      initialDeposit: Number(form.initialDeposit),
    }

    try {
      setIsSubmitting(true)
      const data = await requestJson('/accounts/open', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setStatus({
        type: 'success',
        text: `Сметката е открита успешно. Номер: ${data.accountNumber ?? data.id ?? 'нов'}.`,
      })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Account service"
        title="Откриване на нова сметка"
        text="Служителят въвежда клиент, тип сметка, валута и начално салдо за създаване на нова банкова сметка."
      />

      <section className="single-workspace">
        <form className="loan-form" onSubmit={openAccount}>
          <div className="section-heading">
            <span className="eyebrow">Нова сметка</span>
            <h2>Данни за сметката</h2>
          </div>
          <div className="field-grid">
            <label>
              Клиентски номер
              <input
                name="customerId"
                value={form.customerId}
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
              Валута
              <select name="currency" value={form.currency} onChange={updateField}>
                <option value="BGN">BGN</option>
                <option value="EUR">EUR</option>
                <option value="USD">USD</option>
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
            <label className="field-grid__wide">
              Клон
              <input
                name="branch"
                value={form.branch}
                onChange={updateField}
                placeholder="Напр. София Център"
              />
            </label>
            <label className="checkbox-field field-grid__wide">
              <input
                name="overdraftEnabled"
                type="checkbox"
                checked={form.overdraftEnabled}
                onChange={updateField}
              />
              Разрешен овърдрафт
            </label>
          </div>
          {status.text && <StatusNotice type={status.type} text={status.text} />}
          <div className="actions">
            <button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Откриване...' : 'Открий сметка'}
            </button>
          </div>
        </form>
      </section>
    </main>
  )
}

export default NewAccountPage
