import { http, HttpResponse } from "msw";
import {
  adminUser,
  apiResponse,
  book,
  cartItem,
  category,
  customerUser,
  order,
  pageResponse,
  paymentGroup,
  review,
  secondBook
} from "./mockData.js";

const API = "http://localhost/api/v1";

export const handlers = [
  http.post(`${API}/auth/token`, async ({ request }) => {
    const body = await request.json().catch(() => ({}));
    if (body.username === "bad") {
      return HttpResponse.json(
        { success: false, errorCode: "AUTH_INVALID_CREDENTIALS", message: "Invalid credentials", data: null },
        { status: 401 }
      );
    }
    return HttpResponse.json(apiResponse({ token: "access-token", refreshToken: "refresh-token", user: customerUser }));
  }),
  http.post(`${API}/auth/refresh-token`, () =>
    HttpResponse.json(apiResponse({ token: "new-access-token", refreshToken: "refresh-token", user: customerUser }))
  ),
  http.post(`${API}/auth/google/exchange-ticket`, async ({ request }) => {
    const body = await request.json().catch(() => ({}));
    if (!body.ticket || body.ticket === "bad-ticket") {
      return HttpResponse.json(
        {
          success: false,
          errorCode: "GOOGLE_LOGIN_TICKET_INVALID",
          message: "Google login ticket is invalid",
          data: null
        },
        { status: 400 }
      );
    }
    return HttpResponse.json(
      apiResponse({ token: "google-access-token", refreshToken: "google-refresh-token", user: customerUser })
    );
  }),
  http.get(`${API}/users/me`, ({ request }) => {
    const auth = request.headers.get("authorization") || "";
    return HttpResponse.json(apiResponse(auth.includes("admin") ? adminUser : customerUser));
  }),
  http.get(`${API}/storefront/home`, () =>
    HttpResponse.json(
      apiResponse({
        featuredBooks: [book],
        newArrivals: [book, secondBook],
        bestsellingBooks: [secondBook, book],
        categoryHighlights: [{ ...category, bookCount: 2 }]
      })
    )
  ),
  http.get(`${API}/categories`, () => HttpResponse.json(apiResponse([category]))),
  http.get(`${API}/categories/tree`, () => HttpResponse.json(apiResponse([{ ...category, children: [] }]))),
  http.get(`${API}/products`, () =>
    HttpResponse.json(apiResponse(pageResponse([book, secondBook], { pageSize: 12, totalElements: 2 })))
  ),
  http.get(`${API}/products/:slug`, ({ params }) =>
    HttpResponse.json(apiResponse({ ...book, slug: params.slug || book.slug }))
  ),
  http.get(`${API}/products/:slug/reviews`, () =>
    HttpResponse.json(
      apiResponse(
        pageResponse([{ ...review, approved: true, visible: true, adminReply: "Thanks for reading with Aivira." }])
      )
    )
  ),
  http.get(`${API}/cart`, () => HttpResponse.json(apiResponse({ items: [cartItem], totalAmount: 900000 }))),
  http.post(`${API}/cart/items`, () => HttpResponse.json(apiResponse({ items: [{ ...cartItem, quantity: 1 }] }))),
  http.put(`${API}/cart/items/:id`, () => HttpResponse.json(apiResponse(cartItem))),
  http.delete(`${API}/cart/items/:id`, () => HttpResponse.json(apiResponse(null))),
  http.get(`${API}/users/me/addresses`, () =>
    HttpResponse.json(
      apiResponse([
        {
          id: 801,
          recipientName: "Aivira Reader",
          phoneNumber: "0900000000",
          addressLine: "1 Book Street",
          ward: "Ward 1",
          district: "District 1",
          city: "Ho Chi Minh City",
          defaultAddress: true
        }
      ])
    )
  ),
  http.post(`${API}/checkout/preview`, async ({ request }) => {
    const body = await request.json();
    if (String(body.couponCode || "").toUpperCase() === "BAD") {
      return HttpResponse.json(
        { success: false, errorCode: "COUPON_INVALID", message: "Coupon is invalid", data: null },
        { status: 400 }
      );
    }
    return HttpResponse.json(
      apiResponse({
        subtotal: 900000,
        promotionDiscountAmount: 50000,
        couponDiscountAmount: body.couponCode ? 40000 : 0,
        discountAmount: body.couponCode ? 90000 : 50000,
        shippingFee: 0,
        totalAmount: body.couponCode ? 810000 : 850000,
        couponCode: body.couponCode || null,
        items: [
          {
            cartItemId: cartItem.cartItemId,
            productName: book.productName,
            quantity: 2,
            lineSubtotal: 900000,
            promotionDiscountAmount: 50000,
            promotionName: "Architecture Week",
            finalLineAmount: body.couponCode ? 810000 : 850000
          }
        ],
        appliedPromotions: [{ promotionName: "Architecture Week", discountAmount: 50000 }],
        coupon: body.couponCode ? { code: body.couponCode, discountAmount: 40000 } : null
      })
    );
  }),
  http.post(`${API}/checkout`, () =>
    HttpResponse.json(
      apiResponse({
        paymentGroupCode: paymentGroup.paymentCode,
        paymentMethod: "COD",
        paymentStatus: "PENDING",
        totalAmount: 810000,
        orders: [order]
      })
    )
  ),
  http.get(`${API}/orders`, () => HttpResponse.json(apiResponse(pageResponse([order])))),
  http.get(`${API}/orders/:id`, () => HttpResponse.json(apiResponse(order))),
  http.get(`${API}/payments/groups/:code`, () =>
    HttpResponse.json(apiResponse({ ...paymentGroup, paymentUrl: "https://pay.example.test/continue" }))
  ),
  http.post(`${API}/payments/groups/:code/retry`, () =>
    HttpResponse.json(apiResponse({ ...paymentGroup, paymentUrl: "https://pay.example.test/redirect" }))
  ),
  http.get(`${API}/admin/dashboard/summary`, () =>
    HttpResponse.json(
      apiResponse({
        revenue: 810000,
        orderCount: 3,
        successfulPaymentCount: 2,
        failedPaymentCount: 1,
        newUserCount: 5,
        pendingOrderCount: 2,
        pendingPaymentCount: 1,
        lowStockCount: 1
      })
    )
  ),
  http.get(`${API}/admin/dashboard/sales`, () =>
    HttpResponse.json(apiResponse({ points: [{ date: "2026-06-11", revenue: 810000, orderCount: 1 }] }))
  ),
  http.get(`${API}/admin/dashboard/orders`, () =>
    HttpResponse.json(apiResponse({ statusCounts: [{ status: "PENDING_CONFIRMATION", count: 1 }] }))
  ),
  http.get(`${API}/admin/dashboard/top-books`, () =>
    HttpResponse.json(apiResponse({ books: [{ ...book, quantitySold: 2, revenue: 810000 }] }))
  ),
  http.get(`${API}/admin/dashboard/low-stock`, () => HttpResponse.json(apiResponse({ books: [secondBook] }))),
  http.get(`${API}/admin/products`, () => HttpResponse.json(apiResponse(pageResponse([book])))),
  http.get(`${API}/admin/products/:id`, () => HttpResponse.json(apiResponse(book))),
  http.get(`${API}/admin/orders`, () => HttpResponse.json(apiResponse(pageResponse([order])))),
  http.get(`${API}/admin/orders/:id`, () => HttpResponse.json(apiResponse(order))),
  http.put(`${API}/admin/orders/:id/confirm`, () =>
    HttpResponse.json(apiResponse({ ...order, orderStatus: "CONFIRMED" }))
  ),
  http.get(`${API}/admin/coupons`, () =>
    HttpResponse.json(
      apiResponse(pageResponse([{ id: 901, code: "AIVIRA10", type: "PERCENT", value: 10, active: true, usedCount: 0 }]))
    )
  ),
  http.get(`${API}/admin/promotions`, () =>
    HttpResponse.json(
      apiResponse(
        pageResponse([
          {
            id: 902,
            promotionName: "Architecture Week",
            promotionType: "FIXED",
            value: 50000,
            promotionScope: "PRODUCT",
            targetId: book.id,
            active: true
          }
        ])
      )
    )
  ),
  http.get(`${API}/admin/reviews`, () => HttpResponse.json(apiResponse(pageResponse([review])))),
  http.put(`${API}/admin/reviews/:id/moderate`, async ({ request }) =>
    HttpResponse.json(apiResponse({ ...review, ...(await request.json()) }))
  ),
  http.put(`${API}/admin/reviews/:id/reply`, async ({ request }) =>
    HttpResponse.json(apiResponse({ ...review, ...(await request.json()) }))
  ),
  http.get(`${API}/admin/payments/groups/:code`, () => HttpResponse.json(apiResponse(paymentGroup))),
  http.post(`${API}/admin/payments/groups/:code/reconcile`, () =>
    HttpResponse.json(
      apiResponse({
        paymentGroupCode: paymentGroup.paymentCode,
        method: "VNPAY",
        providerTxnRef: paymentGroup.providerTxnRef,
        localStatusBefore: "PENDING",
        localStatusAfter: "SUCCESS",
        providerStatus: "SUCCESS",
        changed: true,
        message: "Payment status reconciled",
        checkedAt: "2026-06-11T08:05:00Z"
      })
    )
  )
];
