export default function StatCard({ label, value, tone = 'default' }) {
  const tones = {
    default: 'bg-white text-slate-900',
    success: 'bg-emerald-50 text-emerald-900',
    warning: 'bg-amber-50 text-amber-900',
    danger: 'bg-rose-50 text-rose-900',
  };

  return (
    <div className={`rounded-3xl border border-stone-200 p-5 ${tones[tone] || tones.default}`}>
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-4 text-3xl font-semibold">{value}</p>
    </div>
  );
}
