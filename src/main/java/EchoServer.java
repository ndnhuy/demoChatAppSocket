import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EchoServer {
    private ServerSocket serverSocket;
    private Map<String, Client> userToClient = new HashMap<>();
    private static ExecutorService executor = Executors.newCachedThreadPool();

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Start server successfully!");
            executor.submit(() -> {
                while (true) {
                    listenToClient();
                }
            });

            executor.submit(() -> {
                while (true) {
                    Thread.sleep(3000);
                    userToClient.forEach((user, client) -> {
                        try {
                            if (client.getInputStream().available() > 0) {
                                executor.submit(() -> deliverMsg(client.readMsg()));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            });

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deliverMsg(String input) {
        String toUser = input.split("#")[0].split("=")[1];
        String msg = input.split("#")[1].split("=")[1];
        userToClient.get(toUser).sendMsg(msg);
    }

    public void listenToClient() throws IOException {
        Socket clientSocket = this.serverSocket.accept();
        Client client = new Client(clientSocket);
        String initMsg = client.readMsg();
        System.out.println("server receive message: " + initMsg);
        String name = initMsg.split("=")[1];
        client.sendMsg("hello " + name);
        userToClient.put(name, client);
    }

    private class Client {
        private Socket socket;
        private PrintWriter clientOut;
        private BufferedReader clientIn;
        private String clientName;

        public Client(Socket socket) throws IOException {
            this.socket = socket;
            clientOut = new PrintWriter(socket.getOutputStream(), true);
//            clientOut.println("");
            clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public String readMsg() {
            try {
                return this.clientIn.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void sendMsg(String msg) {
            this.clientOut.println(msg);
        }

        public void setName(String name) {
            this.clientName = name;
        }

        public InputStream getInputStream() {
            try {
                return this.socket.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public OutputStream getOutputStream() {
            try {
                return this.socket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        public void close() {
            try {
                this.socket.close();
                clientIn.close();
                clientOut.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        try {
            serverSocket.close();
            userToClient.forEach((k,v)->v.close());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        int port = 4444;
        EchoServer server = new EchoServer();
        server.start(port);

        CompletionService completionService = new ExecutorCompletionService(executor);
        final AtomicInteger index = new AtomicInteger();
        Map<String, EchoClient> userToEchoClient = new ConcurrentHashMap<>();

        int taskSize = 5;
        for (int i=0;i<taskSize;i++) {
            completionService.submit(() -> {
                String name = "user" + index.getAndIncrement();
                EchoClient client = new EchoClient(name);
                userToEchoClient.put(name, client);
                client.startConnection("127.0.0.1", port);
                client.sendMessage("name=" + client.getName());

                if (client.readMessage() != null) {
                    System.out.println(name + " is connected");
                    return client;
                } else {
                    return null;
                }
            });
        }
        for (int i = 0; i < taskSize; i++) {
            Future<EchoClient> f = completionService.take();
            EchoClient client = f.get();
            executor.submit(client::waitForMessage);
        }

        System.out.println(userToEchoClient);
        userToEchoClient.get("user1").sendMessage("Hi user2, I'm user1", "user2");
        userToEchoClient.get("user3").sendMessage("Hi user1, I'm user3", "user1");
    }

}