const paths = {
  stay: <><path d="M3 21V7l9-4 9 4v14M3 21h18M9 21v-5h6v5M7 9h1m8 0h1M7 12h1m8 0h1" /></>,
  user: <><circle cx="12" cy="8" r="4" /><path d="M4 21v-2a8 8 0 0 1 16 0v2" /></>,
  lock: <><rect x="4" y="10" width="16" height="11" rx="3" /><path d="M8 10V7a4 4 0 0 1 8 0v3m-4 5v2" /></>,
};
export default function AccountIcon({ name }) {
  return <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name] || paths.stay}</svg>;
}
