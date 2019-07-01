package chat.dim.stargate.marsgate.bussiness;

public class CGITask {

    public int taskId;
    int channelSelect;
    int cmdId;
    String cgi;
    String host;

    public CGITask() {
        super();
        this.channelSelect = ChannelType.All.value;
    }

    public CGITask(ChannelType channelType, int cmdId, String cgiUri, String host) {
        super();
        this.channelSelect = channelType.value;
        this.cmdId = cmdId;
        this.cgi = cgiUri;
        this.host = host;
    }


}
