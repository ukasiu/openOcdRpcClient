package pl.ukasiu.openOcdRpcClient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents an OpenOCD RPC client.
 * @author Łukasz Gurdek
 */
public class OpenOcdRpcClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OpenOcdRpcClient.class);
    private static final char COMMAND_TOKEN = 0x1a;

    private final String host;
    private final Integer port;

    private Socket socket;
    private Writer writer;
    private Scanner scanner;

    /**
     * Creates OpenOCD RPC client
     * @param host Hostname of OpenOCD RPC server
     * @param port Port of OpenOCD RPC Server
     */
    public OpenOcdRpcClient(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Opens TCP socket to an OpenOCD RPC server
     * @throws IOException on socket failure
     */
    public void connect() throws IOException {
        LOG.info("Connecting client (" + this.host + ":" + this.port + ")");
        this.socket = new Socket(this.host, this.port);
        this.writer = new OutputStreamWriter(this.socket.getOutputStream());
        this.scanner = new Scanner(new InputStreamReader(this.socket.getInputStream()));
        this.scanner.useDelimiter(String.valueOf(COMMAND_TOKEN));
    }

    /**
     * Closes TCP socket to an OpenOCD RPC server
     * @throws IOException on socket failure
     */
    public void disconnect() throws IOException {
        if (this.writer != null)
            this.writer.close();
        if (this.scanner != null)
            this.scanner.close();
        if (this.socket != null) {
            LOG.info("Disconnecting client (" + this.host + ":" + this.port + ")");
            this.socket.close();
        }
        this.socket = null;
        this.writer = null;
        this.scanner = null;
    }

    /**
     * Sends TCL command to an OpenOCD RPC server
     * @param payload TCL command without OpenOCD RPC command delimiter
     * @return command result as returned by OpenOCD
     * @throws IOException on socket failure
     * @throws IllegalStateException when not connected
     */
    public synchronized String send(String payload) throws IOException {
        verifyConnection();
        LOG.debug("Sending: " + payload);
        this.writer.write(payload);
        this.writer.write(COMMAND_TOKEN);
        this.writer.flush();
        return this.receive();
    }

    /**
     * Allows to check the state of TCP socket to an OpenOCD RPC server
     * @return true when socket exists and is connected, false otherwise
     */
    public boolean isConnected() {
        return this.socket != null && this.socket.isConnected();
    }

    private String receive() {
        LOG.debug("Waiting for the response...");
        String received = this.scanner.next();
        LOG.debug("Response: " + received);
        return received;
    }

    private void verifyConnection() {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected");
        }
    }

    /**
     * Calls disconnect method
     * @throws IOException on socket failure
     */
    @Override
    public void close() throws Exception {
        disconnect();
    }
}
