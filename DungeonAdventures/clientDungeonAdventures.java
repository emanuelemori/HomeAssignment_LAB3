import java.util.*;
import java.lang.*;
import java.util.concurrent.*;
import javax.naming.CommunicationException;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.io.PrintWriter;



public class clientDungeonAdventures {

    private final static int PORT = 1313;


    private static int [] resoconto(Scanner aggiornamento, int numeroround) {
        
        System.out.println("\nROUND " + numeroround + ":\n");

        int salute_mostro = Integer.parseInt(aggiornamento.nextLine());
        int salute_player = Integer.parseInt(aggiornamento.nextLine());
        int pozione_player = Integer.parseInt(aggiornamento.nextLine());

        System.out.println("    salute mostro: "+ salute_mostro);
        System.out.println("    salute player: "+ salute_player);
        System.out.println("    pozione player: "+ pozione_player);
        int partitacontinua = Integer.parseInt(aggiornamento.nextLine());
        int esci = Integer.parseInt(aggiornamento.nextLine());
        int pozionefinita = Integer.parseInt(aggiornamento.nextLine());

        if(salute_mostro<=0 && salute_player<=0)
            System.out.println("la partita è finita. tu ed il mostro siete pari\n");
        else if(salute_mostro<=0)
            System.out.println("hai vinto!\n");
        else if(salute_player<=0)
            System.out.println("hai perso\n");

        if(pozionefinita==1)
            System.out.println("\nhai esaurito la pozione\n");
        int res[] = new int[2];
        res[0] = partitacontinua;
        res[1] = esci;
        return res;
    }
    
    public static void main(String [] args) {
        boolean continuaagiocare = true;
        System.out.println("---------------INIZIO PARTITA-----------------");
        Scanner comandolatoutente = new Scanner(System.in);
        while (continuaagiocare) {
            try  {
                InetAddress localhost = InetAddress.getLocalHost();
                Socket socket = new Socket(localhost, PORT);

                //riceve i comandi dell'utente dal terminale

                //riceve gli aggiornamenti della partita dal server
                Scanner aggiornamento = new Scanner(socket.getInputStream());

                //spedisce i comandi ricevuti dal terminale al server
                PrintWriter comandodaspedire = new PrintWriter(socket.getOutputStream(),
            true);

                int numeroround = 1;
                
                //per far visualizzare all'utente la situazione iniziale
                //per prima cosa ricevo i dati, poi entro nel while
                resoconto(aggiornamento,0);
                
                while (true) { 
                    System.out.println("\ndigita 'combatti', 'bevi pozione' o 'exit'\n");
                    String line = comandolatoutente.nextLine();
                    
                    comandodaspedire.println(line);

                    //ora visualizzo gli aggiornamenti

                    //devi gestire il continua partita o no se perdi
                    int [] res = resoconto(aggiornamento,numeroround);
                    int partitacontinua = res[0];
                    int vuoleuscire = res[1];

                    //ora gestisco i casi in cui il cliente ha perso o vuole finire la partita
                    //e quindi dico che ha perso e chiudo la connessione
                    if(partitacontinua==0 && vuoleuscire==1) {
                        continuaagiocare= false;
                        break;
                    }

                    //ora gestisco il caso in cui il cliente ha vinto o c'è parità, quindi
                    //chiedo se vuole giocare ancora o no
                    else if(partitacontinua==0 && vuoleuscire==0) {
                        System.out.println("Vuoi giocare di nuovo?");
                        String siono = comandolatoutente.nextLine();
                        //se vuole continuare a giocare, creo una nuova connessione
                        if(siono.equals("no")|| siono.equals("No")) {
                            continuaagiocare=false;
                            //il server chiude già da sé la connessione appena vede che uno dei due ha vinto
                        }
                        //se sì, continuaagiocare rimane true e quindi 
                        //viene creata una nuova connessione
                        break;
                    }


                    //ora gestisco il caso in cui la partita continua perché nessuno ha perso
                    //non fai niente pk hai già visualizzato i dati
                    else if(partitacontinua==1 && vuoleuscire==0) {
                        ;
                    }

                    //questo caso non dovrebbe verificarsi mai
                    else if(partitacontinua==1 && vuoleuscire==1) {
                        throw new CommunicationException("Errore di comunicazione");
                    }

                    numeroround++;
                    if(!continuaagiocare) break;
                }

            socket.close();
            aggiornamento.close();
            comandodaspedire.close();
            Thread.sleep(1500);
            }
            catch(Throwable e) {
                e.printStackTrace();
            }
        }
        //ho finito definitivamente
        comandolatoutente.close();
        System.out.println("-----------------FINE DEI GIOCHI-----------------");
        
        
    }

}