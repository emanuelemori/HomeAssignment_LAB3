import java.util.*;
import java.lang.*;
import java.lang.Exception;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/*scrivere una applicazione JAVA che
● crea e attiva n thread.
● ogni thread esegue esattamente lo stesso task, ovvero 
conta il numero di interi minori di 10,000,000 che sono primi
● il numero di thread che devono essere attivati e mandati 
in esecuzione viene richiesto all’utente, che lo inserisce 
tramite la CLI (Command Line Interface)
● analizzare come varia il tempo di esecuzione dei thread 
attivati a seconda del loro numero*/

public class ContaPrimi implements Runnable {
    static long numeromassimo = 10000000;

    private long a,b;
    private int threadID;
    public ContaPrimi(int threadID, long a, long b) {
        this.threadID = threadID;
        this.a = a;
        this.b = b;
    }

    public boolean primo(long n) {
        assert n>=0: "n deve essere non negativo";
        if(n==0 || n==1) return false;
        if(n==2) return true;
        for(long i=3;i<=Math.sqrt(n);i++) {
            if(n % i==0) return false;
        }
        return true;
    }

    public void run() {
        try {
            long tinizio = System.currentTimeMillis();
            int numero_primi_trovati = 0;
            for(long i = a;i<=b;i++) {
                if(primo(i)) numero_primi_trovati++;
            }
            long tfine = System.currentTimeMillis();
            System.out.printf("trovati %d primi in %d secondi dal thread %d\n", 
                numero_primi_trovati, Math.round((tfine-tinizio)/1000),
                this.threadID);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //ti manca definire la wait in modo che termini quando i thread hanno terminato. Quindi ogni thread 
    //deve terminare dopo aver eseguito i conti
    public static void main(String[] args) {
        try {
            int numerothread = Integer.parseInt(args[0]);
            if(numerothread<1)
                throw new IllegalArgumentException("Il numero di thread deve essere maggiore di 1");
            ExecutorService service = Executors.newFixedThreadPool(numerothread);
            System.out.println("Tutti i thread sono stati attivati");
            
            try {
                for(int i = 0; i<numerothread; i++) {
                    service.execute(new ContaPrimi(i+1,0,numeromassimo));
                }
                //Comunico che non accetterò nuovi task. Aggiungo le seguenti righe di codice per far
                //sì che il programma termini da sé una volta che i thread sono terminati
                service.shutdown(); 
                try {
                    service.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    System.err.println("Attesa interrotta.");
                }
            }
            catch (Throwable e) {
                if(e instanceof RuntimeException)
                    System.err.println("Errore durante il calcolo del numero dei primi");
                else
                    e.printStackTrace();
            }
        }
        catch(NumberFormatException e) {
            System.err.println("La stringa non è un numero valido.");
        } 
        
        
    }
}

