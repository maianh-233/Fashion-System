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
    <article className="admin-stat-card bg-zinc-900 border border-zinc-800">
      <div className="admin-stat-card__main">
        <div>
          <p className="admin-stat-card__title text-zinc-400">{title}</p>

          <p className="admin-stat-card__value">
            {value}

            {unit && (
              <span className="text-base font-normal text-zinc-400 ml-1">
                {unit}
              </span>
            )}
          </p>
        </div>

        <div className={`admin-stat-card__icon ${iconColor}`}>
          {icon}
        </div>
      </div>

      <p className={`admin-stat-card__change ${changeColor}`}>
        {change}
      </p>
    </article>
  );
}
