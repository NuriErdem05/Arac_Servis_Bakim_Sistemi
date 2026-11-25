export default function PageHeading({ title, description, eyebrow = 'Randevu yönetimi', children }) {
  return <div className="page-heading"><div>{eyebrow && <p className="page-eyebrow">{eyebrow}</p>}<h1 id="page-title" tabIndex={-1}>{title}</h1>{description && <p className="page-description">{description}</p>}</div>{children && <div className="button-row">{children}</div>}</div>;
}
