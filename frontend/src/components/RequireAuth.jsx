import { Navigate, useLocation } from "react-router-dom";
import { hasAccessToken } from "../utils/storage.js";

export default function RequireAuth({ children }) {
  const location = useLocation();

  if (!hasAccessToken()) {
    const next = encodeURIComponent(`${location.pathname}${location.search}${location.hash}`);
    return <Navigate to={`/?auth=login&next=${next}`} replace />;
  }

  return children;
}
