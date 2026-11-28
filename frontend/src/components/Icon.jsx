const paths = {
  car: <><path d="m5 10 1.5-4h11L19 10M3 10h18v8H3zM6 18v2m12-2v2M6 13h2m8 0h2" /><path d="M4 7H2m20 0h-2" /></>,
  grid: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 11h18m-14 4h2m4 0h2" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  users: <><circle cx="9" cy="8" r="3" /><path d="M3 21v-2a6 6 0 0 1 12 0v2m2-16a3 3 0 0 1 0 6m1 4a5 5 0 0 1 3 4v2" /></>,
  plus: <path d="M12 5v14M5 12h14" />,
  arrow: <path d="M5 12h14m-6-6 6 6-6 6" />,
  back: <path d="M19 12H5m6-6-6 6 6 6" />,
  edit: <><path d="m15 5 4 4M4 20l5-1L20 8a2.8 2.8 0 0 0-4-4L5 15z" /><path d="M13 20h8" /></>,
  search: <><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 5 5" /></>,
  check: <path d="m5 12 4 4L19 6" />,
  close: <path d="m6 6 12 12M6 18 18 6" />,
  info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v6M12 7h.01" /></>,
  eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z" /><circle cx="12" cy="12" r="2.5" /></>,
  'eye-off': <><path d="m3 3 18 18M10 6.2c.7-.1 1.3-.2 2-.2 6.5 0 10 6 10 6a17 17 0 0 1-3.4 3.7M6.2 7.8C3.4 9.5 2 12 2 12s3.5 6 10 6c1.3 0 2.5-.2 3.6-.6" /></>,
  gauge: <><path d="M4 19a9 9 0 1 1 16 0M12 13l5-5" /><circle cx="12" cy="13" r="1" /></>,
};

export default function Icon({ name, size = 20, className = '' }) {
  return <svg width={size} height={size} className={className} viewBox="0 0 24 24"
    fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"
    strokeLinejoin="round" aria-hidden="true" focusable="false">{paths[name] || paths.car}</svg>;
}
