package app.kyc.verification;

public class Config {
    // ── API Configuration ─────────────────────────────────────────────────
    // Backend is a Next.js app located in /backend.
    // Run it with: cd backend && npm install && npm run dev
    // Change this IP to match your machine's local network IP.
    public static final String BASE_URL = "http://192.168.1.5:3001";

    public static final String API_KYC = BASE_URL + "/api/kyc";
    public static final String API_USER_STATUS = BASE_URL + "/api/kyc/user/";
}
