export const now = "2026-06-11T08:00:00Z";

export const adminUser = {
  id: "admin-1",
  username: "admin",
  email: "admin@aivira.test",
  firstName: "Aivira",
  lastName: "Admin",
  roles: [{ code: "ADMIN" }],
  permissions: ["DASHBOARD_READ_ADMIN", "ORDER_MANAGE_ALL", "PRODUCT_MANAGE_ALL"]
};

export const customerUser = {
  id: "user-1",
  username: "reader",
  email: "reader@aivira.test",
  firstName: "Aivira",
  lastName: "Reader",
  roles: [{ code: "USER" }]
};

export const category = {
  id: 10,
  categoryId: 10,
  categoryName: "Programming",
  slug: "programming",
  description: "Programming books",
  active: true,
  visible: true
};

export const book = {
  id: 101,
  productId: 101,
  sku: "BOOK-CLEAN-CODE",
  productName: "Clean Architecture",
  slug: "clean-architecture",
  description: "Practical software architecture guidance.",
  bookAuthor: "Robert C. Martin",
  isbn: "9780134494166",
  publisher: "Prentice Hall",
  publicationYear: 2017,
  bookLanguage: "English",
  pageCount: 432,
  bookFormat: "PAPERBACK",
  dimensions: "23 x 15 x 3 cm",
  categoryId: category.id,
  categoryName: category.categoryName,
  categorySlug: category.slug,
  price: 450000,
  originalPrice: 520000,
  discountPercentage: 13,
  stockQuantity: 8,
  soldCount: 42,
  active: true,
  featured: true,
  status: "ACTIVE",
  thumbnailUrl: "https://example.test/clean-architecture.jpg",
  media: [
    {
      id: 501,
      mediaUrl: "https://example.test/clean-architecture.jpg",
      mediaPublicId: "books/clean-architecture",
      mediaType: "IMAGE",
      primary: true,
      active: true,
      sortOrder: 0
    }
  ],
  variations: [
    {
      id: 201,
      sku: "BOOK-CLEAN-CODE-PB",
      size: "Paperback",
      color: "Default",
      additionalPrice: 0,
      stockQuantity: 8,
      active: true
    }
  ],
  createdAt: now,
  updatedAt: now
};

export const secondBook = {
  ...book,
  id: 102,
  productId: 102,
  sku: "BOOK-DOMAIN-DESIGN",
  productName: "Domain-Driven Design",
  slug: "domain-driven-design",
  bookAuthor: "Eric Evans",
  isbn: "9780321125217",
  stockQuantity: 2,
  soldCount: 25,
  featured: false
};

export const review = {
  id: 301,
  rating: 5,
  comment: "Clear writing and excellent book quality.",
  approved: false,
  visible: true,
  username: "reader",
  userId: customerUser.id,
  productId: book.id,
  productVariationId: 201,
  productName: book.productName,
  sku: book.sku,
  orderId: 401,
  orderItemId: 501,
  adminReply: "",
  images: [],
  createdAt: now,
  updatedAt: now
};

export const order = {
  id: 401,
  orderCode: "ORD-20260611-001",
  orderStatus: "PENDING_CONFIRMATION",
  paymentMethod: "COD",
  paymentStatus: "PENDING",
  paymentGroupCode: "PG-20260611-001",
  subtotal: 900000,
  discountAmount: 90000,
  shippingFee: 0,
  totalAmount: 810000,
  itemCount: 1,
  shippingRecipientName: "Aivira Reader",
  shippingPhoneNumber: "0900000000",
  shippingAddressLine: "1 Book Street",
  shippingWard: "Ward 1",
  shippingDistrict: "District 1",
  shippingCity: "Ho Chi Minh City",
  items: [
    {
      id: 501,
      orderItemId: 501,
      productId: book.id,
      productVariationId: 201,
      productName: book.productName,
      sku: book.sku,
      variationSize: "Paperback",
      variationColor: "Default",
      quantity: 2,
      finalPrice: 405000,
      thumbnailUrl: book.thumbnailUrl
    }
  ],
  createdAt: now,
  updatedAt: now
};

export const paymentGroup = {
  paymentCode: "PG-20260611-001",
  paymentGroupCode: "PG-20260611-001",
  method: "VNPAY",
  status: "PENDING",
  amount: 810000,
  providerTxnRef: "VNP-001",
  providerTransactionId: "BANK-001",
  paidAt: null,
  expiresAt: "2026-06-11T08:15:00Z",
  payments: [
    {
      id: 601,
      orderId: order.id,
      orderCode: order.orderCode,
      method: "VNPAY",
      status: "PENDING",
      amount: 810000,
      transactionId: "VNP-001",
      paidAt: null
    }
  ],
  orders: [order]
};

export const cartItem = {
  id: 701,
  cartItemId: 701,
  productId: book.id,
  productVariationId: 201,
  productSlug: book.slug,
  productName: book.productName,
  bookAuthor: book.bookAuthor,
  thumbnailUrl: book.thumbnailUrl,
  sku: book.sku,
  size: "Paperback",
  color: "Default",
  price: 450000,
  quantity: 2,
  stockQuantity: 8,
  available: true
};

export function apiResponse(data) {
  return {
    success: true,
    message: "OK",
    data,
    timestamp: Date.now()
  };
}

export function pageResponse(data, overrides = {}) {
  return {
    data,
    currentPage: overrides.currentPage || 1,
    totalPages: overrides.totalPages || 1,
    pageSize: overrides.pageSize || data.length,
    totalElements: overrides.totalElements ?? data.length,
    hasNext: overrides.hasNext || false,
    hasPrevious: overrides.hasPrevious || false
  };
}
