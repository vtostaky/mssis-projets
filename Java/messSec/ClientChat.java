import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Base64;

public class ClientChat extends Thread {

    BufferedReader inputConsole, inputNetwork;
    PrintStream outputConsole, outputNetwork;
    boolean isAlive = false;
    HashMap<String, OutputStream> transferMap;

    public ClientChat( String[] args ) {
		try {
            int port = 2222;
            String address = "localhost";
            if(args.length > 0)
                address = args[0];
            if(args.length > 1)
                port = Integer.parseInt(args[1]);
            Socket s = new Socket(address, port);
            transferMap = new HashMap<>();
            initInputOutput(s);
            start();
            listenConsole();
            s.close();
		} catch( IOException e ) {
			System.out.println( "connexion problem" );
		} catch( IllegalArgumentException e) {
			System.out.println( "Port should be an integer smaller than 65535" );
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
                if(line.equals("/quit"))
                    isAlive = false;
                else if(line.length() > 5 && line.substring(0,5).equals("/file"))
                {
                    try{
                        String[] textParts = line.split(" ");
                        File file = new File(textParts[2]);
                        // Get the size of the file
                        long length = file.length();
                        byte[] bytes = new byte[16 * 1024];
                        InputStream in = new FileInputStream(file);

                        int count;
                        while ((count = in.read(bytes)) > 0) {
                            byte[] bytes2 = new byte[count];
                            System.arraycopy(bytes, 0, bytes2, 0, count);
                            String encodedString = Base64.getEncoder().withoutPadding().encodeToString(bytes2);
                            outputNetwork.println("/buffer " + textParts[1] + " "+ encodedString);
                            }
                        outputNetwork.println("/endoftransfer " + textParts[1]);
                    } catch(ArrayIndexOutOfBoundsException e ) {
                        System.out.println("/file should be used with user name and file name");
                    }
                }
            }
        } catch( IOException e ) {
            System.out.println( "I/O problem" );
        }
    }

    private void listenNetwork(){
        String line;
        try{
            OutputStream out = null;
            while(isAlive)
            {
                line = inputNetwork.readLine();
                if(line.length() > 4 && line.substring(0,4).equals("FILE"))
                {
                    String[] textParts = line.split(" ");
                    
                    try{
                        File file = new File(textParts[2]);
                        out = new FileOutputStream(file);
                        transferMap.put(textParts[1], out);
                    } catch(ArrayIndexOutOfBoundsException e ) {
                        System.out.println("FILE transfer initiated without file name");
                    }
                }
                else if(line.length() > 6 && line.substring(0,6).equals("BUFFER"))
                {
                    String[] textParts = line.split(" ");
                    
                    try{
                        out = transferMap.get(textParts[1]);
                        if(out == null)
                            System.out.println("BUFFER received outside file transfer context");
                        else
                        {
                            byte[] decodedBytes = Base64.getDecoder().decode(textParts[2]);
                            out.write(decodedBytes);
                            out.flush();
                        }
                    } catch(ArrayIndexOutOfBoundsException e ) {
                        System.out.println("FILE transfer initiated without file name");
                    }
                }
                else if(line.length() > 9 && line.substring(0,9).equals("ENDOFFILE"))
                {
                    String[] textParts = line.split(" ");
                    
                    out = transferMap.get(textParts[1]);
                    if(out != null)
                    {
                        out.close();
                        transferMap.remove(textParts[1]);
                    }
                }
                else
                    outputConsole.println( line );
            }
        } catch( IOException e ) {
            System.out.println( "I/O problem" );
        } catch( NullPointerException e ) {
            System.out.println( "Null pointer encountered" );
        }
    }

	public static void main( String[] args ) {
        new ClientChat(args);
    }
}

