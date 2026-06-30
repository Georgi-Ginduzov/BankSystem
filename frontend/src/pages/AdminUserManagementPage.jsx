import { useEffect, useState } from 'react'
import PageIntro from '../components/PageIntro'
import StatusNotice from '../components/StatusNotice'
import { requestJson } from '../lib/api'

const initialAdminUserForm = {
  id: null,
  ucn: '',
  email: '',
  role: 'LOAN_OFFICER',
  password: '',
  firstName: '',
  lastName: '',
}

function AdminUserManagementPage({ session }) {
  const [query, setQuery] = useState('')
  const [users, setUsers] = useState([])
  const [form, setForm] = useState(initialAdminUserForm)
  const [status, setStatus] = useState({ type: '', text: '' })
  const [isSaving, setIsSaving] = useState(false)
  const [isDeletingId, setIsDeletingId] = useState(null)
  const isAdmin = session?.role === 'ADMIN'

  const loadUsers = async (nextQuery = query) => {
    const search = nextQuery.trim()
    const suffix = search ? `?query=${encodeURIComponent(search)}` : ''
    const data = await requestJson(`/admin/users${suffix}`)
    setUsers(data)
  }

  useEffect(() => {
    if (!isAdmin) {
      return
    }
    loadUsers('').catch((error) => setStatus({ type: 'error', text: error.message }))
  }, [isAdmin])

  if (!isAdmin) {
    return (
      <main className="work-page">
        <section className="page-status">
          <StatusNotice type="error" text="Only admins can access user management." />
        </section>
      </main>
    )
  }

  const isEditing = Boolean(form.id)
  const isCustomerForm = form.role === 'CUSTOMER'

  const resetForm = () => {
    setForm(initialAdminUserForm)
  }

  const updateFormField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const handleSearch = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    try {
      await loadUsers(query)
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    }
  }

  const handleSave = async (event) => {
    event.preventDefault()
    setStatus({ type: '', text: '' })

    if (!/^\d{10}$/.test(form.ucn.trim())) {
      setStatus({ type: 'error', text: 'UCN must contain exactly 10 digits.' })
      return
    }

    if (!form.email.trim()) {
      setStatus({ type: 'error', text: 'Email is required.' })
      return
    }

    if (!isEditing && form.password.trim().length < 6) {
      setStatus({ type: 'error', text: 'New users require a password of at least 6 characters.' })
      return
    }

    if (isCustomerForm && (!form.firstName.trim() || !form.lastName.trim())) {
      setStatus({ type: 'error', text: 'Customer users require first and last name.' })
      return
    }

    try {
      setIsSaving(true)
      const payload = {
        ucn: form.ucn.trim(),
        email: form.email.trim(),
        role: form.role,
        password: form.password.trim(),
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
      }

      await requestJson(isEditing ? `/admin/users/${form.id}` : '/admin/users', {
        method: isEditing ? 'PATCH' : 'POST',
        body: JSON.stringify(payload),
      })

      setStatus({
        type: 'success',
        text: isEditing ? 'User was updated successfully.' : 'User was created successfully.',
      })
      resetForm()
      await loadUsers(query)
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsSaving(false)
    }
  }

  const startEditing = (user) => {
    setForm({
      id: user.id,
      ucn: user.ucn,
      email: user.email,
      role: user.role,
      password: '',
      firstName: user.firstName ?? '',
      lastName: user.lastName ?? '',
    })
    setStatus({ type: '', text: '' })
  }

  const deleteUser = async (userId) => {
    setStatus({ type: '', text: '' })

    try {
      setIsDeletingId(userId)
      await requestJson(`/admin/users/${userId}`, { method: 'DELETE' })
      setStatus({ type: 'success', text: 'User was deleted successfully.' })
      if (form.id === userId) {
        resetForm()
      }
      await loadUsers(query)
    } catch (error) {
      setStatus({ type: 'error', text: error.message })
    } finally {
      setIsDeletingId(null)
    }
  }

  return (
    <main className="work-page">
      <PageIntro
        eyebrow="Admin"
        title="User management"
        text="Search, create, update, and remove user accounts from a dedicated admin workspace."
      >
        <div className="status-panel" aria-label="Admin summary">
          <div>
            <span>Signed in role</span>
            <strong>{session?.role ?? 'ADMIN'}</strong>
          </div>
          <div>
            <span>Loaded users</span>
            <strong>{users.length}</strong>
          </div>
          <div>
            <span>Search query</span>
            <strong>{query.trim() || 'All users'}</strong>
          </div>
        </div>
      </PageIntro>

      <section className="single-workspace" aria-label="Admin user management workspace">
        {status.text && <StatusNotice type={status.type} text={status.text} />}
        <div className="dashboard-form-grid">
          <section className="dashboard-form-card">
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Search</span>
              <h2>Find users</h2>
            </div>
            <form className="admin-search-form" onSubmit={handleSearch}>
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search by email, UCN, role, or name"
              />
              <button type="submit">Search</button>
            </form>
            <div className="admin-user-list">
              {users.length ? (
                users.map((user) => (
                  <article key={user.id} className="admin-user-card">
                    <span>{user.role}</span>
                    <strong>{user.email}</strong>
                    <p>UCN: {user.ucn}</p>
                    {(user.firstName || user.lastName) && (
                      <small>
                        {user.firstName ?? ''} {user.lastName ?? ''}
                      </small>
                    )}
                    <div className="admin-user-card__actions">
                      <button type="button" className="button-secondary" onClick={() => startEditing(user)}>
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => deleteUser(user.id)}
                        disabled={isDeletingId === user.id}
                      >
                        {isDeletingId === user.id ? 'Deleting...' : 'Delete'}
                      </button>
                    </div>
                  </article>
                ))
              ) : (
                <div className="empty-card">No users matched the current search.</div>
              )}
            </div>
          </section>

          <form className="dashboard-form-card" onSubmit={handleSave}>
            <div className="section-heading section-heading--compact">
              <span className="eyebrow">Editor</span>
              <h2>{isEditing ? 'Modify user' : 'Create user'}</h2>
            </div>
            <div className="field-grid">
              <label>
                UCN
                <input
                  name="ucn"
                  value={form.ucn}
                  onChange={updateFormField}
                  placeholder="1234567890"
                  disabled={isEditing}
                />
              </label>
              <label>
                Email
                <input
                  name="email"
                  type="email"
                  value={form.email}
                  onChange={updateFormField}
                  placeholder="user@bank.com"
                />
              </label>
              <label>
                Role
                <select name="role" value={form.role} onChange={updateFormField}>
                  <option value="ADMIN">Admin</option>
                  <option value="LOAN_OFFICER">Loan officer</option>
                  <option value="MANAGER">Manager</option>
                  <option value="CUSTOMER">Customer</option>
                </select>
              </label>
              <label>
                {isEditing ? 'New password (optional)' : 'Password'}
                <input
                  name="password"
                  type="password"
                  value={form.password}
                  onChange={updateFormField}
                  placeholder={isEditing ? 'Leave blank to keep current password' : 'At least 6 characters'}
                />
              </label>
              {isCustomerForm && (
                <>
                  <label>
                    First name
                    <input
                      name="firstName"
                      value={form.firstName}
                      onChange={updateFormField}
                      placeholder="Ivan"
                    />
                  </label>
                  <label>
                    Last name
                    <input
                      name="lastName"
                      value={form.lastName}
                      onChange={updateFormField}
                      placeholder="Petrov"
                    />
                  </label>
                </>
              )}
            </div>
            <div className="actions actions--split">
              <button type="button" className="button-secondary" onClick={resetForm}>
                Reset
              </button>
              <button type="submit" disabled={isSaving}>
                {isSaving ? 'Saving...' : isEditing ? 'Save changes' : 'Create user'}
              </button>
            </div>
          </form>
        </div>
      </section>
    </main>
  )
}

export default AdminUserManagementPage
