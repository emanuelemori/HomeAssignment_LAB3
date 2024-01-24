import java.util.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;

import javax.naming.CommunicationException;

import java.security.spec.InvalidParameterSpecException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

public class EchoClient {
    public static int DEFAULT_PORT = 1919;
    public static void main(String[] args) {
        int port;
        try {
            //l'utente può fornire in input il numero della porta
            port = Integer.parseInt(args[0]);
        }
        catch (RuntimeException ex) {
            port = DEFAULT_PORT; 
        }
        try { 
            InetAddress localhost = InetAddress.getLocalHost();
            SocketAddress address = new InetSocketAddress(localhost, port);

            Scanner comandolatoutente = new Scanner(System.in);            
            SocketChannel client = SocketChannel.open(address);
            //da bufferin il client riceve il messaggio dal server
            ByteBuffer bufferin = ByteBuffer.allocate(4);
            //da bufferout il client spedisce il messaggio al server
            ByteBuffer bufferout = ByteBuffer.allocate(4);

            IntBuffer viewin = bufferin.asIntBuffer();
            IntBuffer viewout = bufferout.asIntBuffer();

            ByteBuffer buffer = ByteBuffer.allocate(4);

            IntBuffer view = bufferin.asIntBuffer();
            while(true) {
                //ricevo l'intero dall'utente
                System.out.println("\ndigita un intero, -1 per uscire'\n");
                String line = comandolatoutente.nextLine();
                int interodaspedire = Integer.parseInt(line);
                if(interodaspedire==-1) 
                    break;
                
                view.put(interodaspedire); 

                //spedisco l'intero al server
                while(buffer.hasRemaining()) {
                    client.write(buffer);
                }
                buffer.flip(); 
                view.rewind(); 
                //attendo l'intero dal server
                client.read(buffer);
                int interoricevuto = view.get();
                buffer.clear();
                view.rewind();
                if(interoricevuto!=interodaspedire) 
                    throw new CommunicationException();
                System.out.println("intero ricevuto: "+ interoricevuto);
            }
        } catch(Throwable ex) {  
            ex.printStackTrace(); 
        } 
    } 
}