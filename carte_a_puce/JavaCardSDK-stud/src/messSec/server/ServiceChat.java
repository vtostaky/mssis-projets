import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Security;

class ServiceChat extends Thread {

    final static int NBCLIENTSMAX = 3;
	final static int DATASIZE = 128;				//128 to use with RSA1024_NO_PAD
    
	Socket socket;
	BufferedReader input;
	static PrintStream[] outputs = new PrintStream[NBCLIENTSMAX];
    static int nbClients = 0;
    static String clientID[] = new String[NBCLIENTSMAX];
    String clientName;
    static HashMap<String, String> credentialsMap = new HashMap<String, String>();
    boolean isAuthenticated;
    boolean nameProvided;
    boolean pendingTransfer = false;
    
    static private PrivateKey priv;
	static private byte[] modulus_b = new byte[DATASIZE];
	static private byte[] public_exponent_b = new byte[3];
	
	byte[] challengeBytes = new byte[DATASIZE];

	public ServiceChat( Socket socket, PrivateKey priv, byte[] mod_b, byte[] exp_b ) {
		this.socket = socket;
		this.priv = priv;
		System.arraycopy(mod_b, 0, modulus_b, 0, 128);
		System.arraycopy(exp_b, 0, public_exponent_b, 0, 3);
		
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
    
    private synchronized void generateAndSendChallenge(String clientPubKey){
    	String[] textParts = clientPubKey.split(" ");
    	byte[] modClient = Base64.getDecoder().decode(textParts[0]);
    	byte[] expClient = Base64.getDecoder().decode(textParts[1]);
    	
    	String mod_s =  HexString.hexify( modClient );
		mod_s = mod_s.replaceAll( " ", "" );
		mod_s = mod_s.replaceAll( "\n", "" );

		String pub_s =  HexString.hexify( expClient );
		pub_s = pub_s.replaceAll( " ", "" );
		pub_s = pub_s.replaceAll( "\n", "" );

		// Load the keys from String into BigIntegers
		BigInteger modulus = new BigInteger(mod_s, 16);
		BigInteger pubExponent = new BigInteger(pub_s, 16);

		// Create private and public key specs from BinIntegers
		RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(modulus, pubExponent);

		// Create the RSA private and public keys
		KeyFactory factory = KeyFactory.getInstance( "RSA" );
		PublicKey pubClient = factory.generatePublic(publicSpec);
    	// Get Cipher able to apply RSA_NOPAD
		// (must use "Bouncy Castle" crypto provider)
		Security.addProvider(new BouncyCastleProvider());
		Cipher cRSA_NO_PAD = Cipher.getInstance( "RSA/NONE/NoPadding", "BC" );

		// Get challenge data
		Random r = new Random( (new Date()).getTime() );
		r.nextBytes( challengeBytes );
		System.out.println("challenge:\n" + Base64.getEncoder().withoutPadding().encodeToString( challengeBytes ) + "\n" );
		
		// Crypt with public key
		cRSA_NO_PAD.init( Cipher.ENCRYPT_MODE, pubClient );
		byte[] ciphered = new byte[DATASIZE];
		System.out.println( "*" );
		cRSA_NO_PAD.doFinal(challengeBytes, 0, DATASIZE, ciphered, 0);
		//ciphered = cRSA_NO_PAD.doFinal( challengeBytes );
		System.out.println( "*" );
		System.out.println("ciphered by server is:\n" + Base64.getEncoder().withoutPadding().encodeToString(ciphered) + "\n" );
    	
    	sendMessage(getClientID(), "[SERVER] AUTH_CHALL "+ Base64.getEncoder().withoutPadding().encodeToString(ciphered));
    }

	private synchronized void initiatePublicKeyExchange() {
		sendMessage(getClientID(), "[SERVER] AUTH_PUBKEY "+ Base64.getEncoder().withoutPadding().encodeToString(modulus_b)+
			" "+Base64.getEncoder().withoutPadding().encodeToString(public_exponent_b));
	}
	
	private boolean compareResponseToSentChallenge(String response) {
		byte[] decodedBytes = Base64.getDecoder().decode(response);
		// Decrypt with private key
		cRSA_NO_PAD.init( Cipher.DECRYPT_MODE, priv );
		byte[] unciphered = new byte[DATASIZE];
		cRSA_NO_PAD.doFinal( decodedBytes, 0, DATASIZE, unciphered, 0);
		System.out.println("unciphered by server is:\n" + Base64.getEncoder().withoutPadding().encodeToString(unciphered) + "\n" );
		
		return Arrays.equals(challengeBytes, unciphered);
	}

    private synchronized void authentication(String texte){

        int currentClientIndex = getClientID();
       
        if(!nameProvided)
        {
            if(findMatchingName(texte) < getNbClients())
            {
                sendMessage(currentClientIndex, "[SERVER] Pseudo " + texte + " is already connected\nPlease enter your name" );
            }
            else
            {
                if(credentialsMap.get(texte) != null)
                {
                    sendMessage(currentClientIndex, "[SERVER] Please authenticate" );
                    generateAndSendChallenge(credentialsMap.get(texte));
                }
                else
                {
                    sendMessage(currentClientIndex, "[SERVER] has sent public key" );
                    initiatePublicKeyExchange();
                }
                nameProvided = true;
                clientName = ""+ texte;
                clientID[currentClientIndex] = clientName;
            }
        }
        else if(credentialsMap.get(clientName) == null)
        {
            //Put Base64 public key received from client into Hash map
            credentialsMap.put(clientName, texte); 
            //Send challenge for authentication
            sendMessage(currentClientIndex, "[SERVER] Thanks for public key, please authenticate" );
            generateAndSendChallenge(texte);
        }
        else
        {
        	//Received String for user with an entry in DB : expecting answer to challenge
            if(compareResponseToSentChallenge(texte))
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
