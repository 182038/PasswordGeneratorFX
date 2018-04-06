package passwordgeneratorfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 *
 * @author Dennis
 */
public class PasswordGeneratorFX extends Application {
    
    public static Stage stage;
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));
        
        Scene scene = new Scene(root);
        
        this.stage = stage;
        stage.setTitle("Passwordgenerator");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    public static void help() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Help");
        
        //I really want to keep that long German sentence. Internationalization is planned
        //alert.setContentText("Dinge die es zu beachten gibt:\n\n1. Gib ein Masterpasswort von mindestens 5 Zeichen Länge ein und merke es Dir!\n\n2. Wähle einen Dienst bei welchem das Passwort verwendet werden soll.\n\n3. Die Passwörter werden verschlüsselt im Ordner \"passwords\" auf dem Desktop abgelegt.\n\n4. Zur Entschlüsselung ist das selbe Masterpasswort nötig wie bei der Verschlüsselung.");
        
        alert.setContentText("Things you need to know:\n\n1. Your masterpassword should contain at least 5 chars. Remember it well!\n\n2. Enter the service the password is used for. So you can identify it, when restoring.\n\n3. The passwords are saved in \""+FXMLDocumentController.SAVEPATH+"\". The following encryption methods are used:\nmasterpassword -> SHA-256\nGenerated password -> AES using hashed masterpassword as cipher\nResulting hash -> BASE64 -> saved as .encpw\n\n4. To retrieve your password you have to use exactly the same masterpassword as when you did save it!");
        
        alert.setTitle("© 2017 Dennis Wiencke");
        alert.showAndWait();
    }
    
    public static String generatePassword(int length, boolean special) {
        Password pw = new Password(length, special);
        return pw.getPassword();
    }
    
}
