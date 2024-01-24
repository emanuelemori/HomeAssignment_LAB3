import java.util.*;
import java.lang.*;
import java.nio.file.NotDirectoryException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ContaOccorrenze {
    /*
     * scrivere un programma che conta le occorrenze dei caratteri alfabetici
    (lettere dalla “A” alla “Z”) in un insieme di file di testo. Il programma prende
    in input una serie di percorsi di file testuali e per ciascuno di essi conta le
    occorrenze dei caratteri, ignorando eventuali caratteri non alfabetici (come
    per esempio le cifre da 0 a 9). Per ogni file, il conteggio viene effettuato da
    un apposito task e tutti i task attivati vengono gestiti tramite un pool di
    thread. I task registrano i loro risultati parziali all’interno di una
    ConcurrentHashMap.
    Prima di terminare, il programma stampa su un apposito file di output il
    numero di occorrenze di ogni carattere. Il file di output contiene una riga
    per ciascun carattere ed è formattato come segue:
     */


    public static void main(String [] args) {
        try {
            int numerofile = args.length;
            if(numerofile<1) 
                throw new IllegalArgumentException("fornire almeno un filepath\n");
            String [] paths = args;
            ExecutorService service = Executors.newFixedThreadPool(numerofile);
            Map <String, Integer> Map = new ConcurrentHashMap <> (26);

            for(String filepath: paths) {
                File file = new File(filepath);
                if(!file.isFile())
                    throw new IllegalArgumentException("fornire solamente filepath\n");
                service.execute(new Scanner(file,Map));
            }


            service.shutdown(); 
            try {
                service.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                System.err.println("Attesa interrotta.");
            }

            File fileoccorrenze = new File("fileoccorrenze");
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileoccorrenze));

            Set<Map.Entry<String, Integer>> entrySet = Map.entrySet();
            Iterator<Map.Entry<String, Integer>> iterator = entrySet.iterator();
            while( iterator.hasNext() ){
                Map.Entry<String, Integer> entry = iterator.next();  
                String stringadatradurre = entry.getKey() + ", " + entry.getValue() + "\n";
                byte[] byteArray = stringadatradurre.getBytes();
                bufferedOutputStream.write(byteArray);
            }
            bufferedOutputStream.close();
        }
        
        
        catch(Throwable e) {
            e.printStackTrace();
        }
    }
}

class Scanner implements Runnable {
    private File file;
    private Map <String, Integer> Map;
    public Scanner(File file, Map <String, Integer> Map) {
        this.file = file;
        this.Map = Map;
    }

    private void aggiungi(String lettera, Map <String, Integer> Map) {
        Map.merge(lettera, 1, Integer::sum);
    }

    public void run() {
        try {
            BufferedInputStream bufferedInput = new BufferedInputStream(new FileInputStream(file));
            
            int byteletto;
            while ((byteletto = bufferedInput.read()) != -1) {
                char carattere = (char) byteletto;
                if (Character.isLetter(carattere)) {
                    String carattereAsString = String.valueOf(carattere);
                    String caratteredaaggiungere = carattereAsString.toLowerCase();
                    aggiungi(caratteredaaggiungere,Map);
                }
            }
            bufferedInput.close();
        } catch (Throwable e) {
                System.err.println("Attesa interrotta");
            }
        
    }
}