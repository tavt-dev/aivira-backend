import { useEffect, useState } from "react";

import {
  createAdminCategory,
  deleteAdminCategory,
  updateAdminCategory,
} from "../../api/adminApi.js";
import { getCategories } from "../../api/catalogApi.js";
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
  const [categories, setCategories] = useState([]);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    refreshCategories();
  }, []);

  async function refreshCategories() {
    try {
      const rows = await getCategories();
      setCategories(pageRows(rows).map(normalizeCategory).filter(Boolean));
    } catch (error) {
      setCategories([]);
      setMessage(error.message || "Could not load backend categories.");
    }
  }

  async function submit(event) {
    event.preventDefault();
    setMessage("");
    try {
      const payload = {
        ...form,
        parentId: form.parentId ? Number(form.parentId) : null,
        displayOrder: Number(form.displayOrder || 0),
      };
      if (editingId) await updateAdminCategory(editingId, payload);
      else await createAdminCategory(payload);
      setMessage(editingId ? "Category updated." : "Category saved through backend /admin/categories.");
      setForm(emptyForm);
      setEditingId(null);
      refreshCategories();
    } catch (error) {
      setMessage(error.message || "Backend pending: category save unavailable.");
    }
  }

  async function remove(category) {
    setMessage("");
    try {
      await deleteAdminCategory(category.id);
      setCategories((current) => current.filter((item) => item.id !== category.id));
      setMessage("Category deleted.");
    } catch (error) {
      setMessage(error.message || "Backend pending: category delete unavailable.");
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
  }

  return (
    <div className="grid gap-8">
      <PageHeader title="Admin Categories" eyebrow="Public list + /admin/categories mutations" />
      {message && <Notice>{message}</Notice>}
      <Panel title="Categories">
        <div className="overflow-hidden rounded-2xl border border-slate-200">
          {categories.map((category) => (
            <div className="grid gap-3 border-b border-slate-100 p-4 last:border-b-0 md:grid-cols-[1fr_180px_120px_auto_auto] md:items-center" key={category.id}>
              <span className="font-bold text-slate-950">{category.label}</span>
              <span className="text-sm text-slate-500">{category.slug}</span>
              <strong className="text-sm">{category.visible === false ? "Hidden" : "Visible"}</strong>
              <SmallButton onClick={() => edit(category)}>Edit</SmallButton>
              <SmallButton danger onClick={() => remove(category)}>Delete</SmallButton>
            </div>
          ))}
          {!categories.length && <div className="p-5 text-sm text-slate-500">No categories loaded.</div>}
        </div>
      </Panel>
      <Panel title={editingId ? "Edit Category" : "Create Category"}>
        <form className="grid gap-4" onSubmit={submit}>
          <div className="grid gap-4 md:grid-cols-2">
            <Input value={form.categoryName} onChange={(e) => setForm({ ...form, categoryName: e.target.value })} placeholder="Category name" required />
            <Input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} placeholder="Slug" required />
          </div>
          <Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Description" />
          <div className="grid gap-4 md:grid-cols-3">
            <Input value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} placeholder="Image URL" />
            <Input value={form.imagePublicId} onChange={(e) => setForm({ ...form, imagePublicId: e.target.value })} placeholder="Image public ID" />
            <Input value={form.displayOrder} onChange={(e) => setForm({ ...form, displayOrder: e.target.value })} placeholder="Display order" type="number" />
          </div>
          <Input value={form.parentId} onChange={(e) => setForm({ ...form, parentId: e.target.value })} placeholder="Parent ID" type="number" />
          <div className="flex flex-wrap gap-4 text-sm font-semibold text-slate-600">
            <label className="flex items-center gap-2"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active</label>
            <label className="flex items-center gap-2"><input type="checkbox" checked={form.visible} onChange={(e) => setForm({ ...form, visible: e.target.checked })} /> Visible</label>
          </div>
          <Button type="submit">{editingId ? "Update category" : "Save category"}</Button>
        </form>
      </Panel>
    </div>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="border-b border-slate-200 pb-6"><span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">{eyebrow}</span><h2 className="mt-3 font-serif text-4xl font-bold text-slate-950">{title}</h2></div>;
}
function Panel({ title, children }) {
  return <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm md:p-6"><h3 className="mb-5 font-serif text-3xl font-bold text-slate-950">{title}</h3>{children}</section>;
}
function Input(props) {
  return <input {...props} className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}
function Textarea(props) {
  return <textarea {...props} className="min-h-28 w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-slate-950 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-100" />;
}
function Button(props) {
  return <button {...props} className="rounded-full bg-slate-950 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600" />;
}
function SmallButton({ danger = false, ...props }) {
  return <button type="button" {...props} className={["rounded-full border px-3 py-1.5 text-xs font-bold transition-colors", danger ? "border-red-100 text-red-600 hover:bg-red-50" : "border-slate-200 text-slate-600 hover:bg-slate-50"].join(" ")} />;
}
function Notice({ children }) {
  return <div className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{children}</div>;
}
