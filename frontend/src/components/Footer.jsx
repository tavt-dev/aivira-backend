import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer>
      <div>
        <span className="ft-logo">AIV<span>IRA</span></span>
        <p>Aivira single-vendor online bookstore. Browse, checkout, pay, and track orders from one catalog.</p>
      </div>
      <div>
        <h4>Categories</h4>
        <Link to="/category/business">Business</Link>
        <Link to="/category/self-help">Self-help</Link>
        <Link to="/category/literature">Literature</Link>
      </div>
      <div>
        <h4>Aivira</h4>
        <Link to="/account">Account</Link>
        <Link to="/orders">Orders</Link>
        <Link to="/admin/products">Admin</Link>
      </div>
      <div>
        <h4>Contact</h4>
        <p>hello@aivira.vn</p>
        <p>Hanoi, Vietnam</p>
      </div>
    </footer>
  );
}
