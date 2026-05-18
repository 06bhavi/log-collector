import { useStats }           from './hooks/useStats';
import KpiCard                from './components/KpiCard';
import PurchasesTimeline      from './components/PurchasesTimeline';
import EventTypeChart         from './components/EventTypeChart';
import TopItemsChart          from './components/TopItemsChart';
import FunnelPie              from './components/FunnelPie';

// ── Helpers ────────────────────────────────────────────────────────────────────
function fmt(n) {
  if (n == null) return '—';
  return n >= 1_000_000
    ? `${(n / 1_000_000).toFixed(1)}M`
    : n >= 1_000
    ? `${(n / 1_000).toFixed(1)}K`
    : String(n);
}

function timeAgo(date) {
  if (!date) return '';
  const s = Math.round((Date.now() - date) / 1000);
  if (s < 5) return 'just now';
  if (s < 60) return `${s}s ago`;
  return `${Math.round(s / 60)}m ago`;
}

// ── Loading skeleton ───────────────────────────────────────────────────────────
function LoadingState() {
  return (
    <div className="state-overlay">
      <div className="spinner" />
      <div>
        <p className="state-title">Connecting to log-collector…</p>
        <p className="state-sub">Streaming aggregated analytics from HDFS</p>
      </div>
    </div>
  );
}

// ── Error state ────────────────────────────────────────────────────────────────
function ErrorState({ message }) {
  return (
    <div className="state-overlay">
      <div className="error-box">
        <h3>⚠️ Unable to load analytics</h3>
        <p>{message}</p>
        <p style={{ marginTop: 6, opacity: 0.7 }}>
          Ensure log-collector is running and CORS is configured.
        </p>
      </div>
    </div>
  );
}

// ── Main App ───────────────────────────────────────────────────────────────────
export default function App() {
  const { data, loading, error, lastFetch, refresh } = useStats();

  return (
    <div className="app-shell">

      {/* ── Top Navigation ──────────────────────────────────────────────── */}
      <header className="topbar">
        <div className="topbar-brand">
          <div className="topbar-logo">📊</div>
          <div>
            <div className="topbar-title">Analytics Dashboard</div>
            <div className="topbar-subtitle">E-Commerce Event Stream · HDFS</div>
          </div>
        </div>
        <div className="topbar-right">
          {lastFetch && (
            <span className="refresh-info">
              Updated {timeAgo(lastFetch)}
            </span>
          )}
          <div className="live-badge">
            <span className="live-dot" />
            Live
          </div>
          <button
            onClick={refresh}
            style={{
              background: 'rgba(99,102,241,0.15)',
              border: '1px solid rgba(99,102,241,0.3)',
              color: '#818cf8',
              borderRadius: 8,
              padding: '5px 12px',
              fontSize: '0.75rem',
              cursor: 'pointer',
              fontFamily: 'inherit',
              fontWeight: 600,
              transition: '0.15s',
            }}
            onMouseOver={e => e.currentTarget.style.background = 'rgba(99,102,241,0.28)'}
            onMouseOut={e  => e.currentTarget.style.background = 'rgba(99,102,241,0.15)'}
          >
            ↻ Refresh
          </button>
        </div>
      </header>

      {/* ── Page Body ───────────────────────────────────────────────────── */}
      <main className="page-content">

        {loading && <LoadingState />}
        {!loading && error && <ErrorState message={error} />}

        {!loading && !error && data && (
          <>
            {/* ── KPI Cards ─────────────────────────────────────────────── */}
            <p className="section-title">Key Metrics</p>
            <div className="kpi-grid">
              <KpiCard
                label="Total Events"
                value={fmt(data.totalEvents)}
                sub="All actions recorded in HDFS"
                icon="⚡"
                color="accent"
              />
              <KpiCard
                label="Purchases"
                value={fmt(data.totalPurchases)}
                sub={`${data.totalEvents ? ((data.totalPurchases/data.totalEvents)*100).toFixed(1) : 0}% conversion rate`}
                icon="💳"
                color="success"
              />
              <KpiCard
                label="Cart Adds"
                value={fmt(data.totalAddToCart)}
                sub={`${data.totalPurchases ? ((data.totalPurchases/data.totalAddToCart)*100).toFixed(1) : 0}% cart-to-purchase`}
                icon="🛒"
                color="warning"
              />
              <KpiCard
                label="Active Users"
                value={fmt(data.activeUsers)}
                sub="Distinct user IDs in log"
                icon="👥"
                color="info"
              />
            </div>

            {/* ── Activity Timeline ──────────────────────────────────────── */}
            <p className="section-title">Activity Timeline</p>
            <div style={{ marginBottom: 20 }}>
              <div className="chart-card">
                <div className="chart-header">
                  <div>
                    <div className="chart-title">Events &amp; Purchases · Last 30 Minutes</div>
                    <div className="chart-subtitle">1-minute local time buckets · auto-refreshes every 10s</div>
                  </div>
                  <span className="chart-badge">Live</span>
                </div>
                <PurchasesTimeline
                  purchasesPerMinute={data.purchasesPerMinute}
                  eventsPerMinute={data.eventsPerMinute}
                />
              </div>
            </div>

            {/* ── Event Breakdown + Funnel ───────────────────────────────── */}
            <p className="section-title">Event Breakdown</p>
            <div className="charts-grid">
              <div className="chart-card">
                <div className="chart-header">
                  <div>
                    <div className="chart-title">Event Type Distribution</div>
                    <div className="chart-subtitle">All time · total events per action</div>
                  </div>
                </div>
                <EventTypeChart eventTypeCounts={data.eventTypeCounts} />
              </div>
              <div className="chart-card">
                <div className="chart-header">
                  <div>
                    <div className="chart-title">Purchase Funnel</div>
                    <div className="chart-subtitle">View → Cart → Checkout → Purchase</div>
                  </div>
                </div>
                <FunnelPie eventTypeCounts={data.eventTypeCounts} />
              </div>
            </div>

            {/* ── Top Products ───────────────────────────────────────────── */}
            <p className="section-title">Product Performance</p>
            <div className="charts-grid">
              <div className="chart-card">
                <div className="chart-header">
                  <div>
                    <div className="chart-title">Top Viewed Products</div>
                    <div className="chart-subtitle">Ranked by item_viewed events · all time</div>
                  </div>
                </div>
                <TopItemsChart
                  items={data.topViewedItems}
                  color="#6366f1"
                  label="Views"
                />
              </div>
              <div className="chart-card">
                <div className="chart-header">
                  <div>
                    <div className="chart-title">Top Purchased Products</div>
                    <div className="chart-subtitle">Ranked by purchase events · all time</div>
                  </div>
                </div>
                <TopItemsChart
                  items={data.topPurchasedItems}
                  color="#10b981"
                  label="Purchases"
                />
              </div>
            </div>
          </>
        )}
      </main>


    </div>
  );
}
