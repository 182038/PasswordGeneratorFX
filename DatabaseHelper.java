package passwordgeneratorfx;

import java.sql.*;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class DatabaseHelper {
    
    private String url;
    private String user;
    private String pw;
    private static Connection con;
    
    public DatabaseHelper(String url, String user, String pw) {
        this.url = url;
        this.user = user;
        this.pw = pw;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con = DriverManager.getConnection(url, user, pw);
        } catch (SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(DatabaseHelper.class.getName()).log(Level.SEVERE, null, ex);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Can't connect to database. Is the server running?", ButtonType.OK);
            alert.showAndWait();
        }
    }
    
    public boolean writeToDB(String[] values) {
        try {
            Calendar calendar = Calendar.getInstance();
            String query = "INSERT INTO entries (service, hash) VALUES (?, ?);";
            PreparedStatement preSt = con.prepareStatement(query);
            preSt.setString(1, values[0]);
            preSt.setString(2, values[1]);
            preSt.execute();
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseHelper.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } finally {
            try {
                con.close();
            } catch (SQLException ex) {
                Logger.getLogger(DatabaseHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
