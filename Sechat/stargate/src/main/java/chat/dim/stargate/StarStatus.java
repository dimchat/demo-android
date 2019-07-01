package chat.dim.stargate;

public enum StarStatus {

    Error (-1),
    Init (0),
    Connecting(1),
    Connected(2);

    public final int value;

    StarStatus(int value) {
        this.value = value;
    }
}
