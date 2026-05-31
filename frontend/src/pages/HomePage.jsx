function HomePage({ onNavigate }) {
  const services = [
    {
      page: 'loan-applications',
      label: 'Кандидатури за кредит',
      value: 'Нова заявка',
      detail: 'Форма, Pending статус и одобрение',
    },
    {
      page: 'new-account',
      label: 'Нова сметка',
      value: 'Откриване',
      detail: 'Клиент, валута и начално салдо',
    },
    {
      page: 'client-profiles',
      label: 'Клиентски профили',
      value: 'Профили',
      detail: 'Търсене, създаване и редакция',
    },
    {
      page: 'loan-management',
      label: 'Кредитни договори',
      value: 'Откриване и промяна',
      detail: 'Операции след одобрение',
    },
  ]

  return (
    <main className="home-page">
      <section className="home-hero" aria-labelledby="home-title">
        <div>
          <span className="eyebrow">Банков портал</span>
          <h1 id="home-title">Работно табло</h1>
          <p>
            Централна страница за клиентски заявки, банкови услуги и служителски
            операции.
          </p>
        </div>
        <button type="button" onClick={() => onNavigate('loan-applications')}>
          Кандидатстване за кредит
        </button>
      </section>

      <section className="home-grid" aria-label="Банкови модули">
        {services.map((service) => (
          <button
            key={service.page}
            type="button"
            className="service-card"
            onClick={() => onNavigate(service.page)}
          >
            <span>{service.label}</span>
            <strong>{service.value}</strong>
            <small>{service.detail}</small>
          </button>
        ))}
      </section>

      <section className="overview-band" aria-labelledby="overview-title">
        <div>
          <span className="eyebrow">Днес</span>
          <h2 id="overview-title">Активни работни потоци</h2>
        </div>
        <div className="metric-row">
          <div>
            <span>Pending кредити</span>
            <strong>0</strong>
          </div>
          <div>
            <span>Нови сметки</span>
            <strong>0</strong>
          </div>
          <div>
            <span>Профили за проверка</span>
            <strong>0</strong>
          </div>
          <div>
            <span>Активни кредити</span>
            <strong>0</strong>
          </div>
        </div>
      </section>
    </main>
  )
}

export default HomePage
