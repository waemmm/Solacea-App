package com.solacea.util;

public class UserSession {
    private static String currentUser;

    // Function: setUser - Stores the current username in memory for this app session.
    public static void setUser(String username) { currentUser = username; }
    // Function: getUser - Returns the username currently stored in session memory.
    public static String getUser() { return currentUser; }
    // Function: cleanUserSession - Clears the current in-memory user session.
    public static void cleanUserSession() { currentUser = null; }
}