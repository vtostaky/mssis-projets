import java.net.*;
import java.io.*;
import java.util.*;

public class Telnet extends Thread {

    BufferedReader inputConsole, inputNetwork;
    PrintStream outputConsole, outputNetwork;
    boolean isAlive = false;

    public Telnet( String[] args ) {
		try {
            Socket s = new Socket(args[0], Integer.parseInt(args[1]));
            initInputOutput(s);
            start();
            listenConsole();
            s.close();
		} catch( IOException e ) {
			System.out.println( "connexion problem" );
		} catch (ArrayIndexOutOfBoundsException OOBe){
			System.out.println( "Usage : java Telnet <host> <port>" );
        }
	}
    
    public void run(){
        listenNetwork();
    }

    private void initInputOutput(Socket s){
        try{
            inputConsole = new BufferedReader( new InputStreamReader( System.in ) );
            inputNetwork = new BufferedReader( new InputStreamReader( s.getInputStream() ) );
            outputConsole = new PrintStream( System.out );
            outputNetwork = new PrintStream( s.getOutputStream() );
            isAlive = true;
        } catch( IOException e ) {
            System.out.println( "I/O problem" );
        }
    }

    private void listenConsole(){
        String line;
        try{
            while(isAlive)
            {
                line = inputConsole.readLine();
                outputNetwork.println( line );
                if(line.equals("/exit"))
                    isAlive = false;
            }
        } catch( IOException e ) {
            System.out.println( "I/O problem" );
        }
    }

    private void listenNetwork(){
        String line;
        try{
            while(isAlive)
            {
                line = inputNetwork.readLine();
                outputConsole.println( line );
            }
        } catch( IOException e ) {
            System.out.println( "I/O problem" );
        }
    }

	public static void main( String[] args ) {
        new Telnet(args);
    }
}

