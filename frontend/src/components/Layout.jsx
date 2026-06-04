import { Outlet, useLocation } from "react-router-dom";
import Footer from "./Footer.jsx";
import Navbar from "./Navbar.jsx";

export default function Layout({ user, onAuth }) {
  const location = useLocation();
  const isHome = location.pathname === "/";

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-900 selection:bg-blue-200">
      <Navbar solid={!isHome} user={user} onAuth={onAuth} />
      <main className="relative z-10 w-full flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
