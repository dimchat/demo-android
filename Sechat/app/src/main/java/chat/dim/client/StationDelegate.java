package chat.dim.client;

public interface StationDelegate {

    /**
     *  Received a new data package from the station
     *
     * @param data - data package to send
     * @param server - current station
     */
    void didReceivePackage(byte[] data, Station server);

    /**
     *  Send data package to station success
     *
     * @param data - data package sent
     * @param server - current station
     */
    void didSendPackage(byte[] data, Station server);

    /**
     *  Failed to send data package to station
     *
     * @param error - error information
     * @param data - data package to send
     * @param server - current station
     */
    void didFailToSendPackage(Error error, byte[] data, Station server);
}
