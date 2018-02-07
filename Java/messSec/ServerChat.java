import java.net.*;
import java.io.*;
import java.util.*;

public class ServerChat {

	public static void main( String[] args ) {
		int port = 1234;
		ServerSocket receiver;
		Socket socket;

		try {
			receiver = new ServerSocket( port );
			while( true ) {
				socket = receiver.accept();
				new ServiceChat( socket );
			}
		} catch( IOException e ) {
			System.out.println( "connexion problem" );
		}
	}
}

