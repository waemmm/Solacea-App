package com.solacea.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public class DatabaseManager {
    private enum DbMode { MYSQL, SQLITE, AUTO }
    private enum DbBackend { MYSQL, SQLITE }

    private static final String MYSQL_URL = readConfig(
        "SOLACEA_MYSQL_URL",
        "solacea.mysql.url",
        "jdbc:mysql://127.0.0.1:3306/solacea_db?createDatabaseIfNotExist=true"
    );
    private static final String MYSQL_USER = readConfig("SOLACEA_MYSQL_USER", "solacea.mysql.user", "root");
    private static final String MYSQL_PASS = readConfig("SOLACEA_MYSQL_PASS", "solacea.mysql.pass", "");

    private static final Path SQLITE_PATH = Paths.get(readConfig(
            "SOLACEA_SQLITE_PATH",
            "solacea.sqlite.path",
            Paths.get(System.getProperty("user.home"), ".solacea", "data", "solacea.db").toString()
    ));
    private static final String SQLITE_URL = "jdbc:sqlite:" + SQLITE_PATH.toAbsolutePath().normalize();

    private static final int CONNECT_TIMEOUT_MS = readIntConfig(
            "SOLACEA_DB_CONNECT_TIMEOUT_MS",
            "solacea.db.connectTimeoutMs",
            5000
    );
    private static final int SOCKET_TIMEOUT_MS = readIntConfig(
            "SOLACEA_DB_SOCKET_TIMEOUT_MS",
            "solacea.db.socketTimeoutMs",
            15000
    );
    private static final boolean ANNOUNCE_BACKEND = readBooleanConfig(
            "SOLACEA_DB_ANNOUNCE",
            "solacea.db.announce",
            false
    );

    private static final DbMode DB_MODE = resolveDbMode();

    private static final Object INIT_LOCK = new Object();
    private static final Set<DbBackend> INITIALIZED_BACKENDS = EnumSet.noneOf(DbBackend.class);
    private static volatile DbBackend activeBackend = DB_MODE == DbMode.SQLITE ? DbBackend.SQLITE : DbBackend.MYSQL;
    private static volatile boolean backendAnnounced = false;

    // Function: getConnection - Returns a database connection and falls back to SQLite in AUTO mode if MySQL fails.
    public static Connection getConnection() throws SQLException {
        ensureBackendReady();
        try {
            return openConnection(activeBackend);
        } catch (SQLException primaryFailure) {
            if (DB_MODE == DbMode.AUTO && activeBackend == DbBackend.MYSQL) {
                switchToSqlite(primaryFailure);
                return openConnection(DbBackend.SQLITE);
            }
            throw primaryFailure;
        }
    }

    // Function: isDatabaseAvailable - Checks if the database is reachable by running a simple test query.
    public static boolean isDatabaseAvailable() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    // Function: registerUser - Creates a user only if the username does not already exist.
    public static boolean registerUser(String username, String password) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
        String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return false;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                return insertStmt.executeUpdate() > 0;
            }
        }
    }

    // Function: validateLogin - Checks if the given username and password match a saved account.
    public static boolean validateLogin(String username, String password) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // Function: saveMood - Saves one mood entry (score, trigger, notes) for a user.
    public static boolean saveMood(String user, int score, String trigger, String notes) throws SQLException {
        String query = "INSERT INTO moods (username, score, trigger_name, notes) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user);
            stmt.setInt(2, score);
            stmt.setString(3, trigger);
            stmt.setString(4, notes);
            return stmt.executeUpdate() > 0;
        }
    }

    // Function: getLatestMoodIntensity - Gets the most recent mood score for a user, or 0 when none exists.
    public static int getLatestMoodIntensity(String username) throws SQLException {
        String query = "SELECT score FROM moods WHERE username = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("score");
            }
        }
        return 0;
    }

    // Function: getAverageMoodScore - Calculates the average mood score for a user.
    public static double getAverageMoodScore(String username) throws SQLException {
        String query = "SELECT AVG(score) FROM moods WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    // Function: getMoodCount - Returns how many mood entries a user has logged.
    public static int getMoodCount(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM moods WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Function: saveJournal - Saves one journal entry for a user.
    public static void saveJournal(String username, String prompt, String entry) throws SQLException {
        String query = "INSERT INTO journals (username, prompt, entry) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, prompt);
            stmt.setString(3, entry);
            stmt.executeUpdate();
        }
    }

    // Function: getJournalCount - Returns how many journal entries a user has saved.
    public static int getJournalCount(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM journals WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Function: getHistory - Builds a newest-first mood history list for display.
    public static List<String> getHistory(String user) throws SQLException {
        List<String> history = new ArrayList<>();
        String query = "SELECT score, trigger_name FROM moods WHERE username = ? ORDER BY id DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    history.add("Intensity: " + rs.getInt("score") +
                            " | Trigger: " + rs.getString("trigger_name"));
                }
            }
        }
        return history;
    }

    // Function: temporaryResetAccount - Deletes mood and journal rows for the selected user.
    public static void temporaryResetAccount(String username) throws SQLException {
        String resetMoods = "DELETE FROM moods WHERE username = ?";
        String resetJournals = "DELETE FROM journals WHERE username = ?";
        try (Connection conn = getConnection()) {
            try (PreparedStatement st1 = conn.prepareStatement(resetMoods)) {
                st1.setString(1, username);
                st1.executeUpdate();
            }
            try (PreparedStatement st2 = conn.prepareStatement(resetJournals)) {
                st2.setString(1, username);
                st2.executeUpdate();
            }
        }
    }

    // Function: hasProlongedLowMood - Returns true when the last three moods are all low (score 4 or below).
    public static boolean hasProlongedLowMood(String username) throws SQLException {
        String query = "SELECT score FROM moods WHERE username = ? ORDER BY id DESC LIMIT 3";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                int count = 0;
                int lowMoodCount = 0;
                while (rs.next()) {
                    count++;
                    if (rs.getInt("score") <= 4) lowMoodCount++;
                }
                return count == 3 && lowMoodCount == 3;
            }
        }
    }

    // Function: ensureBackendReady - Initializes preferred backend(s) and picks the first available one.
    private static void ensureBackendReady() throws SQLException {
        if (INITIALIZED_BACKENDS.contains(activeBackend)) {
            announceActiveBackendOnce();
            return;
        }

        synchronized (INIT_LOCK) {
            if (INITIALIZED_BACKENDS.contains(activeBackend)) {
                announceActiveBackendOnce();
                return;
            }

            SQLException lastError = null;
            for (DbBackend backend : preferredBackends()) {
                try {
                    initializeBackend(backend);
                    activeBackend = backend;
                    announceActiveBackendOnce();
                    return;
                } catch (SQLException e) {
                    lastError = e;
                    System.err.println("Database backend " + backend + " is not available: " + e.getMessage());
                }
            }

            throw lastError != null ? lastError : new SQLException("No database backend is available.");
        }
    }

    // Function: initializeBackend - Opens a backend connection once and applies required schema tables/indexes.
    private static void initializeBackend(DbBackend backend) throws SQLException {
        if (INITIALIZED_BACKENDS.contains(backend)) return;

        try (Connection conn = openConnection(backend)) {
            applySchema(conn, backend);
            INITIALIZED_BACKENDS.add(backend);
        }
    }

    // Function: switchToSqlite - Switches active backend to SQLite after a MySQL connection failure.
    private static void switchToSqlite(SQLException mysqlFailure) throws SQLException {
        synchronized (INIT_LOCK) {
            if (activeBackend == DbBackend.SQLITE) return;

            System.err.println("MySQL connection failed. Switching to SQLite fallback. Cause: " + mysqlFailure.getMessage());
            initializeBackend(DbBackend.SQLITE);
            activeBackend = DbBackend.SQLITE;
            backendAnnounced = false;
            announceActiveBackendOnce();
        }
    }

    // Function: openConnection - Opens a connection for the selected backend type.
    private static Connection openConnection(DbBackend backend) throws SQLException {
        return backend == DbBackend.MYSQL ? openMySqlConnection() : openSqliteConnection();
    }

    // Function: openMySqlConnection - Creates a MySQL connection with configured credentials and timeout settings.
    private static Connection openMySqlConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", MYSQL_USER);
        props.setProperty("password", MYSQL_PASS);
        props.setProperty("connectTimeout", String.valueOf(CONNECT_TIMEOUT_MS));
        props.setProperty("socketTimeout", String.valueOf(SOCKET_TIMEOUT_MS));
        props.setProperty("tcpKeepAlive", "true");
        props.setProperty("allowPublicKeyRetrieval", "true");
        props.setProperty("useSSL", "false");
        return DriverManager.getConnection(MYSQL_URL, props);
    }

    // Function: openSqliteConnection - Ensures SQLite folder exists, opens SQLite, and enables foreign keys.
    private static Connection openSqliteConnection() throws SQLException {
        Path parent = SQLITE_PATH.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new SQLException("Failed to create SQLite directory: " + parent, e);
            }
        }

        Connection conn = DriverManager.getConnection(SQLITE_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    // Function: applySchema - Executes schema statements for the active backend.
    private static void applySchema(Connection conn, DbBackend backend) throws SQLException {
        List<String> statements = backend == DbBackend.MYSQL ? mysqlSchemaStatements() : sqliteSchemaStatements();
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                stmt.execute(sql);
            }
        }
    }

    // Function: mysqlSchemaStatements - Provides table/index creation SQL for MySQL.
    private static List<String> mysqlSchemaStatements() {
        return List.of(
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "username VARCHAR(64) NOT NULL UNIQUE," +
                        "password VARCHAR(255) NOT NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")",
                "CREATE TABLE IF NOT EXISTS moods (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "username VARCHAR(64) NOT NULL," +
                        "score INT NOT NULL," +
                        "trigger_name VARCHAR(128)," +
                        "notes TEXT," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "INDEX idx_moods_username_id (username, id)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS journals (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "username VARCHAR(64) NOT NULL," +
                        "prompt TEXT," +
                        "entry TEXT," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "INDEX idx_journals_username_id (username, id)" +
                        ")"
        );
    }

    // Function: sqliteSchemaStatements - Provides table/index creation SQL for SQLite.
    private static List<String> sqliteSchemaStatements() {
        return List.of(
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT NOT NULL UNIQUE," +
                        "password TEXT NOT NULL," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")",
                "CREATE TABLE IF NOT EXISTS moods (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT NOT NULL," +
                        "score INTEGER NOT NULL," +
                        "trigger_name TEXT," +
                        "notes TEXT," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")",
                "CREATE INDEX IF NOT EXISTS idx_moods_username_id ON moods(username, id)",
                "CREATE TABLE IF NOT EXISTS journals (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT NOT NULL," +
                        "prompt TEXT," +
                        "entry TEXT," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ")",
                "CREATE INDEX IF NOT EXISTS idx_journals_username_id ON journals(username, id)"
        );
    }

    // Function: preferredBackends - Returns backend priority order based on configured DB mode.
    private static List<DbBackend> preferredBackends() {
        return switch (DB_MODE) {
            case MYSQL -> List.of(DbBackend.MYSQL);
            case SQLITE -> List.of(DbBackend.SQLITE);
            case AUTO -> List.of(DbBackend.MYSQL, DbBackend.SQLITE);
        };
    }

    // Function: resolveDbMode - Reads DB mode config and maps it to MYSQL, SQLITE, or AUTO.
    private static DbMode resolveDbMode() {
        String raw = readConfig("SOLACEA_DB_MODE", "solacea.db.mode", "auto").toUpperCase(Locale.ROOT);
        return switch (raw) {
            case "MYSQL" -> DbMode.MYSQL;
            case "SQLITE" -> DbMode.SQLITE;
            default -> DbMode.AUTO;
        };
    }

    // Function: readConfig - Reads a config value from system property, then environment variable, then default.
    private static String readConfig(String envName, String propertyName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) return propertyValue.trim();

        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) return envValue.trim();

        return defaultValue;
    }

    // Function: readIntConfig - Reads and validates a positive integer config value.
    private static int readIntConfig(String envName, String propertyName, int defaultValue) {
        String raw = readConfig(envName, propertyName, String.valueOf(defaultValue));
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : defaultValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    // Function: readBooleanConfig - Reads boolean-like config values (true, 1, yes).
    private static boolean readBooleanConfig(String envName, String propertyName, boolean defaultValue) {
        String raw = readConfig(envName, propertyName, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw);
    }

    // Function: announceActiveBackendOnce - Prints the selected database backend once when announce mode is enabled.
    private static void announceActiveBackendOnce() {
        if (!ANNOUNCE_BACKEND) return;
        if (backendAnnounced) return;
        backendAnnounced = true;
        if (activeBackend == DbBackend.SQLITE) {
            System.err.println("Solacea database backend: SQLITE (" + SQLITE_PATH.toAbsolutePath().normalize() + ")");
        } else {
            System.err.println("Solacea database backend: MYSQL (" + MYSQL_URL + ")");
        }
    }
}
