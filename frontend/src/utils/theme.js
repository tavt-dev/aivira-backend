export const THEME_KEY = "aivira_theme";
export const THEMES = ["light", "dark"];

export function getTheme() {
  const stored = localStorage.getItem(THEME_KEY);
  return THEMES.includes(stored) ? stored : "light";
}

export function applyTheme(theme = getTheme()) {
  const nextTheme = THEMES.includes(theme) ? theme : "light";
  document.documentElement.classList.toggle("dark", nextTheme === "dark");
  document.documentElement.dataset.theme = nextTheme;
  localStorage.setItem(THEME_KEY, nextTheme);
  return nextTheme;
}

export function setTheme(theme) {
  const nextTheme = applyTheme(theme);
  window.dispatchEvent(new CustomEvent("aivira-theme", { detail: { theme: nextTheme } }));
  return nextTheme;
}

export function toggleTheme() {
  return setTheme(getTheme() === "dark" ? "light" : "dark");
}
