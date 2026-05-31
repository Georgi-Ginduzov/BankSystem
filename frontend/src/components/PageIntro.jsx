function PageIntro({ eyebrow, title, text, children }) {
  const titleId = `${eyebrow.toLowerCase().replace(/[^a-z0-9]+/gi, '-')}-title`

  return (
    <section className="page-intro" aria-labelledby={titleId}>
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h1 id={titleId}>{title}</h1>
        <p>{text}</p>
      </div>
      {children}
    </section>
  )
}

export default PageIntro
