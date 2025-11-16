package test.application;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import databasePart1.DatabaseHelper;

public class SimpleStaffTests {

    static DatabaseHelper db;

    @BeforeAll
    public static void setup() throws SQLException {
        db = new DatabaseHelper();
        db.connectToDatabase();
    }

    @AfterAll
    public static void teardown() {
        if (db != null) db.closeConnection();
    }

    @Test
    public void testConnectIdempotent() {
        // calling connect twice should not throw and connection should be available
        assertDoesNotThrow(() -> db.connectToDatabase());
        assertNotNull(db.getConnection());
    }

    @Test
    public void testFlagAndFetchSmoke() {
        // simple smoke test: flag an item and ensure fetchFlaggedItems returns non-null
        assertDoesNotThrow(() -> db.flagItem("Question", 123456, "unittest", "smoke test"));
        List<String[]> rows = db.fetchFlaggedItems();
        assertNotNull(rows);
    }
}
