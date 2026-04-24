package app.kyc.verification;

public class Config {
    // Centralized API configuration for production readiness
    public static final String BASE_URL = "http://192.168.1.5:3001";
    
    public static final String API_KYC = BASE_URL + "/api/kyc";
    public static final String API_USER_STATUS = BASE_URL + "/api/kyc/user/";
}
