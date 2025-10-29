package app.cinematch.util;

import app.cinematch.model.HistoryEntry;
import java.util.ArrayList;
import java.util.List;

public class JsonStorageMock {
    private static List<HistoryEntry> mockHistory = new ArrayList<>();
    public static void reset() { mockHistory.clear(); }
    public static void setMockHistory(List<HistoryEntry> entries) {
        mockHistory = new ArrayList<>(entries);
    }
    public static List<HistoryEntry> loadAll() { return mockHistory; }

    // Pour les autres tests existants
    public static String lastTitle, lastStatus;
    public static void addOrUpdate(String t, String s) {
        lastTitle = t; lastStatus = s;
    }
}
