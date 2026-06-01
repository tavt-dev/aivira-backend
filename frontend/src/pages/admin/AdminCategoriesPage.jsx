import { useEffect, useState } from "react";
import { createAdminCategory, deleteAdminCategory, updateAdminCategory } from "../../api/adminApi.js";
import { getCategories } from "../../api/catalogApi.js";
import { normalizeCategory, pageRows } from "../../utils/mappers.js";

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({ categoryName: "", slug: "", description: "", imageUrl: "", imagePublicId: "", displayOrder: 0, parentId: "", active: true, visible: true });
  const [editingId, setEditingId] = useState(null);

  useEffect(() => {
    refreshCategories();
  }, []);

  async function refreshCategories() {
    try {
      const rows = await getCategories();
      const list = pageRows(rows).map(normalizeCategory).filter(Boolean);
      setCategories(list);
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
        displayOrder: Number(form.displayOrder || 0)
      };
      if (editingId) await updateAdminCategory(editingId, payload);
      else await createAdminCategory(payload);
      setMessage(editingId ? "Category updated." : "Category saved through backend /admin/categories.");
      setForm({ categoryName: "", slug: "", description: "", imageUrl: "", imagePublicId: "", displayOrder: 0, parentId: "", active: true, visible: true });
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
      visible: category.visible !== false
    });
  }

  return (
    <>
      <PageHeader title="Admin Categories" eyebrow="Public list + /admin/categories mutations" />
      {message && <div className="notice page-notice">{message}</div>}
      <div className="table-card">
        {categories.map((category) => (
          <div className="table-row" key={category.id}>
            <span>{category.label}</span>
            <span>{category.slug}</span>
            <strong>{category.visible === false ? "Hidden" : "Visible"}</strong>
            <button onClick={() => edit(category)}>Edit</button>
            <button onClick={() => remove(category)}>Delete</button>
          </div>
        ))}
      </div>
      <form className="panel form-panel" onSubmit={submit}>
        <h3>{editingId ? "Edit Category" : "Create Category"}</h3>
        <div className="form-grid">
          <input value={form.categoryName} onChange={(e) => setForm({ ...form, categoryName: e.target.value })} placeholder="Category name" required />
          <input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} placeholder="Slug" required />
        </div>
        <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Description" />
        <div className="form-grid">
          <input value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} placeholder="Image URL" />
          <input value={form.imagePublicId} onChange={(e) => setForm({ ...form, imagePublicId: e.target.value })} placeholder="Image public ID" />
          <input value={form.displayOrder} onChange={(e) => setForm({ ...form, displayOrder: e.target.value })} placeholder="Display order" type="number" />
        </div>
        <input value={form.parentId} onChange={(e) => setForm({ ...form, parentId: e.target.value })} placeholder="Parent ID" type="number" />
        <label className="check-line"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active</label>
        <label className="check-line"><input type="checkbox" checked={form.visible} onChange={(e) => setForm({ ...form, visible: e.target.checked })} /> Visible</label>
        <button className="btn-fill" type="submit">{editingId ? "Update category" : "Save category"}</button>
      </form>
    </>
  );
}

function PageHeader({ title, eyebrow }) {
  return <div className="page-header"><div className="sec-chip">{eyebrow}</div><h1>{title}</h1></div>;
}
