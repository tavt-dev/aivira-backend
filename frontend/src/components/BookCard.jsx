import { Link } from "react-router-dom";
import { formatVND } from "../utils/formatters.js";

export default function BookCard({ book }) {
  return (
    <Link to={`/product/${book.slug}`} className="book-card">
      {book.badge && <div className="bk-badge">{book.badge}</div>}
      <div className="book-cover">
        <div className="bk-3d">
          <div className="bk-face">
            <img src={book.image} alt={book.title} />
            <div className="bk-ov"><span className="bk-btn">View Details</span></div>
          </div>
          <div className="bk-sp" />
          <div className="bk-sh" />
        </div>
      </div>
      <div className="bk-cat">{book.catLabel}</div>
      <h3>{book.title}</h3>
      <p>{book.author}</p>
      <div className="price-row">
        <strong>{formatVND(book.price)}</strong>
        <span>{formatVND(book.priceOld)}</span>
        <em>{Number(book.rating || 0).toFixed(1)}</em>
      </div>
    </Link>
  );
}
