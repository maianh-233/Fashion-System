export default function AdminCatalogPageHeader({
  icon: Icon,
  eyebrow = "Quản trị danh mục",
  title,
  description,
  status,
  action,
}) {
  return (
    <header className="admin-catalog-header">
      <div className="admin-catalog-header__identity">
        <span className="admin-catalog-header__icon">
          <Icon size={20} aria-hidden="true" />
        </span>
        <div>
          <span className="admin-catalog-header__eyebrow">{eyebrow}</span>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
      </div>
      {action ? (
        <div className="admin-catalog-header__action">{action}</div>
      ) : (
        status && <div className="admin-catalog-header__status">{status}</div>
      )}
    </header>
  );
}
