import { useEffect, useState } from 'react'
import './App.css'
import { pages } from './data/pages'
import ClientProfilesPage from './pages/ClientProfilesPage'
import HomePage from './pages/HomePage'
import LoanApplicationsPage from './pages/LoanApplicationsPage'
import LoanManagementPage from './pages/LoanManagementPage'
import NewAccountPage from './pages/NewAccountPage'

const pageIds = new Set(pages.map((page) => page.id))

const getPageFromHash = () => {
  const id = window.location.hash.replace(/^#\/?/, '') || 'home'
  return pageIds.has(id) ? id : 'home'
}

function App() {
  const [activePage, setActivePage] = useState(getPageFromHash)

  useEffect(() => {
    const handleHashChange = () => setActivePage(getPageFromHash())
    window.addEventListener('hashchange', handleHashChange)
    return () => window.removeEventListener('hashchange', handleHashChange)
  }, [])

  const navigate = (pageId) => {
    const page = pages.find((item) => item.id === pageId) ?? pages[0]
    window.location.hash = page.hash
    setActivePage(page.id)
  }

  return (
    <div className="app-shell">
      <header className="top-bar">
        <button className="brand-button" type="button" onClick={() => navigate('home')}>
          BankSystem
        </button>
        <nav className="main-nav" aria-label="Основна навигация">
          {pages.map((page) => (
            <button
              key={page.id}
              type="button"
              className={activePage === page.id ? 'nav-button nav-button--active' : 'nav-button'}
              onClick={() => navigate(page.id)}
            >
              {page.label}
            </button>
          ))}
        </nav>
      </header>

      {activePage === 'home' && <HomePage onNavigate={navigate} />}
      {activePage === 'loan-applications' && <LoanApplicationsPage />}
      {activePage === 'new-account' && <NewAccountPage />}
      {activePage === 'client-profiles' && <ClientProfilesPage />}
      {activePage === 'loan-management' && <LoanManagementPage />}
    </div>
  )
}

export default App
