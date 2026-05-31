function StatusNotice({ type, text }) {
  if (!text) return null

  return <p className={`notice notice--${type === 'success' ? 'success' : 'error'}`}>{text}</p>
}

export default StatusNotice
