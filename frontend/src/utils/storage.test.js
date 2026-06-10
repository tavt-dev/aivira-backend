import { describe, expect, it } from "vitest";
import { getAuthSnapshot, saveAuth } from "./storage.js";

describe("auth storage", () => {
  it("stores access and refresh tokens from backend token shape", () => {
    saveAuth({
      token: "access-token",
      refreshToken: "refresh-token",
      user: { id: "user-1", username: "reader" }
    });

    expect(getAuthSnapshot()).toEqual({
      accessToken: "access-token",
      refreshToken: "refresh-token",
      user: { id: "user-1", username: "reader" }
    });
  });

  it("stores access token from alternate backend shapes", () => {
    saveAuth({ access_token: "snake-token", refresh_token: "snake-refresh" });
    expect(getAuthSnapshot().accessToken).toBe("snake-token");
    expect(getAuthSnapshot().refreshToken).toBe("snake-refresh");

    saveAuth({ jwt: "jwt-token" });
    expect(getAuthSnapshot().accessToken).toBe("jwt-token");
  });
});
