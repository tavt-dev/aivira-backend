import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function Footer() {
  const { t } = useTranslation();
  const year = new Date().getFullYear();

  return (
    <footer className="tw-footer bg-slate-950 text-slate-400 py-16 px-4 md:px-8 border-t border-slate-900 border-white/5">
      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-4 gap-12">
        
        {/* LOGO & DESC */}
        <div className="col-span-1 md:col-span-1">
          <Link to="/" className="flex items-center gap-2 mb-4">
             <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-blue-600 to-blue-400 flex items-center justify-center">
                <span className="text-white font-serif font-bold text-lg leading-none">A</span>
             </div>
             <span className="font-serif font-bold text-xl tracking-wide text-white">AIVIRA</span>
          </Link>
          <p className="text-sm leading-relaxed mb-6">
            {t("footer.desc")}
          </p>
          <div className="flex gap-4">
             {/* Social links placeholder */}
             <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center hover:bg-blue-600 transition-colors text-white cursor-pointer"><svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M24 4.557c-.883.392-1.832.656-2.828.775 1.017-.609 1.798-1.574 2.165-2.724-.951.564-2.005.974-3.127 1.195-.897-.957-2.178-1.555-3.594-1.555-3.179 0-5.515 2.966-4.797 6.045-4.091-.205-7.719-2.165-10.148-5.144-1.29 2.213-.669 5.108 1.523 6.574-.806-.026-1.566-.247-2.229-.616-.054 2.281 1.581 4.415 3.949 4.89-.693.188-1.452.232-2.224.084.626 1.956 2.444 3.379 4.6 3.419-2.07 1.623-4.678 2.348-7.29 2.04 2.179 1.397 4.768 2.212 7.548 2.212 9.142 0 14.307-7.721 13.995-14.646.962-.695 1.797-1.562 2.457-2.549z"/></svg></div>
             <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center hover:bg-blue-600 transition-colors text-white cursor-pointer"><svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 0c-6.627 0-12 5.373-12 12s5.373 12 12 12 12-5.373 12-12-5.373-12-12-12zm3 8h-1.35c-.538 0-.65.221-.65.778v1.222h2l-.209 2h-1.791v7h-3v-7h-2v-2h2v-2.308c0-1.769.931-2.692 3.029-2.692h1.971v3z"/></svg></div>
          </div>
        </div>
        
        {/* LINKS */}
        <div>
          <h4 className="text-white font-bold tracking-wider uppercase text-sm mb-6">{t("common.categories")}</h4>
          <ul className="space-y-3 text-sm">
            <li><Link to="/category/business" className="hover:text-blue-400 transition-colors">{t("footer.business")}</Link></li>
            <li><Link to="/category/self-help" className="hover:text-blue-400 transition-colors">{t("footer.selfHelp")}</Link></li>
            <li><Link to="/category/literature" className="hover:text-blue-400 transition-colors">{t("footer.literature")}</Link></li>
            <li><Link to="/category/skills" className="hover:text-blue-400 transition-colors">{t("footer.skills")}</Link></li>
          </ul>
        </div>
        
        {/* AIVIRA */}
        <div>
          <h4 className="text-white font-bold tracking-wider uppercase text-sm mb-6">Aivira</h4>
          <ul className="space-y-3 text-sm">
            <li><Link to="/about" className="hover:text-blue-400 transition-colors">{t("footer.ourStory")}</Link></li>
            <li><Link to="/account" className="hover:text-blue-400 transition-colors">{t("common.account")}</Link></li>
            <li><Link to="/orders" className="hover:text-blue-400 transition-colors">{t("footer.orderTracking")}</Link></li>
            <li><Link to="/faq" className="hover:text-blue-400 transition-colors">FAQ</Link></li>
          </ul>
        </div>
        
        {/* CONTACT */}
        <div>
          <h4 className="text-white font-bold tracking-wider uppercase text-sm mb-6">{t("footer.contact")}</h4>
          <ul className="space-y-3 text-sm">
            <li className="flex items-start gap-3">
              <svg className="w-5 h-5 text-slate-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
              <span>tavantien786@gmail.com</span>
            </li>
            <li className="flex items-start gap-3">
              <svg className="w-5 h-5 text-slate-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
              <span>Hanoi, Vietnam</span>
            </li>
          </ul>
        </div>

      </div>
      
      <div className="max-w-7xl mx-auto mt-16 pt-8 border-t border-slate-800 text-center text-xs flex flex-col md:flex-row items-center justify-between gap-4">
         <p>{t("footer.rights", { year })}</p>
         <div className="flex gap-4">
            <a href="#" className="hover:text-white transition-colors">{t("footer.privacy")}</a>
            <a href="#" className="hover:text-white transition-colors">{t("footer.terms")}</a>
         </div>
      </div>
    </footer>
  );
}
