package passwordgeneratorfx;

// I dont't think that this exception will ever be triggered. But I thought it would be funny to have it though...
class TamperDetectionException extends Exception {

    public TamperDetectionException() {
        super();
    }
    
    @Override
    public String getMessage() {
        return "The generated password does not match the set length. Seems like someone is tampering.";
    }
}
