package chat.dim.stargate.marsgate.bussiness;

public enum ChannelType {

    ShortConn(1),
    LongConn(2),
    All(3);

    public final int value;

    ChannelType(int value) {
        this.value = value;
    }
}
