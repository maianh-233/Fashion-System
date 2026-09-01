export default function CustomerPageIntro({ eyebrow, title, description, meta, children }) {
  return (
    <header className="customer-page-intro">
      <div className="customer-page-intro__copy">
        <p>{eyebrow}</p>
        <h1>{title}</h1>
        {description && <span>{description}</span>}
      </div>
      {meta && <strong className="customer-page-intro__meta">{meta}</strong>}
      {children && <div className="customer-page-intro__tools">{children}</div>}
    </header>
  );
}
