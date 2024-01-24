import java.util.*;
import java.lang.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.spec.InvalidParameterSpecException;

public class serverDungeonAdventures {

    private final static int PORT = 1313;

    public static void main(String [] args) {
        //genero un threadpool di 20 thread
        //ogni thread si occupa di gestire la partita col proprio client
        //quindi è dentro il corpo del thread che sarà aperta connessione e 
        //gestito il gioco
        ExecutorService service = new ThreadPoolExecutor(5,20 ,180,TimeUnit.SECONDS, 
                    new LinkedBlockingQueue <Runnable> ());
        int idgiocatore = 1;
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                try {
                    Socket connection = server.accept();
                    service.execute(new Game(connection,idgiocatore));
                    idgiocatore++;
            // connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } 

       catch (Throwable e) {
                    System.err.println("Attesa interrotta.");
                    e.printStackTrace();
        }
        finally {
            service.shutdown(); 
            try {
                    service.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    System.err.println("Attesa interrotta.");
                    e.printStackTrace();
                }
        }
    }
}

class Game implements Runnable {
    Socket connection;
    private int idgiocatore;

    public Game (Socket connection, int idgiocatore) {
        this.connection = connection;
        this.idgiocatore = idgiocatore;
    }

    private void stamparisultati (Mostro mostro, Player player, int round, int idgiocatore) {
        System.out.println("Round "+ round + " col giocatore numero "+ idgiocatore);
        String resMostro = "Mostro:\n   salute-> "+ mostro.getsalute() + "\n";
        String resPlayer = "Player:\n   salute-> "+ player.getsalute() + "\n    pozione-> " + player.getpozione()+ "\n";
        System.out.println(resMostro + resPlayer);        
    }

    private void spediscirisultati(Mostro mostro, Player player, PrintWriter out, int continua, int esci, int pozionefinita) {
        //int salute_mostro, salute_player, pozione_player, continua;
        out.println(mostro.getsalute());
        out.println(player.getsalute());
        out.println(player.getpozione());

        //continua==1 se la partita continua, cioè né server né client hanno vinto la partita
        out.println(continua);

        //esci == 0 se l'utente vuole terminare la partita
        out.println(esci);

        //pozionefinita==1 se la pozione è finita, 0 altrimenti
        out.println(pozionefinita);
    }


    public void run() {
        try {
            Mostro mostro = new Mostro();
            Player player = new Player();

            //mi metto in attesa dei comandi inviati dal server
            try(Scanner in = new Scanner(connection.getInputStream());
            PrintWriter out = new PrintWriter(connection.getOutputStream(),true)) { 

                //per prima cosa spedisco la situazione iniziale, così il cliente
                //decide che cosa fare
                spediscirisultati(mostro, player, out,1,0,0);
                System.out.println("il giocatore "+ idgiocatore + " vuole iniziare una partita\n");
                stamparisultati(mostro, player, 0, idgiocatore);
                int round = 1;
                while (in.hasNextLine()) {
                    String comando = in.nextLine();
                    System.out.println("comando ricevuto dal client: "+ comando);

                    if(comando.equals("exit")) {
                        spediscirisultati(mostro, player, out,0,1,0);
                        System.out.println("fine partita, client non vuole più giocare\n");
                        break;
                    }
                        
                    else if(comando.equals("combatti")) {
                        mostro.combatti();
                        player.combatti();


                        stamparisultati(mostro, player,round,idgiocatore);
                        //se il giocatore ha vinto o pareggiato, può chiedere di 
                        //giocare nuovamente, se invece ha perso deve uscire dal gioco

                        if(player.getsalute()<=0 && mostro.getsalute()<=0) {
                            System.out.println("fine partita, siete pari\n");
                            //devi gestire il cliente che può voler giocare di nuovo
                            spediscirisultati(mostro, player, out, 0,0,0);
                            break;
                        }
                        else if(mostro.getsalute()<=0) {
                            System.out.println("fine partita, client ha vinto\n");
                            spediscirisultati(mostro, player, out, 0,0,0);
                            break;
                        }
                        else if(player.getsalute()<=0) {
                            System.out.println("fine partita, client ha perso\n");
                            spediscirisultati(mostro, player, out, 0,1,0);
                            break;
                        }

                        
                        else 
                            spediscirisultati(mostro, player, out,1,0,0);
                        //al client spedisco una quintupla
                        //(salute mostro, salute player, pozione player, continua,pozionefinita)


                    }

                    else if(comando.equals("bevi pozione")) {
                        int pozionefinita = player.bevi_pozione();
                        stamparisultati(mostro, player,round,idgiocatore);
                        spediscirisultati(mostro, player, out,1,0,pozionefinita);
                    }

                    else {
                        System.out.println("comando fornito: "+ comando);
                        throw new InvalidParameterSpecException("fornire comando adeguato");
                    }
                    round++;
                }
            } catch (Exception e) { 
                System.err.println("Error:" + connection); 
                e.printStackTrace();
            }
            
            //il giocatore ha digitato "exit", dunque la partita è finita
            connection.close();
        }
        catch (IOException e) {
            System.err.println("Attesa interrotta.");
            e.printStackTrace();
        }
    }
}



abstract class Personaggio {
    protected int livello_salute;
    public Personaggio() {
        this.livello_salute = ThreadLocalRandom.current().nextInt(100);
    }

    

    public void combatti() {
        if(livello_salute<=4) {
            this.livello_salute=0;
            return;
        }
        int salutepersa = ThreadLocalRandom.current().nextInt(this.livello_salute);
        this.livello_salute-=salutepersa;
    }
    public int getsalute () {
        return this.livello_salute;
    }

    

}

class Mostro extends Personaggio {
    
}

class Player extends Personaggio {
    private int livello_pozione;
    public Player() {
        super();
        this.livello_pozione = ThreadLocalRandom.current().nextInt(75);
        
    }

    public int bevi_pozione() {
        if(livello_pozione<=0) {
            System.out.println("client ha esaurito la pozione\n");
            return 1;
        }
        else if(livello_pozione<=4) {
            this.livello_salute += this.livello_pozione;
            this.livello_pozione=0;
            return 0;
        }
        else {
            int pozione_bevuta = ThreadLocalRandom.current().nextInt(livello_pozione);
            this.livello_pozione -=pozione_bevuta;
            this.livello_salute+=pozione_bevuta;
            return 0;
        }
        
    }

    public int getpozione () {
        return this.livello_pozione;
    }
}