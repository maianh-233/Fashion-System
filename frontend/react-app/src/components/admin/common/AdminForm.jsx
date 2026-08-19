export function AdminFormSection({ title, description, children, className = "" }) {
  return (
    <section className={`admin-form-section ${className}`}>
      <div className="admin-form-section__heading">
        <h3 className="admin-form-section__title">{title}</h3>
        {description && <p className="admin-form-section__description">{description}</p>}
      </div>
      {children}
    </section>
  );
}

export function AdminField({ label, as = "input", children, className = "", containerClassName = "", ...props }) {
  const Control = as;

  return (
    <label className={`admin-field ${containerClassName}`}>
      <span className="admin-field__label">{label}</span>
      <Control className={`admin-field__control ${className}`} {...props}>
        {children}
      </Control>
    </label>
  );
}
