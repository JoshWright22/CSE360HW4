package databasePart1;

import java.sql.*;
import java.util.UUID;

import application.User;
import application.UserRole;

/**
 * The DatabaseHelper class is responsible for managing the connection to the
 * database, performing operations such as user registration, login validation,
 * and handling invitation codes.
 */
public class DatabaseHelper {

	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "org.h2.Driver";
	static final String DB_URL = "jdbc:h2:~/FoundationDatabase";

	// Database credentials
	static final String USER = "sa";
	static final String PASS = "";

	private Connection connection = null;
	private Statement statement = null;

	public void connectToDatabase() throws SQLException {
		// If already connected and connection is valid, don't reconnect
		try {
			if (this.connection != null && !this.connection.isClosed()) {
				System.out.println("Database already connected; skipping reconnect.");
				return;
			}
			Class.forName(JDBC_DRIVER); // Load the JDBC driver
			System.out.println("Connecting to database...");
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connection.createStatement();

			createTables(); // Create tables if they don't exist
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver not found: " + e.getMessage());
		}
	}

	private void createTables() throws SQLException {
		// Create the users table
		String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255) UNIQUE, "
				+ "password VARCHAR(255), "
				+ "firstName VARCHAR(255), "
				+ "lastName VARCHAR(255), "
				+ "email VARCHAR(255), "
				+ "role VARCHAR(20))";
		statement.execute(userTable);

		// Create the invitation codes table
		String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
				+ "code VARCHAR(10) PRIMARY KEY, "
				+ "isUsed BOOLEAN DEFAULT FALSE)";
		statement.execute(invitationCodesTable);

		// Create the questions table
		String questionsTable = "CREATE TABLE IF NOT EXISTS Questions ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255), "
				+ "creationDate VARCHAR(255), "
				+ "title VARCHAR(255), "
				+ "content TEXT, "
				+ "answers VARCHAR(255), "
				+ "tags TEXT)";
		statement.execute(questionsTable);

		// Create the answers table
		String answersTable = "CREATE TABLE IF NOT EXISTS Answers ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255), "
				+ "creationDate VARCHAR(255), "
				+ "content TEXT)";
		statement.execute(answersTable);

		// Create the comments table
		String commentsTable = "CREATE TABLE IF NOT EXISTS Comments ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255), "
				+ "creationDate VARCHAR(255), "
				+ "content TEXT, "
				+ "tags TEXT, "
				+ "parentId INT)";
		statement.execute(commentsTable);

		// Create the reviews table
		String reviewsTable = "CREATE TABLE IF NOT EXISTS Reviews ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255), "
				+ "creationDate VARCHAR(255), "
				+ "content TEXT, "
				+ "rating INT, "
				+ "questionId INT NULL, "
				+ "answerId INT NULL)";
		statement.execute(reviewsTable);

		// Create the ReviewerProfiles table
		String reviewerProfilesTable = "CREATE TABLE IF NOT EXISTS ReviewerProfiles ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255) UNIQUE, "
				+ "bio TEXT, "
				+ "expertise VARCHAR(255), "
				+ "yearsExperience INT, "
				+ "totalReviews INT DEFAULT 0, "
				+ "averageRating DOUBLE DEFAULT 0.0)";
		statement.execute(reviewerProfilesTable);

		// Create the Messages table
		String messagesTable = "CREATE TABLE IF NOT EXISTS Messages ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "senderName VARCHAR(255), "
				+ "receiverName VARCHAR(255), "
				+ "content TEXT, "
				+ "sentTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
				+ "isRead BOOLEAN DEFAULT FALSE)";
		statement.execute(messagesTable);

		// Create the PendingReviewers table
		String pendingReviewersTable = "CREATE TABLE IF NOT EXISTS PendingReviewers ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY,"
				+ "userName VARCHAR(255) UNIQUE)";
		statement.execute(pendingReviewersTable);

		// Create FlaggedItems table for staff flagging
		String flaggedItemsTable = "CREATE TABLE IF NOT EXISTS FlaggedItems ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "itemType VARCHAR(50), "
				+ "itemId INT, "
				+ "flagger VARCHAR(255), "
				+ "reason TEXT, "
				+ "status VARCHAR(50) DEFAULT 'OPEN', "
				+ "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
		statement.execute(flaggedItemsTable);

		// Create StaffNotes table for private staff notes attached to items
		String staffNotesTable = "CREATE TABLE IF NOT EXISTS StaffNotes ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "itemType VARCHAR(50), "
				+ "itemId INT, "
				+ "author VARCHAR(255), "
				+ "noteText TEXT, "
				+ "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
		statement.execute(staffNotesTable);
	}

	public boolean isDatabaseEmpty() {
		String query = "SELECT COUNT(*) FROM cse360users";
		try (PreparedStatement pstmt = connection.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery()) {
			if (rs.next()) {
				return rs.getInt(1) == 0; // true if no users
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public Statement getStatement() {
		return this.statement;
	}

	public Connection getConnection() {
		return this.connection;
	}

	// PHASE 1: User operations

	public User createUser(String userName, String password, String firstName, String lastName, String email,
			UserRole role) {
		if (this.doesUserExist(userName)) {
			System.err.println("Attempted to create a user with a duplicate username.");
			return null;
		}

		String query = "INSERT INTO cse360users (userName, password, firstName, lastName, email, role) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setString(1, userName);
			stmt.setString(2, password);
			stmt.setString(3, firstName);
			stmt.setString(4, lastName);
			stmt.setString(5, email);
			stmt.setString(6, role.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("Failed to register a user into the database.");
			e.printStackTrace();
		}

		return new User(userName, password, firstName, lastName, email, role);
	}

	public User createUser(String userName, String password, UserRole role) {
		return this.createUser(userName, password, "", "", "", role);
	}

	public void registerUser(User user) {
		String query = "INSERT INTO cse360users (userName, password, firstName, lastName, email, role) "
				+ "VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setString(1, user.getUserName());
			stmt.setString(2, user.getPassword());
			stmt.setString(3, user.getFirstName());
			stmt.setString(4, user.getLastName());
			stmt.setString(5, user.getEmail());
			stmt.setString(6, user.getRole().toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("Failed to register a user into the database.");
			e.printStackTrace();
		}
	}

	public boolean login(User user) throws SQLException {
		String query = "SELECT * FROM cse360users WHERE userName = ? AND password = ? AND role = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			pstmt.setString(3, user.getRole().toString());
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		}
	}

	public User fetchUser(String userName) {
		String query = "SELECT * FROM cse360users WHERE userName = ?";
		User user = null;
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setString(1, userName);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				user = new User(rs.getString("userName"), rs.getString("password"),
						rs.getString("firstName"), rs.getString("lastName"), rs.getString("email"),
						UserRole.valueOf(rs.getString("role").toUpperCase()));
			}
		} catch (SQLException e) {
			System.err.println("Failed to fetch a user from the database.");
			e.printStackTrace();
		}
		return user;
	}

	public boolean doesUserExist(String userName) {
		String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, userName);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public UserRole getUserRole(String userName) {
		String query = "SELECT role FROM cse360users WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, userName);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return UserRole.valueOf(rs.getString("role").toUpperCase());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void updateUserRole(String userName, UserRole newRole) {
		String query = "UPDATE cse360users SET role = ? WHERE userName = ?";

		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, newRole.toString());
			pstmt.setString(2, userName);

			int rowsUpdated = pstmt.executeUpdate();
			if (rowsUpdated == 0) {
				System.out.println("No user found with username: " + userName);
			} else {
				System.out.println("Updated role for user: " + userName + " to " + newRole);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void insertIntoPendingReviewers(String userName) {
		// Check connection to database. Return if not established
		if (connection == null) {
			System.err.println("Connection to database not established. Could not insert user " + userName
					+ " into pending reviewers.");
			return;
		}

		// Attempt to insert user into table. If user already exists,
		// an SQL Exception will be thrown
		String insertSql = "INSERT INTO PendingReviewers (userName) VALUES (?)";
		try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
			stmt.setString(1, userName);
			stmt.executeUpdate();
			System.out.println("Pending reviewer request submitted for: " + userName);
		} catch (SQLException e) {
			// If duplicate, SQL state 23000 or similar
			if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
				System.out.println("User already requested reviewer role.");
			} else {
				System.err.println("Database error while inserting into PendingReviewers.");
				e.printStackTrace();
			}
		}
	}

	public boolean removeFromPendingReviewers(String userName) {
		String query = "DELETE FROM PendingReviewers WHERE userName = ?";

		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setString(1, userName);
			int rowsDeleted = stmt.executeUpdate();
			System.out.println("Deleted " + rowsDeleted + " row(s) from PendingReviewers.");
			return rowsDeleted > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean approvePendingReviewer(String userName) {
		boolean removed = removeFromPendingReviewers(userName);

		if (removed) {
			updateUserRole(userName, UserRole.REVIEWER);
			return true;
		} else {
			System.err.println("User not found.");
			return false;
		}
	}

	// --- Staff / Admin helpers ---

	/**
	 * Flag an item (question/answer/comment/review) for staff/instructor review.
	 */
	public void flagItem(String itemType, int itemId, String flagger, String reason) {
		if (connection == null) {
			System.err.println("DB connection not established. Cannot flag item.");
			return;
		}
		String insert = "INSERT INTO FlaggedItems (itemType, itemId, flagger, reason) VALUES (?, ?, ?, ?)";
		try (PreparedStatement stmt = connection.prepareStatement(insert)) {
			stmt.setString(1, itemType);
			stmt.setInt(2, itemId);
			stmt.setString(3, flagger);
			stmt.setString(4, reason);
			stmt.executeUpdate();
			System.out.println("Flagged item: " + itemType + " #" + itemId + " by " + flagger);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fetch flagged items as a list of rows. Each String[] contains: id, itemType,
	 * itemId, flagger, reason, status, created
	 */
	public java.util.List<String[]> fetchFlaggedItems() {
		java.util.List<String[]> results = new java.util.ArrayList<>();
		String query = "SELECT * FROM FlaggedItems ORDER BY created DESC";
		try (PreparedStatement stmt = connection.prepareStatement(query);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				String[] row = new String[7];
				row[0] = String.valueOf(rs.getInt("id"));
				row[1] = rs.getString("itemType");
				row[2] = String.valueOf(rs.getInt("itemId"));
				row[3] = rs.getString("flagger");
				row[4] = rs.getString("reason");
				row[5] = rs.getString("status");
				row[6] = rs.getString("created");
				results.add(row);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return results;
	}

	/**
	 * Add a private staff note attached to a particular item
	 */
	public void addStaffNote(String itemType, int itemId, String author, String noteText) {
		if (connection == null) {
			System.err.println("DB connection not established. Cannot add staff note.");
			return;
		}
		String insert = "INSERT INTO StaffNotes (itemType, itemId, author, noteText) VALUES (?, ?, ?, ?)";
		try (PreparedStatement stmt = connection.prepareStatement(insert)) {
			stmt.setString(1, itemType);
			stmt.setInt(2, itemId);
			stmt.setString(3, author);
			stmt.setString(4, noteText);
			stmt.executeUpdate();
			System.out.println("Added staff note for " + itemType + " #" + itemId);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fetch staff notes for a given item
	 */
	public java.util.List<String[]> fetchStaffNotes(String itemType, int itemId) {
		java.util.List<String[]> notes = new java.util.ArrayList<>();
		String query = "SELECT * FROM StaffNotes WHERE itemType = ? AND itemId = ? ORDER BY created DESC";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setString(1, itemType);
			stmt.setInt(2, itemId);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					String[] row = new String[4];
					row[0] = String.valueOf(rs.getInt("id"));
					row[1] = rs.getString("author");
					row[2] = rs.getString("noteText");
					row[3] = rs.getString("created");
					notes.add(row);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return notes;
	}

	/**
	 * Update a flagged item's status (e.g., OPEN -> RESOLVED)
	 */
	public boolean updateFlagStatus(int flagId, String newStatus) {
		String update = "UPDATE FlaggedItems SET status = ? WHERE id = ?";
		try (PreparedStatement stmt = connection.prepareStatement(update)) {
			stmt.setString(1, newStatus);
			stmt.setInt(2, flagId);
			int rows = stmt.executeUpdate();
			return rows > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Fetch all questions as a list of rows (id, userName, creationDate, title,
	 * content)
	 */
	public java.util.List<String[]> fetchAllQuestions() {
		java.util.List<String[]> list = new java.util.ArrayList<>();
		String q = "SELECT id, userName, creationDate, title, content FROM Questions ORDER BY id DESC";
		try (PreparedStatement stmt = connection.prepareStatement(q);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				String[] row = new String[5];
				row[0] = String.valueOf(rs.getInt("id"));
				row[1] = rs.getString("userName");
				row[2] = rs.getString("creationDate");
				row[3] = rs.getString("title");
				row[4] = rs.getString("content");
				list.add(row);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	public java.util.List<String[]> fetchAllAnswers() {
		java.util.List<String[]> list = new java.util.ArrayList<>();
		String q = "SELECT id, userName, creationDate, content FROM Answers ORDER BY id DESC";
		try (PreparedStatement stmt = connection.prepareStatement(q);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				String[] row = new String[4];
				row[0] = String.valueOf(rs.getInt("id"));
				row[1] = rs.getString("userName");
				row[2] = rs.getString("creationDate");
				row[3] = rs.getString("content");
				list.add(row);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	public java.util.List<String[]> fetchAllComments() {
		java.util.List<String[]> list = new java.util.ArrayList<>();
		String q = "SELECT id, userName, creationDate, content FROM Comments ORDER BY id DESC";
		try (PreparedStatement stmt = connection.prepareStatement(q);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				String[] row = new String[4];
				row[0] = String.valueOf(rs.getInt("id"));
				row[1] = rs.getString("userName");
				row[2] = rs.getString("creationDate");
				row[3] = rs.getString("content");
				list.add(row);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * Send a message (used to notify instructors/staff)
	 */
	public void sendMessage(String sender, String receiver, String content) {
		String insert = "INSERT INTO Messages (senderName, receiverName, content) VALUES (?, ?, ?)";
		try (PreparedStatement stmt = connection.prepareStatement(insert)) {
			stmt.setString(1, sender);
			stmt.setString(2, receiver);
			stmt.setString(3, content);
			stmt.executeUpdate();
			System.out.println("Sent message from " + sender + " to " + receiver);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write flagged items to a CSV file at the given path. Returns true on success.
	 */
	public boolean writeFlaggedItemsCSV(String filePath) {
		java.util.List<String[]> rows = fetchFlaggedItems();
		java.io.File f = new java.io.File(filePath);
		try (java.io.PrintWriter pw = new java.io.PrintWriter(f, "UTF-8")) {
			// header
			pw.println("id,itemType,itemId,flagger,reason,status,created");
			for (String[] r : rows) {
				// escape double quotes in fields
				for (int i = 0; i < r.length; i++) {
					if (r[i] == null)
						r[i] = "";
					r[i] = r[i].replace("\"", "\"\"");
					if (r[i].contains(",") || r[i].contains("\n") || r[i].contains("\"")) {
						r[i] = "\"" + r[i] + "\"";
					}
				}
				pw.println(String.join(",", r));
			}
			pw.flush();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public String generateInvitationCode() {
		String code = UUID.randomUUID().toString().substring(0, 4);
		String query = "INSERT INTO InvitationCodes (code) VALUES (?)";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, code);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return code;
	}

	public boolean validateInvitationCode(String code) {
		String query = "SELECT * FROM InvitationCodes WHERE code = ? AND isUsed = FALSE";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, code);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				markInvitationCodeAsUsed(code);
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void markInvitationCodeAsUsed(String code) {
		String query = "UPDATE InvitationCodes SET isUsed = TRUE WHERE code = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, code);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void closeConnection() {
		try {
			if (statement != null)
				statement.close();
		} catch (SQLException se2) {
			se2.printStackTrace();
		}
		try {
			if (connection != null)
				connection.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

}
