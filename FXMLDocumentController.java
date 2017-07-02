/*
 * To change this license header, choose License Headers sc Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template sc the editor.
 */
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
import javafx.scene.control.ToggleButton;
import javafx.stage.FileChooser;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Dennis
 */
public class FXMLDocumentController implements Initializable {

    private static final String desktop = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "passwords";
    boolean isHidden = true;

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
        int length = (int) sliderLength.getValue();
        boolean special = checkBoxSpecialChars.isSelected();
        String password = PasswordGeneratorFX.generatePassword(length, special);
        outputPW.setText(password);
        if (outputPW.getText().length() != length) {
            try {
                throw new TamperDetectionException();
            } catch (TamperDetectionException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
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
            new Alert(Alert.AlertType.WARNING, "Das Masterpasswort muss mindestens 5 Zeichen lang sein!").showAndWait();
            return;
        } else if (password.length() < 1) {
            new Alert(Alert.AlertType.WARNING, "Bitte erst ein Passwort generieren").showAndWait();
            return;
        } else if (service.length() < 1) {
            new Alert(Alert.AlertType.WARNING, "Bitte gib an für welchen Service das Passwort gilt, sonst kann es nicht abgespeichert werden.").showAndWait();
            return;
        }
        try {
            String encPW = encrypt(password, masterpass);
            System.out.println(encPW);
            String path = desktop + File.separator + service + ".encpw";
            File file = new File(path);
            file.getParentFile().mkdirs();
            file.createNewFile();
            fw = new FileWriter(file);
            fw.write(encPW);
//            protectFile(file.toPath());
        } catch (IOException ex) {
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
            new Alert(Alert.AlertType.WARNING, "Das Masterpasswort muss mindestens 5 Zeichen lang sein!").showAndWait();
            return;
        }
        outputPW.clear();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Verschl. Passwörter (*.encpw)", "*.encpw");
        FileChooser fc = new FileChooser();
        fc.setTitle("Datei zum entschlüsseln wählen...");
        fc.setInitialDirectory(new File(desktop));
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
            return new String(java.util.Base64.getMimeEncoder().encode(encrypted), StandardCharsets.UTF_8);

        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "Fehler in der Funktion \"encrypt()\"";
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
            new Alert(Alert.AlertType.ERROR, "Das Masterpasswort stimmt nicht mit dem bei der Erstellung verwendeten Masterpasswort überein!").showAndWait();
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

    @FXML
    private void showPassword() {
//        if (isHidden) {
//            inputMasterpass.setVisible(false);
//            inputMasterpassUnmasked.setText(inputMasterpass.getText());
//            inputMasterpassUnmasked.setVisible(true);
//        } else {
//            inputMasterpass.setText(inputMasterpassUnmasked.getText());
//            inputMasterpass.setVisible(true);
//            inputMasterpassUnmasked.setVisible(false);
//        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Passwort: " + inputMasterpass.getText());
        alert.show();
    }
}
