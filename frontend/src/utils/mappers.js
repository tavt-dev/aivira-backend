const FALLBACK_IMAGE = "https://placehold.co/400x600?text=Aivira";

export function pageRows(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.records)) return payload.records;
  return [];
}

export function pageMeta(payload, defaults = {}) {
  const rows = pageRows(payload);
  const pageSize = positiveInt(payload?.pageSize ?? payload?.size ?? defaults.size, 20);
  const totalElements = nonNegativeInt(payload?.totalElements ?? payload?.total ?? defaults.totalElements, rows.length);
  const rawTotalPages = nonNegativeInt(
    payload?.totalPages ?? defaults.totalPages,
    totalElements > 0 ? Math.ceil(totalElements / Math.max(pageSize, 1)) : 1
  );
  const totalPages = totalElements > 0 ? Math.max(rawTotalPages, 1) : rawTotalPages;
  const rawCurrentPage = payload?.currentPage ?? (payload?.number != null ? Number(payload.number) + 1 : payload?.page) ?? defaults.page ?? 1;
  const currentPage = clamp(positiveInt(rawCurrentPage, 1), 1, Math.max(totalPages, 1));

  return {
    currentPage,
    totalPages,
    pageSize,
    totalElements,
    hasNext: Boolean(payload?.hasNext ?? currentPage < totalPages),
    hasPrevious: Boolean(payload?.hasPrevious ?? currentPage > 1)
  };
}

function positiveInt(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function nonNegativeInt(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
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
    brand: row?.brand || fallback.brand,
    material: row?.material || fallback.material,
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
    weight: row?.weight ?? fallback.weight,
    thumbnailUrl: row?.thumbnailUrl || fallback.thumbnailUrl,
    image,
    cover: image,
    desc: row?.description || fallback.desc || "",
    sold: Number(row?.soldCount ?? fallback.sold ?? 0),
    stockQuantity: Number(row?.stockQuantity ?? fallback.stockQuantity ?? 0),
    rating: Number(row?.averageRating ?? fallback.rating ?? 4.7),
    active: row?.active ?? fallback.active,
    featured: row?.featured ?? fallback.featured,
    status: row?.status || fallback.status,
    createdAt: row?.createdAt || fallback.createdAt,
    updatedAt: row?.updatedAt || fallback.updatedAt,
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
    approved: Boolean(row?.approved),
    visible: row?.visible !== false,
    username: row?.username || "Aivira Reader",
    adminReply: row?.adminReply || "",
    images: row?.images || [],
    createdAt: row?.createdAt,
    updatedAt: row?.updatedAt,
    deletedAt: row?.deletedAt,
    productId: row?.productId,
    productVariationId: row?.productVariationId,
    productName: row?.productName,
    sku: row?.sku,
    orderId: row?.orderId,
    orderItemId: row?.orderItemId,
    userId: row?.userId,
    moderatedBy: row?.moderatedBy,
    moderatedAt: row?.moderatedAt,
    repliedBy: row?.repliedBy,
    repliedAt: row?.repliedAt
  };
}

export function normalizeOrder(row) {
  return {
    ...row,
    id: row?.id,
    orderCode: row?.orderCode || String(row?.id || ""),
    orderStatus: row?.orderStatus || row?.status || "UNKNOWN",
    totalAmount: Number(row?.totalAmount || 0),
    subtotal: Number(row?.subtotal || 0),
    shippingFee: Number(row?.shippingFee || 0),
    discountAmount: Number(row?.discountAmount || 0),
    couponCode: row?.couponCode,
    notes: row?.notes,
    cancelReason: row?.cancelReason,
    paymentGroupCode: row?.paymentGroupCode,
    paymentMethod: row?.paymentMethod,
    paymentStatus: row?.paymentStatus,
    paidAt: row?.paidAt,
    refund: row?.refund,
    shippingRecipientName: row?.shippingRecipientName || row?.recipientName,
    shippingPhoneNumber: row?.shippingPhoneNumber || row?.phoneNumber,
    shippingAddressLine: row?.shippingAddressLine || row?.addressLine,
    shippingWard: row?.shippingWard || row?.ward,
    shippingDistrict: row?.shippingDistrict || row?.district,
    shippingCity: row?.shippingCity || row?.city,
    itemCount: row?.itemCount || row?.items?.length || 0,
    items: (row?.items || []).map(normalizeOrderItem),
    createdAt: row?.createdAt,
    updatedAt: row?.updatedAt
  };
}

export function normalizeOrderItem(row) {
  const quantity = Number(row?.quantity || 1);
  const finalPrice = Number(row?.finalPrice ?? row?.price ?? row?.totalPrice ?? 0);

  return {
    ...row,
    id: row?.id ?? row?.orderItemId,
    productId: row?.productId,
    productVariationId: row?.productVariationId,
    productName: row?.productName || row?.title || "Aivira Book",
    sku: row?.sku,
    variationColor: row?.variationColor || row?.color,
    variationSize: row?.variationSize || row?.size,
    thumbnailUrl: row?.thumbnailUrl || row?.image,
    basePrice: Number(row?.basePrice || 0),
    additionalPrice: Number(row?.additionalPrice || 0),
    discountAmount: Number(row?.discountAmount || 0),
    finalPrice,
    quantity,
    lineTotal: finalPrice * quantity
  };
}

export function normalizePaymentGroup(row) {
  return {
    ...row,
    paymentGroupCode: row?.paymentGroupCode || row?.paymentCode,
    paymentCode: row?.paymentCode || row?.paymentGroupCode,
    status: row?.status || row?.paymentStatus,
    method: row?.method || row?.paymentMethod,
    amount: Number(row?.amount ?? row?.totalAmount ?? 0),
    totalAmount: Number(row?.totalAmount ?? row?.amount ?? 0),
    providerTxnRef: row?.providerTxnRef,
    providerTransactionId: row?.providerTransactionId,
    expiresAt: row?.expiresAt,
    paidAt: row?.paidAt,
    payments: row?.payments || [],
    orders: row?.orders || [],
    paymentUrl: row?.paymentUrl,
    deeplink: row?.deeplink,
    qrCodeUrl: row?.qrCodeUrl
  };
}

export function normalizeAddress(row) {
  if (!row) return null;

  return {
    ...row,
    id: row.id,
    recipientName: row.recipientName || "",
    phoneNumber: row.phoneNumber || "",
    addressLine: row.addressLine || "",
    ward: row.ward || "",
    district: row.district || "",
    city: row.city || "",
    defaultAddress: Boolean(row.defaultAddress)
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
  const variationSku = form.variationSku || `${form.sku}-PB`;
  return {
    sku: form.sku,
    productName: form.productName,
    slug: form.slug,
    description: form.description,
    brand: form.brand || "Aivira",
    material: form.material || "Book",
    bookAuthor: form.bookAuthor,
    isbn: optionalString(form.isbn),
    publisher: optionalString(form.publisher),
    publicationYear: optionalNumber(form.publicationYear),
    bookLanguage: optionalString(form.bookLanguage),
    pageCount: optionalNumber(form.pageCount),
    bookFormat: form.bookFormat || "PAPERBACK",
    dimensions: optionalString(form.dimensions),
    categoryId: Number(form.categoryId),
    price,
    originalPrice: form.originalPrice ? Number(form.originalPrice) : price,
    discountPercentage: form.discountPercentage ? Number(form.discountPercentage) : null,
    weight: form.weight ? Number(form.weight) : null,
    variations: [
      {
        sku: variationSku,
        color: form.variationColor || "Default",
        size: form.variationSize || formatLabel(form.bookFormat || "PAPERBACK"),
        additionalPrice: form.variationAdditionalPrice ? Number(form.variationAdditionalPrice) : 0,
        stockQuantity,
        active: true
      }
    ]
  };
}

export function buildProductUpdatePayload(form) {
  return compactPayload({
    sku: form.sku,
    productName: form.productName,
    slug: form.slug,
    description: form.description,
    brand: form.brand,
    material: form.material,
    bookAuthor: form.bookAuthor,
    isbn: form.isbn,
    publisher: form.publisher,
    publicationYear: optionalNumber(form.publicationYear),
    bookLanguage: form.bookLanguage,
    pageCount: optionalNumber(form.pageCount),
    bookFormat: form.bookFormat,
    dimensions: form.dimensions,
    categoryId: optionalNumber(form.categoryId),
    price: optionalNumber(form.price),
    originalPrice: optionalNumber(form.originalPrice),
    discountPercentage: optionalNumber(form.discountPercentage),
    weight: optionalNumber(form.weight),
    featured: form.featured
  });
}

function compactPayload(payload) {
  return Object.fromEntries(Object.entries(payload).filter(([, value]) => value !== undefined));
}

function optionalString(value) {
  if (value === undefined || value === null) return null;
  const text = String(value).trim();
  return text ? text : null;
}

function optionalNumber(value) {
  if (value === undefined || value === null || value === "") return undefined;
  return Number(value);
}

function formatLabel(value) {
  return String(value || "PAPERBACK")
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
