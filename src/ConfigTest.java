public class ConfigTest {
    public static void main(String[] args) {
        System.out.println("=== Config Debug Test ===");
        
        try {
            Config config = Config.getInstance();
            
            System.out.println("1. Token check:");
            String token = config.getLichessToken();
            System.out.println("   Token: " + token.substring(0, 8) + "...");
            
            System.out.println("2. Opening database path:");
            String dbPath = config.getOpeningDatabasePath();
            System.out.println("   Path: " + dbPath);
            
            System.out.println("3. File existence check:");
            java.io.File dbFile = new java.io.File(dbPath);
            System.out.println("   File exists: " + dbFile.exists());
            System.out.println("   File absolute path: " + dbFile.getAbsolutePath());
            
            System.out.println("4. Configuration validation:");
            config.validateConfiguration();
            System.out.println("   Validation passed!");
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}