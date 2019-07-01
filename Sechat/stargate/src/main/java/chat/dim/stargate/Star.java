package chat.dim.stargate;

import java.util.Map;

public interface Star {

    StarStatus getStatus();

    boolean launch(Map<String, Object> options);
    void terminate();

    void enterBackground();
    void enterForeground();

    /**
     *  Send data to the server
     *
     * @param requestData - data to be sent
     * @return 0 on success, -1 on error
     */
    int send(byte[] requestData);
    int send(byte[] requestData, StarDelegate messageHandler);
}
