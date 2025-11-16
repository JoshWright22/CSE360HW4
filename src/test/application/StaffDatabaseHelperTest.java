package test.application;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import databasePart1.DatabaseHelper;

public class StaffDatabaseHelperTest {

    static DatabaseHelper db;

    @BeforeAll
    public static void init() throws SQLException {
        db = new DatabaseHelper();
        db.connectToDatabase();

        // Clean up tables used by tests
        Connection c = db.getConnection();
        try (PreparedStatement p = c.prepareStatement("DELETE FROM FlaggedItems")) {
            p.executeUpdate();
        }
        try (PreparedStatement p = c.prepareStatement("DELETE FROM StaffNotes")) {
            p.executeUpdate();
        }
        try (PreparedStatement p = c.prepareStatement("DELETE FROM Messages")) {
            p.executeUpdate();
        }
    }

    @AfterAll
    public static void teardown() {
        db.closeConnection();
    }

    @Test
    public void testFlagAndFetch() {
        db.flagItem("Question", 9999, "tester", "inappropriate content");
        List<String[]> rows = db.fetchFlaggedItems();
        assertNotNull(rows);
        assertTrue(rows.size() >= 1);
        boolean found = false;
        for (String[] r : rows) {
            if (r.length > 2 && "9999".equals(r[2])) {
                found = true;
                assertEquals("Question", r[1]);
            }
        }
        assertTrue(found);
    }

    @Test
    public void testStaffNotesAndFetch() {
        db.addStaffNote("Question", 9998, "tester", "note text");
        List<String[]> notes = db.fetchStaffNotes("Question", 9998);
        assertNotNull(notes);
        assertTrue(notes.size() >= 1);
        assertEquals("tester", notes.get(0)[1]);
    }

    @Test
    public void testUpdateFlagStatusAndMessaging() {
        db.flagItem("Answer", 8888, "tester2", "spam");
        List<String[]> flags = db.fetchFlaggedItems();
        int flagId = -1;
        for (String[] r : flags) {
            if (r.length > 2 && "8888".equals(r[2])) {
                flagId = Integer.parseInt(r[0]);
                break;
            }
        }
        assertTrue(flagId > 0);
        boolean ok = db.updateFlagStatus(flagId, "RESOLVED");
        assertTrue(ok);

        db.sendMessage("stafftester", "instructors", "Please review flagged items.");
        // No exception thrown is considered success for sendMessage
    }
}
