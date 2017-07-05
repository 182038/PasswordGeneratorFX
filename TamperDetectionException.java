package passwordgeneratorfx;

class TamperDetectionException extends Exception {

    public TamperDetectionException() {
        super();
    }
    
    @Override
    public String getMessage() {
        return "Das generierte Passwort hat nicht die gewünschte Länge. Scheinbar manipuliert hier jemand...";
    }
}
