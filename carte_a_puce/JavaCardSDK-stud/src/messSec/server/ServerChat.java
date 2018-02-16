import java.net.*;
import java.io.*;
import java.util.*;

public class ServerChat {

	public static void main( String[] args ) {
		int port = 2222;
		ServerSocket receiver;
		Socket socket;

		try {
            if(args.length > 0)
                port = Integer.parseInt(args[0]);
			receiver = new ServerSocket( port );
			System.out.println( "Server uses port "+port );
			while( true ) {
				socket = receiver.accept();
				new ServiceChat( socket );
			}
		} catch( IOException e ) {
			System.out.println( "connexion problem" );
		} catch( IllegalArgumentException e) {
			System.out.println( "Port should be an integer smaller than 65535" );
        }
	}
}

