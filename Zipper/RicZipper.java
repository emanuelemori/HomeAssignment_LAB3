import java.util.*;
import java.lang.*;
import java.nio.file.NotDirectoryException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RicZipper {
    /*
     * scrivere un programma che dato in input una lista di directories, comprima
     * tutti i file in esse contenuti, con l'utility gzip
     * ● ipotesi semplificativa:
     * ● zippare solo i file contenuti nelle directories passate in input,
     * ● non considerare ricorsione su eventuali sottodirectories
     * ● il riferimento ad ogni file individuato viene passato ad un task, che deve essere
     * eseguito in un threadpool
     * ● individuare nelle API JAVA la classe di supporto adatta per la compressione
     * ● NOTA: l'utilizzo dei threadpool è indicato, perchè I task presentano un buon
     * mix tra I/O e computazione
     * ● I/O heavy: tutti i file devono essere letti e scritti
     * ● CPU-intensive: la compressione richiede molta computazione
     * ● facoltativo: comprimere ricorsivamente I file in tutte le sottodirectories
     */
    public static final long closingDelay = 60000;
    public static int coreCount = Runtime.getRuntime().availableProcessors();

    public static int  RicZipperFS(String directory, ExecutorService service) {

        File dirAsFile = new File(directory);
        if (dirAsFile.isDirectory()) {
            File[] files = dirAsFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if(file.isFile()) {
                        service.execute(new Gzipper(file));
                    }
                    //chiamo ricorsivamente la funzione sulla directory trovata
                    else if(file.isDirectory()) {
                        int a = RicZipperFS(file.getAbsolutePath(),service);
                        assert a==1;
                    }
                        
                }
            }
        } 
        else { //ho passato una stringa che non è una directory
            return 0;
        }
        return 1;
    }
    public static void main(String[] args) {
        try {
            String[] listadirectory = args;
            //threadpool con coda illimitata e numero fissato di thread che corrisponde al numero di core
            ExecutorService service = Executors.newFixedThreadPool(coreCount);

            //scorro ogni directory. Se in questa directory apro un file lo do a un thread
            if(listadirectory.length<1) {
                throw new IllegalArgumentException("fornire almeno una directory\n");
            }
            for(String directory :listadirectory) 
                if(RicZipperFS(directory, service)==0)
                    throw new NotDirectoryException(directory+ " non è una directory\n");

            

            service.shutdown(); 
            try {
                service.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                System.err.println("Attesa interrotta.");
            }

        } catch(Throwable e) {
            e.printStackTrace();
        }

    }
}


class Gzipper implements Runnable {
    private File file;
    public Gzipper(File file) {
        this.file = file;
    }

    public void run() {
        String pathfilezip = file.getAbsolutePath() + ".zip";

        try {
            ZipOutputStream outputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(pathfilezip)));
            BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(file));

            ZipEntry entry = new ZipEntry(file.getName());
            outputStream.putNextEntry(entry);

            byte[] buffer = new byte[1024];
            int len;

            while ((len = bufferedInput.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

            bufferedInput.close();
            outputStream.closeEntry();
            outputStream.close();
        }
        catch(Throwable e) {
            e.printStackTrace();
        }
    }
}