import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend,
} from 'recharts';

const TOOLTIP_STYLE = {
  backgroundColor: '#1a2235',
  border: '1px solid rgba(255,255,255,0.08)',
  borderRadius: 8,
  fontSize: '0.78rem',
  color: '#f1f5f9',
};

const COLOURS = ['#6366f1', '#10b981', '#f59e0b', '#3b82f6', '#ef4444',
                  '#8b5cf6', '#ec4899', '#14b8a6'];

const RADIAN = Math.PI / 180;
function CustomLabel({ cx, cy, midAngle, innerRadius, outerRadius, percent }) {
  if (percent < 0.05) return null;
  const r = innerRadius + (outerRadius - innerRadius) * 0.55;
  const x = cx + r * Math.cos(-midAngle * RADIAN);
  const y = cy + r * Math.sin(-midAngle * RADIAN);
  return (
    <text x={x} y={y} fill="#fff" textAnchor="middle" dominantBaseline="central"
          fontSize={11} fontWeight={600}>
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  );
}

/**
 * FunnelPie — donut chart visualising the purchase-funnel breakdown:
 * what proportion of all events are views vs. cart-adds vs. purchases.
 */
export default function FunnelPie({ eventTypeCounts = {} }) {
  const funnelKeys = ['item_viewed', 'add_to_cart', 'checkout_started', 'purchase'];
  const data = funnelKeys
    .filter(k => eventTypeCounts[k])
    .map(k => ({
      name: k.replace(/_/g, ' '),
      value: eventTypeCounts[k],
    }));

  if (data.length === 0) {
    return (
      <div style={{ height: 220, display: 'flex', alignItems: 'center',
                    justifyContent: 'center', color: '#475569', fontSize: '0.8rem' }}>
        No funnel data yet
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={220}>
      <PieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          innerRadius={55}
          outerRadius={85}
          paddingAngle={3}
          dataKey="value"
          labelLine={false}
          label={CustomLabel}
        >
          {data.map((_, i) => (
            <Cell key={i} fill={COLOURS[i % COLOURS.length]} stroke="none" />
          ))}
        </Pie>
        <Tooltip contentStyle={TOOLTIP_STYLE} />
        <Legend
          iconType="circle"
          iconSize={8}
          wrapperStyle={{ fontSize: '0.72rem', paddingTop: 8 }}
        />
      </PieChart>
    </ResponsiveContainer>
  );
}
