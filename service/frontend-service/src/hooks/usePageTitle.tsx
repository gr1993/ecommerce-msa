export function usePageTitle() {
  const path = window.location.pathname;
  const isAdmin = path.startsWith("/admin");
  document.title = isAdmin ? "박신사몰 관리자" : "박신사몰";
}