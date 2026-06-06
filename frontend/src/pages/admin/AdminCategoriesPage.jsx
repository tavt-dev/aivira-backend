import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import {
  createAdminCategory,
  deleteAdminCategory,
  updateAdminCategory,
} from "../../api/adminApi.js";
import { getCategories, getCategoryTree } from "../../api/catalogApi.js";
import { normalizeCategory, pageRows } from "../../utils/mappers.js";

const emptyForm = {
  categoryName: "",
  slug: "",
  description: "",
  imageUrl: "",
  imagePublicId: "",
  displayOrder: 0,
  parentId: "",
  active: true,
  visible: true,
};

export default function AdminCategoriesPage() {
  const { t } = useTranslation();
  const [categories, setCategories] = useState([]);
  const [tree, setTree] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);

  const flatCategories = useMemo(() => categories.map(normalizeCategory).filter(Boolean), [categories]);
  const treeRows = useMemo(() => flattenTree(tree), [tree]);

  useEffect(() => {
    refreshCategories();
  }, []);

  async function refreshCategories() {
    setLoading(true);
    try {
      const [listRows, treePayload] = await Promise.all([getCategories(), getCategoryTree()]);
      setCategories(pageRows(listRows));
      setTree(pageRows(treePayload));
    } catch (error) {
      setCategories([]);
      setTree([]);
      setMessage(error.message || t("admin.errors.categories"));
    } finally {
      setLoading(false);
    }
  }

  async function submit(event) {
    event.preventDefault();
    setMessage("");
    const validation = validateCategoryForm(form, t);
    if (validation) {
      setMessage(validation);
      return;
    }

    try {
      const payload = {
        ...form,
        parentId: form.parentId ? Number(form.parentId) : null,
        displayOrder: Number(form.displayOrder || 0),
      };
      if (editingId) await updateAdminCategory(editingId, payload);
      else await createAdminCategory(payload);
      setMessage(editingId ? t("admin.categoryUpdated") : t("admin.categorySaved"));
      resetForm();
      await refreshCategories();
    } catch (error) {
      setMessage(error.message || t("admin.errors.categorySave"));
    }
  }

  async function remove(category) {
    if (!window.confirm(t("admin.confirmDeleteCategory", { name: category.label }))) return;
    setMessage("");
    try {
      await deleteAdminCategory(category.id);
      setMessage(t("admin.categoryDeleted"));
      if (editingId === category.id) resetForm();
      await refreshCategories();
    } catch (error) {
      setMessage(error.message || t("admin.errors.categoryDelete"));
    }
  }

  function edit(category) {
    setEditingId(category.id);
    setForm({
      categoryName: category.label || "",
      slug: category.slug || "",
      description: category.description || "",
      imageUrl: category.imageUrl || "",
      imagePublicId: category.imagePublicId || "",
      displayOrder: category.displayOrder || 0,
      parentId: category.parentId || "",
      active: category.active !== false,
      visible: category.visible !== false,
    });
    setMessage("");
  }

  function resetForm() {
    setEditingId(null);
    setForm(emptyForm);
  }

  return (
    <div className="grid gap-6">
      <PageHeader title={t("admin.categoriesTitle")} eyebrow={t("admin.categoriesEyebrow")} />
      {message && <Notice>{message}</Notice>}

      <Panel title={t("admin.categoryTree")}>
        <div className="overflow-hidden rounded-xl border border-slate-200">
          {treeRows.map(({ category, level }) => (
            <div className="grid gap-3 border-b border-slate-100 p-4 last:border-b-0 md:grid-cols-[1fr_180px_120px_120px_auto] md:items-center" key={category.id}>
              <div style={{ paddingLeft: `${level * 18}px` }}>
                <p className="font-bold text-slate-950">{category.label}</p>
                <p className="text-xs text-slate-500">{category.description || "-"}</p>
              </div>
              <span className="text-sm text-slate-500">{category.slug}</span>
              <span className="text-sm font-semibold">{t("admin.orderValue", { value: category.displayOrder ?? 0 })}</span>
              <div className="flex flex-wrap gap-1">
                <Badge>{category.active === false ? t("common.hidden") : t("common.active")}</Badge>
                <Badge>{category.visible === false ? t("common.hidden") : t("common.visible")}</Badge>
              </div>
              <div className="flex flex-wrap gap-2">
                <SmallButton onClick={() => edit(category)}>{t("common.edit")}</SmallButton>
                <SmallButton danger onClick={() => remove(category)}>{t("common.delete")}</SmallButton>
              </div>
            </div>
          ))}
          {loading && <div className="p-5 text-sm font-semibold text-slate-500">{t("common.loading")}</div>}
          {!loading && !treeRows.length && <div className="p-5 text-sm text-slate-500">{t("admin.noCategories")}</div>}
        </div>
      </Panel>

      <Panel title={editingId ? t("admin.editCategory") : t("admin.createCategory")}>
        <form className="grid gap-4" onSubmit={submit}>
          <div className="flex flex-wrap justify-between gap-3">
            <p className="text-sm text-slate-500">{t("admin.categoryFormHelp")}</p>
            {editingId && <Button secondary type="button" onClick={resetForm}>{t("admin.newCategory")}</Button>}
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <Input value={form.categoryName} onChange={(e) => setForm({ ...form, categoryName: e.target.value })} placeholder={t("admin.categoryName")} required />
            <Input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} placeholder={t("admin.slug")} />
          </div>
          <Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder={t("admin.description")} required />
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <Input value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} placeholder={t("admin.imageUrl")} />
            <Input value={form.imagePublicId} onChange={(e) => setForm({ ...form, imagePublicId: e.target.value })} placeholder={t("admin.imagePublicId")} />
            <Input value={form.displayOrder} onChange={(e) => setForm({ ...form, displayOrder: e.target.value })} placeholder={t("admin.displayOrder")} type="number" min="0" />
            <Select value={form.parentId} onChange={(e) => setForm({ ...form, parentId: e.target.value })}>
              <option value="">{t("admin.rootCategory")}</option>
              {flatCategories
                .filter((category) => category.id !== editingId)
                .map((category) => <option key={category.id} value={category.id}>{category.label}</option>)}
            </Select>
          </div>
          <div className="flex flex-wrap gap-4 text-sm font-semibold text-slate-600">
            <label className="flex items-center gap-2">
              <input checked={form.active} type="checkbox" onChange={(e) => setForm({ ...form, active: e.target.checked })} />
              {t("common.active")}
            </label>
            <label className="flex items-center gap-2">
              <input checked={form.visible} type="checkbox" onChange={(e) => setForm({ ...form, visible: e.target.checked })} />
              {t("common.visible")}
            </label>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button type="submit">{editingId ? t("admin.updateCategory") : t("admin.saveCategory")}</Button>
            {editingId && <Button secondary type="button" onClick={resetForm}>{t("common.cancel")}</Button>}
          </div>
        </form>
      </Panel>
    </div>
  );
}

function flattenTree(rows, level = 0) {
  return pageRows(rows).flatMap((row) => {
    const category = normalizeCategory(row);
    if (!category) return [];
    return [
      { category, level },
      ...flattenTree(row.children || [], level + 1),
    ];
  });
}

function validateCategoryForm(form, t) {
  if (!String(form.categoryName || "").trim()) return t("admin.validationCategoryName");
  if (!String(form.description || "").trim()) return t("admin.validationCategoryDescription");
  if (form.displayOrder !== "" && Number(form.displayOrder) < 0) return t("admin.validationNonNegative");
  return "";
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

function Badge({ children }) {
  return <span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-bold text-slate-600">{children}</span>;
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
  return <button {...props} className={["rounded-full px-5 py-3 text-sm font-bold transition-colors", secondary ? "border border-slate-200 text-slate-700 hover:bg-slate-50" : "bg-slate-950 text-white hover:bg-blue-600"].join(" ")} />;
}

function SmallButton({ danger = false, ...props }) {
  return <button type="button" {...props} className={["rounded-full border px-3 py-1.5 text-xs font-bold transition-colors", danger ? "border-red-100 text-red-600 hover:bg-red-50" : "border-slate-200 text-slate-600 hover:bg-slate-50"].join(" ")} />;
}

function Notice({ children }) {
  return <div className="rounded-xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{children}</div>;
}
