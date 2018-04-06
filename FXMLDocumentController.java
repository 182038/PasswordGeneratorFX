package passwordgeneratorfx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author 182038
 */
public class FXMLDocumentController implements Initializable {

    // Where to save those .encpw files?
    // Don't worry. Your passwords will be SHA-256 encrypted using your masterpassword.
    // The resulting hash will be encrypted with BASE64 again before storing it in the database or filesystem.
    static final String SAVEPATH = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "passwords";

    // You should provide your database credentials here.
    // The database connection is only used as a backup.
    // The appplication does not retreive entries from the DB.
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE_NAME = "pwgen";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    @FXML
    private PasswordField inputMasterpass;

    @FXML
    private TextField inputService;

    @FXML
    private TextArea outputPW;

    @FXML
    private Slider sliderLength;

    @FXML
    private Label lblCount;

    @FXML
    private CheckBox checkBoxSpecialChars;

    @FXML
    private CheckBox checkBoxDatabase;

    @FXML
    private Button buttonGenerate;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        int labelText = (int) sliderLength.getValue();
        lblCount.setText(String.valueOf(labelText));

        sliderLength.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            int count = (int) sliderLength.getValue();
            lblCount.setText(String.valueOf(count));
        });
    }

    @FXML
    private void handleButtonHelpAction(ActionEvent event) {
        PasswordGeneratorFX.help();
    }

    @FXML
    private void handleButtonGenerateAction(ActionEvent event) {
        String masterpass = inputMasterpass.getText();
        if (masterpass.length() < 5) {
            new Alert(Alert.AlertType.WARNING, "Your masterpasswords length must be at least 5!").showAndWait();
            inputMasterpass.requestFocus();
            return;
        }
        int length = (int) sliderLength.getValue();
        boolean special = checkBoxSpecialChars.isSelected();
        String password = PasswordGeneratorFX.generatePassword(length, special);
        outputPW.setText(password);
        if (outputPW.getText().length() != length) {
            try {
                throw new TamperDetectionException();
            } catch (TamperDetectionException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
                alert.show();
            }
        }
    }

    @FXML
    private void handleButtonSaveAction(ActionEvent event) {
        FileWriter fw = null;
        String masterpass = inputMasterpass.getText();
        String password = outputPW.getText();
        String service = inputService.getText();
        if (masterpass.length() < 5) {
            new Alert(Alert.AlertType.WARNING, "Your masterpasswords length must be at least 5!").showAndWait();
            inputMasterpass.requestFocus();
            return;
        } else if (password.length() < 1) {
            new Alert(Alert.AlertType.WARNING, "You forgot to generate a password.").showAndWait();
            buttonGenerate.requestFocus();
            return;
        } else if (service.length() < 1) {
            new Alert(Alert.AlertType.WARNING, "Please enter a name for that password. Otherwise it can't be saved.").showAndWait();
            inputService.requestFocus();
            return;
        }
        try {
            String encPW = encrypt(password, masterpass);
            System.out.println("Hash: " + encPW);
            String path = SAVEPATH + File.separator + service + ".encpw";
            File file = new File(path);
            if (file.exists()) {
                Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION, "This service was already configured. Do you want to override the old password?").showAndWait();
                if(result.isPresent() && result.get() != ButtonType.OK) return;
            }
            file.getParentFile().mkdirs();
            file.createNewFile();
            fw = new FileWriter(file);
            fw.write(encPW);

            if (checkBoxDatabase.isSelected()) {
                //Write to database
                //Replace with your own variables
                String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE_NAME + "";
                DatabaseHelper db = new DatabaseHelper(url, USER, PASSWORD);
                String[] values = {inputService.getText(), encPW};
                db.writeToDB(values);
            }
        } catch (IOException | NullPointerException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @FXML
    private void handleButtonRetrieveAction(ActionEvent event) {
        String masterpass = inputMasterpass.getText();
        if (masterpass.length() < 5) {
            new Alert(Alert.AlertType.WARNING, "Your masterpasswords length must be at least 5!").showAndWait();
            inputMasterpass.requestFocus();
            return;
        }
        outputPW.clear();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("encrypted passwords (*.encpw)", "*.encpw");
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose a file for decryption...");
        fc.setInitialDirectory(new File(SAVEPATH));
        fc.getExtensionFilters().add(extFilter);
        File file = fc.showOpenDialog(PasswordGeneratorFX.stage);
        if (file != null) {
            readFile(file);
        }
    }

    private String encrypt(String password, String masterpass) {
        try {
            SecretKeySpec secretKeySpec = getKey(masterpass);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(password.getBytes());
            System.out.println(new String(encrypted));
            return new String(java.util.Base64.getMimeEncoder().encode(encrypted), StandardCharsets.UTF_8);

        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private String decrypt(String encPW, String masterpass) {
        try {
            SecretKeySpec secretKeySpec = getKey(masterpass);
            byte[] decoded = java.util.Base64.getMimeDecoder().decode(encPW);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] cipherData = cipher.doFinal(decoded);
            String decPW = new String(cipherData);

            return decPW;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            new Alert(Alert.AlertType.ERROR, "The masterpassword does not match the masterpassword used when encrypting!").showAndWait();
        }
        return null;
    }

    private SecretKeySpec getKey(String keyStr) {
        try {
            byte[] key = keyStr.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            key = md.digest(key);
            key = Arrays.copyOf(key, 16);
            return new SecretKeySpec(key, "AES");
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void readFile(File file) {
        String masterpass = inputMasterpass.getText();
        try {
            Scanner sc = new Scanner(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            while (sc.hasNext()) {
                sb.append(sc.next());
            }
            sc.close();
            outputPW.setText(decrypt(sb.toString(), masterpass));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
