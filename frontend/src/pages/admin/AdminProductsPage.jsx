import { useEffect, useState } from "react";
import { createAdminProduct, createProductVariation, deleteAdminProduct, deleteProductVariation, getAdminProducts, updateProductStock, uploadProductMedia } from "../../api/adminApi.js";
import { getCategories } from "../../api/catalogApi.js";
import { formatVND } from "../../utils/formatters.js";
import { buildProductPayload, normalizeBook, normalizeCategory, pageRows } from "../../utils/mappers.js";

export default function AdminProductsPage() {
  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([]);
  const [source, setSource] = useState("api");
  const [message, setMessage] = useState("");
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [mediaForm, setMediaForm] = useState({ file: null, altText: "", sortOrder: 0, primary: true });
  const [variationForm, setVariationForm] = useState({ sku: "", color: "Default", size: "Default", additionalPrice: 0, stockQuantity: 0, imageUrl: "", imagePublicId: "", active: true });
  const [stockForm, setStockForm] = useState({ variationId: "", stockQuantity: "" });
  const [form, setForm] = useState({
    sku: "",
    productName: "",
    slug: "",
    description: "",
    brand: "Aivira",
    material: "Book",
    categoryId: "",
    price: "",
    originalPrice: "",
    discountPercentage: "",
    stockQuantity: ""
  });

  useEffect(() => {
    refreshAdminProducts();
    getCategories().then((rows) => setCategories(pageRows(rows).map(normalizeCategory).filter(Boolean))).catch(() => {});
  }, []);

  async function refreshAdminProducts() {
    try {
      const page = await getAdminProducts({ page: 1, size: 50 });
      const rows = pageRows(page);
      setBooks(rows.map((row) => normalizeBook(row)));
      setSource("api");
    } catch (error) {
      setBooks([]);
      setSource("error");
      setMessage(error.message || "Backend pending: /admin/products unavailable.");
    }
  }

  async function submit(event) {
    event.preventDefault();
    setMessage("");
    try {
      await createAdminProduct(buildProductPayload(form));
      setMessage("Product saved through backend /admin/products.");
      setForm({ ...form, sku: "", productName: "", slug: "", description: "", price: "", originalPrice: "", discountPercentage: "", stockQuantity: "" });
      refreshAdminProducts();
    } catch (error) {
      setMessage(error.message || "Backend pending: product save unavailable.");
    }
  }

  async function remove(book) {
    setMessage("");
    try {
      await deleteAdminProduct(book.id);
      setBooks((current) => current.filter((item) => item.id !== book.id));
      setMessage("Product deleted.");
    } catch (error) {
      setMessage(error.message || "Backend pending: delete unavailable.");
    }
  }

  return (
    <>
      <PageHeader title="Admin Products" eyebrow={source === "api" ? "Backend /admin/products ready" : "Backend unavailable"} />
      {message && <div className="notice page-notice">{message}</div>}
      <div className="table-card">
        {books.slice(0, 10).map((book) => (
          <div className="table-row" key={book.id}>
            <span>{book.title}</span>
            <span>{book.author}</span>
            <strong>{formatVND(book.price)}</strong>
            <span>{book.catLabel}</span>
            <button onClick={() => setSelectedProduct(book)}>Manage</button>
            <button onClick={() => remove(book)}>Delete</button>
          </div>
        ))}
      </div>
      <form className="panel form-panel" onSubmit={submit}>
        <h3>Create/Edit Book</h3>
        <p>Maps to current backend Product DTO. Book-specific author/ISBN fields can be added after backend supports them.</p>
        <div className="form-grid">
          <input value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} placeholder="SKU" required />
          <input value={form.productName} onChange={(e) => setForm({ ...form, productName: e.target.value })} placeholder="Book title" required />
        </div>
        <input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} placeholder="Slug" required />
        <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Description" />
        <div className="form-grid">
          <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
            <option value="">Category</option>
            {categories.map((category) => <option key={category.id} value={category.id}>{category.label}</option>)}
          </select>
          <input value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} placeholder="Brand/author fallback" />
        </div>
        <div className="form-grid">
          <input value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} placeholder="Price" type="number" min="0" required />
          <input value={form.originalPrice} onChange={(e) => setForm({ ...form, originalPrice: e.target.value })} placeholder="Original price" type="number" min="0" />
          <input value={form.stockQuantity} onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })} placeholder="Stock" type="number" min="0" />
        </div>
        <button className="btn-fill" type="submit">Save to backend</button>
      </form>
      {selectedProduct && (
        <div className="panel form-panel">
          <h3>Manage {selectedProduct.title}</h3>
          <div className="table-card">
            {(selectedProduct.variations || []).map((variation) => (
              <div className="table-row" key={variation.id}>
                <span>{variation.sku}</span>
                <span>{variation.color} / {variation.size}</span>
                <strong>Stock {variation.stockQuantity}</strong>
                <button onClick={() => setStockForm({ variationId: variation.id, stockQuantity: variation.stockQuantity || 0 })}>Stock</button>
                <button onClick={() => removeVariation(variation.id)}>Delete</button>
              </div>
            ))}
          </div>
          <form className="compact-form form-panel" onSubmit={saveVariation}>
            <h3>Add variation</h3>
            <div className="form-grid">
              <input value={variationForm.sku} onChange={(e) => setVariationForm({ ...variationForm, sku: e.target.value })} placeholder="Variation SKU" required />
              <input value={variationForm.stockQuantity} onChange={(e) => setVariationForm({ ...variationForm, stockQuantity: e.target.value })} placeholder="Stock" type="number" min="0" required />
            </div>
            <div className="form-grid">
              <input value={variationForm.color} onChange={(e) => setVariationForm({ ...variationForm, color: e.target.value })} placeholder="Color" required />
              <input value={variationForm.size} onChange={(e) => setVariationForm({ ...variationForm, size: e.target.value })} placeholder="Size" required />
              <input value={variationForm.additionalPrice} onChange={(e) => setVariationForm({ ...variationForm, additionalPrice: e.target.value })} placeholder="Additional price" type="number" min="0" />
            </div>
            <button className="btn-line dark" type="submit">Add variation</button>
          </form>
          <form className="compact-form form-panel" onSubmit={saveStock}>
            <h3>Update stock</h3>
            <div className="form-grid">
              <select value={stockForm.variationId} onChange={(e) => setStockForm({ ...stockForm, variationId: e.target.value })} required>
                <option value="">Variation</option>
                {(selectedProduct.variations || []).map((variation) => <option key={variation.id} value={variation.id}>{variation.sku}</option>)}
              </select>
              <input value={stockForm.stockQuantity} onChange={(e) => setStockForm({ ...stockForm, stockQuantity: e.target.value })} placeholder="Stock quantity" type="number" min="0" required />
            </div>
            <button className="btn-line dark" type="submit">Update stock</button>
          </form>
          <form className="compact-form form-panel" onSubmit={saveMedia}>
            <h3>Upload media</h3>
            <input type="file" accept="image/*" onChange={(e) => setMediaForm({ ...mediaForm, file: e.target.files?.[0] || null })} />
            <input value={mediaForm.altText} onChange={(e) => setMediaForm({ ...mediaForm, altText: e.target.value })} placeholder="Alt text" />
            <div className="form-grid">
              <input value={mediaForm.sortOrder} onChange={(e) => setMediaForm({ ...mediaForm, sortOrder: e.target.value })} placeholder="Sort order" type="number" min="0" />
              <label className="check-line"><input type="checkbox" checked={mediaForm.primary} onChange={(e) => setMediaForm({ ...mediaForm, primary: e.target.checked })} /> Primary</label>
            </div>
            <button className="btn-line dark" type="submit">Upload media</button>
          </form>
        </div>
      )}
    </>
  );

  async function saveVariation(event) {
    event.preventDefault();
    setMessage("");
    try {
      await createProductVariation(selectedProduct.id, {
        ...variationForm,
        additionalPrice: Number(variationForm.additionalPrice || 0),
        stockQuantity: Number(variationForm.stockQuantity || 0)
      });
      setMessage("Variation created.");
      setVariationForm({ sku: "", color: "Default", size: "Default", additionalPrice: 0, stockQuantity: 0, imageUrl: "", imagePublicId: "", active: true });
      refreshAdminProducts();
    } catch (error) {
      setMessage(error.message || "Create variation failed.");
    }
  }

  async function removeVariation(variationId) {
    setMessage("");
    try {
      await deleteProductVariation(selectedProduct.id, variationId);
      setMessage("Variation deleted.");
      refreshAdminProducts();
    } catch (error) {
      setMessage(error.message || "Delete variation failed.");
    }
  }

  async function saveStock(event) {
    event.preventDefault();
    setMessage("");
    try {
      await updateProductStock(selectedProduct.id, stockForm.variationId, stockForm.stockQuantity);
      setMessage("Stock updated.");
      refreshAdminProducts();
    } catch (error) {
      setMessage(error.message || "Update stock failed.");
    }
  }

  async function saveMedia(event) {
    event.preventDefault();
    setMessage("");
    if (!mediaForm.file) {
      setMessage("Choose an image file first.");
      return;
    }
    try {
      await uploadProductMedia(selectedProduct.id, mediaForm.file, {
        altText: mediaForm.altText,
        sortOrder: mediaForm.sortOrder,
        primary: mediaForm.primary
      });
      setMessage("Media uploaded.");
      setMediaForm({ file: null, altText: "", sortOrder: 0, primary: true });
      refreshAdminProducts();
    } catch (error) {
      setMessage(error.message || "Upload media failed.");
    }
  }
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}
