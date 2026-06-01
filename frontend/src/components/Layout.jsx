import { Outlet, useLocation } from "react-router-dom";
import Footer from "./Footer.jsx";
import Navbar from "./Navbar.jsx";

export default function Layout({ user, onAuth }) {
  const location = useLocation();
  const isHome = location.pathname === "/";
  return (
    <>
      <Navbar solid={!isHome} user={user} onAuth={onAuth} />
      <main><Outlet /></main>
      <Footer />
    </>
  );
}
