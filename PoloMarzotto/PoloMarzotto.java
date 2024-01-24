import java.util.*;
import java.lang.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PoloMarzotto {
    /*
     * Il laboratorio di Informatica del Polo Marzotto è utilizzato da tre tipi di utenti:
     *  studenti, tesisti e professori ed ogni utente deve fare una richiesta al tutor per accedere 
     * al laboratorio. I computers del laboratorio sono numerati da 1 a 20. 
     * Le richieste di accesso sono diverse a seconda del tipo dell'utente:
     * a) i professori accedono in modo esclusivo a tutto il laboratorio, poiché hanno necessità 
     *      di utilizzare tutti i computers per effettuare prove in rete.
     * b) i tesisti richiedono l'uso esclusivo di un solo computer, identificato dall'indice i, 
     *      poiché su quel computer è istallato un particolare software necessario per lo sviluppo della tesi.
     * c) gli studenti richiedono l'uso esclusivo di un qualsiasi computer.
     * I professori hanno priorità su tutti nell'accesso al laboratorio, i tesisti hanno priorità 
     * sugli studenti.
     * Nessuno però può essere interrotto mentre sta usando un computer (prosegue nella pagina successiva)
     */

     /*
      * Scrivere un programma JAVA che simuli il comportamento degli utenti e del tutor. 
      Il programma riceve in ingresso il numero di studenti, tesisti e professori che utilizzano il laboratorio 
      ed attiva un thread per ogni utente. Ogni utente accede k volte al laboratorio, con k generato casualmente. 
      Simulare l'intervallo di tempo che intercorre tra un accesso ed il successivo e l'intervallo 
      di permanenza in laboratorio mediante il metodo sleep della classe Thread. 
      Il tutor deve coordinare gli accessi al laboratorio. 
      Il programma deve terminare quando tutti gli utenti hanno completato i loro accessi al laboratorio.
      Simulare gli utenti con dei thread e incapsulare la logica di gestione del laboratorio all'interno di un monitor.
      */

      
    
      public static void main(String[] args) {
        try {
            if(args.length!=3) {
                throw new IllegalArgumentException("fornire il numero di studenti, tesisti e professori che utilizzano il laboratorio\n");
            }
            int numerostudenti = Integer.parseInt(args[0]);
            int numerotesisti = Integer.parseInt(args[1]);
            int numeroprofessori = Integer.parseInt(args[2]);

            if(numerostudenti<0 ||numerotesisti<0 || numeroprofessori<0)
                throw new IllegalArgumentException("il numero di studenti, tesisti e professori deve essere positivo\n");
            //inserirò i thread in una coda per poi attenderne la terminazione
            ArrayBlockingQueue <Thread> CodaThread = new ArrayBlockingQueue <Thread> (numeroprofessori+numerostudenti+numerotesisti);
            
            //gestisco i 20 computer come un array di booleani
            int numerocomputer = 20;
            Boolean[] computer = new Boolean[numerocomputer];
            for(int i = 0;i<numerocomputer;i++) computer[i] = false;

            AtomicInteger wstudenti = new AtomicInteger(0);
            AtomicInteger wtesisti = new AtomicInteger(0);
            AtomicInteger  wprof = new AtomicInteger(0);
            AtomicInteger  astudenti = new AtomicInteger(0);
            AtomicInteger  atesisti = new AtomicInteger(0);
            AtomicInteger  aprof = new AtomicInteger(0);

            //definisco una variabile su cui bloccarsi,
            //altrimenti professori, studenti e tesisti non acquisiscono e rilasciano
            //il computer in mutua esclusione
            Object lock = new Object();

            for(int i = 0;i<numerostudenti;i++) {
                Thread Studente = new Studente(i+1,computer,wstudenti, wtesisti,  wprof,  astudenti,  atesisti,  aprof,
                lock);
                Studente.start();
                CodaThread.put(Studente);
            }

            for(int i = 0;i<numerotesisti;i++) {
                int computerindice = new Random().nextInt(19);
                Thread Tesista = new Tesista(i+1,computer,wstudenti, wtesisti,  wprof,  astudenti,  atesisti,  aprof, 
                    computerindice,lock);
                Tesista.start();
                CodaThread.put(Tesista);
            }

            for(int i = 0;i<numeroprofessori;i++) {
                Thread Professore = new Professore(i+1,computer, wstudenti, wtesisti,  wprof,  astudenti,  atesisti,  aprof, 
                    numerocomputer,lock);
                Professore.start();
                CodaThread.put(Professore);
            }

            //Attendo che tutti i thread terminino
            for (Thread thread : CodaThread) {
                try {
                    thread.join(); 
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        System.out.println("Il laboratorio sta per chiudere, arrivederci\n");
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
//devo aggiornarre le chiamate di notifyAll. Conviene definire due variabili di condizione diverse,
//e chiamare prima quella dei professori e poi quella dei tesisti, + simile per le altre due classi

//le guardie per lettori-scrittori dovrò metterle qua
class Studente extends Thread {
    //uso AtomicInteger perché voglio che il valore sia passato per riferimento
    private int id;
    private AtomicInteger wstudenti, wtesisti,  wprof,  astudenti,  atesisti,  aprof;
    private Boolean[] computer;
    private Object lock;

    public Studente(int id, Boolean[] computer, AtomicInteger wstudenti, AtomicInteger wtesisti, AtomicInteger wprof, AtomicInteger astudenti, AtomicInteger atesisti, AtomicInteger aprof,
    Object lock) {
        this.id = id;
        this.computer = computer;
        this.wstudenti = wstudenti;
        this.wtesisti = wtesisti;
        this.wprof = wprof;
        this.astudenti = astudenti;
        this.atesisti = atesisti;
        this.aprof = aprof;
        this.lock = lock;
    }

    private int acquisiscicomputer() {
        synchronized(lock) {
            wstudenti.incrementAndGet();
            //se ci sono professori in attesa faccio entrare prima loro.
            //se ci sono professori al lavoro mi sospendo
            //se ci sono tesisti in attesa, faccio prendere loro il loro computer, poi ne userò uno qualsiasi
            //se tutti i computer sono occupati, mi sospendo
            while(wprof.get()>0 || aprof.get()>0 || wtesisti.get()>0 || (astudenti.get()+atesisti.get())==20) {
                try {
                    lock.wait();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            wstudenti.decrementAndGet();
            astudenti.incrementAndGet();

            int i=0;
            boolean trovato = false;
            while(i<20 && !trovato) {
                if(this.computer[i]==false) {
                    this.computer[i]=true;
                    trovato = true;
                }
                i++;
            }
            assert i<20;
            return i-1;
        }
        
    }

    private synchronized void rilasciacomputer(int i) {

        synchronized(lock) {
            astudenti.decrementAndGet();
            this.computer[i] = false;
            lock.notifyAll();
        }

    }
    public void run() {
        
        int numerovolteuso = ThreadLocalRandom.current().nextInt(20);
        for(int i = 0;i<numerovolteuso;i++) {
            int computerusato = acquisiscicomputer();
            //System.out.println("Studente accede al computer numero"+(computerusato+1));
            try {
                System.out.println("Studente "+ id +" inizia col computer numero "+(computerusato+1));
                Thread.sleep(2000);
                System.out.println("Studente "+ id +" finisce col computer numero "+(computerusato+1));
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            rilasciacomputer(computerusato);
        }
    }
}



class Tesista extends Thread {
    
    private int id;
    private int indicecomputer;
    //uso Integer perché voglio che il valore sia passato per riferimento
    private AtomicInteger wstudenti, wtesisti,  wprof,  astudenti,  atesisti,  aprof;
    private Boolean[] computer;
    private Object lock;

    public Tesista(int id, Boolean[] computer, AtomicInteger wstudenti, AtomicInteger wtesisti, AtomicInteger wprof, AtomicInteger astudenti, AtomicInteger atesisti, AtomicInteger aprof, 
    int i, Object lock) {
        this.id = id;
        this.computer = computer;
        this.wstudenti = wstudenti;
        this.wtesisti = wtesisti;
        this.wprof = wprof;
        this.astudenti = astudenti;
        this.atesisti = atesisti;
        this.aprof = aprof;
        this.indicecomputer = i;
        this.lock = lock;
    }

    private synchronized void acquisiscicomputer(int indicecomputer) {
        synchronized(lock) {
            wtesisti.incrementAndGet();
            while(wprof.get()>0 || aprof.get()>0 || (astudenti.get()+atesisti.get())==20 || computer[indicecomputer]) {
                try {
                    lock.wait();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            wtesisti.decrementAndGet();
            atesisti.incrementAndGet();

            computer[indicecomputer]=true;
        }
    }

    private synchronized void rilasciacomputer(int indicecomputer) {
        synchronized(lock) {
            atesisti.decrementAndGet();
            computer[indicecomputer] = false;

            lock.notifyAll();
        }
    }

    public void run() {
        int numerovolteuso = ThreadLocalRandom.current().nextInt(20);
        for(int i = 0;i<numerovolteuso;i++) {
            acquisiscicomputer(indicecomputer);
            try {
                System.out.println("Tesista "+ id +" inizia col computer numero "+(indicecomputer+1));
                Thread.sleep(2000);
                System.out.println("Tesista "+ id +" finisce col computer numero "+(indicecomputer+1));
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            rilasciacomputer(indicecomputer);
        }
    }
}




class Professore extends Thread {
    private int id;
    private AtomicInteger wstudenti, wtesisti,  wprof,  astudenti,  atesisti,  aprof;
    private Boolean[] computer;
    private int numerocomputer;
    private Object lock;

    public Professore(int id, Boolean[] computer, AtomicInteger wstudenti, AtomicInteger wtesisti, AtomicInteger wprof, AtomicInteger astudenti, AtomicInteger atesisti, AtomicInteger aprof, 
    int numerocomputer, Object lock) {
        this.id = id;
        this.computer = computer;
        this.wstudenti = wstudenti;
        this.wtesisti = wtesisti;
        this.wprof = wprof;
        this.astudenti = astudenti;
        this.atesisti = atesisti;
        this.aprof = aprof;
        this.numerocomputer = numerocomputer;
        this.lock = lock;
    }

    private synchronized void acquisiscicomputer() {
        synchronized(lock) {
            wprof.incrementAndGet();
            while(aprof.get()>0 || astudenti.get()>0 || atesisti.get()>0) {
                try {
                    lock.wait();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            wprof.decrementAndGet();
            aprof.incrementAndGet();
            for(int i = 0;i<numerocomputer;i++) computer[i] = true;
        }
    }

    private synchronized void rilasciacomputer() {
        synchronized(lock) {
            for(int i = 0;i<numerocomputer;i++) computer[i] = false;
            aprof.decrementAndGet();
            
            lock.notifyAll();
        }
    }

    public void run() {
        int numerovolteuso = ThreadLocalRandom.current().nextInt(20);
        for(int i = 0;i<numerovolteuso;i++) {
            acquisiscicomputer();
            try {
                System.out.println("Professore "+ id +" inizia con tutti i computer");
                Thread.sleep(2000);
                System.out.println("Professore "+ id +" finisce con tutti i computer");

            }
            catch(Exception e) {
                e.printStackTrace();
            }
            rilasciacomputer();
        }
    }
}