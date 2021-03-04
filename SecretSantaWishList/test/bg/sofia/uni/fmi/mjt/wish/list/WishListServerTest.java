package bg.sofia.uni.fmi.mjt.wish.list;

import org.junit.Test;
import org.junit.BeforeClass;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertEquals;


public class WishListServerTest {
    @BeforeClass
    public static void startServer() {
        Thread serverThread = new Thread(() -> {
            WishListServer wishListServer = new WishListServer(7777);
            wishListServer.start();
        });
        serverThread.start();
    }

    @Test
    public void testPostSingleWish() {

        final int SERVER_PORT = 7777;
        final String SERVER_HOST = "localhost";
        ByteBuffer buffer = ByteBuffer.allocateDirect(512);

        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            String message = "register user 123";

            buffer.clear(); // switch to writing mode
            buffer.put(message.getBytes()); // buffer fill

            buffer.flip(); // switch to reading mode
            socketChannel.write(buffer); // buffer drain

            buffer.clear(); // switch to writing mode
            socketChannel.read(buffer); // buffer fill
            buffer.flip(); // switch to reading mode

            byte[] byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);

            message = "post-wish name1 gift1";

            buffer.clear(); // switch to writing mode
            buffer.put(message.getBytes()); // buffer fill

            buffer.flip(); // switch to reading mode
            socketChannel.write(buffer); // buffer drain

            buffer.clear(); // switch to writing mode
            socketChannel.read(buffer); // buffer fill
            buffer.flip(); // switch to reading mode

            byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);
            String reply = new String(byteArray, "UTF-8"); // buffer drain

            assertEquals("[ Gift gift1 for student name1 submitted successfully ]"
                    + System.lineSeparator(), reply);
        } catch (IOException e) {
            throw new RuntimeException("There is a problem with the network communication",e);
        }
    }

    @Test
    public void testPostSameWish() {

        final int SERVER_PORT = 7777;
        final String SERVER_HOST = "localhost";
        ByteBuffer buffer = ByteBuffer.allocateDirect(512);

        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            String message = "login user 123";

            buffer.clear(); // switch to writing mode
            buffer.put(message.getBytes()); // buffer fill

            buffer.flip(); // switch to reading mode
            socketChannel.write(buffer); // buffer drain

            buffer.clear(); // switch to writing mode
            socketChannel.read(buffer); // buffer fill
            buffer.flip(); // switch to reading mode

            byte[] byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);

            message = "post-wish name1 gift1";

            buffer.clear(); // switch to writing mode
            buffer.put(message.getBytes()); // buffer fill

            buffer.flip(); // switch to reading mode
            socketChannel.write(buffer); // buffer drain

            buffer.clear(); // switch to writing mode
            socketChannel.read(buffer); // buffer fill
            buffer.flip(); // switch to reading mode

            byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);

            message = "post-wish name1 gift1";
            buffer.clear(); // switch to writing mode
            buffer.put(message.getBytes()); // buffer fill

            buffer.flip(); // switch to reading mode
            socketChannel.write(buffer); // buffer drain

            buffer.clear(); // switch to writing mode
            socketChannel.read(buffer); // buffer fill
            buffer.flip(); // switch to reading mode

            String reply = new String(byteArray, "UTF-8"); // buffer drain
            byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);
            reply = new String(byteArray, "UTF-8"); // buffer drain

            assertEquals("[ The same gift for student name1 was already submitted ]"
                    + System.lineSeparator(), reply);
        } catch (IOException e) {
            throw new RuntimeException("There is a problem with the network communication",e);
        }
    }

    @Test
    public void testGetWish() {

        final int SERVER_PORT = 7777;
        final String SERVER_HOST = "localhost";
        ByteBuffer buffer = ByteBuffer.allocateDirect(512);

        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            String message = "login user 123";

            buffer.clear(); // switch to writing mode
            buffer.put(message.getBytes()); // buffer fill

            buffer.flip(); // switch to reading mode
            socketChannel.write(buffer); // buffer drain

            buffer.clear(); // switch to writing mode
            socketChannel.read(buffer); // buffer fill
            buffer.flip(); // switch to reading mode

            byte[] byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);

            message = "post-wish name1 gift1";

            buffer.clear(); // switch to writing mode
            buffer.put(message.getBytes()); // buffer fill

            buffer.flip(); // switch to reading mode
            socketChannel.write(buffer); // buffer drain

            buffer.clear(); // switch to writing mode
            socketChannel.read(buffer); // buffer fill
            buffer.flip(); // switch to reading mode

            byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);

            message = "get-wish";
            buffer.clear(); // switch to writing mode
            buffer.put(message.getBytes()); // buffer fill

            buffer.flip(); // switch to reading mode
            socketChannel.write(buffer); // buffer drain

            buffer.clear(); // switch to writing mode
            socketChannel.read(buffer); // buffer fill
            buffer.flip(); // switch to reading mode

            String reply = new String(byteArray, "UTF-8"); // buffer drain
            byteArray = new byte[buffer.remaining()];
            buffer.get(byteArray);
            reply = new String(byteArray, "UTF-8"); // buffer drain

            assertEquals("[ name1: [gift1] ]" + System.lineSeparator(), reply);
        } catch (IOException e) {
            throw new RuntimeException("There is a problem with the network communication",e);
        }
    }
}

