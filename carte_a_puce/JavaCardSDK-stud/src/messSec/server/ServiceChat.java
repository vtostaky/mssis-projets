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
    static HashMap<String, String> credentialsMap = new HashMap<>();
    boolean isAuthenticated;
    boolean nameProvided;
    boolean pendingTransfer = false;

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

    private void broadcastMessage(String name, String texte){
        int i;
        for(i = 0; i < getNbClients(); i++)
        {
            sendMessage(i, name + " " + texte);
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
    
    private void unicastFileTransfer(String userName, String texte){
        int destIndex = findMatchingName(userName);
        if(destIndex < getNbClients())
        {
            if(!pendingTransfer)
                sendMessage(destIndex, "FILE " + clientName + " " + texte );
            else
                sendMessage(destIndex, "BUFFER " + clientName + " " + texte );
            pendingTransfer = true;
        }   
        else
        {
            destIndex = getClientID();
            sendMessage(destIndex, "Sorry, " + userName + " is not connected to the chat");
        }
    }

    private synchronized void processMessage(String texte){
        
        if(!isAuthenticated)
        {
            if(texte.equals(""))
                if(nameProvided)
                    sendMessage(getClientID(), "Please specify a valid password");
                else
                    sendMessage(getClientID(), "Please specify a valid user name");
            else
                authentication(texte);
        }
        else
        {
            try
            {
                if(!texte.substring(0,7).equals("/buffer"))
                    System.out.println(clientName + ": " + texte);
                if(texte.substring(0,1).equals("/"))
                    processCommand(texte.substring(1,texte.length()));
                else if(texte.substring(0,1).equals("@"))
                {
                    String[] textParts = texte.split(" ");
                    unicastMessage(textParts[0].substring(1,textParts[0].length()), texte.substring(textParts[0].length()+1, texte.length()));
                }
                else
                    broadcastMessage(clientName, "> " + texte);
            }
            catch(StringIndexOutOfBoundsException e)
            {
                broadcastMessage(clientName, "> " + texte);
            }
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
        if(isAuthenticated)
        {
            System.out.println(clientName + " connected");
            sendMessage(getClientID(), "[SERVER] Hello "+clientName+"!");
            broadcastMessage("[SERVER]", clientName+" has joined the chat / "+nbClients+" users connected.");
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
            case "list":
                sendMessage(getClientID(), "List of Users :");
                for(i = 0; i < getNbClients(); i++)
                {
                    sendMessage(getClientID(), clientID[i]);
                }
                break;
            case "quit":
                deconnexion();
                break;
            case "nickname":
                try{
                    String pwd = credentialsMap.remove(clientName);
                    clientName = textParts[1];
                    credentialsMap.put(clientName, pwd); 
		        } catch(ArrayIndexOutOfBoundsException e ) {
                    sendMessage(getClientID(), "/nickname <name>    : change nickname");
                }
                break;
            case "file":
                try{
                    unicastFileTransfer(textParts[1], texte.substring(textParts[0].length()+textParts[1].length()+2, texte.length()));
		        } catch(ArrayIndexOutOfBoundsException e ) {
                    sendMessage(getClientID(), "/file <name> <file> : transmit file");
                }
                break;
            case "buffer":
                if(!pendingTransfer)
                {
                    sendMessage(getClientID(), "Command "+ texte + " unknown");
                    sendMessage(getClientID(), "/quit                   : quit the chat");
                    sendMessage(getClientID(), "/nusers                 : get number of connected users");
                    sendMessage(getClientID(), "/list                   : get list of connected users");
                    sendMessage(getClientID(), "/nickname <name>        : change nickname");
                    sendMessage(getClientID(), "/file <name> <file>     : transmit file");
                    sendMessage(getClientID(), "/msg <username> <text>  : send private message to given user");
                    sendMessage(getClientID(), "@<username> <text>      : send private message to given user");
                }

                try{
                    unicastFileTransfer(textParts[1], texte.substring(textParts[0].length()+textParts[1].length()+2, texte.length()));
		        } catch(ArrayIndexOutOfBoundsException e ) {
                    sendMessage(getClientID(), "Buffer transmission issue");
                }
                break;
            case "endoftransfer":
                try{
                    pendingTransfer = false;
                    int ID = findMatchingName(textParts[1]);
                    sendMessage(ID, "ENDOFFILE " + clientName +" "+ textParts[2]);
		        } catch(ArrayIndexOutOfBoundsException e ) {
                    sendMessage(getClientID(), "Buffer end of file issue");
                }
                break;

            case "msg":
                try{
                    unicastMessage(textParts[1], texte.substring(textParts[0].length()+textParts[1].length()+2, texte.length()));
		        } catch(ArrayIndexOutOfBoundsException e ) {
                    sendMessage(getClientID(), "/msg <username> <text>  : send private message to given user");
                }
                break;

            default:
                if(!textParts[0].equals("help"))
                    sendMessage(getClientID(), "Command "+ texte + " unknown");
                sendMessage(getClientID(), "/quit                   : quit the chat");
                sendMessage(getClientID(), "/nusers                 : get number of connected users");
                sendMessage(getClientID(), "/list                   : get list of connected users");
                sendMessage(getClientID(), "/nickname <name>        : change nickname");
                sendMessage(getClientID(), "/file <name> <file>     : transmit file");
                sendMessage(getClientID(), "/msg <username> <text>  : send private message to given user");
                sendMessage(getClientID(), "@<username> <text>      : send private message to given user");
                break;
        }
    }

    private synchronized void deconnexion(){
        int i;
        try{
            int currentClientIndex = getClientID();

            isAuthenticated = false;
            nameProvided = false;
            sendMessage(getClientID(), "[SERVER] Bye "+clientName+"!");

            for(i = currentClientIndex; i < nbClients-1; i++)
            {
                outputs[i] = outputs[i+1];
                clientID[i] = clientID[i+1];
            }

            socket.close();
            nbClients--;

            broadcastMessage("[SERVER]", clientName + " has left the chat / "+nbClients+" users connected.");
            System.out.println(clientName + " disconnected");
        } catch( IOException e ) {
			System.out.println( "problem during disconnection" );
		}
    }

    private synchronized void connexion(){
        try{
            isAuthenticated = false;
            nameProvided = false;
            outputs[nbClients] = new PrintStream( socket.getOutputStream() );
            sendMessage(nbClients, "Please enter your name");
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
                processMessage(texte);
            }
		} catch( IOException e ) {
			System.out.println( "problem during run" );
		}
        catch(NullPointerException e){}
        
        deconnexion();
	}
}
