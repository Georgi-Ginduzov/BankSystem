import { useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { loanTypes } from '../data/loanTypes'
import { requestJson } from '../lib/api'

const initialOpenLoanForm = {
  applicationId: '',
  customerId: '',
  loanType: loanTypes[0].value,
  principal: 10000,
  periodMonths: 24,
  interestRate: 7.9,
  startDate: new Date().toISOString().slice(0, 10),
}

const initialModifyLoanForm = {
  loanId: '',
  principal: 10000,
  periodMonths: 24,
  interestRate: 7.9,
  status: 'ACTIVE',
}

function LoanManagementPage() {
  const [openLoanForm, setOpenLoanForm] = useState(initialOpenLoanForm)
  const [modifyLoanForm, setModifyLoanForm] = useState(initialModifyLoanForm)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isOpeningLoan, setIsOpeningLoan] = useState(false)
  const [isModifyingLoan, setIsModifyingLoan] = useState(false)

  const updateOpenLoanField = (event) => {
    const { name, value } = event.target
    setOpenLoanForm((current) => ({ ...current, [name]: value }))
  }

  const updateModifyLoanField = (event) => {
    const { name, value } = event.target
    setModifyLoanForm((current) => ({ ...current, [name]: value }))
  }

  const openLoan = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!openLoanForm.applicationId.trim() || !openLoanForm.customerId.trim()) {
      setStatus({
        type: 'error',
        text: 'Въведете номер на кандидатура и клиентски номер.',
      })
      return
    }

    const payload = {
      ...openLoanForm,
      principal: Number(openLoanForm.principal),
      periodMonths: Number(openLoanForm.periodMonths),
      interestRate: Number(openLoanForm.interestRate),
    }

    try {
      setIsOpeningLoan(true)
      const data = await requestJson('/loans/open', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setStatus({
        type: 'success',
        text: `Кредитът е открит успешно. Номер: ${data.id ?? data.loanId ?? 'нов'}.`,
      })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsOpeningLoan(false)
    }
  }

  const modifyLoan = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!modifyLoanForm.loanId.trim()) {
      setStatus({ type: 'error', text: 'Въведете номер на кредит.' })
      return
    }

    const { loanId, ...loanFields } = modifyLoanForm
    const payload = {
      ...loanFields,
      principal: Number(loanFields.principal),
      periodMonths: Number(loanFields.periodMonths),
      interestRate: Number(loanFields.interestRate),
    }

    try {
      setIsModifyingLoan(true)
      await requestJson(`/loans/${loanId.trim()}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
      })
      setStatus({ type: 'success', text: `Кредит #${loanId.trim()} е обновен.` })
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsModifyingLoan(false)
    }
  }

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Loan operations"
        title="Откриване и промяна на кредити"
        text="След одобрена кандидатура служителят може да открие кредитен договор или да промени параметри на активен кредит."
      />

      <section className="dual-workspace" aria-label="Управление на кредити">
        <form className="loan-form form-shell" onSubmit={openLoan}>
          <div className="section-heading">
            <span className="eyebrow">Откриване</span>
            <h2>Нов кредитен договор</h2>
          </div>
          <div className="field-grid">
            <label>
              Номер на кандидатура
              <input
                name="applicationId"
                value={openLoanForm.applicationId}
                onChange={updateOpenLoanField}
                placeholder="Напр. 42"
              />
            </label>
            <label>
              Клиентски номер
              <input
                name="customerId"
                value={openLoanForm.customerId}
                onChange={updateOpenLoanField}
                placeholder="Напр. 1024"
              />
            </label>
            <label>
              Вид кредит
              <select name="loanType" value={openLoanForm.loanType} onChange={updateOpenLoanField}>
                {loanTypes.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Главница
              <input
                name="principal"
                type="number"
                min="0"
                step="100"
                value={openLoanForm.principal}
                onChange={updateOpenLoanField}
              />
            </label>
            <label>
              Период в месеци
              <input
                name="periodMonths"
                type="number"
                min="1"
                value={openLoanForm.periodMonths}
                onChange={updateOpenLoanField}
              />
            </label>
            <label>
              Лихвен процент
              <input
                name="interestRate"
                type="number"
                min="0"
                step="0.1"
                value={openLoanForm.interestRate}
                onChange={updateOpenLoanField}
              />
            </label>
            <label className="field-grid__wide">
              Начална дата
              <input
                name="startDate"
                type="date"
                value={openLoanForm.startDate}
                onChange={updateOpenLoanField}
              />
            </label>
          </div>
          <div className="actions">
            <button type="submit" disabled={isOpeningLoan}>
              {isOpeningLoan ? 'Откриване...' : 'Открий кредит'}
            </button>
          </div>
        </form>

        <form className="loan-form form-shell" onSubmit={modifyLoan}>
          <div className="section-heading">
            <span className="eyebrow">Промяна</span>
            <h2>Редакция на кредит</h2>
          </div>
          <div className="field-grid">
            <label className="field-grid__wide">
              Номер на кредит
              <input
                name="loanId"
                value={modifyLoanForm.loanId}
                onChange={updateModifyLoanField}
                placeholder="Напр. 7001"
              />
            </label>
            <label>
              Главница
              <input
                name="principal"
                type="number"
                min="0"
                step="100"
                value={modifyLoanForm.principal}
                onChange={updateModifyLoanField}
              />
            </label>
            <label>
              Период в месеци
              <input
                name="periodMonths"
                type="number"
                min="1"
                value={modifyLoanForm.periodMonths}
                onChange={updateModifyLoanField}
              />
            </label>
            <label>
              Лихвен процент
              <input
                name="interestRate"
                type="number"
                min="0"
                step="0.1"
                value={modifyLoanForm.interestRate}
                onChange={updateModifyLoanField}
              />
            </label>
            <label>
              Статус
              <select name="status" value={modifyLoanForm.status} onChange={updateModifyLoanField}>
                <option value="ACTIVE">Active</option>
                <option value="PAUSED">Paused</option>
                <option value="CLOSED">Closed</option>
                <option value="DEFAULTED">Defaulted</option>
              </select>
            </label>
          </div>
          <div className="actions">
            <button type="submit" disabled={isModifyingLoan}>
              {isModifyingLoan ? 'Запис...' : 'Запази промяна'}
            </button>
          </div>
        </form>
      </section>
      {status.text && (
        <div className="page-status">
          <StatusNotice type={status.type} text={status.text} />
        </div>
      )}
    </main>
  )
}

export default LoanManagementPage
