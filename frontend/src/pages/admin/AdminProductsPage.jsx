import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

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
  const { t } = useTranslation();
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
      setMessage(error.message || t("admin.errors.products"));
    }
  }

  async function submit(event) {
    event.preventDefault();
    setMessage("");
    try {
      await createAdminProduct(buildProductPayload(form));
      setMessage(t("admin.productSaved"));
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
      setMessage(error.message || t("admin.errors.productSave"));
    }
  }

  async function remove(book) {
    setMessage("");
    try {
      await deleteAdminProduct(book.id);
      setBooks((current) => current.filter((item) => item.id !== book.id));
      setMessage(t("admin.productDeleted"));
    } catch (error) {
      setMessage(error.message || t("admin.errors.delete"));
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
      setMessage(t("admin.variationCreated"));
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
      setMessage(error.message || t("admin.errors.variationCreate"));
    }
  }

  async function removeVariation(variationId) {
    setMessage("");
    try {
      await deleteProductVariation(selectedProduct.id, variationId);
      setMessage(t("admin.variationDeleted"));
      refreshAdminProducts();
    } catch (error) {
      setMessage(error.message || t("admin.errors.variationDelete"));
    }
  }

  async function saveStock(event) {
    event.preventDefault();
    setMessage("");
    try {
      await updateProductStock(selectedProduct.id, stockForm.variationId, stockForm.stockQuantity);
      setMessage(t("admin.stockUpdated"));
      refreshAdminProducts();
    } catch (error) {
      setMessage(error.message || t("admin.errors.stock"));
    }
  }

  async function saveMedia(event) {
    event.preventDefault();
    setMessage("");
    if (!mediaForm.file) {
      setMessage(t("admin.chooseImage"));
      return;
    }
    try {
      await uploadProductMedia(selectedProduct.id, mediaForm.file, {
        altText: mediaForm.altText,
        sortOrder: mediaForm.sortOrder,
        primary: mediaForm.primary,
      });
      setMessage(t("admin.mediaUploaded"));
      setMediaForm({ file: null, altText: "", sortOrder: 0, primary: true });
      refreshAdminProducts();
    } catch (error) {
      setMessage(error.message || t("admin.errors.media"));
    }
  }

  return (
    <div className="grid gap-8">
      <PageHeader
        title={t("admin.productsTitle")}
        eyebrow={source === "api" ? t("admin.productsReady") : t("admin.backendUnavailable")}
      />
      {message && <Notice>{message}</Notice>}

      <Panel title={t("admin.latestBooks")}>
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
              <SmallButton onClick={() => setSelectedProduct(book)}>{t("admin.manage")}</SmallButton>
              <SmallButton danger onClick={() => remove(book)}>
                {t("common.delete")}
              </SmallButton>
            </div>
          ))}
          {!books.length && <div className="p-5 text-sm text-slate-500">{t("admin.noProducts")}</div>}
        </div>
      </Panel>

      <Panel title={t("admin.createEditBook")}>
        <p className="mb-5 text-sm text-slate-500">
          {t("admin.productNote")}
        </p>
        <form className="grid gap-4" onSubmit={submit}>
          <div className="grid gap-4 md:grid-cols-2">
            <Input value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} placeholder={t("admin.sku")} required />
            <Input value={form.productName} onChange={(e) => setForm({ ...form, productName: e.target.value })} placeholder={t("admin.bookTitle")} required />
          </div>
          <Input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} placeholder={t("admin.slug")} required />
          <Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder={t("admin.description")} />
          <div className="grid gap-4 md:grid-cols-2">
            <Select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
              <option value="">{t("admin.category")}</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.label}</option>
              ))}
            </Select>
            <Input value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} placeholder={t("admin.brandAuthor")} />
          </div>
          <div className="grid gap-4 md:grid-cols-3">
            <Input value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} placeholder={t("admin.price")} type="number" min="0" required />
            <Input value={form.originalPrice} onChange={(e) => setForm({ ...form, originalPrice: e.target.value })} placeholder={t("admin.originalPrice")} type="number" min="0" />
            <Input value={form.stockQuantity} onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })} placeholder={t("admin.stock")} type="number" min="0" />
          </div>
          <Button type="submit">{t("admin.saveBackend")}</Button>
        </form>
      </Panel>

      {selectedProduct && (
        <Panel title={t("admin.manageProduct", { title: selectedProduct.title })}>
          <div className="overflow-hidden rounded-2xl border border-slate-200">
            {(selectedProduct.variations || []).map((variation) => (
              <div
                className="grid gap-3 border-b border-slate-100 p-4 last:border-b-0 md:grid-cols-[1fr_160px_120px_auto_auto] md:items-center"
                key={variation.id}
              >
                <span className="font-bold text-slate-950">{variation.sku}</span>
                <span className="text-sm text-slate-500">{variation.color} / {variation.size}</span>
                <strong>{t("admin.stock")} {variation.stockQuantity}</strong>
                <SmallButton onClick={() => setStockForm({ variationId: variation.id, stockQuantity: variation.stockQuantity || 0 })}>{t("admin.stock")}</SmallButton>
                <SmallButton danger onClick={() => removeVariation(variation.id)}>{t("common.delete")}</SmallButton>
              </div>
            ))}
            {!selectedProduct.variations?.length && (
              <div className="p-5 text-sm text-slate-500">{t("admin.noVariations")}</div>
            )}
          </div>

          <div className="mt-6 grid gap-6 xl:grid-cols-3">
            <form className="grid gap-4 rounded-2xl bg-slate-50 p-5" onSubmit={saveVariation}>
              <h3 className="font-serif text-2xl font-bold text-slate-950">{t("admin.addVariation")}</h3>
              <Input value={variationForm.sku} onChange={(e) => setVariationForm({ ...variationForm, sku: e.target.value })} placeholder={t("admin.variationSku")} required />
              <Input value={variationForm.stockQuantity} onChange={(e) => setVariationForm({ ...variationForm, stockQuantity: e.target.value })} placeholder={t("admin.stock")} type="number" min="0" required />
              <Input value={variationForm.color} onChange={(e) => setVariationForm({ ...variationForm, color: e.target.value })} placeholder={t("admin.color")} required />
              <Input value={variationForm.size} onChange={(e) => setVariationForm({ ...variationForm, size: e.target.value })} placeholder={t("admin.size")} required />
              <Input value={variationForm.additionalPrice} onChange={(e) => setVariationForm({ ...variationForm, additionalPrice: e.target.value })} placeholder={t("admin.additionalPrice")} type="number" min="0" />
              <Button secondary type="submit">{t("admin.addVariation")}</Button>
            </form>

            <form className="grid gap-4 rounded-2xl bg-slate-50 p-5" onSubmit={saveStock}>
              <h3 className="font-serif text-2xl font-bold text-slate-950">{t("admin.updateStock")}</h3>
              <Select value={stockForm.variationId} onChange={(e) => setStockForm({ ...stockForm, variationId: e.target.value })} required>
                <option value="">{t("admin.variation")}</option>
                {(selectedProduct.variations || []).map((variation) => (
                  <option key={variation.id} value={variation.id}>{variation.sku}</option>
                ))}
              </Select>
              <Input value={stockForm.stockQuantity} onChange={(e) => setStockForm({ ...stockForm, stockQuantity: e.target.value })} placeholder={t("admin.stockQuantity")} type="number" min="0" required />
              <Button secondary type="submit">{t("admin.updateStock")}</Button>
            </form>

            <form className="grid gap-4 rounded-2xl bg-slate-50 p-5" onSubmit={saveMedia}>
              <h3 className="font-serif text-2xl font-bold text-slate-950">{t("admin.uploadMedia")}</h3>
              <Input type="file" accept="image/*" onChange={(e) => setMediaForm({ ...mediaForm, file: e.target.files?.[0] || null })} />
              <Input value={mediaForm.altText} onChange={(e) => setMediaForm({ ...mediaForm, altText: e.target.value })} placeholder={t("admin.altText")} />
              <Input value={mediaForm.sortOrder} onChange={(e) => setMediaForm({ ...mediaForm, sortOrder: e.target.value })} placeholder={t("admin.sortOrder")} type="number" min="0" />
              <label className="flex items-center gap-2 text-sm font-semibold text-slate-600">
                <input type="checkbox" checked={mediaForm.primary} onChange={(e) => setMediaForm({ ...mediaForm, primary: e.target.checked })} />
                {t("admin.primary")}
              </label>
              <Button secondary type="submit">{t("admin.uploadMedia")}</Button>
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
