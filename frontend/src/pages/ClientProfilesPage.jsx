import { useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'

const initialClientProfileForm = {
  id: '',
  fullName: '',
  email: '',
  phone: '',
  address: '',
  segment: 'RETAIL',
}

function ClientProfilesPage() {
  const [form, setForm] = useState(initialClientProfileForm)
  const [clientLookupId, setClientLookupId] = useState('')
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isSaving, setIsSaving] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const loadClientProfile = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!clientLookupId.trim()) {
      setStatus({ type: 'error', text: 'Въведете клиентски номер за търсене.' })
      return
    }

    try {
      setIsLoading(true)
      const data = await requestJson(`/clients/${clientLookupId.trim()}`)
      setForm({
        id: String(data.id ?? clientLookupId.trim()),
        fullName: data.fullName ?? data.name ?? '',
        email: data.email ?? '',
        phone: data.phone ?? '',
        address: data.address ?? '',
        segment: data.segment ?? 'RETAIL',
      })
      setStatus({ type: 'success', text: 'Профилът е зареден.' })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsLoading(false)
    }
  }

  const saveClientProfile = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!form.fullName.trim()) {
      setStatus({ type: 'error', text: 'Въведете име на клиента.' })
      return
    }

    const isUpdate = Boolean(form.id.trim())
    const path = isUpdate ? `/clients/${form.id.trim()}` : '/clients'

    try {
      setIsSaving(true)
      const data = await requestJson(path, {
        method: isUpdate ? 'PATCH' : 'POST',
        body: JSON.stringify(form),
      })
      setForm((current) => ({
        ...current,
        id: String(data.id ?? current.id),
      }))
      setStatus({
        type: 'success',
        text: isUpdate ? 'Профилът е обновен.' : 'Профилът е създаден.',
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
        eyebrow="Client service"
        title="Клиентски профили"
        text="Търсене, създаване и редактиране на клиентски данни от едно място."
      />

      <section className="workspace" aria-label="Клиентски профили">
        <aside className="employee-panel">
          <span className="eyebrow">Търсене</span>
          <h2>Зареждане на профил</h2>
          <form onSubmit={loadClientProfile}>
            <label htmlFor="clientLookupId">Клиентски номер</label>
            <div className="approval-row">
              <input
                id="clientLookupId"
                value={clientLookupId}
                onChange={(event) => setClientLookupId(event.target.value)}
                placeholder="Напр. 1024"
              />
              <button type="submit" disabled={isLoading}>
                {isLoading ? 'Търсене...' : 'Зареди'}
              </button>
            </div>
          </form>
        </aside>

        <div className="form-shell">
          <form className="loan-form" onSubmit={saveClientProfile}>
            <div className="section-heading">
              <span className="eyebrow">Профил</span>
              <h2>Данни за клиента</h2>
            </div>
            <div className="field-grid">
              <label>
                Клиентски номер
                <input
                  name="id"
                  value={form.id}
                  onChange={updateField}
                  placeholder="Оставете празно за нов клиент"
                />
              </label>
              <label>
                Сегмент
                <select name="segment" value={form.segment} onChange={updateField}>
                  <option value="RETAIL">Физическо лице</option>
                  <option value="BUSINESS">Бизнес клиент</option>
                  <option value="PREMIUM">Премиум</option>
                </select>
              </label>
              <label>
                Име
                <input
                  name="fullName"
                  value={form.fullName}
                  onChange={updateField}
                  placeholder="Мария Иванова"
                />
              </label>
              <label>
                Имейл
                <input
                  name="email"
                  type="email"
                  value={form.email}
                  onChange={updateField}
                  placeholder="maria@example.com"
                />
              </label>
              <label>
                Телефон
                <input
                  name="phone"
                  value={form.phone}
                  onChange={updateField}
                  placeholder="+359 88 000 0000"
                />
              </label>
              <label className="field-grid__wide">
                Адрес
                <textarea
                  name="address"
                  value={form.address}
                  onChange={updateField}
                  placeholder="Адрес за кореспонденция"
                  rows="3"
                />
              </label>
            </div>
            {status.text && <StatusNotice type={status.type} text={status.text} />}
            <div className="actions">
              <button type="submit" disabled={isSaving}>
                {isSaving ? 'Запис...' : form.id ? 'Обнови профил' : 'Създай профил'}
              </button>
            </div>
          </form>
        </div>
      </section>
    </main>
  )
}

export default ClientProfilesPage
