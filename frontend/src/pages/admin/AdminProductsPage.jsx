import { useEffect, useState } from "react";

import {
  createAdminProduct,
  createProductVariation,
  deleteAdminProduct,
  deleteProductVariation,
  getAdminProducts,
  updateProductStock,
  uploadProductMedia,
} from "../../api/adminApi.js";
import { getCategories } from "../../api/catalogApi.js";
import { formatVND } from "../../utils/formatters.js";
import {
  buildProductPayload,
  normalizeBook,
  normalizeCategory,
  pageRows,
} from "../../utils/mappers.js";

export default function AdminProductsPage() {
  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([]);
  const [source, setSource] = useState("api");
  const [message, setMessage] = useState("");
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [mediaForm, setMediaForm] = useState({
    file: null,
    altText: "",
    sortOrder: 0,
    primary: true,
  });
  const [variationForm, setVariationForm] = useState({
    sku: "",
    color: "Default",
    size: "Default",
    additionalPrice: 0,
    stockQuantity: 0,
    imageUrl: "",
    imagePublicId: "",
    active: true,
  });
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
    stockQuantity: "",
  });

  useEffect(() => {
    refreshAdminProducts();
    getCategories()
      .then((rows) => setCategories(pageRows(rows).map(normalizeCategory).filter(Boolean)))
      .catch(() => {});
  }, []);

  async function refreshAdminProducts() {
    try {
      const page = await getAdminProducts({ page: 1, size: 50 });
      setBooks(pageRows(page).map((row) => normalizeBook(row)));
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
      setForm({
        ...form,
        sku: "",
        productName: "",
        slug: "",
        description: "",
        price: "",
        originalPrice: "",
        discountPercentage: "",
        stockQuantity: "",
      });
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

  async function saveVariation(event) {
    event.preventDefault();
    setMessage("");
    try {
      await createProductVariation(selectedProduct.id, {
        ...variationForm,
        additionalPrice: Number(variationForm.additionalPrice || 0),
        stockQuantity: Number(variationForm.stockQuantity || 0),
      });
      setMessage("Variation created.");
      setVariationForm({
        sku: "",
        color: "Default",
        size: "Default",
        additionalPrice: 0,
        stockQuantity: 0,
        imageUrl: "",
        imagePublicId: "",
        active: true,
      });
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
        primary: mediaForm.primary,
      });
      setMessage("Media uploaded.");
      setMediaForm({ file: null, altText: "", sortOrder: 0, primary: true });
      refreshAdminProducts();
    } catch (error) {
      setMessage(error.message || "Upload media failed.");
    }
  }

  return (
    <div className="grid gap-8">
      <PageHeader
        title="Admin Products"
        eyebrow={source === "api" ? "Backend /admin/products ready" : "Backend unavailable"}
      />
      {message && <Notice>{message}</Notice>}

      <Panel title="Latest books">
        <div className="overflow-hidden rounded-2xl border border-slate-200">
          {books.slice(0, 10).map((book) => (
            <div
              className="grid gap-3 border-b border-slate-100 p-4 last:border-b-0 md:grid-cols-[1fr_160px_140px_140px_auto_auto] md:items-center"
              key={book.id}
            >
              <span className="font-bold text-slate-950">{book.title}</span>
              <span className="text-sm text-slate-500">{book.author}</span>
              <strong>{formatVND(book.price)}</strong>
              <span className="text-sm text-slate-500">{book.catLabel}</span>
              <SmallButton onClick={() => setSelectedProduct(book)}>Manage</SmallButton>
              <SmallButton danger onClick={() => remove(book)}>
                Delete
              </SmallButton>
            </div>
          ))}
          {!books.length && <div className="p-5 text-sm text-slate-500">No admin products loaded.</div>}
        </div>
      </Panel>

      <Panel title="Create/Edit Book">
        <p className="mb-5 text-sm text-slate-500">
          Maps to current backend Product DTO. Book-specific author/ISBN fields can be added after
          backend supports them.
        </p>
        <form className="grid gap-4" onSubmit={submit}>
          <div className="grid gap-4 md:grid-cols-2">
            <Input value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} placeholder="SKU" required />
            <Input value={form.productName} onChange={(e) => setForm({ ...form, productName: e.target.value })} placeholder="Book title" required />
          </div>
          <Input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} placeholder="Slug" required />
          <Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Description" />
          <div className="grid gap-4 md:grid-cols-2">
            <Select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
              <option value="">Category</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.label}</option>
              ))}
            </Select>
            <Input value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} placeholder="Brand/author fallback" />
          </div>
          <div className="grid gap-4 md:grid-cols-3">
            <Input value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} placeholder="Price" type="number" min="0" required />
            <Input value={form.originalPrice} onChange={(e) => setForm({ ...form, originalPrice: e.target.value })} placeholder="Original price" type="number" min="0" />
            <Input value={form.stockQuantity} onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })} placeholder="Stock" type="number" min="0" />
          </div>
          <Button type="submit">Save to backend</Button>
        </form>
      </Panel>

      {selectedProduct && (
        <Panel title={`Manage ${selectedProduct.title}`}>
          <div className="overflow-hidden rounded-2xl border border-slate-200">
            {(selectedProduct.variations || []).map((variation) => (
              <div
                className="grid gap-3 border-b border-slate-100 p-4 last:border-b-0 md:grid-cols-[1fr_160px_120px_auto_auto] md:items-center"
                key={variation.id}
              >
                <span className="font-bold text-slate-950">{variation.sku}</span>
                <span className="text-sm text-slate-500">{variation.color} / {variation.size}</span>
                <strong>Stock {variation.stockQuantity}</strong>
                <SmallButton onClick={() => setStockForm({ variationId: variation.id, stockQuantity: variation.stockQuantity || 0 })}>Stock</SmallButton>
                <SmallButton danger onClick={() => removeVariation(variation.id)}>Delete</SmallButton>
              </div>
            ))}
            {!selectedProduct.variations?.length && (
              <div className="p-5 text-sm text-slate-500">No variations loaded.</div>
            )}
          </div>

          <div className="mt-6 grid gap-6 xl:grid-cols-3">
            <form className="grid gap-4 rounded-2xl bg-slate-50 p-5" onSubmit={saveVariation}>
              <h3 className="font-serif text-2xl font-bold text-slate-950">Add variation</h3>
              <Input value={variationForm.sku} onChange={(e) => setVariationForm({ ...variationForm, sku: e.target.value })} placeholder="Variation SKU" required />
              <Input value={variationForm.stockQuantity} onChange={(e) => setVariationForm({ ...variationForm, stockQuantity: e.target.value })} placeholder="Stock" type="number" min="0" required />
              <Input value={variationForm.color} onChange={(e) => setVariationForm({ ...variationForm, color: e.target.value })} placeholder="Color" required />
              <Input value={variationForm.size} onChange={(e) => setVariationForm({ ...variationForm, size: e.target.value })} placeholder="Size" required />
              <Input value={variationForm.additionalPrice} onChange={(e) => setVariationForm({ ...variationForm, additionalPrice: e.target.value })} placeholder="Additional price" type="number" min="0" />
              <Button secondary type="submit">Add variation</Button>
            </form>

            <form className="grid gap-4 rounded-2xl bg-slate-50 p-5" onSubmit={saveStock}>
              <h3 className="font-serif text-2xl font-bold text-slate-950">Update stock</h3>
              <Select value={stockForm.variationId} onChange={(e) => setStockForm({ ...stockForm, variationId: e.target.value })} required>
                <option value="">Variation</option>
                {(selectedProduct.variations || []).map((variation) => (
                  <option key={variation.id} value={variation.id}>{variation.sku}</option>
                ))}
              </Select>
              <Input value={stockForm.stockQuantity} onChange={(e) => setStockForm({ ...stockForm, stockQuantity: e.target.value })} placeholder="Stock quantity" type="number" min="0" required />
              <Button secondary type="submit">Update stock</Button>
            </form>

            <form className="grid gap-4 rounded-2xl bg-slate-50 p-5" onSubmit={saveMedia}>
              <h3 className="font-serif text-2xl font-bold text-slate-950">Upload media</h3>
              <Input type="file" accept="image/*" onChange={(e) => setMediaForm({ ...mediaForm, file: e.target.files?.[0] || null })} />
              <Input value={mediaForm.altText} onChange={(e) => setMediaForm({ ...mediaForm, altText: e.target.value })} placeholder="Alt text" />
              <Input value={mediaForm.sortOrder} onChange={(e) => setMediaForm({ ...mediaForm, sortOrder: e.target.value })} placeholder="Sort order" type="number" min="0" />
              <label className="flex items-center gap-2 text-sm font-semibold text-slate-600">
                <input type="checkbox" checked={mediaForm.primary} onChange={(e) => setMediaForm({ ...mediaForm, primary: e.target.checked })} />
                Primary
              </label>
              <Button secondary type="submit">Upload media</Button>
            </form>
          </div>
        </Panel>
      )}
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return (
    <div className="border-b border-slate-200 pb-6">
      <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">{eyebrow}</span>
      <h2 className="mt-3 font-serif text-4xl font-bold text-slate-950">{title}</h2>
    </div>
  );
}

function Panel({ title, children }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
      <h3 className="mb-5 font-serif text-3xl font-bold text-slate-950">{title}</h3>
      {children}
    </section>
  );
}

function Input(props) {
  return <input {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}

function Textarea(props) {
  return <textarea {...props} className="min-h-28 w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}

function Select(props) {
  return <select {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}

function Button({ secondary = false, ...props }) {
  return <button {...props} className={["rounded-full px-5 py-3 text-sm font-bold transition-colors", secondary ? "border border-slate-200 text-slate-700 hover:bg-white" : "bg-slate-950 text-white hover:bg-blue-600"].join(" ")} />;
}

function SmallButton({ danger = false, ...props }) {
  return <button type="button" {...props} className={["rounded-full border px-3 py-1.5 text-xs font-bold transition-colors", danger ? "border-red-100 text-red-600 hover:bg-red-50" : "border-slate-200 text-slate-600 hover:bg-slate-50"].join(" ")} />;
}

function Notice({ children }) {
  return <div className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{children}</div>;
}
