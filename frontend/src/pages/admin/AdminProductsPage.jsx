import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { motion, AnimatePresence } from "motion/react";
import {
  BookOpen, Edit, Package, Plus, Search,
  Star, Trash2, Upload, X, Image, Layers, RefreshCw,
  Filter,
} from "lucide-react";

import {
  createAdminProduct,
  createProductVariation,
  deleteAdminProduct,
  deleteProductMedia,
  deleteProductVariation,
  getAdminProduct,
  getAdminProducts,
  updateAdminProduct,
  updateProductMedia,
  updateProductStock,
  updateProductVariation,
  uploadProductMedia,
} from "../../api/adminApi.js";
import { Drawer, Modal, Pagination, useConfirm } from "../../components/ui/index.jsx";
import { getCategories } from "../../api/catalogApi.js";
import { formatDateTime, formatVND } from "../../utils/formatters.js";
import {
  buildProductPayload,
  buildProductUpdatePayload,
  normalizeBook,
  normalizeCategory,
  pageMeta as readPageMeta,
  pageRows,
} from "../../utils/mappers.js";

/* ── Constants ─────────────────────────────────── */
const PRODUCT_STATUSES = ["DRAFT", "PENDING_REVIEW", "ACTIVE", "INACTIVE", "REJECTED"];
const BOOK_FORMATS = ["PAPERBACK", "HARDCOVER", "EBOOK", "BOXSET", "OTHER"];
const PAGE_SIZES = [10, 20, 50];

const STATUS_STYLES = {
  ACTIVE:         "bg-emerald-50 text-emerald-700 border-emerald-200/60 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20",
  INACTIVE:       "bg-slate-100 text-slate-600 border-slate-200/60 dark:bg-slate-800 dark:text-slate-400 dark:border-slate-700",
  DRAFT:          "bg-amber-50 text-amber-700 border-amber-200/60 dark:bg-amber-500/10 dark:text-amber-400 dark:border-amber-500/20",
  PENDING_REVIEW: "bg-sky-50 text-sky-700 border-sky-200/60 dark:bg-sky-500/10 dark:text-sky-400 dark:border-sky-500/20",
  REJECTED:       "bg-rose-50 text-rose-700 border-rose-200/60 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20",
};

const emptyFilters = { keyword: "", status: "", categoryId: "", page: 1, size: 20 };

const emptyBookForm = {
  sku: "", productName: "", slug: "", description: "",
  brand: "Aivira", material: "Book",
  bookAuthor: "", isbn: "", publisher: "", publicationYear: "",
  bookLanguage: "", pageCount: "", bookFormat: "PAPERBACK", dimensions: "",
  categoryId: "", price: "", originalPrice: "", discountPercentage: "",
  weight: "", featured: false,
  variationSku: "", variationColor: "Default", variationSize: "Paperback",
  variationAdditionalPrice: 0, stockQuantity: "",
};

const emptyVariationForm = {
  sku: "", color: "Default", size: "Paperback",
  additionalPrice: 0, stockQuantity: 0,
  imageUrl: "", imagePublicId: "", active: true,
};

const emptyMediaForm    = { file: null, altText: "", sortOrder: 0, primary: true };
const emptyMediaEditForm = { id: "", altText: "", sortOrder: 0, primary: false, active: true };

/* ── Shared styled primitives ──────────────────── */
function PInput({ label, ...props }) {
  return (
    <div className="grid gap-1.5">
      {label && <label className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wide">{label}</label>}
      <input
        className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none transition-shadow focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 disabled:opacity-50"
        {...props}
      />
    </div>
  );
}

function PSelect({ label, children, ...props }) {
  return (
    <div className="grid gap-1.5">
      {label && <label className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wide">{label}</label>}
      <select
        className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 outline-none transition-shadow focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
        {...props}
      >
        {children}
      </select>
    </div>
  );
}

function PTextarea({ label, ...props }) {
  return (
    <div className="grid gap-1.5">
      {label && <label className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wide">{label}</label>}
      <textarea
        rows={3}
        className="w-full rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none transition-shadow focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 resize-none"
        {...props}
      />
    </div>
  );
}

function PrimaryBtn({ children, loading, icon: Icon, ...props }) {
  return (
    <motion.button whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.97 }}
      className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 px-4 py-2 text-sm font-semibold text-white shadow-sm shadow-indigo-200 dark:shadow-none transition-colors focus:ring-2 focus:ring-offset-2 focus:ring-indigo-600 disabled:opacity-50"
      {...props}>
      {loading ? <RefreshCw size={14} className="animate-spin"/> : Icon ? <Icon size={14}/> : null}
      {children}
    </motion.button>
  );
}

function SecondaryBtn({ children, icon: Icon, danger, ...props }) {
  return (
    <motion.button whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.97 }}
      className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-semibold transition-colors focus:ring-2 focus:ring-offset-1 disabled:opacity-50 ${
        danger
          ? "border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-400 dark:hover:bg-rose-500/20 focus:ring-rose-500"
          : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 focus:ring-indigo-500"
      }`}
      {...props}>
      {Icon && <Icon size={13}/>}
      {children}
    </motion.button>
  );
}

function Card({ title, icon: Icon, children, action }) {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
      <div className="flex items-center justify-between gap-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
        <div className="flex items-center gap-2">
          {Icon && <Icon size={16} className="text-indigo-500"/>}
          <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{title}</h3>
        </div>
        {action}
      </div>
      <div className="p-5">{children}</div>
    </div>
  );
}

function SubCard({ title, children }) {
  return (
    <div className="rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 p-4">
      {title && <h5 className="mb-3 text-sm font-bold text-slate-800 dark:text-slate-200">{title}</h5>}
      {children}
    </div>
  );
}

function Toast({ message, onClose }) {
  if (!message) return null;
  const isError = /err|fail|lỗi|không|invalid|required/i.test(message);
  return (
    <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
      className={`flex items-center justify-between gap-3 rounded-xl border px-4 py-3 text-sm font-semibold mb-4 ${
        isError
          ? "border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-400"
          : "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-400"
      }`}>
      <span>{message}</span>
      <button type="button" onClick={onClose}><X size={14}/></button>
    </motion.div>
  );
}

/* ══════════════════════════════════════════════
   MAIN PAGE
══════════════════════════════════════════════ */
export default function AdminProductsPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();

  // ── State (100% preserved from original) ────
  const [filters, setFilters] = useState(emptyFilters);
  const [appliedFilters, setAppliedFilters] = useState(emptyFilters);
  const [books, setBooks] = useState([]);
  const [pageMeta, setPageMeta] = useState(createEmptyMeta(emptyFilters));
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [bookForm, setBookForm] = useState(emptyBookForm);
  const [editingProductId, setEditingProductId] = useState(null);
  const [slugTouched, setSlugTouched] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [variationForm, setVariationForm] = useState(emptyVariationForm);
  const [editingVariationId, setEditingVariationId] = useState(null);
  const [stockForm, setStockForm] = useState({ variationId: "", stockQuantity: "" });
  const [mediaForm, setMediaForm] = useState(emptyMediaForm);
  const [mediaEditForm, setMediaEditForm] = useState(emptyMediaEditForm);
  const [showForm, setShowForm] = useState(false);
  const [variationModalOpen, setVariationModalOpen] = useState(false);
  const [stockModalOpen, setStockModalOpen] = useState(false);
  const [mediaUploadOpen, setMediaUploadOpen] = useState(false);
  const [mediaEditOpen, setMediaEditOpen] = useState(false);

  const categoryOptions = useMemo(() => categories.map(normalizeCategory).filter(Boolean), [categories]);

  // ── API calls (100% preserved) ──────────────
  const refreshCategories = useCallback(async () => {
    try { setCategories(pageRows(await getCategories())); }
    catch { setCategories([]); }
  }, []);

  const refreshAdminProducts = useCallback(async (nextFilters = appliedFilters) => {
    setLoading(true); setMessage("");
    try {
      const page = await getAdminProducts(toProductQuery(nextFilters));
      setBooks(pageRows(page).map(normalizeBook));
      setPageMeta(readPageMeta(page, { page: nextFilters.page, size: nextFilters.size }));
    } catch (error) {
      setBooks([]); setPageMeta(createEmptyMeta(nextFilters));
      setMessage(error.message || t("admin.errors.products"));
    } finally { setLoading(false); }
  }, [appliedFilters, t]);

  useEffect(() => { refreshCategories(); }, [refreshCategories]);
  useEffect(() => { refreshAdminProducts(appliedFilters); }, [appliedFilters, refreshAdminProducts]);

  async function refreshSelectedProduct(productId = selectedProduct?.id) {
    if (!productId) return;
    setDetailLoading(true);
    try { setSelectedProduct(normalizeBook(await getAdminProduct(productId))); }
    catch (error) { setMessage(error.message || t("admin.errors.productDetail")); }
    finally { setDetailLoading(false); }
  }

  // ── Filter handlers (100% preserved) ────────
  function submitFilters(event) {
    event.preventDefault();
    setAppliedFilters({ ...filters, page: 1, size: Number(filters.size || 20) });
    setFilters(c => ({ ...c, page: 1 }));
  }
  function clearFilters() { setFilters(emptyFilters); setAppliedFilters(emptyFilters); }
  function changePage(page) {
    const p = Math.max(1, page);
    setFilters(c => ({ ...c, page: p }));
    setAppliedFilters(c => ({ ...c, page: p }));
  }
  function changePageSize(size) {
    const next = { ...filters, page: 1, size: Number(size || 20) };
    setFilters(next); setAppliedFilters(next);
  }

  // ── Book form handlers (100% preserved) ──────
  function startCreate() {
    setEditingProductId(null); setSlugTouched(false);
    setBookForm(emptyBookForm); setMessage(""); setShowForm(true);
  }
  function startEdit(book) {
    setEditingProductId(book.id); setSlugTouched(true);
    setBookForm(productToForm(book)); setMessage(""); setShowForm(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  function handleBookField(field, value) {
    if (field === "productName") {
      setBookForm(c => ({
        ...c, productName: value,
        slug: !slugTouched && !editingProductId ? slugify(value) : c.slug,
      }));
      return;
    }
    if (field === "slug") setSlugTouched(true);
    setBookForm(c => {
      const next = { ...c, [field]: value };
      if (field === "bookFormat" && !editingProductId) next.variationSize = formatLabel(value);
      if (field === "sku" && !editingProductId && !c.variationSku) next.variationSku = value ? `${value}-PB` : "";
      return next;
    });
  }

  async function submitBook(event) {
    event.preventDefault(); setMessage("");
    const validation = validateBookForm(bookForm, Boolean(editingProductId), t);
    if (validation) { setMessage(validation); return; }
    try {
      const response = editingProductId
        ? await updateAdminProduct(editingProductId, buildProductUpdatePayload(bookForm))
        : await createAdminProduct(buildProductPayload(bookForm));
      const normalized = normalizeBook(response);
      setMessage(editingProductId ? t("admin.productUpdated") : t("admin.productSaved"));
      setEditingProductId(null); setBookForm(emptyBookForm); setSlugTouched(false);
      setSelectedProduct(normalized); setShowForm(false);
      await refreshAdminProducts(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.errors.productSave")); }
  }

  async function remove(book) {
    const confirmed = await confirm({
      title: t("common.delete"),
      message: t("admin.confirmDeleteProduct", { title: book.title }),
      confirmLabel: t("common.delete"), cancelLabel: t("common.cancel"), danger: true,
    });
    if (!confirmed) return;
    setMessage("");
    try {
      await deleteAdminProduct(book.id);
      setMessage(t("admin.productDeleted"));
      if (selectedProduct?.id === book.id) setSelectedProduct(null);
      await refreshAdminProducts(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.errors.delete")); }
  }

  async function manageProduct(book) {
    setSelectedProduct(book); resetVariationForm();
    setMediaForm(emptyMediaForm); setMediaEditForm(emptyMediaEditForm);
    setVariationModalOpen(false); setStockModalOpen(false); setMediaUploadOpen(false); setMediaEditOpen(false);
    await refreshSelectedProduct(book.id);
  }

  // ── Variation handlers (100% preserved) ──────
  function resetVariationForm() { setEditingVariationId(null); setVariationForm(emptyVariationForm); }
  function openVariationCreate() {
    resetVariationForm();
    setVariationModalOpen(true);
  }
  function editVariation(variation) {
    setEditingVariationId(variation.id);
    setVariationForm({
      sku: variation.sku || "", color: variation.color || "Default",
      size: variation.size || "Paperback", additionalPrice: variation.additionalPrice ?? 0,
      stockQuantity: variation.stockQuantity ?? 0, imageUrl: variation.imageUrl || "",
      imagePublicId: variation.imagePublicId || "", active: variation.active !== false,
    });
    setVariationModalOpen(true);
  }
  async function saveVariation(event) {
    event.preventDefault(); if (!selectedProduct?.id) return; setMessage("");
    try {
      const payload = variationPayload(variationForm);
      if (editingVariationId) { await updateProductVariation(selectedProduct.id, editingVariationId, payload); setMessage(t("admin.variationUpdated")); }
      else { await createProductVariation(selectedProduct.id, payload); setMessage(t("admin.variationCreated")); }
      resetVariationForm();
      setVariationModalOpen(false);
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.errors.variationCreate")); }
  }
  async function removeVariation(variationId) {
    if (!selectedProduct?.id) return;
    const confirmed = await confirm({ title: t("common.delete"), message: t("admin.confirmDeleteVariation"), confirmLabel: t("common.delete"), cancelLabel: t("common.cancel"), danger: true });
    if (!confirmed) return;
    setMessage("");
    try {
      await deleteProductVariation(selectedProduct.id, variationId);
      setMessage(t("admin.variationDeleted"));
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.errors.variationDelete")); }
  }

  // ── Stock handler (100% preserved) ──────────
  async function saveStock(event) {
    event.preventDefault(); if (!selectedProduct?.id) return; setMessage("");
    try {
      await updateProductStock(selectedProduct.id, stockForm.variationId, stockForm.stockQuantity);
      setMessage(t("admin.stockUpdated")); setStockForm({ variationId: "", stockQuantity: "" });
      setStockModalOpen(false);
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.errors.stock")); }
  }

  // ── Media handlers (100% preserved) ─────────
  async function saveMedia(event) {
    event.preventDefault(); if (!selectedProduct?.id) return;
    if (!mediaForm.file) { setMessage(t("admin.chooseImage")); return; }
    setMessage("");
    try {
      await uploadProductMedia(selectedProduct.id, mediaForm.file, { altText: mediaForm.altText, sortOrder: mediaForm.sortOrder, primary: mediaForm.primary });
      setMessage(t("admin.mediaUploaded")); setMediaForm(emptyMediaForm);
      setMediaUploadOpen(false);
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.errors.media")); }
  }
  function editMedia(media) {
    setMediaEditForm({ id: media.id, altText: media.altText || "", sortOrder: media.sortOrder ?? 0, primary: Boolean(media.primary), active: media.active !== false });
    setMediaEditOpen(true);
  }
  async function saveMediaEdit(event) {
    event.preventDefault(); if (!selectedProduct?.id || !mediaEditForm.id) return; setMessage("");
    try {
      await updateProductMedia(selectedProduct.id, mediaEditForm.id, { altText: mediaEditForm.altText, sortOrder: Number(mediaEditForm.sortOrder || 0), primary: mediaEditForm.primary, active: mediaEditForm.active });
      setMessage(t("admin.mediaUpdated")); setMediaEditForm(emptyMediaEditForm);
      setMediaEditOpen(false);
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.errors.media")); }
  }
  async function removeMedia(mediaId) {
    if (!selectedProduct?.id) return;
    const confirmed = await confirm({ title: t("common.delete"), message: t("admin.confirmDeleteMedia"), confirmLabel: t("common.delete"), cancelLabel: t("common.cancel"), danger: true });
    if (!confirmed) return;
    setMessage("");
    try {
      await deleteProductMedia(selectedProduct.id, mediaId);
      setMessage(t("admin.mediaDeleted"));
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) { setMessage(error.message || t("admin.errors.media")); }
  }

  // ── RENDER ───────────────────────────────────
  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="max-w-full space-y-6">

      {/* ── Page header ── */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-50 tracking-tight">{t("admin.productsTitle", "Quản lý sách")}</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">{t("admin.productsReady", "Quản lý catalog sách của Aivira")}</p>
        </div>
        <PrimaryBtn icon={Plus} onClick={startCreate}>{t("admin.newBook", "Thêm sách mới")}</PrimaryBtn>
      </div>

      {/* ── Toast ── */}
      <AnimatePresence>
        {message && <Toast message={message} onClose={() => setMessage("")}/>}
      </AnimatePresence>

      {/* ── Filter bar ── */}
      <Card title={t("admin.bookFilters", "Bộ lọc")} icon={Filter}>
        <form className="grid gap-3 sm:grid-cols-2 lg:grid-cols-[1fr_160px_200px_100px_auto_auto]" onSubmit={submitFilters}>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400"/>
            <input
              value={filters.keyword}
              onChange={e => setFilters({ ...filters, keyword: e.target.value })}
              placeholder={t("admin.searchBooks", "Tìm kiếm sách...")}
              className="w-full pl-9 pr-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 outline-none focus:ring-2 focus:ring-indigo-500 transition-shadow"
            />
          </div>
          <select value={filters.status} onChange={e => setFilters({ ...filters, status: e.target.value })}
            className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 outline-none focus:ring-2 focus:ring-indigo-500">
            <option value="">{t("admin.allStatuses", "Tất cả trạng thái")}</option>
            {PRODUCT_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <select value={filters.categoryId} onChange={e => setFilters({ ...filters, categoryId: e.target.value })}
            className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 outline-none focus:ring-2 focus:ring-indigo-500">
            <option value="">{t("admin.allCategories", "Tất cả danh mục")}</option>
            {categoryOptions.map(c => <option key={c.id} value={c.id}>{c.label}</option>)}
          </select>
          <select value={filters.size} onChange={e => changePageSize(e.target.value)}
            className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 px-3 py-2 text-sm text-slate-900 dark:text-slate-100 outline-none focus:ring-2 focus:ring-indigo-500">
            {PAGE_SIZES.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <PrimaryBtn type="submit">{t("admin.applyFilters", "Lọc")}</PrimaryBtn>
          <SecondaryBtn type="button" onClick={clearFilters}>{t("admin.clearFilters", "Xóa lọc")}</SecondaryBtn>
        </form>
      </Card>

      {/* ── Books table ── */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        <div className="flex items-center justify-between gap-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-4">
          <div className="flex items-center gap-2">
            <BookOpen size={16} className="text-indigo-500"/>
            <h3 className="text-sm font-bold text-slate-800 dark:text-slate-100">{t("admin.booksList", "Danh sách sách")}</h3>
          </div>
          <span className="text-xs font-semibold text-slate-400">{pageMeta.totalElements ?? 0} {t("admin.books","sách")}</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm whitespace-nowrap min-w-[900px]">
            <thead className="bg-slate-50 dark:bg-slate-800/50 text-slate-500 dark:text-slate-400 font-medium text-xs uppercase tracking-wider">
              <tr>
                <th className="px-5 py-4">{t("admin.book","Sách")}</th>
                <th className="px-5 py-4">{t("admin.isbn","ISBN")}</th>
                <th className="px-5 py-4">{t("admin.category","Danh mục")}</th>
                <th className="px-5 py-4">{t("admin.price","Giá")}</th>
                <th className="px-5 py-4">{t("admin.stock","Tồn kho")}</th>
                <th className="px-5 py-4">{t("common.status","Trạng thái")}</th>
                <th className="px-5 py-4">{t("admin.updated","Cập nhật")}</th>
                <th className="px-5 py-4 text-right">{t("admin.actions","Hành động")}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
              {loading && !books.length
                ? [...Array(5)].map((_, i) => (
                    <tr key={i} className="animate-pulse">
                      {[...Array(8)].map((_, j) => (
                        <td key={j} className="px-5 py-4"><div className="h-4 bg-slate-100 dark:bg-slate-800 rounded w-3/4"/></td>
                      ))}
                    </tr>
                  ))
                : books.map((book, index) => (
                    <motion.tr
                      key={book.id}
                      initial={{ opacity: 0, y: 8 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: index * 0.04, duration: 0.3 }}
                      className="hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <img className="h-14 w-10 rounded-lg object-cover bg-slate-100 dark:bg-slate-800 flex-shrink-0" src={book.cover} alt={book.title}/>
                          <div className="min-w-0">
                            <p className="font-semibold text-slate-900 dark:text-slate-100 truncate max-w-[180px]">{book.title}</p>
                            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5 truncate max-w-[180px]">{book.author}</p>
                            <p className="text-xs text-slate-400 dark:text-slate-500 font-mono">{book.sku}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-4 text-slate-500 dark:text-slate-400 font-mono text-xs">{book.isbn || "—"}</td>
                      <td className="px-5 py-4">
                        <span className="px-2 py-1 text-xs font-medium rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300">{book.catLabel || "—"}</span>
                      </td>
                      <td className="px-5 py-4 font-semibold text-slate-900 dark:text-slate-100">{formatVND(book.price, i18n.language)}</td>
                      <td className="px-5 py-4 text-slate-600 dark:text-slate-300">{book.stockQuantity}</td>
                      <td className="px-5 py-4">
                        <div className="flex flex-wrap gap-1">
                          <span className={`px-2.5 py-1 text-xs font-semibold rounded-full border ${STATUS_STYLES[book.status] || STATUS_STYLES.INACTIVE}`}>
                            {book.status || "—"}
                          </span>
                          {book.featured && (
                            <span className="flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded-full border border-amber-200/60 bg-amber-50 text-amber-700 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-400">
                              <Star size={9}/> {t("admin.featured","Nổi bật")}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-5 py-4 text-xs text-slate-400 dark:text-slate-500">{formatDateTime(book.updatedAt || book.createdAt, i18n.language)}</td>
                      <td className="px-5 py-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <SecondaryBtn onClick={() => manageProduct(book)}><Layers size={13}/> {t("admin.manage","Quản lý")}</SecondaryBtn>
                          <SecondaryBtn onClick={() => startEdit(book)}><Edit size={13}/> {t("common.edit","Sửa")}</SecondaryBtn>
                          <SecondaryBtn danger onClick={() => remove(book)}><Trash2 size={13}/> {t("common.delete","Xóa")}</SecondaryBtn>
                        </div>
                      </td>
                    </motion.tr>
                  ))
              }
            </tbody>
          </table>
          {!loading && !books.length && (
            <div className="flex flex-col items-center justify-center py-16 text-slate-400">
              <Package size={32} className="mb-3 opacity-40"/>
              <p className="text-sm font-semibold">{t("admin.noProducts","Không có sách nào")}</p>
            </div>
          )}
        </div>
        <div className="border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 px-5 py-3">
          <Pagination meta={pageMeta} loading={loading} onPage={changePage} t={t}/>
        </div>
      </div>

      <Drawer
        onClose={() => { setShowForm(false); setEditingProductId(null); setBookForm(emptyBookForm); }}
        open={showForm}
        size="xl"
        title={editingProductId ? t("admin.editBook","Sửa sách") : t("admin.createBook","Thêm sách mới")}
      >
              <form className="grid gap-5" onSubmit={submitBook}>
                {/* Basic info */}
                <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                  <PInput required={!editingProductId} label={t("admin.sku","SKU")} value={bookForm.sku} onChange={e => handleBookField("sku", e.target.value)} placeholder="BOOK-001"/>
                  <PInput required={!editingProductId} label={t("admin.bookTitle","Tên sách")} value={bookForm.productName} onChange={e => handleBookField("productName", e.target.value)} placeholder="Tên sách"/>
                  <PInput label={t("admin.slug","Slug")} value={bookForm.slug} onChange={e => handleBookField("slug", e.target.value)} placeholder="ten-sach"/>
                </div>
                <PTextarea required={!editingProductId} label={t("admin.description","Mô tả")} value={bookForm.description} onChange={e => handleBookField("description", e.target.value)} placeholder="Mô tả nội dung sách..."/>

                {/* Book metadata */}
                <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                  <PInput required={!editingProductId} label={t("admin.bookAuthor","Tác giả")} value={bookForm.bookAuthor} onChange={e => handleBookField("bookAuthor", e.target.value)} placeholder="Tên tác giả"/>
                  <PInput maxLength={20} label={t("admin.isbn","ISBN")} value={bookForm.isbn} onChange={e => handleBookField("isbn", e.target.value)} placeholder="978-3-16-148410-0"/>
                  <PInput label={t("admin.publisher","NXB")} value={bookForm.publisher} onChange={e => handleBookField("publisher", e.target.value)}/>
                  <PSelect label={t("admin.bookFormat","Định dạng")} value={bookForm.bookFormat} onChange={e => handleBookField("bookFormat", e.target.value)}>
                    {BOOK_FORMATS.map(f => <option key={f} value={f}>{formatLabel(f)}</option>)}
                  </PSelect>
                  <PInput label={t("admin.publicationYear","Năm XB")} value={bookForm.publicationYear} onChange={e => handleBookField("publicationYear", e.target.value)} type="number" min="1000"/>
                  <PInput label={t("admin.bookLanguage","Ngôn ngữ")} value={bookForm.bookLanguage} onChange={e => handleBookField("bookLanguage", e.target.value)} placeholder="Tiếng Việt"/>
                  <PInput label={t("admin.pageCount","Số trang")} value={bookForm.pageCount} onChange={e => handleBookField("pageCount", e.target.value)} type="number" min="1"/>
                  <PInput label={t("admin.dimensions","Kích thước")} value={bookForm.dimensions} onChange={e => handleBookField("dimensions", e.target.value)} placeholder="14x20cm"/>
                </div>

                {/* Pricing */}
                <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                  <PSelect required={!editingProductId} label={t("admin.category","Danh mục")} value={bookForm.categoryId} onChange={e => handleBookField("categoryId", e.target.value)}>
                    <option value="">{t("admin.category","Chọn danh mục")}</option>
                    {categoryOptions.map(c => <option key={c.id} value={c.id}>{c.label}</option>)}
                  </PSelect>
                  <PInput required={!editingProductId} label={t("admin.price","Giá bán")} value={bookForm.price} onChange={e => handleBookField("price", e.target.value)} type="number" min="0" placeholder="0"/>
                  <PInput label={t("admin.originalPrice","Giá gốc")} value={bookForm.originalPrice} onChange={e => handleBookField("originalPrice", e.target.value)} type="number" min="0"/>
                  <PInput label={t("admin.discountPercentage","% Giảm giá")} value={bookForm.discountPercentage} onChange={e => handleBookField("discountPercentage", e.target.value)} type="number" min="0"/>
                  <PInput label={t("admin.productBrand","Thương hiệu")} value={bookForm.brand} onChange={e => handleBookField("brand", e.target.value)}/>
                  <PInput label={t("admin.material","Chất liệu")} value={bookForm.material} onChange={e => handleBookField("material", e.target.value)}/>
                  <PInput label={t("admin.weight","Khối lượng (g)")} value={bookForm.weight} onChange={e => handleBookField("weight", e.target.value)} type="number" min="0"/>
                  <label className="flex items-center gap-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 px-4 py-3 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors">
                    <input type="checkbox" checked={bookForm.featured} onChange={e => handleBookField("featured", e.target.checked)} className="w-4 h-4 text-indigo-600 rounded"/>
                    <span className="text-sm font-semibold text-slate-700 dark:text-slate-300">{t("admin.featured","Sách nổi bật")}</span>
                  </label>
                </div>

                {/* Default variation (create only) */}
                {!editingProductId && (
                  <SubCard title={t("admin.defaultVariation","Biến thể mặc định")}>
                    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
                      <PInput required label={t("admin.variationSku","SKU biến thể")} value={bookForm.variationSku} onChange={e => handleBookField("variationSku", e.target.value)}/>
                      <PInput label={t("admin.color","Màu")} value={bookForm.variationColor} onChange={e => handleBookField("variationColor", e.target.value)}/>
                      <PInput label={t("admin.size","Cỡ")} value={bookForm.variationSize} onChange={e => handleBookField("variationSize", e.target.value)}/>
                      <PInput label={t("admin.additionalPrice","Giá thêm")} value={bookForm.variationAdditionalPrice} onChange={e => handleBookField("variationAdditionalPrice", e.target.value)} type="number" min="0"/>
                      <PInput required label={t("admin.stock","Tồn kho")} value={bookForm.stockQuantity} onChange={e => handleBookField("stockQuantity", e.target.value)} type="number" min="0"/>
                    </div>
                  </SubCard>
                )}

                <div className="flex flex-wrap gap-3 pt-1">
                  <PrimaryBtn type="submit">{editingProductId ? t("admin.updateBook","Cập nhật") : t("admin.saveBackend","Lưu sách")}</PrimaryBtn>
                  {editingProductId && <SecondaryBtn type="button" onClick={startCreate}>{t("common.cancel","Hủy")}</SecondaryBtn>}
                </div>
              </form>
      </Drawer>

      <Drawer
        onClose={() => setSelectedProduct(null)}
        open={Boolean(selectedProduct)}
        size="xl"
        title={selectedProduct ? t("admin.manageProduct", "Quản lý: {{title}}", { title: selectedProduct.title }) : ""}
      >
        {selectedProduct && (
          <>
              {detailLoading && (
                <div className="flex items-center gap-2 mb-4 text-sm font-semibold text-slate-400">
                  <RefreshCw size={14} className="animate-spin"/> {t("common.loading","Đang tải...")}
                </div>
              )}

              {/* Product overview */}
              <div className="flex gap-5 mb-6 p-4 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800">
                <img className="h-32 w-24 rounded-xl object-cover flex-shrink-0 shadow-sm" src={selectedProduct.cover} alt={selectedProduct.title}/>
                <div className="min-w-0 text-sm text-slate-600 dark:text-slate-300 space-y-1">
                  <h3 className="text-xl font-bold text-slate-900 dark:text-slate-50 truncate">{selectedProduct.title}</h3>
                  <p>{selectedProduct.author}</p>
                  <p className="font-mono text-xs">{selectedProduct.isbn || t("admin.noIsbn","Chưa có ISBN")}</p>
                  <p>{selectedProduct.publisher || "—"} {selectedProduct.publicationYear ? `· ${selectedProduct.publicationYear}` : ""}</p>
                  <p>{selectedProduct.bookLanguage || "—"} · {selectedProduct.pageCount || "—"} tr · {selectedProduct.bookFormat || "—"}</p>
                </div>
              </div>

              <div className="grid gap-6 xl:grid-cols-2">
                {/* ── Variations ── */}
                <section className="grid gap-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h4 className="font-bold text-slate-900 dark:text-slate-100">{t("admin.variations","Biến thể")}</h4>
                    <SecondaryBtn type="button" onClick={openVariationCreate}><Plus size={12}/> {t("admin.addVariation","Thêm biến thể")}</SecondaryBtn>
                  </div>
                  <div className="overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700">
                    {(selectedProduct.variations || []).map(v => (
                      <div key={v.id} className="grid gap-3 border-b border-slate-100 dark:border-slate-800 p-4 last:border-0 md:grid-cols-[1fr_110px_90px_auto] md:items-center">
                        <div>
                          <p className="font-semibold text-slate-900 dark:text-slate-100 font-mono text-sm">{v.sku}</p>
                          <p className="text-xs text-slate-400 mt-0.5">{v.color} / {v.size} / {v.active === false ? t("common.hidden","Ẩn") : t("common.active","Hiện")}</p>
                        </div>
                        <span className="text-sm text-slate-600 dark:text-slate-300">{formatVND(v.additionalPrice || 0, i18n.language)}</span>
                        <span className="text-sm text-slate-600 dark:text-slate-300">{t("admin.stockLeft","Còn {{count}}",{ count: v.stockQuantity || 0 })}</span>
                        <div className="flex flex-wrap gap-1.5">
                          <SecondaryBtn onClick={() => editVariation(v)}><Edit size={12}/> {t("common.edit","Sửa")}</SecondaryBtn>
                          <SecondaryBtn onClick={() => { setStockForm({ variationId: v.id, stockQuantity: v.stockQuantity || 0 }); setStockModalOpen(true); }}><Package size={12}/> {t("admin.stock","Kho")}</SecondaryBtn>
                          <SecondaryBtn danger onClick={() => removeVariation(v.id)}><Trash2 size={12}/></SecondaryBtn>
                        </div>
                      </div>
                    ))}
                    {!selectedProduct.variations?.length && (
                      <div className="p-5 text-sm text-slate-400 text-center">{t("admin.noVariations","Chưa có biến thể")}</div>
                    )}
                  </div>
                  <SecondaryBtn type="button" onClick={() => setStockModalOpen(true)}><Package size={12}/> {t("admin.updateStock","Cập nhật tồn kho")}</SecondaryBtn>
                </section>

                {/* ── Media ── */}
                <section className="grid gap-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h4 className="font-bold text-slate-900 dark:text-slate-100">{t("admin.media","Hình ảnh")}</h4>
                    <SecondaryBtn type="button" icon={Upload} onClick={() => setMediaUploadOpen(true)}>{t("admin.uploadMedia","Tải ảnh lên")}</SecondaryBtn>
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    {(selectedProduct.media || []).map(media => (
                      <div key={media.id} className="rounded-xl border border-slate-200 dark:border-slate-700 overflow-hidden">
                        <img className="aspect-[4/3] w-full object-cover" src={media.mediaUrl} alt={media.altText || selectedProduct.title}/>
                        <div className="p-3">
                          <div className="flex items-center justify-between mb-1">
                            <span className={`text-xs font-bold ${media.primary ? "text-indigo-600 dark:text-indigo-400" : "text-slate-500"}`}>
                              {media.primary ? t("admin.primary","Ảnh chính") : t("admin.mediaItem","Ảnh phụ")}
                            </span>
                            <span className="text-xs text-slate-400">{t("admin.sortOrder","Thứ tự")}: {media.sortOrder ?? 0}</span>
                          </div>
                          {media.altText && <p className="text-xs text-slate-400 truncate mb-2">{media.altText}</p>}
                          <div className="flex gap-2">
                            <SecondaryBtn onClick={() => editMedia(media)}><Edit size={12}/> {t("common.edit","Sửa")}</SecondaryBtn>
                            <SecondaryBtn danger onClick={() => removeMedia(media.id)}><Trash2 size={12}/></SecondaryBtn>
                          </div>
                        </div>
                      </div>
                    ))}
                    {!selectedProduct.media?.length && (
                      <div className="col-span-2 flex flex-col items-center justify-center py-10 rounded-xl border border-dashed border-slate-200 dark:border-slate-700 text-slate-400">
                        <Image size={28} className="mb-2 opacity-40"/>
                        <p className="text-sm">{t("admin.noMedia","Chưa có hình ảnh")}</p>
                      </div>
                    )}
                  </div>
                </section>
              </div>
          </>
        )}
      </Drawer>
      {selectedProduct && (
        <>
          <Modal
            onClose={() => { setVariationModalOpen(false); resetVariationForm(); }}
            open={variationModalOpen}
            size="lg"
            title={editingVariationId ? t("admin.editVariation","Sửa biến thể") : t("admin.addVariation","Thêm biến thể")}
          >
            <form className="grid gap-3" onSubmit={saveVariation}>
              <div className="grid gap-3 sm:grid-cols-2">
                <PInput required label={t("admin.variationSku","SKU")} value={variationForm.sku} onChange={e => setVariationForm({ ...variationForm, sku: e.target.value })}/>
                <PInput required label={t("admin.stockQuantity","Tồn kho")} value={variationForm.stockQuantity} onChange={e => setVariationForm({ ...variationForm, stockQuantity: e.target.value })} type="number" min="0"/>
                <PInput required label={t("admin.color","Màu")} value={variationForm.color} onChange={e => setVariationForm({ ...variationForm, color: e.target.value })}/>
                <PInput required label={t("admin.size","Cỡ")} value={variationForm.size} onChange={e => setVariationForm({ ...variationForm, size: e.target.value })}/>
                <PInput label={t("admin.additionalPrice","Giá thêm")} value={variationForm.additionalPrice} onChange={e => setVariationForm({ ...variationForm, additionalPrice: e.target.value })} type="number" min="0"/>
                <PInput label={t("admin.imageUrl","URL ảnh")} value={variationForm.imageUrl} onChange={e => setVariationForm({ ...variationForm, imageUrl: e.target.value })}/>
                <PInput label={t("admin.imagePublicId","Public ID")} value={variationForm.imagePublicId} onChange={e => setVariationForm({ ...variationForm, imagePublicId: e.target.value })}/>
                <label className="flex cursor-pointer items-center gap-3 rounded-lg border border-slate-200 px-3 py-2.5 transition-colors hover:bg-white dark:border-slate-700 dark:hover:bg-slate-900">
                  <input type="checkbox" checked={variationForm.active} onChange={e => setVariationForm({ ...variationForm, active: e.target.checked })} className="h-4 w-4 rounded text-indigo-600"/>
                  <span className="text-sm font-semibold text-slate-700 dark:text-slate-300">{t("common.active","Hiển thị")}</span>
                </label>
              </div>
              <div className="flex gap-2">
                <SecondaryBtn type="submit">{editingVariationId ? t("admin.updateVariation","Cập nhật") : t("admin.addVariation","Thêm")}</SecondaryBtn>
                <SecondaryBtn type="button" onClick={() => { setVariationModalOpen(false); resetVariationForm(); }}>{t("common.cancel","Hủy")}</SecondaryBtn>
              </div>
            </form>
          </Modal>
          <Modal
            onClose={() => setStockModalOpen(false)}
            open={stockModalOpen}
            title={t("admin.updateStock","Cập nhật tồn kho")}
          >
            <form className="grid gap-3" onSubmit={saveStock}>
              <PSelect required value={stockForm.variationId} onChange={e => setStockForm({ ...stockForm, variationId: e.target.value })}>
                <option value="">{t("admin.variation","Chọn biến thể")}</option>
                {(selectedProduct.variations || []).map(v => <option key={v.id} value={v.id}>{v.sku}</option>)}
              </PSelect>
              <PInput required label="" value={stockForm.stockQuantity} onChange={e => setStockForm({ ...stockForm, stockQuantity: e.target.value })} placeholder={t("admin.stockQuantity","Số lượng")} type="number" min="0"/>
              <div className="flex gap-2">
                <SecondaryBtn type="submit">{t("admin.updateStock","Cập nhật")}</SecondaryBtn>
                <SecondaryBtn type="button" onClick={() => setStockModalOpen(false)}>{t("common.cancel","Hủy")}</SecondaryBtn>
              </div>
            </form>
          </Modal>
          <Modal
            onClose={() => setMediaUploadOpen(false)}
            open={mediaUploadOpen}
            title={t("admin.uploadMedia","Tải ảnh lên")}
          >
            <form className="grid gap-3" onSubmit={saveMedia}>
              <PInput type="file" accept="image/*" onChange={e => setMediaForm({ ...mediaForm, file: e.target.files?.[0] || null })}/>
              <PInput label={t("admin.altText","Mô tả ảnh")} value={mediaForm.altText} onChange={e => setMediaForm({ ...mediaForm, altText: e.target.value })} placeholder="Mô tả ảnh bìa..."/>
              <PInput label={t("admin.sortOrder","Thứ tự hiển thị")} value={mediaForm.sortOrder} onChange={e => setMediaForm({ ...mediaForm, sortOrder: e.target.value })} type="number" min="0"/>
              <label className="flex cursor-pointer items-center gap-3 text-sm font-semibold text-slate-700 dark:text-slate-300">
                <input type="checkbox" checked={mediaForm.primary} onChange={e => setMediaForm({ ...mediaForm, primary: e.target.checked })} className="h-4 w-4 rounded text-indigo-600"/>
                {t("admin.primary","Đặt làm ảnh chính")}
              </label>
              <div className="flex gap-2">
                <SecondaryBtn type="submit" icon={Upload}>{t("admin.uploadMedia","Tải lên")}</SecondaryBtn>
                <SecondaryBtn type="button" onClick={() => setMediaUploadOpen(false)}>{t("common.cancel","Hủy")}</SecondaryBtn>
              </div>
            </form>
          </Modal>
          <Modal
            onClose={() => { setMediaEditOpen(false); setMediaEditForm(emptyMediaEditForm); }}
            open={mediaEditOpen && Boolean(mediaEditForm.id)}
            title={t("admin.editMedia","Sửa thông tin ảnh")}
          >
            <form className="grid gap-3" onSubmit={saveMediaEdit}>
              <PInput label={t("admin.altText","Mô tả ảnh")} value={mediaEditForm.altText} onChange={e => setMediaEditForm({ ...mediaEditForm, altText: e.target.value })}/>
              <PInput label={t("admin.sortOrder","Thứ tự")} value={mediaEditForm.sortOrder} onChange={e => setMediaEditForm({ ...mediaEditForm, sortOrder: e.target.value })} type="number" min="0"/>
              <div className="flex gap-4">
                <label className="flex cursor-pointer items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-300">
                  <input type="checkbox" checked={mediaEditForm.primary} onChange={e => setMediaEditForm({ ...mediaEditForm, primary: e.target.checked })} className="h-4 w-4 rounded text-indigo-600"/>
                  {t("admin.primary","Ảnh chính")}
                </label>
                <label className="flex cursor-pointer items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-300">
                  <input type="checkbox" checked={mediaEditForm.active} onChange={e => setMediaEditForm({ ...mediaEditForm, active: e.target.checked })} className="h-4 w-4 rounded text-indigo-600"/>
                  {t("common.active","Hiển thị")}
                </label>
              </div>
              <div className="flex gap-2">
                <SecondaryBtn type="submit">{t("admin.updateMedia","Cập nhật")}</SecondaryBtn>
                <SecondaryBtn type="button" onClick={() => { setMediaEditOpen(false); setMediaEditForm(emptyMediaEditForm); }}>{t("common.cancel","Hủy")}</SecondaryBtn>
              </div>
            </form>
          </Modal>
        </>
      )}
    </motion.div>
  );
}

/* ── Pure utility functions (100% preserved) ── */
function toProductQuery(filters) {
  return {
    status: filters.status || undefined, categoryId: filters.categoryId || undefined,
    keyword: filters.keyword || undefined, page: Number(filters.page || 1), size: Number(filters.size || 20),
  };
}

function productToForm(book) {
  return {
    ...emptyBookForm,
    sku: book.sku || "", productName: book.title || "", slug: book.slug || "",
    description: book.desc || "", brand: book.brand || "Aivira", material: book.material || "Book",
    bookAuthor: book.author || "", isbn: book.isbn || "", publisher: book.publisher || "",
    publicationYear: book.publicationYear || "", bookLanguage: book.bookLanguage || "",
    pageCount: book.pageCount || "", bookFormat: book.bookFormat || "PAPERBACK",
    dimensions: book.dimensions || "", categoryId: book.categoryId || "",
    price: book.price || "", originalPrice: book.priceOld || "",
    discountPercentage: book.discountPercentage || "", weight: book.weight || "",
    featured: Boolean(book.featured),
  };
}

function validateBookForm(form, editing, t) {
  const required = editing ? [] : ["sku", "productName", "description", "bookAuthor", "categoryId", "price", "variationSku", "stockQuantity"];
  const missing = required.find(f => String(form[f] ?? "").trim() === "");
  if (missing) return t("admin.validationRequired");
  if (form.isbn && String(form.isbn).length > 20) return t("admin.validationIsbn");
  const maxYear = new Date().getFullYear() + 1;
  if (form.publicationYear && (Number(form.publicationYear) < 1000 || Number(form.publicationYear) > maxYear)) return t("admin.validationYear");
  if (form.pageCount && Number(form.pageCount) <= 0) return t("admin.validationPageCount");
  for (const field of ["price", "originalPrice", "discountPercentage", "weight", "variationAdditionalPrice", "stockQuantity"]) {
    if (form[field] !== "" && form[field] !== null && Number(form[field]) < 0) return t("admin.validationNonNegative");
  }
  return "";
}

function variationPayload(form) {
  return {
    sku: form.sku, color: form.color || "Default", size: form.size || "Paperback",
    additionalPrice: Number(form.additionalPrice || 0), stockQuantity: Number(form.stockQuantity || 0),
    imageUrl: form.imageUrl || null, imagePublicId: form.imagePublicId || null, active: form.active !== false,
  };
}

function slugify(value) {
  return String(value || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase()
    .replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "");
}

function formatLabel(value) {
  return String(value || "").toLowerCase().split("_").map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(" ");
}

function createEmptyMeta(filters) {
  return readPageMeta([], { page: filters.page || 1, size: filters.size || 20, totalPages: 0 });
}
