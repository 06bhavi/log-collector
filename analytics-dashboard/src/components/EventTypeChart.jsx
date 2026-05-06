import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Cell,
} from 'recharts';

const TOOLTIP_STYLE = {
  backgroundColor: '#1a2235',
  border: '1px solid rgba(255,255,255,0.08)',
  borderRadius: 8,
  fontSize: '0.78rem',
  color: '#f1f5f9',
};

// Colour palette cycling through brand palette
const COLOURS = [
  '#6366f1', '#818cf8', '#a5b4fc',
  '#10b981', '#34d399', '#6ee7b7',
  '#f59e0b', '#fbbf24', '#fde68a',
  '#3b82f6',
];

/**
 * EventTypeChart — vertical bar chart showing how many events occurred per
 * action type. Bars are individually coloured for quick visual scanning.
 */
export default function EventTypeChart({ eventTypeCounts = {} }) {
  const data = Object.entries(eventTypeCounts)
    .map(([action, count]) => ({ action: action.replace(/_/g, ' '), count }))
    .sort((a, b) => b.count - a.count);

  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={data} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" vertical={false} />
        <XAxis
          dataKey="action"
          tick={{ fill: '#475569', fontSize: 10 }}
          tickLine={false}
          axisLine={false}
        />
        <YAxis
          tick={{ fill: '#475569', fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          allowDecimals={false}
        />
        <Tooltip contentStyle={TOOLTIP_STYLE} cursor={{ fill: 'rgba(255,255,255,0.03)' }} />
        <Bar dataKey="count" name="Events" radius={[4, 4, 0, 0]} maxBarSize={48}>
          {data.map((_, i) => (
            <Cell key={i} fill={COLOURS[i % COLOURS.length]} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
