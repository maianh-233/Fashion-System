export default function StatsCard({
  title,
  value,
  unit,
  icon,
  iconColor,
  change,
  changeColor,
}) {
  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800 hover:-translate-y-1 transition-all">
      <div className="flex justify-between">
        <div>
          <p className="text-zinc-400">{title}</p>

          <p className="text-3xl font-bold mt-2">
            {value}

            {unit && (
              <span className="text-base font-normal text-zinc-400 ml-1">
                {unit}
              </span>
            )}
          </p>
        </div>

        <div className={iconColor}>
          {icon}
        </div>
      </div>

      <p className={`text-sm mt-4 ${changeColor}`}>
        {change}
      </p>
    </div>
  );
}