import java.util.*;
import java.lang.*;
import java.lang.Exception;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
/*
 * Simulare il flusso di clienti in un ufficio postale che ha 4 sportelli. Nell'ufficio esiste:

un'ampia sala d'attesa in cui ogni persona può entrare liberamente. 
Quando entra, ogni persona prende il numero dalla numeratrice e aspetta il proprio turno in questa sala.
una seconda sala, meno ampia, posta davanti agli sportelli, in cui si può entrare solo a gruppi di k persone
una persona si mette quindi prima in coda nella prima sala, poi passa nella seconda sala.
ogni persona impiega un tempo differente per la propria operazione allo sportello. 
Una volta terminata l'operazione, la persona esce dall'ufficio.
Scrivere un programma in cui:
l'ufficio viene modellato come una classe JAVA, in cui viene attivato un ThreadPool di dimensione uguale al numero degli sportelli
la coda delle persone presenti nella sala d'attesa è gestita esplicitament dal programma
la seconda coda (davanti agli sportelli) è quella gestita implicitamente dal ThreadPool
ogni persona viene modellata come un task, un task che deve essere assegnato ad uno dei thread associati agli sportelli
si preveda di far entrare tutte le persone nell'ufficio postale, all'inizio del programma

Facoltativo: prevedere il caso di un flusso continuo di clienti e la possibilità che l'operatore chiuda lo sportello stesso 
dopo che in un certo intervallo di tempo non si presentano clienti al suo sportello.
 */

//è come se fosse prod cons. Sala d'attesa mette roba (non so ancora cosa) in una coda e uno sportello lo prende

/*
 * Nella mia soluzione la Sala Sportelli è implementata come un threadpool. Si suppone che sia l'utente a
 * simulare di essere l'amministratore di un ufficio postale, quindi sarà l'utente a scegliere il numero di sportelli.
 * I clienti vengono trattati come Task, e di conseguenza messi in una coda, la quale simula la Sala d'Attesa.
 * Poi il threadPool
 */


/*
 * Devo aggiungere k Task nella Coda del ThreadPool. 
 * Farò quindi partire un thread che si occupa di avere sempre k Task nella coda del threadpool.
 * Questo thread termina quando la coda è vuota.
 */
public class UfficioPostale {
    
    public static void main(String[] args) {
        //attivo un ThreadPool di dimensione uguale al numero degli sportelli
        try {
            int numerosportelli = Integer.parseInt(args[0]);
            if(numerosportelli<0)
                throw new IllegalArgumentException("Il numero di thread deve essere maggiore o uguale a 1");
            ExecutorService service = new ThreadPoolExecutor(numerosportelli,numerosportelli,60,TimeUnit.SECONDS, 
                new LinkedBlockingQueue <Runnable> (numerosportelli));
            //In LinkedBlockingQueue i thread sono limitati, quindi la coda per tenere i task è illimitata e di conseguenza nuovi thread non sono mai creati
            //
            System.out.println("Tutti i thread sono stati attivati\n");
            
            try {
                //creo la coda dei task
                LinkedBlockingQueue <Task> Coda = new LinkedBlockingQueue <Task> ();

                //per prima cosa apro gli sportelli
                for(int i = 0; i<numerosportelli; i++) {
                    service.execute(new Sportello(Coda));
                }
                //in secondo luogo inizio a mettere i task in coda
                int numeroticket = 0;
                for(int i = 0; i<new Random().nextInt(1901)+100; i++) {
                    Coda.put(new Task((i+1)*1000,numeroticket));
                    numeroticket++;
                    System.out.printf("numero ticket %d\n", numeroticket);
                }
                for(int i =0;i<numerosportelli;i++) {
                    Coda.put(new Task(0,0));
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
                if(e instanceof RuntimeException) {
                    System.err.println("Errore durante il calcolo del numero dei primi");
                    e.printStackTrace();
                }
                    
                else
                    e.printStackTrace();
            }
        }
        catch(NumberFormatException e) {
            System.err.println("La stringa non è un numero valido.");
        } 
    }
}

class Task {
    public int tempoallosportello;
    public int numeroticket;
    public Task(int i, int numeroticket) {
        this.tempoallosportello = i;
        this.numeroticket = numeroticket;
    }

    public void facoseallosportello(int i) {
    
        try {
            System.out.printf("ticket numero %d fa robe allo sportello\n", this.numeroticket);
            // Metti in pausa l'esecuzione per i millisecondi
            Thread.sleep(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


class Sportello implements Runnable {
    public LinkedBlockingQueue <Task> Coda;
    
    public Sportello (LinkedBlockingQueue <Task> Coda) {
        this.Coda = Coda;
    }
    
    public void run() {
        try {
            while(true) {
                Task TaskPrelevato = this.Coda.take();
                if( TaskPrelevato.numeroticket==0 && TaskPrelevato.tempoallosportello==0) break;
                TaskPrelevato.facoseallosportello(TaskPrelevato.tempoallosportello);
            }
        }
        catch (InterruptedException e) {
            System.err.println("Attesa interrotta.");
        }
    }
}
