import java.net.*;
import java.io.*;
import java.util.*;


class ServiceChat extends Thread {

    final static int NBCLIENTSMAX = 3;
	Socket socket;
	BufferedReader input;
	static PrintStream[] outputs = new PrintStream[NBCLIENTSMAX];
    static int nbClients = 0;
    static String clientID[] = new String[NBCLIENTSMAX];
    String clientName;
    static HashMap credentialsMap = new HashMap();
    boolean isAuthenticated;
    boolean nameProvided;

	public ServiceChat( Socket socket ) {
		this.socket = socket;
		this.start();
	}

    private synchronized int getNbClients(){
        return nbClients;
    }

    private synchronized int findMatchingName(String texte){
        int i;
        for(i = 0; i < getNbClients(); i++)
        {
            if(clientID[i].equals(texte))
                break;
        }
        return i;
    }

    private int getClientID(){
        return findMatchingName(clientName);
    }

    private synchronized void sendMessage(int id, String texte){
        outputs[id].println(texte);
    }

    private void broadcastMessage(String texte){
        int i;
        for(i = 0; i < getNbClients(); i++)
        {
            sendMessage(i, clientName + " " + texte);
        }
    }
    
    private void unicastMessage(String userName, String texte){
        int destIndex = findMatchingName(userName);
        if(destIndex < getNbClients())
            sendMessage(destIndex, "FROM " + clientName + "> " + texte );
        else
        {
            destIndex = getClientID();
            sendMessage(destIndex, "Sorry, " + userName + " is not connected to the chat");
        }
    }

    private synchronized void processMessage(String texte){
        
        if(!isAuthenticated)
        {
            authentication(texte);
        }
        else
        {
            if(texte.substring(0,1).equals("/"))
                processCommand(texte.substring(1,texte.length()));
            else if(texte.substring(0,1).equals("@"))
            {
                String[] textParts = texte.split(" ");
                unicastMessage(textParts[0].substring(1,textParts[0].length()), texte.substring(textParts[0].length()+1, texte.length()));
            }
            else
                broadcastMessage("> " + texte);
        }
    }

    private synchronized void authentication(String texte){

        int currentClientIndex = getClientID();
       
        if(!nameProvided)
        {
            if(findMatchingName(texte) < getNbClients())
            {
                sendMessage(currentClientIndex, "Pseudo " + texte + " is already connected\nPlease enter your name" );
            }
            else
            {
                if(credentialsMap.get(texte) != null)
                    sendMessage(currentClientIndex, "Please authenticate with your password" );
                else
                    sendMessage(currentClientIndex, "Please provide a password for further authentication" );
                nameProvided = true;
                clientName = ""+ texte;
                clientID[currentClientIndex] = clientName;
            }
        }
        else
        {
            if(credentialsMap.get(clientName) != null)
            {
                if(credentialsMap.get(clientName).equals(texte))
                    isAuthenticated = true;
                else
                {
                    sendMessage(currentClientIndex, "Authentication failure\nPlease enter your name" );
                    isAuthenticated = false;
                    nameProvided = false;
                    clientName = "";
                    clientID[currentClientIndex] = clientName;
                }
            }
            else
            {
                credentialsMap.put(clientName, texte); 
                isAuthenticated = true;
            }
        }
    }

    private synchronized void processCommand(String texte){

        int i;
        String[] textParts = texte.split(" ");

        switch(textParts[0])
        {
            case "nusers":
                sendMessage(getClientID(), ""+ getNbClients());
                break;
            case "users":
                sendMessage(getClientID(), "List of Users :");
                for(i = 0; i < getNbClients(); i++)
                {
                    sendMessage(getClientID(), clientID[i]);
                }
                break;
            case "exit":
                deconnexion();
                break;
            default:
                if(!textParts[0].equals("help"))
                    sendMessage(getClientID(), "Command "+ texte + " unknown");
                sendMessage(getClientID(), "/exit               : quit the chat");
                sendMessage(getClientID(), "/nusers             : get number of connected users");
                sendMessage(getClientID(), "/users              : get list of connected users");
                sendMessage(getClientID(), "@<username> <text>  : send private message to given user");
                break;
        }
    }

    private synchronized void deconnexion(){
        int i;
        try{
            int currentClientIndex = getClientID();

            isAuthenticated = false;
            nameProvided = false;

            for(i = currentClientIndex; i < nbClients-1; i++)
            {
                outputs[i] = outputs[i+1];
                clientID[i] = clientID[i+1];
            }

            socket.close();
            nbClients--;

            broadcastMessage("has left the chat");
        } catch( IOException e ) {
			System.out.println( "problem during deconnexion" );
		}
    }

    private synchronized void connexion(){
        try{
            isAuthenticated = false;
            nameProvided = false;
            outputs[nbClients] = new PrintStream( socket.getOutputStream() );
            outputs[nbClients].println("Please enter your name" );
            clientName = ""+ socket.getPort();
            clientID[nbClients] = clientName;
            nbClients++;
		} catch( IOException | ArrayIndexOutOfBoundsException e ) {
			try {
				socket.close();
			} catch( IOException e2 ) {
				System.out.println( "problem while closing socket" );
			}
		}
    }

	public void run() {
		try {
			input = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            connexion();
		} catch( IOException e ) {
			try {
				socket.close();
			} catch( IOException e2 ) {
				System.out.println( "problem while closing socket" );
			}
		}

		String texte;

		try {
			while(true)
            {
                texte = input.readLine();
                try{
                    processMessage(texte);
                }
                catch(NullPointerException e)
                {
			        System.out.println( "NULL exception dans la socket" );
                    break;
                }
            }

            deconnexion();

		} catch( IOException e ) {
			System.out.println( "problem during run" );
		}
	}
}
