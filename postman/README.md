# Aivira Postman Collection

Import these two files into Postman:

- `Aivira_Backend.postman_collection.json`
- `Aivira_Backend.postman_environment.json`

Recommended flow:

1. Select environment `Aivira Backend - Local`.
2. Start backend at `http://localhost:8080/api/v1`.
3. Run `Auth / Login` first. It saves `accessToken` and `refreshToken` automatically.
4. Update ids in the environment when needed: `categoryId`, `productId`, `variationId`, `addressId`, `cartItemId`, `orderId`.
5. For demo data, enable backend seed flags before startup:
   - `SEED_ENABLED=true`
   - `SEED_DEMO_CATALOG_ENABLED=true`

Admin requests require an account with admin permissions. If login fails, update `username`, `password`, and `email` in the environment.
