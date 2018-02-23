package server;

import java.net.*;
import java.io.*;
import java.util.*;

public class ServerChat extends Thread{
	boolean isAlive = false;
    BufferedReader inputConsole;
    PrintStream outputConsole;

	public static void main( String[] args ) {
		int port = 2222;
		ServerSocket receiver;
		Socket socket;
		int i = 0;

		try {
            if(args.length > 0)
                port = Integer.parseInt(args[0]);
			ServerChat server = new ServerChat();
			receiver = new ServerSocket( port );
			System.out.println( "Server uses port "+port );
			while( server.isAlive ) {
				socket = receiver.accept();
				new ServiceChat( socket );
			}
		} catch( IOException e ) {
			System.out.println( "connexion problem" );
		} catch( IllegalArgumentException e) {
			System.out.println( "Port should be an integer smaller than 65535" );
        }
	}
	
    public ServerChat() {
        //Start listenConsole thread
		initInputOutput();
        start();
    }
	
    public void run(){
        listenConsole();
    }
	
	private void initInputOutput(){
        inputConsole = new BufferedReader( new InputStreamReader( System.in ) );
        outputConsole = new PrintStream( System.out );
        isAlive = true;
    }
	
	private void listenConsole(){
        String line;

        try{
            while(isAlive)
            {
                line = inputConsole.readLine();
				StringTokenizer st = new StringTokenizer(line);
				if(st.hasMoreTokens()) {
					String text = st.nextToken();

					switch(text)
					{
						case "/list":
							ServiceChat.showClientsList();
							break;
						case "/kill":
							if(st.hasMoreTokens()) {
								String name = st.nextToken();
								ServiceChat.killUser(name);
							}
							break;
						case "/broadcast":
							if(line.length() > 11)
								ServiceChat.broadcastMessage("[SERVER]", line.substring(11,line.length()));
							break;
						case "/shutdown":
							if(st.hasMoreTokens()) {
								String time = st.nextToken();
								shutdownServer(Integer.parseInt(time));
								ServiceChat.broadcastMessage("[SERVER]", "Shutdown in "+time+" minute(s).");
							}
							break;
						default:
							outputConsole.println("Unknown command");
							break;
					}
				}
			}
		} catch( IOException e ) {
			System.out.println( "I/O problem" );
        } catch( IllegalArgumentException e) {
			System.out.println( "Please specify a valid time value in minutes for shutdown" );
        }
	}
	
	private void shutdownServer(int duration){
		Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                doShutdown();
            }
        }, duration*1000*60);
	}
	
	private void doShutdown(){
		ServiceChat.shutdownServer();
		isAlive = false;
	}
}

