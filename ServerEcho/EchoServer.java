import java.util.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import java.security.spec.InvalidParameterSpecException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;


public class EchoServer {
    public static int DEFAULT_PORT = 1919;
    public static void main(String [] argv) {
        //seguo gli esempi, in input riceve la porta
        int port;
        try {
            port = Integer.parseInt(argv[0]);
        }
        catch (RuntimeException ex) {
            port = DEFAULT_PORT; 
        }
        ServerSocketChannel serverChannel;
        Selector selector;

        try {     
            serverChannel = ServerSocketChannel.open();
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);
            serverChannel.configureBlocking(false);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }     
        while (true) {
            try {
                selector.select();
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }
            Set <SelectionKey> readyKeys = selector.selectedKeys();
            Iterator <SelectionKey> iterator = readyKeys.iterator();
        
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                // rimuove la chiave dal Selected Set, ma non dal Registered Set
                try {
                    //prima volta che lo uso, ci creo un bytebuffer
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        System.out.println("Accepted connection from " + client);
                        client.configureBlocking(false);
                        SelectionKey key2 = client.register(selector,
                        SelectionKey.OP_WRITE | SelectionKey.OP_READ)  ;
                        ByteBuffer output = ByteBuffer.allocate(4);
                        key2.attach(output); 
                    }
                    else if(key.isReadable() && key.isWritable()) {
                        //per prima cosa leggo l'intero
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        IntBuffer view = buffer.asIntBuffer();
                        
                        client.read(buffer); 
                        int interoricevuto = view.get();
                    
                        //lo rispedisco al client
                        buffer.flip(); //AGGIUNTO ORA
                        
                        //il buffer ha già dentro di sé l'intero, 
                        //quindi lo spedisco e basta al client
                        while(buffer.hasRemaining()) {
                                client.write(buffer);
                            }
                        buffer.clear(); 
                    }
                } catch (IOException ex) { 
                    key.cancel();
                    try { 
                        key.channel().close(); 
                    }
                    catch (IOException cex) {} 
                }
            }
        }
    }
}
    
