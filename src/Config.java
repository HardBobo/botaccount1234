import java.io.*;
import java.util.Properties;

public class Config {
    private static final String CONFIG_FILE = "bot.properties";
    private static Config instance;
    private Properties properties;
    
    private Config() {
        properties = new Properties();
        loadConfig();
    }
    
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }
    
    private void loadConfig() {
        // Try to load from properties file first
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                System.out.println("Loaded configuration from " + CONFIG_FILE);
                return;
            } catch (IOException e) {
                System.err.println("Error loading config file: " + e.getMessage());
            }
        }
        
        // If no properties file, create a default one
        createDefaultConfigFile();
    }
    
    private void createDefaultConfigFile() {
        Properties defaultProps = new Properties();
        defaultProps.setProperty("lichess.api.token", "YOUR_LICHESS_BOT_TOKEN_HERE");
        defaultProps.setProperty("opening.database.path", "openingdatabank/gm_games5moves.txt");
        // NNUE defaults
        defaultProps.setProperty("nnue.enabled", "false");
        defaultProps.setProperty("nnue.path", "nnue/quantised.bin");
        
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            defaultProps.store(fos, "Bot Configuration - Replace YOUR_LICHESS_BOT_TOKEN_HERE with your actual token");
            System.out.println("Created default config file: " + CONFIG_FILE);
            System.out.println("Please edit " + CONFIG_FILE + " and add your Lichess bot token");
        } catch (IOException e) {
            System.err.println("Could not create config file: " + e.getMessage());
        }
        
        this.properties = defaultProps;
    }
    
    public String getLichessToken() {
        // First check environment variable
        String token = System.getenv("LICHESS_BOT_TOKEN");
        if (token != null && !token.isEmpty()) {
            return token;
        }
        
        // Then check properties file
        token = properties.getProperty("lichess.api.token");
        if (token == null || token.equals("YOUR_LICHESS_BOT_TOKEN_HERE")) {
            throw new RuntimeException("Lichess bot token not configured. Please set LICHESS_BOT_TOKEN environment variable or edit " + CONFIG_FILE);
        }
        
        return token;
    }
    
    public String getOpeningDatabasePath() {
        // First check environment variable
        String path = System.getenv("OPENING_DATABASE_PATH");
        if (path != null && !path.isEmpty()) {
            return path;
        }
        
        // Then check properties file
        path = properties.getProperty("opening.database.path", "openingdatabank/gm_games5moves.txt");
        
        // Make path relative to current working directory if not absolute
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), path);
        }
        
        return file.getAbsolutePath();
    }

    // --- NNUE configuration helpers ---
    public boolean isNnueEnabled() {
        String env = System.getenv("NNUE_ENABLED");
        if (env != null) {
            return env.equalsIgnoreCase("1") || env.equalsIgnoreCase("true") || env.equalsIgnoreCase("yes");
        }
        String p = properties.getProperty("nnue.enabled", "false");
        return p.equalsIgnoreCase("1") || p.equalsIgnoreCase("true") || p.equalsIgnoreCase("yes");
    }

    public String getNnuePath() {
        String env = System.getenv("NNUE_PATH");
        String path = (env != null && !env.isEmpty()) ? env : properties.getProperty("nnue.path", "");
        if (path == null || path.isEmpty()) return null;
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), path);
        }
        return file.getAbsolutePath();
    }

    // Optional NNUE sign-flip for debugging integration mismatches.
    public boolean getNnueFlipSign() {
        String env = System.getenv("NNUE_FLIP_SIGN");
        if (env != null) {
            return env.equalsIgnoreCase("1") || env.equalsIgnoreCase("true") || env.equalsIgnoreCase("yes");
        }
        String p = properties.getProperty("nnue.flip.sign", "false");
        return p.equalsIgnoreCase("1") || p.equalsIgnoreCase("true") || p.equalsIgnoreCase("yes");
    }

    // Optional NNUE mapping mode: direct (default) or premirror (legacy attempt).
    public String getNnueMapping() {
        String env = System.getenv("NNUE_MAPPING");
        String m = (env != null && !env.isEmpty()) ? env : properties.getProperty("nnue.mapping", "direct");
        return m.toLowerCase();
    }
    
    public void validateConfiguration() {
        try {
            getLichessToken();
        } catch (RuntimeException e) {
            System.err.println("Configuration Error: " + e.getMessage());
            System.exit(1);
        }
        
        String dbPath = getOpeningDatabasePath();
        if (!new File(dbPath).exists()) {
            System.err.println("Warning: Opening database file not found at: " + dbPath);
        }
    }
}
