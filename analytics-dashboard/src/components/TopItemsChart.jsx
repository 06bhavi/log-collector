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

// Friendly product names derived from the product IDs used by mock-storefront
const PRODUCT_NAMES = {
  'prod-101': 'Wireless Earbuds Pro',
  'prod-102': 'Mechanical Keyboard',
  'prod-103': 'USB-C Hub 7-in-1',
  'prod-104': '4K Webcam',
  'prod-105': 'Laptop Stand',
  'prod-106': 'LED Desk Lamp',
  'prod-107': 'Portable SSD 1TB',
  'prod-108': 'Noise-Cancel. Headphones',
  'prod-109': 'Ergonomic Mouse',
  'prod-110': 'Monitor Stand',
};

/**
 * TopItemsChart — horizontal bar chart showing the top N products by view
 * count (or purchase count, depending on which dataset is passed).
*/
export default function TopItemsChart({ items = [], color = '#6366f1', label = 'Count' }) {
  const data = items.slice(0, 8).map(({ productId, count }) => ({
    name: PRODUCT_NAMES[productId] ?? productId,
    count,
  }));

  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart
        layout="vertical"
        data={data}
        margin={{ top: 4, right: 16, left: 8, bottom: 0 }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" horizontal={false} />
        <XAxis
          type="number"
          tick={{ fill: '#475569', fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          allowDecimals={false}
        />
        <YAxis
          type="category"
          dataKey="name"
          tick={{ fill: '#94a3b8', fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          width={140}
        />
        <Tooltip
          contentStyle={TOOLTIP_STYLE}
          cursor={{ fill: 'rgba(255,255,255,0.03)' }}
          formatter={(v) => [v, label]}
        />
        <Bar dataKey="count" name={label} radius={[0, 4, 4, 0]} maxBarSize={24}>
          {data.map((_, i) => (
            <Cell
              key={i}
              fill={color}
              fillOpacity={1 - i * 0.07}
            />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
