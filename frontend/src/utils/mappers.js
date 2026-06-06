const FALLBACK_IMAGE = "https://placehold.co/400x600?text=Aivira";

export function pageRows(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.records)) return payload.records;
  return [];
}

export function normalizeCategory(row) {
  if (!row) return null;
  return {
    id: row.id ?? row.categoryId ?? row.slug,
    slug: row.slug || row.categorySlug || String(row.id ?? row.categoryId),
    label: row.categoryName || row.name || row.label || row.slug,
    categoryName: row.categoryName || row.name || row.label || row.slug,
    description: row.description,
    imageUrl: row.imageUrl,
    imagePublicId: row.imagePublicId,
    displayOrder: row.displayOrder,
    parentId: row.parentId,
    active: row.active,
    visible: row.visible
  };
}

export function normalizeCategoryHighlight(row, fallback = {}) {
  if (!row) return null;
  return {
    id: row.categoryId ?? row.id ?? fallback.id ?? row.slug,
    categoryId: row.categoryId ?? row.id ?? fallback.categoryId,
    slug: row.slug || fallback.slug || String(row.categoryId ?? row.id ?? fallback.id ?? ""),
    label: row.categoryName || row.name || row.label || fallback.label || row.slug,
    categoryName: row.categoryName || row.name || row.label || fallback.categoryName || row.slug,
    description: row.description || fallback.description || "",
    imageUrl: row.imageUrl || fallback.imageUrl,
    imagePublicId: row.imagePublicId || fallback.imagePublicId,
    displayOrder: row.displayOrder ?? fallback.displayOrder,
    bookCount: Number(row.bookCount ?? fallback.bookCount ?? 0)
  };
}

export function normalizeBook(row, fallback = {}) {
  const variation = row?.variations?.find((item) => item.active) || row?.variations?.[0] || {};
  const image = row?.thumbnailUrl || row?.image || row?.cover || row?.media?.find((item) => item.primary)?.mediaUrl || fallback.cover || fallback.image || FALLBACK_IMAGE;
  return {
    id: row?.id ?? fallback.id ?? row?.slug,
    productId: row?.productId ?? row?.id ?? fallback.id,
    slug: row?.slug || fallback.slug || String(row?.id ?? fallback.id ?? ""),
    title: row?.productName || row?.title || fallback.title || "Untitled book",
    author: row?.author || row?.bookAuthor || row?.brand || fallback.author || "Aivira",
    cat: row?.categorySlug || fallback.cat,
    catLabel: row?.categoryName || fallback.catLabel || "Books",
    categoryId: row?.categoryId ?? fallback.categoryId,
    categoryName: row?.categoryName || fallback.categoryName,
    categorySlug: row?.categorySlug || fallback.categorySlug,
    sku: row?.sku || fallback.sku,
    isbn: row?.isbn || fallback.isbn,
    publisher: row?.publisher || fallback.publisher,
    publicationYear: row?.publicationYear ?? fallback.publicationYear,
    bookLanguage: row?.bookLanguage || fallback.bookLanguage,
    pageCount: row?.pageCount ?? fallback.pageCount,
    bookFormat: row?.bookFormat || fallback.bookFormat,
    dimensions: row?.dimensions || fallback.dimensions,
    price: Number(row?.price ?? fallback.price ?? 0),
    priceOld: Number(row?.originalPrice ?? fallback.priceOld ?? row?.price ?? fallback.price ?? 0),
    discountPercentage: Number(row?.discountPercentage ?? fallback.discountPercentage ?? 0),
    thumbnailUrl: row?.thumbnailUrl || fallback.thumbnailUrl,
    image,
    cover: image,
    desc: row?.description || fallback.desc || "",
    sold: Number(row?.soldCount ?? fallback.sold ?? 0),
    stockQuantity: Number(row?.stockQuantity ?? fallback.stockQuantity ?? 0),
    rating: Number(row?.averageRating ?? fallback.rating ?? 4.7),
    productVariationId: variation.id || row?.productVariationId,
    variations: (row?.variations || []).map((item) => ({
      ...item,
      additionalPrice: Number(item.additionalPrice || 0),
      stockQuantity: Number(item.stockQuantity || 0),
      active: item.active !== false
    })),
    media: row?.media || []
  };
}

export function normalizeReview(row) {
  return {
    ...row,
    id: row?.id,
    rating: Number(row?.rating || 0),
    comment: row?.comment || "",
    username: row?.username || "Aivira Reader",
    adminReply: row?.adminReply || "",
    images: row?.images || [],
    createdAt: row?.createdAt,
    orderId: row?.orderId,
    orderItemId: row?.orderItemId,
    userId: row?.userId
  };
}

export function normalizeOrder(row) {
  return {
    ...row,
    id: row?.id,
    orderCode: row?.orderCode || String(row?.id || ""),
    orderStatus: row?.orderStatus || row?.status || "UNKNOWN",
    totalAmount: Number(row?.totalAmount || 0),
    paymentGroupCode: row?.paymentGroupCode,
    paymentMethod: row?.paymentMethod,
    paymentStatus: row?.paymentStatus,
    itemCount: row?.itemCount || row?.items?.length || 0
  };
}

export function normalizePaymentGroup(row) {
  return {
    ...row,
    paymentGroupCode: row?.paymentGroupCode || row?.paymentCode,
    status: row?.status || row?.paymentStatus,
    method: row?.method || row?.paymentMethod,
    totalAmount: Number(row?.totalAmount ?? row?.amount ?? 0),
    paymentUrl: row?.paymentUrl,
    deeplink: row?.deeplink,
    qrCodeUrl: row?.qrCodeUrl
  };
}

export function normalizeCartItem(item) {
  const image = item.thumbnailUrl || item.image || item.cover || FALLBACK_IMAGE;
  const quantity = Number(item.quantity || 1);
  const price = Number(item.finalPrice || item.price || item.basePrice || 0);
  return {
    id: item.id || item.cartItemId || item.productId,
    cartItemId: item.cartItemId || item.id,
    productId: item.productId || item.id,
    slug: item.productSlug || item.slug || String(item.productId || item.id),
    title: item.productName || item.title || "Aivira Book",
    author: item.author || item.bookAuthor || item.brand || "Aivira",
    sku: item.sku,
    color: item.color,
    size: item.size,
    price,
    basePrice: Number(item.basePrice || price || 0),
    additionalPrice: Number(item.additionalPrice || 0),
    image,
    cover: image,
    quantity,
    lineSubtotal: price * quantity,
    productVariationId: item.productVariationId,
    stockQuantity: item.stockQuantity == null ? null : Number(item.stockQuantity),
    available: item.available !== false
  };
}

export function buildProductPayload(form) {
  const price = Number(form.price || 0);
  const stockQuantity = Number(form.stockQuantity || 0);
  return {
    sku: form.sku,
    productName: form.productName,
    slug: form.slug,
    description: form.description,
    brand: form.brand || "Aivira",
    material: form.material || "Book",
    categoryId: Number(form.categoryId),
    price,
    originalPrice: form.originalPrice ? Number(form.originalPrice) : price,
    discountPercentage: form.discountPercentage ? Number(form.discountPercentage) : null,
    weight: form.weight ? Number(form.weight) : null,
    variations: [
      {
        sku: `${form.sku}-DEFAULT`,
        color: "Default",
        size: "Default",
        additionalPrice: 0,
        stockQuantity,
        active: true
      }
    ]
  };
}
