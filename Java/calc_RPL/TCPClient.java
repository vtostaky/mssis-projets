import java.net.*;
import java.io.*;




class TCPClient {


	public static void main( String[] args ) {
		int socketPort = 12345;
		BufferedReader feedInput;
		BufferedReader socketInput;
		PrintStream socketOutput;
		String feedLine;
        String socketLine;
		Socket socket;
        int i;
        String serverAddress = "127.0.0.1";
        InputStream inStream = System.in;

        if(args.length%2 != 0)
        {
            System.out.println("Args error, please use the exe as follows:\n"+
                                "-s <server_address>: server address, default is 127.0.0.1\n"+
                                "-i <input_file>    : input file, default is stdin\n"+
                                "-p <port_number>   : server port number, default is 12345");
            return;
        }

        for(i = 0; i < args.length/2; i++)
        {
            // Check if an input file is given.
            if(args[2*i].equals("-i"))
            {
                try{
                    inStream = new FileInputStream(args[2*i+1]);
                }
                catch(FileNotFoundException notFoundE){
                    System.out.println("Please enter a valid input file name after -i");
                    return;
                }
            }
            // Check if a port number is given
            else if(args[2*i].equals("-p"))
            {
                try
                {
                    socketPort = Integer.parseInt(args[2*i+1]);
                }
                catch(NumberFormatException numE){
                    System.out.println("Please enter an integer for port number when using -p");
                    return;
                }
            }
            // Check if a server address is given
            else if(args[2*i].equals("-s"))
            {
                serverAddress = args[2*i+1];
            }
            else
            {
                System.out.println("Args error, please use the exe as follows:\n"+
                                    "-s <server_address>: server address, default is 127.0.0.1\n"+
                                    "-i <input_file>    : input file, default is stdin\n"+
                                    "-p <port_number>   : server port, default is 12345");
                return;
            }
        }

		try {
			socket = new Socket( serverAddress, socketPort );
			feedInput = new BufferedReader( new InputStreamReader(inStream) );
			socketInput = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
			socketOutput = new PrintStream( socket.getOutputStream() );

            if((socketLine = socketInput.readLine()) != null)
                System.out.println( socketLine );

            // while there is something to read in the input, read it 
			while( (feedLine = feedInput.readLine()) != null )
            {
                // send it into the socket
				socketOutput.println( feedLine );
                // while there is something coming from the socket, read it
                while( (socketLine = socketInput.readLine()) != null)
                {
                    // Print it
                    System.out.println( socketLine );
                    try{
                        /*
                         * If we reach "end of stack" or "please enter an integer...",
                         * Stop waiting from new lines because the calculator is waiting for inputs,
                         * and won't send any further information to display until it gets inputs.
                         */
                        if(socketLine.equals("End of Stack") || socketLine.substring(0,6).equals("Please"))
                            break;
                    }
                    catch(StringIndexOutOfBoundsException e){}
                }
                if(feedLine.equals("exit"))
                    break;
            }
			socket.close();
		} catch( FileNotFoundException e ) {
			System.out.println( "Unknown file" );
		} catch( UnknownHostException e ) {
			System.out.println( "Unknown host" );
		} catch( IOException e ) {
			System.out.println( "I/O exception!!" );
		}
	}


}
