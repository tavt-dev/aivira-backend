import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { addCartItem } from "../api/cartApi.js";
import { getProduct } from "../api/catalogApi.js";
import { discount, formatSold, formatVND } from "../utils/formatters.js";
import { normalizeBook } from "../utils/mappers.js";
import { getAccessToken } from "../utils/storage.js";

export default function ProductPage({ onAuth }) {
  const { slug } = useParams();
  const [book, setBook] = useState(null);
  const [message, setMessage] = useState("");
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    let alive = true;
    getProduct(slug)
      .then((data) => {
        if (alive) {
          setBook(normalizeBook(data));
          setMessage("");
        }
      })
      .catch((error) => {
        if (alive) {
          setBook(null);
          setMessage(error.message || "Product not found or backend unavailable.");
        }
      });
    return () => {
      alive = false;
    };
  }, [slug]);

  async function addToCart() {
    if (!getAccessToken()) {
      onAuth();
      return;
    }
    const variationId = book.productVariationId || book.variations?.[0]?.id;
    if (!variationId) {
      alert("This product has no backend variation to add to cart.");
      return;
    }
    try {
      await addCartItem({ productVariationId: variationId, quantity });
      alert("Added to cart.");
      window.dispatchEvent(new Event("aivira-cart"));
    } catch (error) {
      alert(error.message || "Add to cart failed.");
    }
  }

  if (!book) {
    return <div className="page-shell"><div className="notice">{message || "Loading book..."}</div></div>;
  }

  return (
    <div className="prod-page">
      <div className="bc"><Link to="/">Home</Link> / <Link to={`/category/${book.cat}`}>{book.catLabel}</Link> / <span>{book.title}</span></div>
      <div className="p-card">
        <div className="p-img-box"><img src={book.image} alt={book.title} className="p-img" /></div>
        <div className="p-info">
          <div className="p-cat">{book.catLabel}</div>
          <h1 className="p-ttl">{book.title}</h1>
          <div className="p-auth">by {book.author}</div>
          <div className="p-stats"><span>{book.rating.toFixed(1)} rating</span><span>{formatSold(book.sold)} sold</span><span>Backend</span></div>
          <div className="p-price-box">
            <span className="p-old">{formatVND(book.priceOld)}</span>
            <span className="p-new">{formatVND(book.price)}</span>
            <span className="p-discount">{discount(book)}% OFF</span>
          </div>
          <p className="p-desc">{book.desc}</p>
          <div className="qty-row">
            <label>Quantity</label>
            <input type="number" min="1" value={quantity} onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))} />
          </div>
          <div className="p-actions">
            <button className="btn-cart" onClick={addToCart}>Add To Cart</button>
            <Link className="btn-buy" to="/checkout">Checkout</Link>
          </div>
        </div>
      </div>
      <Reviews book={book} onAuth={onAuth} />
    </div>
  );
}

function Reviews() {
  return (
    <div className="p-reviews">
      <h3>Customer Reviews</h3>
      <div className="notice">Review API is not implemented in the backend yet.</div>
    </div>
  );
}
