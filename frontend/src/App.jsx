import { useEffect, useState } from 'react'
import './App.css'
import { customerPages, employeePages } from './data/pages'
import { clearStoredSession, getStoredSession, storeSession } from './lib/auth'
import AccountManagementPage from './pages/AccountManagementPage'
import AdminUserManagementPage from './pages/AdminUserManagementPage'
import ClientProfilesPage from './pages/ClientProfilesPage'
import CustomerAccountsPage from './pages/CustomerAccountsPage'
import CustomerLoanDetailsPage from './pages/CustomerLoanDetailsPage'
import CustomerLoansPage from './pages/CustomerLoansPage'
import CustomerLoanRequestPage from './pages/CustomerLoanRequestPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import LoanManagementPage from './pages/LoanManagementPage'
import NewAccountPage from './pages/NewAccountPage'
import RegisterPage from './pages/RegisterPage'

const customerExtraPageIds = new Set(['customer-loan-request', 'customer-loan-details'])
const authPageIds = new Set(['login', 'register'])

const formatRoleLabel = (role) => role.toLowerCase().replace(/_/g, ' ')
const isCustomerRole = (session) => session?.role === 'CUSTOMER'

const getVisiblePages = (session) => {
  if (isCustomerRole(session)) {
    return customerPages
  }

  return employeePages.filter((page) => !page.roles || page.roles.includes(session?.role))
}
const getActiveNavPageId = (pageId) =>
  pageId === 'customer-loan-request' || pageId === 'customer-loan-details' ? 'loans' : pageId

const parseRouteFromHash = () => {
  const raw = window.location.hash.replace(/^#\/?/, '')
  const [path] = raw.split('?')

  if (!path) {
    return { pageId: '', params: {} }
  }

  if (path === 'customer-loan-request') {
    return { pageId: 'customer-loan-request', params: {} }
  }

  if (path.startsWith('customer-loans/')) {
    const loanId = path.slice('customer-loans/'.length)
    return { pageId: 'customer-loan-details', params: { loanId } }
  }

  return { pageId: path, params: {} }
}

const getHashForPage = (pageId, session, params = {}) => {
  if (pageId === 'customer-loan-request') {
    return '#/customer-loan-request'
  }

  if (pageId === 'customer-loan-details' && params.loanId) {
    return `#/customer-loans/${params.loanId}`
  }

  const protectedPage = getVisiblePages(session).find((page) => page.id === pageId)
  if (protectedPage) {
    return protectedPage.hash
  }

  return pageId === 'register' ? '#/register' : '#/login'
}

const getPageFromHash = (session) => {
  const isAuthenticated = Boolean(session)
  const route = parseRouteFromHash()
  const id = route.pageId || (isAuthenticated ? 'home' : 'login')

  if (isAuthenticated) {
    if (isCustomerRole(session) && customerExtraPageIds.has(id)) {
      return route
    }

    return {
      pageId: getVisiblePages(session).some((page) => page.id === id) ? id : 'home',
      params: route.params,
    }
  }

  return { pageId: authPageIds.has(id) ? id : 'login', params: {} }
}

function App() {
  const [session, setSession] = useState(() => getStoredSession())
  const [activeRoute, setActiveRoute] = useState(() => getPageFromHash(getStoredSession()))

  useEffect(() => {
    const resolvedRoute = getPageFromHash(session)
    setActiveRoute(resolvedRoute)

    const expectedHash = getHashForPage(resolvedRoute.pageId, session, resolvedRoute.params)
    if (window.location.hash !== expectedHash) {
      window.location.hash = expectedHash
    }
  }, [session])

  useEffect(() => {
    const handleHashChange = () => setActiveRoute(getPageFromHash(session))
    window.addEventListener('hashchange', handleHashChange)
    return () => window.removeEventListener('hashchange', handleHashChange)
  }, [session])

  const navigate = (pageId, params = {}) => {
    const visiblePages = getVisiblePages(session)
    const nextPage = session
      ? visiblePages.some((page) => page.id === pageId) || customerExtraPageIds.has(pageId)
        ? pageId
        : 'home'
      : authPageIds.has(pageId)
        ? pageId
        : 'login'

    window.location.hash = getHashForPage(nextPage, session, params)
    setActiveRoute({ pageId: nextPage, params })
  }

  const handleAuthenticated = (nextSession) => {
    storeSession(nextSession)
    setSession(nextSession)
  }

  const logout = () => {
    clearStoredSession()
    setSession(null)
  }

  return (
    <div className="app-shell">
      <header className="top-bar">
        <button
          className="brand-button"
          type="button"
          onClick={() => navigate(session ? 'home' : 'login')}
        >
          BankSystem
        </button>
        {session ? (
          <div className="top-bar__actions">
            <nav className="main-nav" aria-label="Main navigation">
              {getVisiblePages(session).map((page) => (
                <button
                  key={page.id}
                  type="button"
                  className={
                    getActiveNavPageId(activeRoute.pageId) === page.id
                      ? 'nav-button nav-button--active'
                      : 'nav-button'
                  }
                  onClick={() => navigate(page.id)}
                >
                  {page.label}
                </button>
              ))}
            </nav>
            <div className="session-badge" aria-label="Signed in user">
              <strong>{formatRoleLabel(session.role)}</strong>
              <span>{session.email}</span>
            </div>
            <button type="button" className="nav-button" onClick={logout}>
              Logout
            </button>
          </div>
        ) : (
          <nav className="main-nav" aria-label="Authentication navigation">
            <button
              type="button"
              className={activeRoute.pageId === 'login' ? 'nav-button nav-button--active' : 'nav-button'}
              onClick={() => navigate('login')}
            >
              Login
            </button>
            <button
              type="button"
              className={activeRoute.pageId === 'register' ? 'nav-button nav-button--active' : 'nav-button'}
              onClick={() => navigate('register')}
            >
              Register
            </button>
          </nav>
        )}
      </header>

      {!session && activeRoute.pageId === 'login' && (
        <LoginPage onLogin={handleAuthenticated} onNavigate={navigate} />
      )}
      {!session && activeRoute.pageId === 'register' && (
        <RegisterPage onRegister={handleAuthenticated} onNavigate={navigate} />
      )}

      {session && activeRoute.pageId === 'home' && <HomePage onNavigate={navigate} session={session} />}
      {session && activeRoute.pageId === 'accounts' && (
        <CustomerAccountsPage onNavigate={navigate} session={session} />
      )}
      {session && activeRoute.pageId === 'loans' && (
        <CustomerLoansPage onNavigate={navigate} session={session} />
      )}
      {session && activeRoute.pageId === 'customer-loan-request' && (
        <CustomerLoanRequestPage onNavigate={navigate} session={session} />
      )}
      {session && activeRoute.pageId === 'customer-loan-details' && (
        <CustomerLoanDetailsPage
          loanId={activeRoute.params.loanId}
          onNavigate={navigate}
          session={session}
        />
      )}
      {session && activeRoute.pageId === 'admin-users' && <AdminUserManagementPage session={session} />}
      {session && activeRoute.pageId === 'new-account' && <NewAccountPage onNavigate={navigateToPage} />}
      {session && activeRoute.pageId === 'account-management' && <AccountManagementPage />}
      {session && activeRoute.pageId === 'client-profiles' && <ClientProfilesPage />}
      {session && activeRoute.pageId === 'loan-management' && <LoanManagementPage />}
    </div>
  )
}

export default App
