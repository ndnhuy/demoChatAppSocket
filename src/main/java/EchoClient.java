import java.io.*;
import java.net.*;

public class EchoClient {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String name;

    public EchoClient() {}

    public EchoClient(String name) {
        this.name = name;
    }
    public void startConnection(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Error when initializing connection");
        }

    }

    public String sendMessage(String msg) {
        try {
            out.println(msg);
            return "sent " + msg;
        } catch (Exception e) {
            return null;
        }
    }

    public String sendMessage(String msg, String toUser) {
        try {
            out.println("to="+toUser+"#msg="+msg);
            return "sent " + msg;
        } catch (Exception e) {
            return null;
        }
    }

    public void waitForMessage() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                System.out.println("client " + this.name + " receive msg: " + msg);
            }
        } catch (Exception e) {

        }
    }

    public String readMessage() {
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("error when closing");
        }

    }

    public String getName() {
        return this.name;
    }
}