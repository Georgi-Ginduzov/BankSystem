import { useState } from 'react'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'

const initialForm = {
  email: 'admin@bank.com',
  password: 'admin123',
}

function LoginPage({ onLogin, onNavigate }) {
  const [form, setForm] = useState(initialForm)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const submitLogin = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!form.email.trim() || !form.password) {
      setStatus({ type: 'error', text: 'Enter both email and password.' })
      return
    }

    try {
      setIsSubmitting(true)
      const session = await requestJson('/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          email: form.email.trim(),
          password: form.password,
        }),
      })
      onLogin(session)
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
          <span className="eyebrow">Secure access</span>
          <h1>Sign in to BankSystem.</h1>
          <p>
            Customers can review their accounts and loan history, while employees can access
            analytics and operational banking tools from the same secure entry point.
          </p>
          <div className="auth-panel__highlights">
            <div>
              <strong>Customer dashboard</strong>
              <p>See account balances and your personal credit history right after login.</p>
            </div>
            <div>
              <strong>Employee analytics</strong>
              <p>Track portfolio activity, pending loans, and active banking operations.</p>
            </div>
            <div>
              <strong>Default demo access</strong>
              <p>Use `admin@bank.com` and `admin123` to test the flow right away.</p>
            </div>
          </div>
        </div>

        <section className="auth-card" aria-labelledby="login-title">
          <div className="section-heading">
            <span className="eyebrow">Login</span>
            <h2 id="login-title">Welcome back</h2>
          </div>
          <form className="auth-form" onSubmit={submitLogin}>
            <label>
              Email
              <input
                name="email"
                type="email"
                value={form.email}
                onChange={updateField}
                autoComplete="email"
              />
            </label>
            <label>
              Password
              <input
                name="password"
                type="password"
                value={form.password}
                onChange={updateField}
                autoComplete="current-password"
              />
            </label>
            {status.text && <StatusNotice type={status.type} text={status.text} />}
            <div className="actions">
              <button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Signing in...' : 'Sign in'}
              </button>
            </div>
          </form>
          <p className="auth-card__footer">
            Need a new account?{' '}
            <button type="button" className="inline-link" onClick={() => onNavigate('register')}>
              Create one here
            </button>
            .
          </p>
        </section>
      </section>
    </main>
  )
}

export default LoginPage
