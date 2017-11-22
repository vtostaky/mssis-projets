import java.io.*;
import java.net.*;
import java.util.*;

public class CalcRPL extends Thread {

	Socket socket;
	BufferedReader inBuffer;
	PrintStream printStream;
    BufferedWriter logBuffer;
    BufferedWriter histoBuffer;
    int stackSize;

    // Constructor for CalcRPL in socket mode
	public CalcRPL( Socket socket, BufferedWriter logBuffer, BufferedWriter histoBuffer, int stackSize ) {
		this.socket = socket;
        this.logBuffer = logBuffer;
        this.histoBuffer = histoBuffer;
        this.stackSize = stackSize;
		this.start();
	}

    // Constructor for CalcRPL in local mode
	public CalcRPL(BufferedWriter logBuffer, BufferedWriter histoBuffer, int stackSize, BufferedReader inBuffer, PrintStream printStream ) {
        this.logBuffer = logBuffer;
        this.histoBuffer = histoBuffer;
        this.stackSize = stackSize;
        this.printStream = printStream;
        this.inBuffer = inBuffer;
	}

    // Thread for socket mode
	public void run() {
		try {
			this.inBuffer = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
			this.printStream = new PrintStream( socket.getOutputStream() );
		} catch( IOException e ) {
			try {
				socket.close();
			} catch( IOException e2 ) {
				System.out.println( "problem while closing socket" );
			}
		}

		try {
            this.readAndCalculate();
			socket.close();
		} catch( IOException e ) {
			System.out.println( "IO exception during run" );
		}
	}

    public static void main(String[] args){
        InputStream inStream = System.in;
        OutputStream logStream = System.out;
        OutputStream histoStream = null;
        PrintStream printStream = new PrintStream(System.out);
        int stackSize = 3;
        int i;
        BufferedWriter histoBuffer = null;
		int serverPort = 12345;
		ServerSocket receiver;
		Socket socket;
        boolean useSocket = false;

        
        if(args.length%2 != 0)
        {
            System.out.println("Args error, please use the exe as follows:\n"+
                                "-n <number>        : size of the calculator stack, default is 3\n"+
                                "-i <input_file>    : input file, default is stdin\n"+
                                "-l <debug_log_file>: debug log file, default is stdout\n"+
                                "-h <history_file>  : file to save history, default is none\n"+
                                "-p <port_number>   : when using socket, use -p to specify port");
            return;
        }

        for(i = 0; i < args.length/2; i++)
        {
            // Check if the size of the stack has been specified.
            if(args[2*i].equals("-n"))
            {
                try
                {
                    stackSize = Integer.parseInt(args[2*i+1]);
                }
                catch(NumberFormatException numE){
                    System.out.println("Please enter an integer size for the stack when using -n");
                    return;
                }
            }
            // Check if a file has been specified to save session history.
            else if(args[2*i].equals("-h"))
            {
                try{
                    histoStream = new FileOutputStream(args[2*i+1]);
                }
                catch(FileNotFoundException notFoundE){
                    System.out.println("Please enter a valid input file name after -h");
                    return;
                }
            }
            // Check if an input file has been specified, to replay a sequence.
            else if(args[2*i].equals("-i"))
            {
                try{
                    inStream = new FileInputStream(args[2*i+1]);
                }
                catch(FileNotFoundException notFoundE){
                    System.out.println("Please enter a valid input file name after -i");
                    return;
                }
            }
            // Check if a log file has been specified.
            else if(args[2*i].equals("-l"))
            {
                try{
                    logStream = new FileOutputStream(args[2*i+1]);
                }
                catch(FileNotFoundException notFoundE){
                    System.out.println("Please enter a valid log file name after -l");
                    return;
                }
            }
            // Check if a port has been specified to use socket mode.
            else if(inStream == System.in && args[2*i].equals("-p"))
            {
                try
                {
                    serverPort = Integer.parseInt(args[2*i+1]);
                    useSocket = true;
                }
                catch(NumberFormatException numE){
                    System.out.println("Please enter an integer for the port when using -p");
                    return;
                }
            }
            else
            {
                System.out.println("Args error, please use the exe as follows:\n"+
                                    "-n <number>        : size of the calculator stack, default is 3\n"+
                                    "-i <input_file>    : input file, default is stdin\n"+
                                    "-l <debug_log_file>: debug log file, default is stdout\n"+
                                    "-h <history_file>  : file to store the history, default is none\n"+
                                    "-p <port_number>   : when using socket, use -p to specify port");
                return;
            }
        }

        // Initialize buffers used for input, logs, and history.
        BufferedReader inBuffer
            = new BufferedReader(new InputStreamReader(inStream));
        BufferedWriter logBuffer
            = new BufferedWriter(new OutputStreamWriter(logStream));
        if(histoStream != null)
        {
            histoBuffer
                = new BufferedWriter(new OutputStreamWriter(histoStream));
        }
        
        if(useSocket)
        {
            /* remote calculator */
            try {
                receiver = new ServerSocket( serverPort );
                while( true ) {
                    socket = receiver.accept();
                    new CalcRPL( socket, logBuffer, histoBuffer, stackSize );
                }
            } catch( IOException e ) {
                System.out.println( "Connection issue" );
            }
        }
        else
        {
            /* local calculator */
            CalcRPL my_calc = new CalcRPL( logBuffer, histoBuffer, stackSize, inBuffer, printStream );
            my_calc.readAndCalculate();
        }
    }

    /*
     * Reads operands and operators from input buffer (std input, local file or socket)
     * and perform the appropriate operations.
     * Also enables to save history, display the stack content to the output buffer, and redirect logs to specified log buffer.
     */
    public void readAndCalculate()
    {
        String line, token,
               delimiter = " ,;:_|";
        StringTokenizer tokenizer;
        ObjEmp obj;

        // Create the stack that will be used for RPL calculator
        PileRPL myStack = new PileRPL(this.stackSize,this.logBuffer,this.printStream);
        try
        {
            printStream.println("Please enter an integer for the calculator, save to keep history or exit to leave.");
            /* For each line in the input stream */
            while((line = this.inBuffer.readLine()) != null)
            {
                /* If exit is matched, then the program exits */
                if(line.equals("exit"))
                    break;
                /* If save is matched, then try & save the history into given file */
                if(line.equals("save"))
                {
                    try{
                        if(histoBuffer != null)
                        {
                            // Actually save to history file, when "save" is entered.
                            histoBuffer.flush();
                            printStream.println("History saved!");
                        }
                    }
                    catch(IOException e){
                        try{
                            logBuffer.write("Problem while writting to history file : did you specify one?\n");
                            logBuffer.flush();
                        }
                        catch(IOException e2){
                            printStream.println("I/O Exceptions while writting to output & log files");
                        }
                    }
                    continue;
                }
                tokenizer = new StringTokenizer(line, delimiter);

                while(tokenizer.hasMoreTokens()) {
                    token = tokenizer.nextToken();
                    // Process each operator and call corresponding PileRPL method.
                    if(token.equals("/"))
                    {
                        myStack.division();
                    }else if(token.equals("*"))
                    {
                        myStack.multiplication();
                    }else if(token.equals("-"))
                    {
                        myStack.soustraction();
                    }else if(token.equals("+"))
                    {
                        myStack.addition();
                    }else
                    {
                        // For each entered integer, try and add it to PileRPL.
                        try
                        {
                            obj = new ObjEmp(Integer.parseInt(token));
                            myStack.empile(obj);
                        }
                        catch(NumberFormatException nume){
                            printStream.println("Please enter an integer for the calculator, save to keep history or exit to leave.");
                            continue;
                        }
                    }
                    // Print the updated stack at each change.
                    printStream.println(myStack.toString());
                    try{
                        // Add any new entry to the history file, if specified, to be able to replay sequence later.
                        if(histoBuffer != null)
                            histoBuffer.write(token+",");
                    }
                    catch(IOException|NullPointerException e){
                        try{
                            logBuffer.write("Problem while writting to history file : did you specify one?\n");
                            logBuffer.flush();
                        }
                        catch(IOException e2){
                            printStream.println("I/O Exceptions while writting to output & log files");
                        }
                    }
                }
            }
        }
        catch(IOException e){
            try{
                logBuffer.write("I/O exception from input stream!!!\n");
            }
            catch(IOException e2){
                printStream.println("I/O exception in output stream!!!");
            }
        }
    }
}
