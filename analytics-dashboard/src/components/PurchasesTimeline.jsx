import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Legend,
} from 'recharts';

const TOOLTIP_STYLE = {
  backgroundColor: '#1a2235',
  border: '1px solid rgba(255,255,255,0.08)',
  borderRadius: 8,
  fontSize: '0.78rem',
  color: '#f1f5f9',
};

/**
 * PurchasesTimeline — stacked area chart showing purchases vs. all events
 * over the last 30 minutes in 1-minute buckets.
 */
export default function PurchasesTimeline({ purchasesPerMinute = [], eventsPerMinute = [] }) {
  // Merge the two series on the `minute` key
  const minuteMap = {};
  eventsPerMinute.forEach(({ minute, count }) => {
    minuteMap[minute] = { minute, allEvents: count, purchases: 0 };
  });
  purchasesPerMinute.forEach(({ minute, count }) => {
    if (minuteMap[minute]) minuteMap[minute].purchases = count;
    else minuteMap[minute] = { minute, allEvents: 0, purchases: count };
  });

  const data = Object.values(minuteMap).sort((a, b) =>
    a.minute.localeCompare(b.minute)
  );

  return (
    <ResponsiveContainer width="100%" height={240}>
      <AreaChart data={data} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
        <defs>
          <linearGradient id="gradAll" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%"  stopColor="#6366f1" stopOpacity={0.3} />
            <stop offset="95%" stopColor="#6366f1" stopOpacity={0.02} />
          </linearGradient>
          <linearGradient id="gradPurchase" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%"  stopColor="#10b981" stopOpacity={0.4} />
            <stop offset="95%" stopColor="#10b981" stopOpacity={0.02} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.04)" />
        <XAxis
          dataKey="minute"
          tick={{ fill: '#475569', fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          interval="preserveStartEnd"
        />
        <YAxis
          tick={{ fill: '#475569', fontSize: 11 }}
          tickLine={false}
          axisLine={false}
          allowDecimals={false}
        />
        <Tooltip contentStyle={TOOLTIP_STYLE} />
        <Legend
          iconType="circle"
          iconSize={8}
          wrapperStyle={{ fontSize: '0.75rem', paddingTop: 12 }}
        />
        <Area
          type="monotone"
          dataKey="allEvents"
          name="All Events"
          stroke="#6366f1"
          strokeWidth={2}
          fill="url(#gradAll)"
          dot={false}
          activeDot={{ r: 4, fill: '#6366f1' }}
        />
        <Area
          type="monotone"
          dataKey="purchases"
          name="Purchases"
          stroke="#10b981"
          strokeWidth={2}
          fill="url(#gradPurchase)"
          dot={false}
          activeDot={{ r: 4, fill: '#10b981' }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
