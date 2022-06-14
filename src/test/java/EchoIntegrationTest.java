import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class EchoIntegrationTest {
    private static int port;

    @BeforeClass
    public static void start() throws InterruptedException, IOException {

        // Take an available port
        ServerSocket s = new ServerSocket(0);
        port = s.getLocalPort();
        s.close();

        Executors.newSingleThreadExecutor()
                .submit(() -> new EchoServer().start(port));
        Thread.sleep(500);
    }

    private EchoClient client = new EchoClient();

    @Before
    public void init() {
        client.startConnection("127.0.0.1", port);
    }

    @After
    public void tearDown() {
        client.stopConnection();
    }

    //

    @Test
    public void givenClient_whenServerEchosMessage_thenCorrect() throws InterruptedException {
        Thread.sleep(1000);

        String resp1 = client.sendMessage("hello");
        String resp2 = client.sendMessage("world");
        String resp3 = client.sendMessage("!");
        String resp4 = client.sendMessage(".");
        System.out.println(resp1);
        System.out.println(resp2);
        System.out.println(resp3);
        System.out.println(resp4);
    }

    @Test
    public void fo45452() {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        final AtomicInteger index = new AtomicInteger();
        for (int i=0;i<5;i++) {
            executor.submit(() -> {
                EchoClient client = new EchoClient("user" + index.getAndIncrement());
                client.startConnection("127.0.0.1", port);
                client.sendMessage("name=" + client.getName());
                client.waitForMessage();
            });
        }
    }

    @Test
    public void foo() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String s = "199                                                                                                                                                                                                        00000000000000000000            00000000000000                                                                                            ";

//        List<String> resultList = getResultList(s2);
//        System.out.println(resultList);
        String text = s;
        System.out.println("--original content--\n" + text);
        System.out.println("--result list ---\n" + getResultList(text));
        System.out.println("--checksum--\n" + checksum(getResultList(text)).toUpperCase());
    }

    String checksum(List<String> resultList) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String checksumKey = "f45c8370sd";

        List<String> list = new ArrayList<String>();
        list.add(resultList.get(4));
        list.add(resultList.get(6));
        list.add(resultList.get(8));
        list.add(resultList.get(7));
        list.add(resultList.get(0));
        list.add(resultList.get(1));
        list.add(resultList.get(2));
        list.add(resultList.get(3));
        list.add(resultList.get(5));
        String dataToHash = checksumKey;

        for (String field : list) {
            if (field != null & !field.isEmpty()) {
                dataToHash += "&" + field;
            }
        }
        System.out.println("--content to hash--\n" + dataToHash);
        return hmacSha256(checksumKey, dataToHash);
    }

    @Test
    public void foo2() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String s = "f45c8370sd&029205028753&    &                    &    &121&Transaction has been marked confirmed by Merchant.                                                                                                                                                      &220124429533&00000000&00000000000000";
        System.out.println(hmacSha256("f45c8370sd", s));
    }

    List<String> getResultList(String s) {
        List<Integer> intervals = new ArrayList<>(Arrays.asList(3,200,12,8,12,14,4,4,20,64));
        List<String> resultList = new ArrayList<String>();
        int start = 0;
        for (int interval : intervals) {
            resultList.add(s.substring(start, start + interval));
            start +=  interval;
        }
        return resultList;
    }

    String hmacSha256(String key, String data) throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-16"), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-16")));
    }
}