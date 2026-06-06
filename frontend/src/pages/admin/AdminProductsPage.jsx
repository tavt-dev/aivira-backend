import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

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
import { useConfirm } from "../../components/ui/index.jsx";
import { getCategories } from "../../api/catalogApi.js";
import { formatDateTime, formatVND } from "../../utils/formatters.js";
import {
  buildProductPayload,
  buildProductUpdatePayload,
  normalizeBook,
  normalizeCategory,
  pageRows,
} from "../../utils/mappers.js";

const PRODUCT_STATUSES = ["DRAFT", "PENDING_REVIEW", "ACTIVE", "INACTIVE", "REJECTED"];
const BOOK_FORMATS = ["PAPERBACK", "HARDCOVER", "EBOOK", "BOXSET", "OTHER"];
const PAGE_SIZES = [10, 20, 50];

const emptyFilters = {
  keyword: "",
  status: "",
  categoryId: "",
  page: 1,
  size: 20,
};

const emptyBookForm = {
  sku: "",
  productName: "",
  slug: "",
  description: "",
  brand: "Aivira",
  material: "Book",
  bookAuthor: "",
  isbn: "",
  publisher: "",
  publicationYear: "",
  bookLanguage: "",
  pageCount: "",
  bookFormat: "PAPERBACK",
  dimensions: "",
  categoryId: "",
  price: "",
  originalPrice: "",
  discountPercentage: "",
  weight: "",
  featured: false,
  variationSku: "",
  variationColor: "Default",
  variationSize: "Paperback",
  variationAdditionalPrice: 0,
  stockQuantity: "",
};

const emptyVariationForm = {
  sku: "",
  color: "Default",
  size: "Paperback",
  additionalPrice: 0,
  stockQuantity: 0,
  imageUrl: "",
  imagePublicId: "",
  active: true,
};

const emptyMediaForm = {
  file: null,
  altText: "",
  sortOrder: 0,
  primary: true,
};

const emptyMediaEditForm = {
  id: "",
  altText: "",
  sortOrder: 0,
  primary: false,
  active: true,
};

export default function AdminProductsPage() {
  const { t, i18n } = useTranslation();
  const confirm = useConfirm();
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

  const categoryOptions = useMemo(() => categories.map(normalizeCategory).filter(Boolean), [categories]);

  useEffect(() => {
    refreshCategories();
  }, []);

  useEffect(() => {
    refreshAdminProducts(appliedFilters);
  }, [appliedFilters]);

  async function refreshCategories() {
    try {
      const rows = await getCategories();
      setCategories(pageRows(rows));
    } catch {
      setCategories([]);
    }
  }

  async function refreshAdminProducts(nextFilters = appliedFilters) {
    setLoading(true);
    setMessage("");
    try {
      const page = await getAdminProducts(toProductQuery(nextFilters));
      const rows = pageRows(page).map((row) => normalizeBook(row));
      setBooks(rows);
      setPageMeta({
        currentPage: Number(page?.currentPage || nextFilters.page),
        totalPages: Number(page?.totalPages || 0),
        pageSize: Number(page?.pageSize || nextFilters.size),
        totalElements: Number(page?.totalElements || rows.length),
        hasNext: Boolean(page?.hasNext),
        hasPrevious: Boolean(page?.hasPrevious),
      });
    } catch (error) {
      setBooks([]);
      setPageMeta(createEmptyMeta(nextFilters));
      setMessage(error.message || t("admin.errors.products"));
    } finally {
      setLoading(false);
    }
  }

  async function refreshSelectedProduct(productId = selectedProduct?.id) {
    if (!productId) return;
    setDetailLoading(true);
    try {
      const detail = await getAdminProduct(productId);
      setSelectedProduct(normalizeBook(detail));
    } catch (error) {
      setMessage(error.message || t("admin.errors.productDetail"));
    } finally {
      setDetailLoading(false);
    }
  }

  function submitFilters(event) {
    event.preventDefault();
    setAppliedFilters({ ...filters, page: 1, size: Number(filters.size || 20) });
    setFilters((current) => ({ ...current, page: 1 }));
  }

  function clearFilters() {
    setFilters(emptyFilters);
    setAppliedFilters(emptyFilters);
  }

  function changePage(page) {
    const nextPage = Math.max(1, page);
    setFilters((current) => ({ ...current, page: nextPage }));
    setAppliedFilters((current) => ({ ...current, page: nextPage }));
  }

  function changePageSize(size) {
    const next = { ...filters, page: 1, size: Number(size || 20) };
    setFilters(next);
    setAppliedFilters(next);
  }

  function startCreate() {
    setEditingProductId(null);
    setSlugTouched(false);
    setBookForm(emptyBookForm);
    setMessage("");
  }

  function startEdit(book) {
    setEditingProductId(book.id);
    setSlugTouched(true);
    setBookForm(productToForm(book));
    setMessage("");
  }

  function handleBookField(field, value) {
    if (field === "productName") {
      setBookForm((current) => ({
        ...current,
        productName: value,
        slug: !slugTouched && !editingProductId ? slugify(value) : current.slug,
      }));
      return;
    }

    if (field === "slug") setSlugTouched(true);

    setBookForm((current) => {
      const next = { ...current, [field]: value };
      if (field === "bookFormat" && !editingProductId) {
        next.variationSize = formatLabel(value);
      }
      if (field === "sku" && !editingProductId && !current.variationSku) {
        next.variationSku = value ? `${value}-PB` : "";
      }
      return next;
    });
  }

  async function submitBook(event) {
    event.preventDefault();
    setMessage("");
    const validation = validateBookForm(bookForm, Boolean(editingProductId), t);
    if (validation) {
      setMessage(validation);
      return;
    }

    try {
      const response = editingProductId
        ? await updateAdminProduct(editingProductId, buildProductUpdatePayload(bookForm))
        : await createAdminProduct(buildProductPayload(bookForm));
      const normalized = normalizeBook(response);
      setMessage(editingProductId ? t("admin.productUpdated") : t("admin.productSaved"));
      setEditingProductId(null);
      setBookForm(emptyBookForm);
      setSlugTouched(false);
      setSelectedProduct(normalized);
      await refreshAdminProducts(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.errors.productSave"));
    }
  }

  async function remove(book) {
    const confirmed = await confirm({
      title: t("common.delete"),
      message: t("admin.confirmDeleteProduct", { title: book.title }),
      confirmLabel: t("common.delete"),
      cancelLabel: t("common.cancel"),
      danger: true,
    });
    if (!confirmed) return;
    setMessage("");
    try {
      await deleteAdminProduct(book.id);
      setMessage(t("admin.productDeleted"));
      if (selectedProduct?.id === book.id) setSelectedProduct(null);
      await refreshAdminProducts(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.errors.delete"));
    }
  }

  async function manageProduct(book) {
    setSelectedProduct(book);
    resetVariationForm();
    setMediaForm(emptyMediaForm);
    setMediaEditForm(emptyMediaEditForm);
    await refreshSelectedProduct(book.id);
  }

  function resetVariationForm() {
    setEditingVariationId(null);
    setVariationForm(emptyVariationForm);
  }

  function editVariation(variation) {
    setEditingVariationId(variation.id);
    setVariationForm({
      sku: variation.sku || "",
      color: variation.color || "Default",
      size: variation.size || "Paperback",
      additionalPrice: variation.additionalPrice ?? 0,
      stockQuantity: variation.stockQuantity ?? 0,
      imageUrl: variation.imageUrl || "",
      imagePublicId: variation.imagePublicId || "",
      active: variation.active !== false,
    });
  }

  async function saveVariation(event) {
    event.preventDefault();
    if (!selectedProduct?.id) return;
    setMessage("");
    try {
      const payload = variationPayload(variationForm);
      if (editingVariationId) {
        await updateProductVariation(selectedProduct.id, editingVariationId, payload);
        setMessage(t("admin.variationUpdated"));
      } else {
        await createProductVariation(selectedProduct.id, payload);
        setMessage(t("admin.variationCreated"));
      }
      resetVariationForm();
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.errors.variationCreate"));
    }
  }

  async function removeVariation(variationId) {
    if (!selectedProduct?.id) return;
    const confirmed = await confirm({
      title: t("common.delete"),
      message: t("admin.confirmDeleteVariation"),
      confirmLabel: t("common.delete"),
      cancelLabel: t("common.cancel"),
      danger: true,
    });
    if (!confirmed) return;
    setMessage("");
    try {
      await deleteProductVariation(selectedProduct.id, variationId);
      setMessage(t("admin.variationDeleted"));
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.errors.variationDelete"));
    }
  }

  async function saveStock(event) {
    event.preventDefault();
    if (!selectedProduct?.id) return;
    setMessage("");
    try {
      await updateProductStock(selectedProduct.id, stockForm.variationId, stockForm.stockQuantity);
      setMessage(t("admin.stockUpdated"));
      setStockForm({ variationId: "", stockQuantity: "" });
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.errors.stock"));
    }
  }

  async function saveMedia(event) {
    event.preventDefault();
    if (!selectedProduct?.id) return;
    if (!mediaForm.file) {
      setMessage(t("admin.chooseImage"));
      return;
    }
    setMessage("");
    try {
      await uploadProductMedia(selectedProduct.id, mediaForm.file, {
        altText: mediaForm.altText,
        sortOrder: mediaForm.sortOrder,
        primary: mediaForm.primary,
      });
      setMessage(t("admin.mediaUploaded"));
      setMediaForm(emptyMediaForm);
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.errors.media"));
    }
  }

  function editMedia(media) {
    setMediaEditForm({
      id: media.id,
      altText: media.altText || "",
      sortOrder: media.sortOrder ?? 0,
      primary: Boolean(media.primary),
      active: media.active !== false,
    });
  }

  async function saveMediaEdit(event) {
    event.preventDefault();
    if (!selectedProduct?.id || !mediaEditForm.id) return;
    setMessage("");
    try {
      await updateProductMedia(selectedProduct.id, mediaEditForm.id, {
        altText: mediaEditForm.altText,
        sortOrder: Number(mediaEditForm.sortOrder || 0),
        primary: mediaEditForm.primary,
        active: mediaEditForm.active,
      });
      setMessage(t("admin.mediaUpdated"));
      setMediaEditForm(emptyMediaEditForm);
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.errors.media"));
    }
  }

  async function removeMedia(mediaId) {
    if (!selectedProduct?.id) return;
    const confirmed = await confirm({
      title: t("common.delete"),
      message: t("admin.confirmDeleteMedia"),
      confirmLabel: t("common.delete"),
      cancelLabel: t("common.cancel"),
      danger: true,
    });
    if (!confirmed) return;
    setMessage("");
    try {
      await deleteProductMedia(selectedProduct.id, mediaId);
      setMessage(t("admin.mediaDeleted"));
      await refreshSelectedProduct(selectedProduct.id);
      await refreshAdminProducts(appliedFilters);
    } catch (error) {
      setMessage(error.message || t("admin.errors.media"));
    }
  }

  return (
    <div className="grid gap-6">
      <PageHeader title={t("admin.productsTitle")} eyebrow={t("admin.productsReady")} />
      {message && <Notice>{message}</Notice>}

      <Panel title={t("admin.bookFilters")}>
        <form className="grid gap-3 lg:grid-cols-[1fr_180px_220px_120px_auto_auto]" onSubmit={submitFilters}>
          <Input value={filters.keyword} onChange={(e) => setFilters({ ...filters, keyword: e.target.value })} placeholder={t("admin.searchBooks")} />
          <Select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
            <option value="">{t("admin.allStatuses")}</option>
            {PRODUCT_STATUSES.map((status) => <option key={status} value={status}>{status}</option>)}
          </Select>
          <Select value={filters.categoryId} onChange={(e) => setFilters({ ...filters, categoryId: e.target.value })}>
            <option value="">{t("admin.allCategories")}</option>
            {categoryOptions.map((category) => <option key={category.id} value={category.id}>{category.label}</option>)}
          </Select>
          <Select value={filters.size} onChange={(e) => changePageSize(e.target.value)}>
            {PAGE_SIZES.map((size) => <option key={size} value={size}>{size}</option>)}
          </Select>
          <Button type="submit">{t("admin.applyFilters")}</Button>
          <Button secondary type="button" onClick={clearFilters}>{t("admin.clearFilters")}</Button>
        </form>
      </Panel>

      <Panel title={t("admin.booksList")}>
        <div className="overflow-x-auto rounded-xl border border-slate-200">
          <table className="min-w-[980px] w-full border-collapse text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">{t("admin.book")}</th>
                <th className="px-4 py-3">{t("admin.isbn")}</th>
                <th className="px-4 py-3">{t("admin.category")}</th>
                <th className="px-4 py-3">{t("admin.price")}</th>
                <th className="px-4 py-3">{t("admin.stock")}</th>
                <th className="px-4 py-3">{t("common.status")}</th>
                <th className="px-4 py-3">{t("admin.updated")}</th>
                <th className="px-4 py-3">{t("admin.actions")}</th>
              </tr>
            </thead>
            <tbody>
              {books.map((book) => (
                <tr className="border-t border-slate-100 align-middle" key={book.id}>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <img className="h-16 w-11 rounded-md object-cover ring-1 ring-slate-200" src={book.cover} alt={book.title} />
                      <div>
                        <p className="font-bold text-slate-950">{book.title}</p>
                        <p className="text-xs text-slate-500">{book.author}</p>
                        <p className="text-xs text-slate-400">{book.sku}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-slate-600">{book.isbn || "-"}</td>
                  <td className="px-4 py-3 text-slate-600">{book.catLabel}</td>
                  <td className="px-4 py-3 font-semibold">{formatVND(book.price, i18n.language)}</td>
                  <td className="px-4 py-3">{book.stockQuantity}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={book.status} featured={book.featured} />
                  </td>
                  <td className="px-4 py-3 text-slate-500">{formatDateTime(book.updatedAt || book.createdAt, i18n.language)}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <SmallButton onClick={() => manageProduct(book)}>{t("admin.manage")}</SmallButton>
                      <SmallButton onClick={() => startEdit(book)}>{t("common.edit")}</SmallButton>
                      <SmallButton danger onClick={() => remove(book)}>{t("common.delete")}</SmallButton>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {loading && <div className="p-5 text-sm font-semibold text-slate-500">{t("common.loading")}</div>}
          {!loading && !books.length && <div className="p-5 text-sm text-slate-500">{t("admin.noProducts")}</div>}
        </div>
        <Pagination meta={pageMeta} loading={loading} onPage={changePage} t={t} />
      </Panel>

      <Panel title={editingProductId ? t("admin.editBook") : t("admin.createBook")}>
        <form className="grid gap-5" onSubmit={submitBook}>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-slate-500">{t("admin.productNote")}</p>
            <Button secondary type="button" onClick={startCreate}>{t("admin.newBook")}</Button>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            <Input required={!editingProductId} value={bookForm.sku} onChange={(e) => handleBookField("sku", e.target.value)} placeholder={t("admin.sku")} />
            <Input required={!editingProductId} value={bookForm.productName} onChange={(e) => handleBookField("productName", e.target.value)} placeholder={t("admin.bookTitle")} />
            <Input value={bookForm.slug} onChange={(e) => handleBookField("slug", e.target.value)} placeholder={t("admin.slug")} />
          </div>
          <Textarea required={!editingProductId} value={bookForm.description} onChange={(e) => handleBookField("description", e.target.value)} placeholder={t("admin.description")} />
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Input required={!editingProductId} value={bookForm.bookAuthor} onChange={(e) => handleBookField("bookAuthor", e.target.value)} placeholder={t("admin.bookAuthor")} />
            <Input maxLength={20} value={bookForm.isbn} onChange={(e) => handleBookField("isbn", e.target.value)} placeholder={t("admin.isbn")} />
            <Input value={bookForm.publisher} onChange={(e) => handleBookField("publisher", e.target.value)} placeholder={t("admin.publisher")} />
            <Select value={bookForm.bookFormat} onChange={(e) => handleBookField("bookFormat", e.target.value)}>
              {BOOK_FORMATS.map((format) => <option key={format} value={format}>{formatLabel(format)}</option>)}
            </Select>
            <Input value={bookForm.publicationYear} onChange={(e) => handleBookField("publicationYear", e.target.value)} placeholder={t("admin.publicationYear")} type="number" min="1000" />
            <Input value={bookForm.bookLanguage} onChange={(e) => handleBookField("bookLanguage", e.target.value)} placeholder={t("admin.bookLanguage")} />
            <Input value={bookForm.pageCount} onChange={(e) => handleBookField("pageCount", e.target.value)} placeholder={t("admin.pageCount")} type="number" min="1" />
            <Input value={bookForm.dimensions} onChange={(e) => handleBookField("dimensions", e.target.value)} placeholder={t("admin.dimensions")} />
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Select required={!editingProductId} value={bookForm.categoryId} onChange={(e) => handleBookField("categoryId", e.target.value)}>
              <option value="">{t("admin.category")}</option>
              {categoryOptions.map((category) => <option key={category.id} value={category.id}>{category.label}</option>)}
            </Select>
            <Input required={!editingProductId} value={bookForm.price} onChange={(e) => handleBookField("price", e.target.value)} placeholder={t("admin.price")} type="number" min="0" />
            <Input value={bookForm.originalPrice} onChange={(e) => handleBookField("originalPrice", e.target.value)} placeholder={t("admin.originalPrice")} type="number" min="0" />
            <Input value={bookForm.discountPercentage} onChange={(e) => handleBookField("discountPercentage", e.target.value)} placeholder={t("admin.discountPercentage")} type="number" min="0" />
            <Input value={bookForm.brand} onChange={(e) => handleBookField("brand", e.target.value)} placeholder={t("admin.brand")} />
            <Input value={bookForm.material} onChange={(e) => handleBookField("material", e.target.value)} placeholder={t("admin.material")} />
            <Input value={bookForm.weight} onChange={(e) => handleBookField("weight", e.target.value)} placeholder={t("admin.weight")} type="number" min="0" />
            <label className="flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-700">
              <input checked={bookForm.featured} type="checkbox" onChange={(e) => handleBookField("featured", e.target.checked)} />
              {t("admin.featured")}
            </label>
          </div>

          {!editingProductId && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <h4 className="mb-3 font-bold text-slate-900">{t("admin.defaultVariation")}</h4>
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
                <Input required value={bookForm.variationSku} onChange={(e) => handleBookField("variationSku", e.target.value)} placeholder={t("admin.variationSku")} />
                <Input value={bookForm.variationColor} onChange={(e) => handleBookField("variationColor", e.target.value)} placeholder={t("admin.color")} />
                <Input value={bookForm.variationSize} onChange={(e) => handleBookField("variationSize", e.target.value)} placeholder={t("admin.size")} />
                <Input value={bookForm.variationAdditionalPrice} onChange={(e) => handleBookField("variationAdditionalPrice", e.target.value)} placeholder={t("admin.additionalPrice")} type="number" min="0" />
                <Input required value={bookForm.stockQuantity} onChange={(e) => handleBookField("stockQuantity", e.target.value)} placeholder={t("admin.stock")} type="number" min="0" />
              </div>
            </div>
          )}

          <div className="flex flex-wrap gap-3">
            <Button type="submit">{editingProductId ? t("admin.updateBook") : t("admin.saveBackend")}</Button>
            {editingProductId && <Button secondary type="button" onClick={startCreate}>{t("common.cancel")}</Button>}
          </div>
        </form>
      </Panel>

      {selectedProduct && (
        <Panel title={t("admin.manageProduct", { title: selectedProduct.title })}>
          {detailLoading && <p className="mb-4 text-sm font-semibold text-slate-500">{t("common.loading")}</p>}
          <div className="mb-6 grid gap-4 lg:grid-cols-[180px_1fr_auto]">
            <img className="aspect-[3/4] w-full rounded-xl object-cover ring-1 ring-slate-200" src={selectedProduct.cover} alt={selectedProduct.title} />
            <div className="grid gap-2 text-sm text-slate-600">
              <h3 className="text-2xl font-bold text-slate-950">{selectedProduct.title}</h3>
              <p>{selectedProduct.author}</p>
              <p>{selectedProduct.isbn || t("admin.noIsbn")}</p>
              <p>{selectedProduct.publisher || "-"} {selectedProduct.publicationYear ? `- ${selectedProduct.publicationYear}` : ""}</p>
              <p>{selectedProduct.bookLanguage || "-"} / {selectedProduct.pageCount || "-"} / {selectedProduct.bookFormat || "-"}</p>
            </div>
            <Button secondary type="button" onClick={() => setSelectedProduct(null)}>{t("common.close")}</Button>
          </div>

          <div className="grid gap-6 xl:grid-cols-2">
            <section className="grid gap-4">
              <h4 className="font-bold text-slate-950">{t("admin.variations")}</h4>
              <div className="overflow-hidden rounded-xl border border-slate-200">
                {(selectedProduct.variations || []).map((variation) => (
                  <div className="grid gap-3 border-b border-slate-100 p-4 last:border-b-0 md:grid-cols-[1fr_120px_100px_auto] md:items-center" key={variation.id}>
                    <div>
                      <p className="font-bold text-slate-950">{variation.sku}</p>
                      <p className="text-xs text-slate-500">{variation.color} / {variation.size} / {variation.active === false ? t("common.hidden") : t("common.active")}</p>
                    </div>
                    <span>{formatVND(variation.additionalPrice || 0, i18n.language)}</span>
                    <span>{t("admin.stockLeft", { count: variation.stockQuantity || 0 })}</span>
                    <div className="flex flex-wrap gap-2">
                      <SmallButton onClick={() => editVariation(variation)}>{t("common.edit")}</SmallButton>
                      <SmallButton onClick={() => setStockForm({ variationId: variation.id, stockQuantity: variation.stockQuantity || 0 })}>{t("admin.stock")}</SmallButton>
                      <SmallButton danger onClick={() => removeVariation(variation.id)}>{t("common.delete")}</SmallButton>
                    </div>
                  </div>
                ))}
                {!selectedProduct.variations?.length && <div className="p-5 text-sm text-slate-500">{t("admin.noVariations")}</div>}
              </div>

              <form className="grid gap-3 rounded-xl bg-slate-50 p-4" onSubmit={saveVariation}>
                <h5 className="font-bold text-slate-900">{editingVariationId ? t("admin.editVariation") : t("admin.addVariation")}</h5>
                <div className="grid gap-3 md:grid-cols-2">
                  <Input required value={variationForm.sku} onChange={(e) => setVariationForm({ ...variationForm, sku: e.target.value })} placeholder={t("admin.variationSku")} />
                  <Input required value={variationForm.stockQuantity} onChange={(e) => setVariationForm({ ...variationForm, stockQuantity: e.target.value })} placeholder={t("admin.stockQuantity")} type="number" min="0" />
                  <Input required value={variationForm.color} onChange={(e) => setVariationForm({ ...variationForm, color: e.target.value })} placeholder={t("admin.color")} />
                  <Input required value={variationForm.size} onChange={(e) => setVariationForm({ ...variationForm, size: e.target.value })} placeholder={t("admin.size")} />
                  <Input value={variationForm.additionalPrice} onChange={(e) => setVariationForm({ ...variationForm, additionalPrice: e.target.value })} placeholder={t("admin.additionalPrice")} type="number" min="0" />
                  <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-600">
                    <input checked={variationForm.active} type="checkbox" onChange={(e) => setVariationForm({ ...variationForm, active: e.target.checked })} />
                    {t("common.active")}
                  </label>
                  <Input value={variationForm.imageUrl} onChange={(e) => setVariationForm({ ...variationForm, imageUrl: e.target.value })} placeholder={t("admin.imageUrl")} />
                  <Input value={variationForm.imagePublicId} onChange={(e) => setVariationForm({ ...variationForm, imagePublicId: e.target.value })} placeholder={t("admin.imagePublicId")} />
                </div>
                <div className="flex gap-2">
                  <Button secondary type="submit">{editingVariationId ? t("admin.updateVariation") : t("admin.addVariation")}</Button>
                  {editingVariationId && <Button secondary type="button" onClick={resetVariationForm}>{t("common.cancel")}</Button>}
                </div>
              </form>

              <form className="grid gap-3 rounded-xl bg-slate-50 p-4" onSubmit={saveStock}>
                <h5 className="font-bold text-slate-900">{t("admin.updateStock")}</h5>
                <div className="grid gap-3 md:grid-cols-[1fr_160px_auto]">
                  <Select required value={stockForm.variationId} onChange={(e) => setStockForm({ ...stockForm, variationId: e.target.value })}>
                    <option value="">{t("admin.variation")}</option>
                    {(selectedProduct.variations || []).map((variation) => <option key={variation.id} value={variation.id}>{variation.sku}</option>)}
                  </Select>
                  <Input required value={stockForm.stockQuantity} onChange={(e) => setStockForm({ ...stockForm, stockQuantity: e.target.value })} placeholder={t("admin.stockQuantity")} type="number" min="0" />
                  <Button secondary type="submit">{t("admin.updateStock")}</Button>
                </div>
              </form>
            </section>

            <section className="grid gap-4">
              <h4 className="font-bold text-slate-950">{t("admin.media")}</h4>
              <div className="grid gap-3 sm:grid-cols-2">
                {(selectedProduct.media || []).map((media) => (
                  <div className="rounded-xl border border-slate-200 p-3" key={media.id}>
                    <img className="aspect-[4/3] w-full rounded-lg object-cover" src={media.mediaUrl} alt={media.altText || selectedProduct.title} />
                    <div className="mt-3 grid gap-1 text-xs text-slate-500">
                      <strong className="text-slate-800">{media.primary ? t("admin.primary") : t("admin.mediaItem")}</strong>
                      <span>{media.altText || "-"}</span>
                      <span>{t("admin.sortOrder")}: {media.sortOrder ?? 0} / {media.active === false ? t("common.hidden") : t("common.active")}</span>
                    </div>
                    <div className="mt-3 flex gap-2">
                      <SmallButton onClick={() => editMedia(media)}>{t("common.edit")}</SmallButton>
                      <SmallButton danger onClick={() => removeMedia(media.id)}>{t("common.delete")}</SmallButton>
                    </div>
                  </div>
                ))}
                {!selectedProduct.media?.length && <div className="rounded-xl border border-slate-200 p-5 text-sm text-slate-500">{t("admin.noMedia")}</div>}
              </div>

              <form className="grid gap-3 rounded-xl bg-slate-50 p-4" onSubmit={saveMedia}>
                <h5 className="font-bold text-slate-900">{t("admin.uploadMedia")}</h5>
                <Input type="file" accept="image/*" onChange={(e) => setMediaForm({ ...mediaForm, file: e.target.files?.[0] || null })} />
                <Input value={mediaForm.altText} onChange={(e) => setMediaForm({ ...mediaForm, altText: e.target.value })} placeholder={t("admin.altText")} />
                <Input value={mediaForm.sortOrder} onChange={(e) => setMediaForm({ ...mediaForm, sortOrder: e.target.value })} placeholder={t("admin.sortOrder")} type="number" min="0" />
                <label className="flex items-center gap-2 text-sm font-semibold text-slate-600">
                  <input checked={mediaForm.primary} type="checkbox" onChange={(e) => setMediaForm({ ...mediaForm, primary: e.target.checked })} />
                  {t("admin.primary")}
                </label>
                <Button secondary type="submit">{t("admin.uploadMedia")}</Button>
              </form>

              {mediaEditForm.id && (
                <form className="grid gap-3 rounded-xl bg-slate-50 p-4" onSubmit={saveMediaEdit}>
                  <h5 className="font-bold text-slate-900">{t("admin.editMedia")}</h5>
                  <Input value={mediaEditForm.altText} onChange={(e) => setMediaEditForm({ ...mediaEditForm, altText: e.target.value })} placeholder={t("admin.altText")} />
                  <Input value={mediaEditForm.sortOrder} onChange={(e) => setMediaEditForm({ ...mediaEditForm, sortOrder: e.target.value })} placeholder={t("admin.sortOrder")} type="number" min="0" />
                  <label className="flex items-center gap-2 text-sm font-semibold text-slate-600">
                    <input checked={mediaEditForm.primary} type="checkbox" onChange={(e) => setMediaEditForm({ ...mediaEditForm, primary: e.target.checked })} />
                    {t("admin.primary")}
                  </label>
                  <label className="flex items-center gap-2 text-sm font-semibold text-slate-600">
                    <input checked={mediaEditForm.active} type="checkbox" onChange={(e) => setMediaEditForm({ ...mediaEditForm, active: e.target.checked })} />
                    {t("common.active")}
                  </label>
                  <div className="flex gap-2">
                    <Button secondary type="submit">{t("admin.updateMedia")}</Button>
                    <Button secondary type="button" onClick={() => setMediaEditForm(emptyMediaEditForm)}>{t("common.cancel")}</Button>
                  </div>
                </form>
              )}
            </section>
          </div>
        </Panel>
      )}
    </div>
  );
}

function toProductQuery(filters) {
  return {
    status: filters.status || undefined,
    categoryId: filters.categoryId || undefined,
    keyword: filters.keyword || undefined,
    page: Number(filters.page || 1),
    size: Number(filters.size || 20),
  };
}

function productToForm(book) {
  return {
    ...emptyBookForm,
    sku: book.sku || "",
    productName: book.title || "",
    slug: book.slug || "",
    description: book.desc || "",
    brand: book.brand || "Aivira",
    material: book.material || "Book",
    bookAuthor: book.author || "",
    isbn: book.isbn || "",
    publisher: book.publisher || "",
    publicationYear: book.publicationYear || "",
    bookLanguage: book.bookLanguage || "",
    pageCount: book.pageCount || "",
    bookFormat: book.bookFormat || "PAPERBACK",
    dimensions: book.dimensions || "",
    categoryId: book.categoryId || "",
    price: book.price || "",
    originalPrice: book.priceOld || "",
    discountPercentage: book.discountPercentage || "",
    weight: book.weight || "",
    featured: Boolean(book.featured),
  };
}

function validateBookForm(form, editing, t) {
  const required = editing ? [] : ["sku", "productName", "description", "bookAuthor", "categoryId", "price", "variationSku", "stockQuantity"];
  const missing = required.find((field) => String(form[field] ?? "").trim() === "");
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
    sku: form.sku,
    color: form.color || "Default",
    size: form.size || "Paperback",
    additionalPrice: Number(form.additionalPrice || 0),
    stockQuantity: Number(form.stockQuantity || 0),
    imageUrl: form.imageUrl || null,
    imagePublicId: form.imagePublicId || null,
    active: form.active !== false,
  };
}

function slugify(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function formatLabel(value) {
  return String(value || "")
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function createEmptyMeta(filters) {
  return {
    currentPage: Number(filters.page || 1),
    totalPages: 0,
    pageSize: Number(filters.size || 20),
    totalElements: 0,
    hasNext: false,
    hasPrevious: false,
  };
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
    <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm md:p-6">
      <h3 className="mb-5 text-xl font-bold text-slate-950">{title}</h3>
      {children}
    </section>
  );
}

function Pagination({ meta, loading, onPage, t }) {
  if (!meta.totalPages || meta.totalPages <= 1) return null;
  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm">
      <span className="font-semibold text-slate-500">
        {t("catalog.pageIndicator", { page: meta.currentPage, total: meta.totalPages })} - {meta.totalElements}
      </span>
      <div className="flex flex-wrap gap-2">
        <SmallButton disabled={loading || !meta.hasPrevious} onClick={() => onPage(1)}>{t("catalog.firstPage")}</SmallButton>
        <SmallButton disabled={loading || !meta.hasPrevious} onClick={() => onPage(meta.currentPage - 1)}>{t("catalog.previousPage")}</SmallButton>
        <SmallButton disabled={loading || !meta.hasNext} onClick={() => onPage(meta.currentPage + 1)}>{t("catalog.nextPage")}</SmallButton>
        <SmallButton disabled={loading || !meta.hasNext} onClick={() => onPage(meta.totalPages)}>{t("catalog.lastPage")}</SmallButton>
      </div>
    </div>
  );
}

function StatusBadge({ status, featured }) {
  return (
    <div className="flex flex-wrap gap-1">
      <span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-bold text-slate-700">{status || "-"}</span>
      {featured && <span className="rounded-full bg-amber-100 px-2 py-1 text-xs font-bold text-amber-700">Featured</span>}
    </div>
  );
}

function Input(props) {
  return <input {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100 disabled:bg-slate-50" />;
}

function Textarea(props) {
  return <textarea {...props} className="min-h-28 w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}

function Select(props) {
  return <select {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}

function Button({ secondary = false, ...props }) {
  return <button {...props} className={["rounded-full px-5 py-3 text-sm font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50", secondary ? "border border-slate-200 text-slate-700 hover:bg-slate-50" : "bg-slate-950 text-white hover:bg-blue-600"].join(" ")} />;
}

function SmallButton({ danger = false, ...props }) {
  return <button type="button" {...props} className={["rounded-full border px-3 py-1.5 text-xs font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-50", danger ? "border-red-100 text-red-600 hover:bg-red-50" : "border-slate-200 text-slate-600 hover:bg-slate-50"].join(" ")} />;
}

function Notice({ children }) {
  return <div className="rounded-xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{children}</div>;
}
