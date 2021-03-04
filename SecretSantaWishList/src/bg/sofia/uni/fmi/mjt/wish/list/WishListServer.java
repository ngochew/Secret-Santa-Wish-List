package bg.sofia.uni.fmi.mjt.wish.list;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WishListServer {
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 1024;

    private int serverPort;
    private boolean serverState = false;
    private Map<String, String> accounts = new LinkedHashMap<>();
    private Map<SocketAddress, Boolean> isLoggedIn = new LinkedHashMap<>();
    private Map<String, ArrayList<String>> wishes = new LinkedHashMap<>();

    public WishListServer(int serverPort) {
        this.serverPort = serverPort;
    }

    public void start() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {

            this.serverState = true;
            serverSocketChannel.bind(new InetSocketAddress(SERVER_HOST, this.serverPort));
            serverSocketChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            while (serverState) {
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext() && serverState) {
                    SelectionKey key = keyIterator.next();
                    if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();

                        buffer.clear();
                        int r = socketChannel.read(buffer);
                        if (r <= 0) {
                            System.out.println("nothing to read, will close channel" + System.lineSeparator());
                            socketChannel.close();
                            break;
                        }

                        buffer.flip();
                        byte[] byteArray = new byte[buffer.remaining()];
                        buffer.get(byteArray);
                        String message = new String(byteArray, "UTF-8");
                        message = message.replace(System.lineSeparator(), "");

                        handleRequest(message, socketChannel);

                    } else if (key.isAcceptable()) {
                        ServerSocketChannel sockChannel = (ServerSocketChannel) key.channel();
                        SocketChannel accept = sockChannel.accept();
                        accept.configureBlocking(false);
                        accept.register(selector, SelectionKey.OP_READ);
                    }
                    keyIterator.remove();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("There is a problem with the server socket", e);
        }
    }

    public void stop() {
        this.serverState = false;
    }

    private void registerAccount(String name, String password, SocketChannel socketChannel) throws IOException {
        if (this.accounts.containsKey(name)) {
            socketChannel.write(ByteBuffer.wrap(("[ Username " + name +
                    " is already taken, select another one ]"
                    + System.lineSeparator()).getBytes()));

        } else if (!name.matches("^[a-zA-Z0-9-._]*$")) {
            socketChannel.write(ByteBuffer.wrap(("[ Username " + name +
                    " is invalid, select a valid one ]"
                    + System.lineSeparator()).getBytes()));

        } else {
            this.accounts.put(name, password);
            isLoggedIn.put(socketChannel.getRemoteAddress(),true);
            socketChannel.write(ByteBuffer.wrap(("[ Successfully registered. ] "+ System.lineSeparator()).getBytes()));
        }
    }

    private void login(String name, String password, SocketChannel socketChannel) throws IOException {
        if (!this.accounts.containsKey(name)) {
            socketChannel.write(ByteBuffer.wrap(("[ Invalid username/password combination ]"
                    + System.lineSeparator()).getBytes()));

        } else if (!this.accounts.get(name).equals(password)){
            socketChannel.write(ByteBuffer.wrap(("[ Invalid username/password combination ]"
                    + System.lineSeparator()).getBytes()));

        } else if (isLoggedIn.containsKey(socketChannel.getRemoteAddress())){
            if (!isLoggedIn.get(socketChannel.getRemoteAddress())) {
                socketChannel.write(ByteBuffer.wrap(("[ You have to logout in order to login again. ]"
                        + System.lineSeparator()).getBytes()));
            }

        } else {
            this.isLoggedIn.put(socketChannel.getRemoteAddress(),true);
            socketChannel.write(ByteBuffer.wrap(("[ Successfully logged in. ]" + System.lineSeparator()).getBytes()));
        }
    }

    private void logout(SocketChannel socketChannel) throws IOException {
        if (!this.isLoggedIn.containsKey(socketChannel.getRemoteAddress())){
            socketChannel.write(ByteBuffer.wrap(("[ You are not logged in ]" + System.lineSeparator()).getBytes()));

        } else if (!this.isLoggedIn.get(socketChannel.getRemoteAddress())){
            socketChannel.write(ByteBuffer.wrap(("[ You are not logged in ]" + System.lineSeparator()).getBytes()));

        } else {
            this.isLoggedIn.put(socketChannel.getRemoteAddress(),false);
            socketChannel.write(ByteBuffer.wrap(("[ Successfully logged out ]" + System.lineSeparator()).getBytes()));
        }
    }

    private void postWish(String name, String gift, SocketChannel socketChannel) throws IOException {
        if (!this.isLoggedIn.containsKey(socketChannel.getRemoteAddress())){
            socketChannel.write(ByteBuffer.wrap(("[ You are not logged in ]" + System.lineSeparator()).getBytes()));
            return;
        }
        if (!this.isLoggedIn.get(socketChannel.getRemoteAddress())){
            socketChannel.write(ByteBuffer.wrap(("[ You are not logged in ]" + System.lineSeparator()).getBytes()));
            return;
        }
        if (this.wishes.containsKey(name)) {
            if (this.wishes.get(name).contains(gift)) {
                socketChannel.write(ByteBuffer.wrap(("[ The same gift for student " + name +
                        " was already submitted ]"
                        + System.lineSeparator()).getBytes()));
            } else {
                this.wishes.get(name).add(gift);
                socketChannel.write(ByteBuffer.wrap(("[ Gift " + gift +
                        " for student " + name + " submitted successfully ]"
                        + System.lineSeparator()).getBytes()));
            }
        } else {
            this.wishes.put(name, new ArrayList<>(Collections.singleton(gift)));
            socketChannel.write(ByteBuffer.wrap(("[ Gift " + gift +
                    " for student " + name + " submitted successfully ]"
                    + System.lineSeparator()).getBytes()));
        }
    }

    private void getWish(SocketChannel socketChannel) throws IOException {
        if (!this.isLoggedIn.containsKey(socketChannel.getRemoteAddress())){
            socketChannel.write(ByteBuffer.wrap(("[ You are not logged in ]" + System.lineSeparator()).getBytes()));
            return;
        }
        if (!this.isLoggedIn.get(socketChannel.getRemoteAddress())){
            socketChannel.write(ByteBuffer.wrap(("[ You are not logged in ]" + System.lineSeparator()).getBytes()));
            return;
        }
        if (this.wishes.isEmpty()) {
            socketChannel.write(ByteBuffer.wrap(("[ There are no students present in the wish list ]"
                    + System.lineSeparator())
                    .getBytes()));
        } else {
            List<String> keysAsArray = new ArrayList<>(this.wishes.keySet());
            Random random = new Random();
            String randomName = keysAsArray.get(random.nextInt(keysAsArray.size()));
            socketChannel.write(ByteBuffer.wrap(("[ " + randomName + ": "
                    + this.wishes.get(randomName).toString() + " ]"
                    + System.lineSeparator()).getBytes()));
            this.wishes.remove(randomName);
        }
    }

    private void handleRequest(String message, SocketChannel socketChannel) throws IOException {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        String name = "";
        String[] words = message.split("\\s+");
        String command = words[0];
        if (words.length > 1) {
            name = words[1];
        }
        if (command.equals("disconnect")) {
            socketChannel.close();

        } else if (command.toLowerCase().equals("register")) {
            String password = words[2];
            registerAccount(name, password, socketChannel);

        } else if (command.toLowerCase().equals("login")) {
            String password = words[2];
            login(name, password, socketChannel);

        } else if (command.toLowerCase().equals("logout")) {
            logout(socketChannel);

        } else if (command.toLowerCase().equals("post-wish")) {
            String gift = message.substring(command.length() + name.length() + 2);
            postWish(name, gift, socketChannel);

        } else if (command.toLowerCase().equals("get-wish")) {
            getWish(socketChannel);

        } else {
            socketChannel.write(ByteBuffer.wrap(("[ Unknown command ]" + System.lineSeparator()).getBytes()));
        }
    }

    public static void main(String[] args) {
        WishListServer wishListServer = new WishListServer(7777);
        wishListServer.start();
    }
}
