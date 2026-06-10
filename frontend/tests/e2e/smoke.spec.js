import { expect, test } from "@playwright/test";

const now = "2026-06-11T08:00:00Z";

const category = {
  id: 10,
  categoryId: 10,
  categoryName: "Programming",
  slug: "programming",
  active: true,
  visible: true,
  bookCount: 2
};

const book = {
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
  stockQuantity: 8,
  soldCount: 42,
  active: true,
  featured: true,
  status: "ACTIVE",
  thumbnailUrl: "https://example.test/clean-architecture.jpg",
  media: [],
  variations: [
    { id: 201, sku: "BOOK-CLEAN-CODE-PB", size: "Paperback", color: "Default", stockQuantity: 8, active: true }
  ],
  createdAt: now,
  updatedAt: now
};

const secondBook = {
  ...book,
  id: 102,
  productId: 102,
  sku: "BOOK-DOMAIN-DESIGN",
  productName: "Domain-Driven Design",
  slug: "domain-driven-design",
  bookAuthor: "Eric Evans",
  isbn: "9780321125217",
  stockQuantity: 2,
  featured: false
};

const order = {
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
  items: [{ id: 501, orderItemId: 501, productName: book.productName, sku: book.sku, quantity: 2, finalPrice: 405000 }],
  createdAt: now,
  updatedAt: now
};

const cartItem = {
  id: 701,
  cartItemId: 701,
  productId: book.id,
  productVariationId: 201,
  productSlug: book.slug,
  productName: book.productName,
  bookAuthor: book.bookAuthor,
  sku: book.sku,
  size: "Paperback",
  color: "Default",
  price: 450000,
  quantity: 2,
  stockQuantity: 8,
  available: true
};

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    window.sessionStorage.setItem("aivira_intro_seen", "true");
  });
  await mockApi(page);
});

test("public browse and product detail smoke", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByText("Clean Architecture").first()).toBeVisible();

  await page.goto("/category/all?search=architecture&author=Robert");
  await expect(page.getByText("Clean Architecture").first()).toBeVisible();

  await page.goto("/product/clean-architecture");
  await expect(page.getByRole("heading", { name: /Clean Architecture/i })).toBeVisible();
  await expect(page.getByText("9780134494166")).toBeVisible();
  await expect(page.getByText(/Clear writing and excellent book quality/i)).toBeVisible();
});

test("customer checkout and orders smoke", async ({ page }) => {
  await seedCustomer(page);
  await page.goto("/cart");
  await expect(page.getByText("Clean Architecture").first()).toBeVisible();

  await page.goto("/checkout");
  await page.getByRole("combobox").first().selectOption("801");
  await expect(page.getByText("Architecture Week").first()).toBeVisible();
  await expect(page.getByText(/850\.000/).first()).toBeVisible();

  await page.goto("/orders");
  await expect(page.getByText("ORD-20260611-001").first()).toBeVisible();
});

test("admin dashboard, orders, discounts, reviews and payments smoke", async ({ page }) => {
  await seedAdmin(page);

  await page.goto("/admin/dashboard");
  await expect(page.getByText("Clean Architecture").first()).toBeVisible();

  await page.goto("/admin/products");
  await expect(page.getByText("Clean Architecture").first()).toBeVisible();

  await page.goto("/admin/orders?keyword=ORD-20260611-001");
  await expect(page.getByText("ORD-20260611-001").first()).toBeVisible();
  await expect(page.getByRole("button", { name: /Xác nhận|Confirm/i })).toBeVisible();

  await page.goto("/admin/discounts");
  await expect(page.getByText("AIVIRA10").first()).toBeVisible();
  await page.getByRole("button", { name: /Khuyến mãi|Promotion/i }).click();
  await expect(page.getByText("Architecture Week").first()).toBeVisible();

  await page.goto("/admin/reviews");
  await expect(page.getByText(/Clear writing and excellent book quality/i).first()).toBeVisible();

  await page.goto("/admin/payments?code=PG-20260611-001");
  await expect(page.getByText("PG-20260611-001").first()).toBeVisible();
  await page.getByRole("button", { name: /Đối soát|Reconcile/i }).click();
  await expect(page.getByText("Payment status reconciled")).toBeVisible();
});

async function seedCustomer(page) {
  await page.addInitScript(() => {
    window.localStorage.setItem("aivira_access_token", "access-token");
    window.localStorage.setItem("aivira_refresh_token", "refresh-token");
    window.localStorage.setItem(
      "aivira_user",
      JSON.stringify({ id: "user-1", username: "reader", roles: [{ code: "USER" }] })
    );
  });
}

async function seedAdmin(page) {
  await page.addInitScript(() => {
    window.localStorage.setItem("aivira_access_token", "admin-token");
    window.localStorage.setItem("aivira_refresh_token", "refresh-token");
    window.localStorage.setItem(
      "aivira_user",
      JSON.stringify({ id: "admin-1", username: "admin", roles: [{ code: "ADMIN" }] })
    );
  });
}

async function mockApi(page) {
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace("/api/v1", "");
    const method = request.method();

    if (method === "GET" && path === "/users/me") {
      const auth = request.headers().authorization || "";
      return ok(
        route,
        auth.includes("admin")
          ? { id: "admin-1", username: "admin", roles: [{ code: "ADMIN" }], permissions: ["DASHBOARD_READ_ADMIN"] }
          : { id: "user-1", username: "reader", roles: [{ code: "USER" }] }
      );
    }
    if (method === "GET" && path === "/storefront/home") {
      return ok(route, {
        featuredBooks: [book],
        newArrivals: [book, secondBook],
        bestsellingBooks: [secondBook, book],
        categoryHighlights: [{ ...category, bookCount: 2 }]
      });
    }
    if (method === "GET" && path === "/categories") return ok(route, [category]);
    if (method === "GET" && path === "/categories/tree") return ok(route, [{ ...category, children: [] }]);
    if (method === "GET" && path === "/products") return ok(route, pageResponse([book, secondBook]));
    if (method === "GET" && path.startsWith("/products/") && path.endsWith("/reviews")) {
      return ok(
        route,
        pageResponse([
          {
            id: 301,
            rating: 5,
            comment: "Clear writing and excellent book quality.",
            approved: true,
            visible: true,
            username: "reader",
            productName: book.productName,
            createdAt: now
          }
        ])
      );
    }
    if (method === "GET" && path.startsWith("/products/")) return ok(route, book);
    if (method === "GET" && path === "/cart") return ok(route, { items: [cartItem], totalAmount: 900000 });
    if (method === "GET" && path === "/users/me/addresses") {
      return ok(route, [
        {
          id: 801,
          recipientName: "Aivira Reader",
          phoneNumber: "0900000000",
          addressLine: "1 Book Street",
          city: "Ho Chi Minh City",
          defaultAddress: true
        }
      ]);
    }
    if (method === "POST" && path === "/checkout/preview") {
      return ok(route, {
        subtotal: 900000,
        promotionDiscountAmount: 50000,
        couponDiscountAmount: 0,
        discountAmount: 50000,
        shippingFee: 0,
        totalAmount: 850000,
        items: [
          {
            cartItemId: 701,
            productName: book.productName,
            quantity: 2,
            lineSubtotal: 900000,
            promotionDiscountAmount: 50000,
            promotionName: "Architecture Week",
            finalLineAmount: 850000
          }
        ],
        appliedPromotions: [{ promotionName: "Architecture Week", discountAmount: 50000 }]
      });
    }
    if (method === "GET" && path === "/orders") return ok(route, pageResponse([order]));
    if (method === "GET" && path.startsWith("/orders/")) return ok(route, order);
    if (method === "GET" && path === "/admin/dashboard/summary")
      return ok(route, {
        revenue: 810000,
        orderCount: 3,
        successfulPaymentCount: 2,
        failedPaymentCount: 1,
        newUserCount: 5,
        pendingOrderCount: 2,
        pendingPaymentCount: 1,
        lowStockCount: 1
      });
    if (method === "GET" && path === "/admin/dashboard/sales")
      return ok(route, { points: [{ date: "2026-06-11", revenue: 810000, orderCount: 1 }] });
    if (method === "GET" && path === "/admin/dashboard/orders")
      return ok(route, { statusCounts: [{ status: "PENDING_CONFIRMATION", count: 1 }] });
    if (method === "GET" && path === "/admin/dashboard/top-books")
      return ok(route, { books: [{ ...book, quantitySold: 2, revenue: 810000 }] });
    if (method === "GET" && path === "/admin/dashboard/low-stock") return ok(route, { books: [secondBook] });
    if (method === "GET" && path === "/admin/products") return ok(route, pageResponse([book]));
    if (method === "GET" && path === "/admin/orders") return ok(route, pageResponse([order]));
    if (method === "GET" && path.startsWith("/admin/orders/")) return ok(route, order);
    if (method === "GET" && path === "/admin/coupons")
      return ok(route, pageResponse([{ id: 901, code: "AIVIRA10", type: "PERCENT", value: 10, active: true }]));
    if (method === "GET" && path === "/admin/promotions")
      return ok(
        route,
        pageResponse([
          {
            id: 902,
            promotionName: "Architecture Week",
            promotionType: "FIXED",
            value: 50000,
            promotionScope: "PRODUCT",
            targetId: 101,
            active: true
          }
        ])
      );
    if (method === "GET" && path === "/admin/reviews")
      return ok(
        route,
        pageResponse([
          {
            id: 301,
            rating: 5,
            comment: "Clear writing and excellent book quality.",
            approved: false,
            visible: true,
            username: "reader",
            productName: book.productName,
            createdAt: now
          }
        ])
      );
    if (method === "GET" && path.startsWith("/admin/payments/groups/")) return ok(route, paymentGroup());
    if (method === "POST" && path.includes("/admin/payments/groups/") && path.endsWith("/reconcile")) {
      return ok(route, {
        paymentGroupCode: "PG-20260611-001",
        localStatusBefore: "PENDING",
        localStatusAfter: "SUCCESS",
        providerStatus: "SUCCESS",
        changed: true,
        message: "Payment status reconciled",
        checkedAt: now
      });
    }

    return route.fulfill({
      status: 404,
      contentType: "application/json",
      body: JSON.stringify({ success: false, message: `Unhandled ${method} ${path}` })
    });
  });
}

function pageResponse(data) {
  return {
    data,
    currentPage: 1,
    totalPages: 1,
    pageSize: data.length,
    totalElements: data.length,
    hasNext: false,
    hasPrevious: false
  };
}

function paymentGroup() {
  return {
    paymentCode: "PG-20260611-001",
    paymentGroupCode: "PG-20260611-001",
    method: "VNPAY",
    status: "PENDING",
    amount: 810000,
    providerTxnRef: "VNP-001",
    payments: [
      { id: 601, orderId: order.id, orderCode: order.orderCode, method: "VNPAY", status: "PENDING", amount: 810000 }
    ],
    orders: [order]
  };
}

async function ok(route, data) {
  return route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify({ success: true, message: "OK", data, timestamp: Date.now() })
  });
}
