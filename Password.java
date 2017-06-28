package passwordgeneratorfx;

import java.util.Random;

public class Password {
    
    private String pass;
    private static int length;
    private static boolean special;
    
    public Password(int length, boolean special) {
        System.out.println("Password Intialisiert");
        this.length = length;
        this.special = special;
        this.pass = generate();
    }
    
    static String generate() {
        System.out.println("Password wird generiert");
        String capsChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String chars= "abcdefghijlmnopqrstuvwxyz";
        String numChars = "0123456789";
        String symbols = "!§$%&/()=?";
        
        String passChars = (special) ? capsChars + chars + numChars + symbols : capsChars + chars + numChars;
        Random rand = new Random();

        char[] password = new char[length];

        for (int i = 0; i < length; i++) {
            password[i] = passChars.charAt(rand.nextInt(passChars.length()));
        }
        System.out.println("Passwort wurde generiert");
        return String.copyValueOf(password);
    }
    
    public String getPassword() {
        System.out.println("Passwort: " + this.pass);
        return this.pass;
    }
}
