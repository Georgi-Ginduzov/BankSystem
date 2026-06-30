export const employeePages = [
  { id: 'home', label: 'Начало', hash: '#/' },
  { id: 'new-account', label: 'Нова сметка', hash: '#/new-account' },
  { id: 'account-management', label: 'Управление сметки', hash: '#/account-management' },
  { id: 'client-profiles', label: 'Клиенти', hash: '#/client-profiles' },
  { id: 'loan-management', label: 'Кредити', hash: '#/loan-management', roles: ['LOAN_OFFICER', 'MANAGER'] },
  { id: 'admin-users', label: 'Потребители', hash: '#/admin-users', roles: ['ADMIN'] },
]

export const customerPages = [
  { id: 'home', label: 'Home', hash: '#/' },
  { id: 'accounts', label: 'Accounts', hash: '#/accounts' },
  { id: 'loans', label: 'Loans', hash: '#/loans' },
]
