import { useMemo, useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { loanTypes } from '../data/loanTypes'
import { requestJson } from '../lib/api'
import { formatCurrency } from '../lib/format'

const initialLoanApplicationForm = {
  customerName: '',
  email: '',
  phone: '',
  loanType: loanTypes[0].value,
  amount: 10000,
  periodMonths: 24,
  monthlyIncome: 2500,
  purpose: '',
}

function LoanApplicationsPage() {
  const [form, setForm] = useState(initialLoanApplicationForm)
  const [application, setApplication] = useState(null)
  const [formError, setFormError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [approvalId, setApprovalId] = useState('')
  const [approvalMessage, setApprovalMessage] = useState('')
  const [approvalError, setApprovalError] = useState('')
  const [isApproving, setIsApproving] = useState(false)

  const selectedType = useMemo(
    () => loanTypes.find((type) => type.value === form.loanType) ?? loanTypes[0],
    [form.loanType],
  )

  const validationError = useMemo(() => {
    const amount = Number(form.amount)
    const months = Number(form.periodMonths)
    const income = Number(form.monthlyIncome)

    if (!form.customerName.trim()) return 'Въведете име на клиента.'
    if (!form.email.trim()) return 'Въведете имейл за контакт.'
    if (!form.phone.trim()) return 'Въведете телефон за контакт.'
    if (amount < selectedType.minAmount || amount > selectedType.maxAmount) {
      return `Сумата за ${selectedType.label.toLowerCase()} трябва да е между ${formatCurrency(
        selectedType.minAmount,
      )} и ${formatCurrency(selectedType.maxAmount)}.`
    }
    if (months < selectedType.minMonths || months > selectedType.maxMonths) {
      return `Периодът трябва да е между ${selectedType.minMonths} и ${selectedType.maxMonths} месеца.`
    }
    if (!income || income <= 0) return 'Въведете валиден месечен доход.'
    if (!form.purpose.trim()) return 'Опишете целта на кредита.'
    return ''
  }, [form, selectedType])

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
    setFormError('')

    if (validationError) {
      setFormError(validationError)
      return
    }

    const payload = {
      ...form,
      amount: Number(form.amount),
      periodMonths: Number(form.periodMonths),
      monthlyIncome: Number(form.monthlyIncome),
    }

    try {
      setIsSubmitting(true)
      const data = await requestJson('/loans/apply', {
        method: 'POST',
        body: JSON.stringify(payload),
      })

      setApplication({
        id: data.id ?? data.applicationId ?? 'нова',
        status: data.status ?? 'Pending',
        customerName: data.customerName ?? form.customerName,
        amount: data.amount ?? payload.amount,
        loanType: data.loanType ?? payload.loanType,
        periodMonths: data.periodMonths ?? payload.periodMonths,
      })
      setApprovalId(String(data.id ?? data.applicationId ?? ''))
    } catch (error) {
      setFormError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  const approveApplication = async (event) => {
    event.preventDefault()
    setApprovalError('')
    setApprovalMessage('')

    if (!approvalId.trim()) {
      setApprovalError('Въведете номер на кандидатура.')
      return
    }

    try {
      setIsApproving(true)
      const data = await requestJson(`/loans/${approvalId.trim()}/approve`, { method: 'PATCH' })
      const approvedStatus = data.status ?? 'Approved'
      setApprovalMessage(`Кандидатура #${approvalId.trim()} е със статус "${approvedStatus}".`)
      setApplication((current) =>
        current && String(current.id) === approvalId.trim()
          ? { ...current, status: approvedStatus }
          : current,
      )
    } catch (error) {
      setApprovalError(error.message)
    } finally {
      setIsApproving(false)
    }
  }

  const startNewApplication = () => {
    setApplication(null)
    setForm(initialLoanApplicationForm)
    setFormError('')
  }

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Loan service"
        title="Кандидатури за кредит"
        text="Клиентът попълва форма, системата записва заявката със статус Pending, а служител може да я одобри."
      >
        <div className="status-panel" aria-label="Статус на процеса">
          <div>
            <span>01</span>
            <strong>Форма</strong>
          </div>
          <div>
            <span>02</span>
            <strong>Pending</strong>
          </div>
          <div>
            <span>03</span>
            <strong>Одобрение</strong>
          </div>
        </div>
      </PageIntro>

      <section className="workspace" aria-label="Кредитен работен панел">
        <div className="form-shell">
          {application ? (
            <AcceptedApplication
              application={application}
              onNewApplication={startNewApplication}
            />
          ) : (
            <LoanApplicationForm
              form={form}
              formError={formError}
              isSubmitting={isSubmitting}
              selectedType={selectedType}
              validationError={validationError}
              onChange={updateField}
              onSubmit={submitApplication}
            />
          )}
        </div>

        <aside className="employee-panel" aria-labelledby="employee-title">
          <span className="eyebrow">За служител</span>
          <h2 id="employee-title">Одобряване на кандидатура</h2>
          <form onSubmit={approveApplication}>
            <label htmlFor="approvalId">Номер на кандидатура</label>
            <div className="approval-row">
              <input
                id="approvalId"
                name="approvalId"
                value={approvalId}
                onChange={(event) => setApprovalId(event.target.value)}
                placeholder="Напр. 42"
              />
              <button type="submit" disabled={isApproving}>
                {isApproving ? 'Одобряване...' : 'Одобри'}
              </button>
            </div>
          </form>
          {approvalMessage && <StatusNotice type="success" text={approvalMessage} />}
          {approvalError && <StatusNotice type="error" text={approvalError} />}
        </aside>
      </section>
    </main>
  )
}

function LoanApplicationForm({
  form,
  formError,
  isSubmitting,
  selectedType,
  validationError,
  onChange,
  onSubmit,
}) {
  return (
    <form className="loan-form" onSubmit={onSubmit}>
      <div className="section-heading">
        <span className="eyebrow">Клиентска форма</span>
        <h2>Данни за кандидатстване</h2>
      </div>

      <div className="field-grid">
        <label>
          Име и фамилия
          <input
            name="customerName"
            value={form.customerName}
            onChange={onChange}
            placeholder="Мария Иванова"
            autoComplete="name"
          />
        </label>
        <label>
          Имейл
          <input
            name="email"
            type="email"
            value={form.email}
            onChange={onChange}
            placeholder="maria@example.com"
            autoComplete="email"
          />
        </label>
        <label>
          Телефон
          <input
            name="phone"
            value={form.phone}
            onChange={onChange}
            placeholder="+359 88 000 0000"
            autoComplete="tel"
          />
        </label>
        <label>
          Вид кредит
          <select name="loanType" value={form.loanType} onChange={onChange}>
            {loanTypes.map((type) => (
              <option key={type.value} value={type.value}>
                {type.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Сума
          <input
            name="amount"
            type="number"
            min={selectedType.minAmount}
            max={selectedType.maxAmount}
            step="100"
            value={form.amount}
            onChange={onChange}
          />
        </label>
        <label>
          Период в месеци
          <input
            name="periodMonths"
            type="number"
            min={selectedType.minMonths}
            max={selectedType.maxMonths}
            value={form.periodMonths}
            onChange={onChange}
          />
        </label>
        <label>
          Месечен доход
          <input
            name="monthlyIncome"
            type="number"
            min="1"
            step="50"
            value={form.monthlyIncome}
            onChange={onChange}
          />
        </label>
        <label className="field-grid__wide">
          Цел на кредита
          <textarea
            name="purpose"
            value={form.purpose}
            onChange={onChange}
            placeholder="Кратко описание на нуждата от финансиране"
            rows="4"
          />
        </label>
      </div>

      <div className="limits-bar">
        <div>
          <span>Сума</span>
          <strong>
            {formatCurrency(selectedType.minAmount)} - {formatCurrency(selectedType.maxAmount)}
          </strong>
        </div>
        <div>
          <span>Период</span>
          <strong>
            {selectedType.minMonths} - {selectedType.maxMonths} м.
          </strong>
        </div>
        <div>
          <span>Ориентировъчна лихва</span>
          <strong>{selectedType.rate}</strong>
        </div>
      </div>

      {(formError || validationError) && (
        <StatusNotice type="error" text={formError || validationError} />
      )}

      <div className="actions">
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Изпращане...' : 'Изпрати кандидатура'}
        </button>
      </div>
    </form>
  )
}

function AcceptedApplication({ application, onNewApplication }) {
  const typeLabel =
    loanTypes.find((type) => type.value === application.loanType)?.label ?? application.loanType

  return (
    <div className="accepted-view" role="status" aria-live="polite">
      <span className="accepted-mark" aria-hidden="true">
        ✓
      </span>
      <span className="eyebrow">Заявката е приета</span>
      <h2>Кандидатурата е записана успешно</h2>
      <p>
        Благодарим, {application.customerName}. Заявката е получена и очаква
        преглед от служител.
      </p>
      <dl className="application-summary">
        <div>
          <dt>Номер</dt>
          <dd>#{application.id}</dd>
        </div>
        <div>
          <dt>Статус</dt>
          <dd>{application.status}</dd>
        </div>
        <div>
          <dt>Вид</dt>
          <dd>{typeLabel}</dd>
        </div>
        <div>
          <dt>Сума</dt>
          <dd>{formatCurrency(application.amount)}</dd>
        </div>
        <div>
          <dt>Период</dt>
          <dd>{application.periodMonths} месеца</dd>
        </div>
      </dl>
      <button type="button" onClick={onNewApplication}>
        Нова кандидатура
      </button>
    </div>
  )
}

export default LoanApplicationsPage
