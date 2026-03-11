package com.tien.aivirabackend.constant;

public class UrlConstant {
    private UrlConstant() {
    }

    public static class Auth {
        private static final String PRE_FIX = "/auth";

        public static final String LOGIN = PRE_FIX + "/token";
        public static final String REGISTER = PRE_FIX + "/register";
        public static final String VERIFY_USER = PRE_FIX + "/verify-user";
        public static final String RESEND_VERIFICATION_OTP = PRE_FIX + "/resend-verification";
        public static final String FORGOT_PASSWORD = PRE_FIX + "/forgot-password";
        public static final String RESET_PASSWORD = PRE_FIX + "/reset-password";
        public static final String REFRESH_TOKEN = PRE_FIX + "/refresh-token";
        public static final String LOGOUT = PRE_FIX + "/logout";

        private Auth() {
        }
    }
}
