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
      onAuth?.();
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
    return (
      <div className="mx-auto w-full max-w-5xl px-4 pb-20 pt-28 md:px-8">
        <div className="rounded-3xl border border-slate-200 bg-white px-8 py-16 text-center shadow-sm">
          <h1 className="font-serif text-3xl font-bold text-slate-950">
            {message || "Loading book..."}
          </h1>
        </div>
      </div>
    );
  }

  const hasDiscount = discount(book) > 0;

  return (
    <div className="mx-auto w-full max-w-7xl px-4 pb-20 pt-28 md:px-8">
      <div className="mb-8 flex flex-wrap items-center gap-2 text-sm font-semibold text-slate-500">
        <Link className="hover:text-blue-600" to="/">
          Home
        </Link>
        <span>/</span>
        <Link className="hover:text-blue-600" to={`/category/${book.cat || "all"}`}>
          {book.catLabel || "Books"}
        </Link>
        <span>/</span>
        <span className="text-slate-900">{book.title}</span>
      </div>

      <div className="grid gap-10 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm md:p-8 lg:grid-cols-[minmax(280px,440px)_1fr]">
        <div className="relative overflow-hidden rounded-2xl bg-slate-100">
          <div className="aspect-[3/4]">
            <img
              src={book.image || book.cover}
              alt={book.title}
              className="h-full w-full object-cover"
            />
          </div>
          <div className="absolute left-0 top-0 h-full w-3 bg-slate-950/80" />
        </div>

        <section className="flex min-w-0 flex-col justify-center">
          <span className="mb-4 inline-flex w-fit rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
            {book.catLabel || "Books"}
          </span>
          <h1 className="font-serif text-4xl font-bold leading-tight text-slate-950 md:text-6xl">
            {book.title}
          </h1>
          <p className="mt-3 text-lg font-medium text-slate-500">by {book.author}</p>

          <div className="mt-6 flex flex-wrap gap-3 text-sm font-bold text-slate-600">
            <span className="rounded-full bg-slate-100 px-4 py-2">
              {Number(book.rating || 0).toFixed(1)} rating
            </span>
            <span className="rounded-full bg-slate-100 px-4 py-2">
              {formatSold(book.sold)} sold
            </span>
            <span className="rounded-full bg-slate-100 px-4 py-2">Backend catalog</span>
          </div>

          <div className="mt-8 flex flex-wrap items-end gap-4">
            {hasDiscount && (
              <span className="text-lg font-semibold text-slate-400 line-through">
                {formatVND(book.priceOld)}
              </span>
            )}
            <span className="font-serif text-5xl font-bold text-slate-950">
              {formatVND(book.price)}
            </span>
            {hasDiscount && (
              <span className="rounded-full bg-orange-50 px-3 py-1 text-sm font-bold text-orange-600">
                {discount(book)}% off
              </span>
            )}
          </div>

          <p className="mt-8 max-w-2xl text-base leading-7 text-slate-600">
            {book.desc || "No description has been added for this book yet."}
          </p>

          <div className="mt-8 flex flex-wrap items-center gap-4">
            <label className="flex items-center gap-3 text-sm font-bold text-slate-600">
              Quantity
              <input
                type="number"
                min="1"
                value={quantity}
                onChange={(event) => setQuantity(Math.max(1, Number(event.target.value)))}
                className="w-24 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100"
              />
            </label>
          </div>

          <div className="mt-8 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={addToCart}
              className="rounded-full bg-slate-950 px-7 py-4 text-sm font-bold text-white transition-colors hover:bg-blue-600"
            >
              Add To Cart
            </button>
            <Link
              to="/checkout"
              className="rounded-full border border-slate-200 px-7 py-4 text-sm font-bold text-slate-700 transition-colors hover:bg-slate-50"
            >
              Checkout
            </Link>
          </div>
        </section>
      </div>

      <Reviews />
    </div>
  );
}

function Reviews() {
  return (
    <section className="mt-10 rounded-3xl border border-slate-200 bg-white p-8 shadow-sm">
      <h2 className="font-serif text-3xl font-bold text-slate-950">Customer Reviews</h2>
      <div className="mt-4 rounded-2xl border border-blue-100 bg-blue-50 px-5 py-4 text-sm font-semibold text-blue-700">
        Review API is not implemented in the backend yet.
      </div>
    </section>
  );
}
