
// KpiCard — animated metric tile with accent color and icon.
export default function KpiCard({ label, value, sub, icon, color = 'accent' }) {
  return (
    <div className={`kpi-card ${color}`}>
      <div className="kpi-header">
        <span className="kpi-label">{label}</span>
        <span className="kpi-icon">{icon}</span>
      </div>
      <div className={`kpi-value ${color}`}>{value ?? '—'}</div>
      {sub && <div className="kpi-sub">{sub}</div>}
    </div>
  );
}
