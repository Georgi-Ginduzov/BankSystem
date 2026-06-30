import { useState } from 'react'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'

const initialForm = {
  clientType: 'CUSTOMER',
  ucn: '',
  eik: '',
  email: '',
  firstName: '',
  lastName: '',
  companyName: '',
  representativeFirstName: '',
  representativeLastName: '',
  password: '',
  confirmPassword: '',
}

function RegisterPage({ onRegister, onNavigate }) {
  const [form, setForm] = useState(initialForm)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const isMerchant = form.clientType === 'MERCHANT'

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const submitRegistration = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!form.email.trim() || !form.password) {
      setStatus({ type: 'error', text: 'Complete the required fields before registering.' })
      return
    }

    if (form.password.length < 6) {
      setStatus({ type: 'error', text: 'Password must be at least 6 characters.' })
      return
    }

    if (isMerchant) {
      if (!/^\d{9}$|^\d{13}$/.test(form.eik.trim())) {
        setStatus({ type: 'error', text: 'Enter a 9-digit or 13-digit EIK.' })
        return
      }

      if (
        !form.companyName.trim() ||
        !form.representativeFirstName.trim() ||
        !form.representativeLastName.trim()
      ) {
        setStatus({
          type: 'error',
          text: 'Company name and representative names are required for merchants.',
        })
        return
      }
    } else {
      if (!/^\d{10}$/.test(form.ucn.trim())) {
        setStatus({ type: 'error', text: 'Enter a 10-digit UCN.' })
        return
      }

      if (!form.firstName.trim() || !form.lastName.trim()) {
        setStatus({ type: 'error', text: 'First name and last name are required.' })
        return
      }
    }

    if (form.password !== form.confirmPassword) {
      setStatus({ type: 'error', text: 'Passwords do not match.' })
      return
    }

    try {
      setIsSubmitting(true)
      const session = await requestJson('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          clientType: form.clientType,
          ucn: form.ucn.trim(),
          eik: form.eik.trim(),
          email: form.email.trim(),
          password: form.password,
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim(),
          companyName: form.companyName.trim(),
          representativeFirstName: form.representativeFirstName.trim(),
          representativeLastName: form.representativeLastName.trim(),
        }),
      })
      onRegister(session)
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-layout">
        <div className="auth-panel">
          <span className="eyebrow">Client registration</span>
          <h1>Create a secure BankSystem profile.</h1>
          <p>
            Create a customer or merchant profile for self-service access to account balances and
            credit history after login.
          </p>
          <div className="auth-panel__highlights">
            <div>
              <strong>Client workspace</strong>
              <p>Review your accounts and loan history immediately after signing in.</p>
            </div>
            <div>
              <strong>Immediate access</strong>
              <p>Successful registration signs the user in and opens the workspace.</p>
            </div>
            <div>
              <strong>Flexible onboarding</strong>
              <p>Register with either personal customer details or merchant company details.</p>
            </div>
          </div>
        </div>

        <section className="auth-card" aria-labelledby="register-title">
          <div className="section-heading">
            <span className="eyebrow">Register</span>
            <h2 id="register-title">Create account</h2>
          </div>
          <form className="auth-form" onSubmit={submitRegistration}>
            <label>
              Client type
              <select name="clientType" value={form.clientType} onChange={updateField}>
                <option value="CUSTOMER">Customer</option>
                <option value="MERCHANT">Merchant</option>
              </select>
            </label>
            {isMerchant ? (
              <>
                <label>
                  EIK
                  <input
                    name="eik"
                    value={form.eik}
                    onChange={updateField}
                    inputMode="numeric"
                    placeholder="123456789 or 1234567890123"
                    autoComplete="off"
                  />
                </label>
                <label>
                  Company name
                  <input
                    name="companyName"
                    value={form.companyName}
                    onChange={updateField}
                    placeholder="Acme Trading Ltd"
                  />
                </label>
                <label>
                  Representative first name
                  <input
                    name="representativeFirstName"
                    value={form.representativeFirstName}
                    onChange={updateField}
                    autoComplete="given-name"
                    placeholder="Ivan"
                  />
                </label>
                <label>
                  Representative last name
                  <input
                    name="representativeLastName"
                    value={form.representativeLastName}
                    onChange={updateField}
                    autoComplete="family-name"
                    placeholder="Petrov"
                  />
                </label>
              </>
            ) : (
              <>
                <label>
                  UCN
                  <input
                    name="ucn"
                    value={form.ucn}
                    onChange={updateField}
                    inputMode="numeric"
                    placeholder="1234567890"
                    autoComplete="off"
                  />
                </label>
                <label>
                  First name
                  <input
                    name="firstName"
                    value={form.firstName}
                    onChange={updateField}
                    autoComplete="given-name"
                    placeholder="Ivan"
                  />
                </label>
                <label>
                  Last name
                  <input
                    name="lastName"
                    value={form.lastName}
                    onChange={updateField}
                    autoComplete="family-name"
                    placeholder="Petrov"
                  />
                </label>
              </>
            )}
            <label>
              Email
              <input
                name="email"
                type="email"
                value={form.email}
                onChange={updateField}
                autoComplete="email"
                placeholder="user@gmail.com"
              />
            </label>
            <label>
              Password
              <input
                name="password"
                type="password"
                value={form.password}
                onChange={updateField}
                autoComplete="new-password"
              />
            </label>
            <label>
              Confirm password
              <input
                name="confirmPassword"
                type="password"
                value={form.confirmPassword}
                onChange={updateField}
                autoComplete="new-password"
              />
            </label>
            {status.text && <StatusNotice type={status.type} text={status.text} />}
            <div className="actions">
              <button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Creating account...' : 'Register'}
              </button>
            </div>
          </form>
          <p className="auth-card__footer">
            Already registered?{' '}
            <button type="button" className="inline-link" onClick={() => onNavigate('login')}>
              Go to login
            </button>
            .
          </p>
        </section>
      </section>
    </main>
  )
}

export default RegisterPage
