import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import vi from "./locales/vi/translation.js";
import en from "./locales/en/translation.js";

export const LANGUAGE_KEY = "aivira_language";
export const LANGUAGES = ["vi", "en"];

export function getLanguage() {
  const stored = localStorage.getItem(LANGUAGE_KEY);
  return LANGUAGES.includes(stored) ? stored : "vi";
}

export function setLanguage(language) {
  const nextLanguage = LANGUAGES.includes(language) ? language : "vi";
  localStorage.setItem(LANGUAGE_KEY, nextLanguage);
  document.documentElement.lang = nextLanguage;
  return i18n.changeLanguage(nextLanguage);
}

i18n
  .use(initReactI18next)
  .init({
    resources: {
      vi: { translation: vi },
      en: { translation: en },
    },
    lng: getLanguage(),
    fallbackLng: "vi",
    interpolation: {
      escapeValue: false,
    },
  });

document.documentElement.lang = i18n.language || "vi";
i18n.on("languageChanged", (language) => {
  document.documentElement.lang = language;
  localStorage.setItem(LANGUAGE_KEY, language);
});

export default i18n;
