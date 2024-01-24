import java.util.*;
import java.lang.*;
import java.nio.file.NotDirectoryException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.net.*;




public class WebLog {
    public static void main (String[] args) {
        try {
            if(args.length!=1)
                throw new IllegalArgumentException("fornire il nome di un file di log");
            File filelog = new File(args[0]);
            if(!filelog.isFile())
                throw new IllegalArgumentException("fornire il nome di un file di log");
            
            /*
             * 
             * Il log file di un web server contiene un insieme di linee, 
             * con il seguente formato:

            150.108.64.57 - - [15/Feb/2001:09:40:58 -0500] "GET / HTTP 1.0" 200 2511

            in cui:

            150.108.64.57 indica l'host remoto, in genere secondo la dotted quad form
            [data]
            "HTTP request" è il tipo di richiesta http
            status
            bytes sent
            eventuale tipo del client "Mozilla/4.0......."
            Scrivere un'applicazione Weblog che prende in input il nome del log file 
            (che saràfornito) e ne stampa ogni linea, in cui ogni indirizzo IP è 
            sostituito con l'hostname. Sviluppare due versioni del programma, 
            la prima single-threaded, la seconda invece utilizza un thread pool, 
            in cui il task assegnato ad ogni thread riguarda la traduzione di un 
            insieme di linee del file. Confrontare i tempi delle due versioni.
            */
            long startTimeST, elapsedTimeST, startTimeTP, elapsedTimeTP;
            //prima faccio partire il single threaded, poi il threadpool
            Thread singleThread = new Thread(new SingleThreadedWebLog(filelog));
            startTimeST = System.nanoTime();
            singleThread.start();
            singleThread.join();
            elapsedTimeST = System.nanoTime() - startTimeST;
            System.out.println("Elapsed Time with only one thread is " + (elapsedTimeST / 1000000.0) + " msec");

            //alla classe col thread pool passo il file. questa poi si occuperà di fare i file da sé
            Thread ThreadPool = new Thread(new ThreadPoolWebLog(filelog));
            startTimeTP = System.nanoTime();
            ThreadPool.start();
            ThreadPool.join();
            elapsedTimeTP = System.nanoTime() - startTimeTP;
            System.out.println("Elapsed Time with a thread pool is " + (elapsedTimeTP / 1000000.0) + " msec");
            System.out.println("Time difference is "+ ((elapsedTimeST / 1000000.0)-(elapsedTimeTP / 1000000.0))+ " msec");
        }
        catch(Throwable e) {
            e.printStackTrace();
        }

    }
}


class SingleThreadedWebLog extends Thread {
    private File file;
    public SingleThreadedWebLog(File file) {
        this.file = file;
    }
    public void run() {
        try {            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String linealetta;
            while ((linealetta = reader.readLine()) != null) {
                String [] linea = linealetta.split(" - - ");
                String IPaddressAsString = linea[0];
                String [] IPStringarray = IPaddressAsString.split("\\.");
                byte [] indirizzo = new byte[IPStringarray.length];
                for(int i =0;i<IPStringarray.length;i++) {
                    indirizzo[i] = (byte) Integer.parseInt(IPStringarray[i]);
                }
                InetAddress address = InetAddress.getByAddress(indirizzo);
                String hostname = address.getHostName();
                System.out.println(hostname +" - - "+linea[1]+"\n");
            }
            reader.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }    
    }
}

//Il threadpool è usato solamente per accedere al DNS, altrimenti l'esperimento
//non sarebbe valido per valutare quanto ciò influisca nell'efficienza

class ThreadPoolWebLog extends Thread{

    private File file;
    public ThreadPoolWebLog(File file) {
        this.file = file;
    }


    public void run () {
        try {
            ExecutorService service = Executors.newFixedThreadPool(10);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String linealetta;
            while ((linealetta = reader.readLine()) != null) {
                String [] linea = linealetta.split(" - - ");
                String IPaddressAsString = linea[0];

                String [] IPStringarray = IPaddressAsString.split("\\.");
                byte [] indirizzo = new byte[IPStringarray.length];
                for(int i =0;i<IPStringarray.length;i++) {
                    indirizzo[i] = (byte) Integer.parseInt(IPStringarray[i]);
                }

                Future <String> hostname = service.submit(new Callable <String> () {
                    public String call() throws Exception {
                        try {
                            InetAddress address = InetAddress.getByAddress(indirizzo);
                            String hostname = address.getHostName();
                            return hostname;
                        }
                        catch (UnknownHostException e) {
                            System.err.println("IPaddress non trovato");
                            e.printStackTrace();
                            return null;
                        }
                        
                    }
                });
                try {
                    System.out.println(hostname.get()+" - - "+linea[1]+"\n");
                }
                catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                
            }
            reader.close();

            service.shutdown();
            try {
                service.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                System.err.println("Attesa interrotta.");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }    

    }
}

