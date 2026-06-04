import { useTranslation } from "react-i18next";
import { LANGUAGES, setLanguage } from "../i18n.js";

export default function LanguageSwitcher({ compact = false, inverted = false }) {
  const { i18n } = useTranslation();
  const currentLanguage = i18n.language?.startsWith("en") ? "en" : "vi";

  return (
    <div
      className={[
        "inline-flex items-center rounded-full border p-1 text-xs font-black",
        inverted
          ? "border-white/15 bg-white/10 text-white/65"
          : "border-slate-200 bg-white/80 text-slate-500 shadow-sm",
      ].join(" ")}
      aria-label="Language"
    >
      {LANGUAGES.map((language) => {
        const active = currentLanguage === language;
        return (
          <button
            key={language}
            type="button"
            onClick={() => setLanguage(language)}
            className={[
              "rounded-full uppercase tracking-wider transition-colors",
              compact ? "px-2.5 py-1.5" : "px-3 py-2",
              active
                ? inverted
                  ? "bg-white text-slate-950"
                  : "bg-slate-950 text-white"
                : inverted
                  ? "hover:bg-white/10 hover:text-white"
                  : "hover:bg-slate-100 hover:text-slate-950",
            ].join(" ")}
            aria-pressed={active}
          >
            {language}
          </button>
        );
      })}
    </div>
  );
}
