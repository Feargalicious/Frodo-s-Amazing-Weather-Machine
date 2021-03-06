package service.users;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
	
	private Connection conn;
	private static final String DB_URL = "jdbc:sqlite:Database/users.db";
	
	public DatabaseHandler() {
		try {
			conn = DriverManager.getConnection(DB_URL);
		} catch (SQLException e) {
			System.err.println("Could not connect to the database");
		}
		
		try {
			//create the tables if they do not exist
			Statement s = conn.createStatement();
			String queryUsers =//user table 
						"CREATE TABLE IF NOT EXISTS users (\n" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
						"user_name TEXT NOT NULL,\n" +
						"password TEXT NOT NULL\n);";
			String queryEvents = //events table
						"CREATE TABLE IF NOT EXISTS events (\n" +
						"id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
						"userID INTEGER NOT NULL,\n" +
						"event TEXT NOT NULL);\n";
			s.execute(queryUsers);
			s.execute(queryEvents);
		} catch (SQLException e) {
			System.err.println("Error verifying tables");
		}
	}
	
	public boolean insertSearch(String searchParams, int userID) {
		try {
			String query = 
						"INSERT INTO event_searches "+
						"(userID, params)\n" +
						"VALUES (?,?);";
			PreparedStatement prepState = conn.prepareStatement(query);
			prepState.setString(2, searchParams);
			prepState.setInt(1, userID);
			prepState.execute();
			return true;
		} catch (SQLException e) {
			System.err.println("Error inserting search");
			return false;
		}
	}
	
	public UserInfo getUserProfile(String userName, String password) {
		try {
			String query = 
						"SELECT id FROM users\n" +
						"WHERE user_name = ? AND password = ?";
			PreparedStatement prepState = conn.prepareStatement(query);
			prepState.setString(1, userName);
			prepState.setString(2, password);
			ResultSet res = prepState.executeQuery();
			int userID = res.getInt("id");
			UserInfo profile = new UserInfo(userName, userID);
			return profile;
		} catch (SQLException e) {
			System.out.println("here");
			return null;
		}
	}
	
	public boolean insertNewUser(String userName, String password) {
		try {
			//prelim test to see if user exists already
			String testQuery = 
						"SELECT COUNT(user_name) FROM users\n" +
						"WHERE user_name = ?";
			PreparedStatement testStatement = conn.prepareStatement(testQuery);
			testStatement.setString(1, userName);
			ResultSet checkResults = testStatement.executeQuery();
			int count = checkResults.getInt(1);
			if (count > 0) {
				return false;
			}
			String query = 
					"INSERT INTO users(user_name, password) \n" +
					"VALUES(?,?);";
			PreparedStatement prepState = conn.prepareStatement(query);
			prepState.setString(1, userName);
			prepState.setString(2, password);
			prepState.execute();
			return true;
		} catch (SQLException e ) {
			e.printStackTrace();
			System.err.println("Failed inserting new user");
			return false;
		}
	}
	
	public boolean insertEvent(int userID, String event) {
		try {
			String query = 
					"INSERT INTO events(userID,event)\n" +
					"VALUES(?,?)";
			PreparedStatement prepState = conn.prepareStatement(query);
			prepState.setInt(1, userID);
			prepState.setString(2, event);
			prepState.execute();
			return true;
		} catch (SQLException e) {
			System.err.println("Failed inserting event");
			e.printStackTrace();
			return false;
		}
	}

	public String getLastEvent(UserInfo user){
		try{
			String query = 
					"SELECT * FROM events\n" +
					"WHERE userID = ?";
			PreparedStatement prepState = conn.prepareStatement(query);
			prepState.setInt(1,user.getID());
			ResultSet results = prepState.executeQuery();
			List<String> cats = new ArrayList<String>();
			while(results.next()) {
				cats.add(results.getString(3));
			}
			
			if(cats.isEmpty()) {
				return null;
			}
			else {
				return cats.get(cats.size()-1);
			}
		}
		catch(Exception e){
			return null;
		}
	}
}
