package passwordgeneratorfx;

class TamperDetectionException extends Exception {

    public TamperDetectionException() {
        super();
    }
    
    @Override
    public String getMessage() {
        return "Das eingegebene Masterpasswort ist nicht korrekt";
    }
}
