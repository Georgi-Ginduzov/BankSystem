export const loanTypes = [
  {
    value: 'PERSONAL',
    label: 'Потребителски кредит',
    minAmount: 500,
    maxAmount: 50000,
    minMonths: 6,
    maxMonths: 84,
    rate: '7.9%',
  },
  {
    value: 'MORTGAGE',
    label: 'Ипотечен кредит',
    minAmount: 20000,
    maxAmount: 500000,
    minMonths: 60,
    maxMonths: 360,
    rate: '4.2%',
  },
  {
    value: 'AUTO',
    label: 'Автокредит',
    minAmount: 3000,
    maxAmount: 100000,
    minMonths: 12,
    maxMonths: 120,
    rate: '5.8%',
  },
]
