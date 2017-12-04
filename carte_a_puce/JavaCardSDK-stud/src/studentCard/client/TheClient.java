package client;

import java.io.*;
import opencard.core.service.*;
import opencard.core.terminal.*;
import opencard.core.util.*;
import opencard.opt.util.*;




public class TheClient {

    private PassThruCardService servClient = null;
    boolean DISPLAY = true;
    boolean loop = true;

    static final byte CLA                   = (byte)0x00;
    static final byte P1                    = (byte)0x00;
    static final byte P2                    = (byte)0x00;
    static final byte UPDATECARDKEY             = (byte)0x14;
    static final byte UNCIPHERFILEBYCARD            = (byte)0x13;
    static final byte CIPHERFILEBYCARD          = (byte)0x12;
    static final byte CIPHERANDUNCIPHERNAMEBYCARD       = (byte)0x11;
    static final byte READFILEFROMCARD          = (byte)0x10;
    static final byte WRITEFILETOCARD           = (byte)0x09;
    static final byte UPDATEWRITEPIN            = (byte)0x08;
    static final byte UPDATEREADPIN             = (byte)0x07;
    static final byte DISPLAYPINSECURITY            = (byte)0x06;
    static final byte DESACTIVATEACTIVATEPINSECURITY    = (byte)0x05;
    static final byte ENTERREADPIN              = (byte)0x04;
    static final byte ENTERWRITEPIN             = (byte)0x03;
    static final byte READNAMEFROMCARD          = (byte)0x02;
    static final byte WRITENAMETOCARD           = (byte)0x01;


    public TheClient() {
        try {
            SmartCard.start();
            System.out.print( "Smartcard inserted?... " ); 

            CardRequest cr = new CardRequest (CardRequest.ANYCARD,null,null); 

            SmartCard sm = SmartCard.waitForCard (cr);

            if (sm != null) {
                System.out.println ("got a SmartCard object!\n");
            } else
                System.out.println( "did not get a SmartCard object!\n" );

            this.initNewCard( sm ); 

            SmartCard.shutdown();

        } catch( Exception e ) {
            System.out.println( "TheClient error: " + e.getMessage() );
        }
        java.lang.System.exit(0) ;
    }

    private ResponseAPDU sendAPDU(CommandAPDU cmd) {
        return sendAPDU(cmd, true);
    }

    private ResponseAPDU sendAPDU( CommandAPDU cmd, boolean display ) {
        ResponseAPDU result = null;
        try {
            result = this.servClient.sendCommandAPDU( cmd );
            if(display)
                displayAPDU(cmd, result);
        } catch( Exception e ) {
            System.out.println( "Exception caught in sendAPDU: " + e.getMessage() );
            java.lang.System.exit( -1 );
        }
        return result;
    }


    /************************************************
     *   * *********** BEGINNING OF TOOLS ***************
     *       * **********************************************/


    private String apdu2string( APDU apdu ) {
        return removeCR( HexString.hexify( apdu.getBytes() ) );
    }


    public void displayAPDU( APDU apdu ) {
        System.out.println( removeCR( HexString.hexify( apdu.getBytes() ) ) + "\n" );
    }


    public void displayAPDU( CommandAPDU termCmd, ResponseAPDU cardResp ) {
        System.out.println( "--> Term: " + removeCR( HexString.hexify( termCmd.getBytes() ) ) );
        System.out.println( "<-- Card: " + removeCR( HexString.hexify( cardResp.getBytes() ) ) );
    }


    private String removeCR( String string ) {
        return string.replace( '\n', ' ' );
    }


    /******************************************
     *   * *********** END OF TOOLS ***************
     *       * ****************************************/


    private boolean selectApplet() {
        boolean cardOk = false;
        try {
            CommandAPDU cmd = new CommandAPDU( new byte[] {
                (byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x0A,
                    (byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x62, 
                    (byte)0x03, (byte)0x01, (byte)0x0C, (byte)0x06, (byte)0x01
            } );
            ResponseAPDU resp = this.sendAPDU( cmd );
            if( this.apdu2string( resp ).equals( "90 00" ) )
                cardOk = true;
        } catch(Exception e) {
            System.out.println( "Exception caught in selectApplet: " + e.getMessage() );
            java.lang.System.exit( -1 );
        }
        return cardOk;
    }


    private void initNewCard( SmartCard card ) {
        if( card != null )
            System.out.println( "Smartcard inserted\n" );
        else {
            System.out.println( "Did not get a smartcard" );
            System.exit( -1 );
        }

        System.out.println( "ATR: " + HexString.hexify( card.getCardID().getATR() ) + "\n");


        try {
            this.servClient = (PassThruCardService)card.getCardService( PassThruCardService.class, true );
        } catch( Exception e ) {
            System.out.println( e.getMessage() );
        }

        System.out.println("Applet selecting...");
        if( !this.selectApplet() ) {
            System.out.println( "Wrong card, no applet to select!\n" );
            System.exit( 1 );
            return;
        } else 
            System.out.println( "Applet selected" );

        mainLoop();
    }


    void updateCardKey() {
    }


    void uncipherFileByCard() {
    }


    void cipherFileByCard() {
    }


    void cipherAndUncipherNameByCard() {
    }


    void readFileFromCard() {
    }


    void writeFileToCard() {    
        CommandAPDU cmdAPDU;
        ResponseAPDU resp;
        byte buffer_offset = 0x00;

        String filename = readKeyboard();
        long filesize = new File(filename).length();

        try {
            byte[] info = filename.getBytes();
            byte[] cmd = { CLA, WRITEFILETOCARD, P1, P2, (byte)(info.length + 3)};
            byte[] command = new byte[5+info.length+3];
            System.out.println(info.length);

            System.arraycopy(cmd,0,command,0,5);
            command[5] = (byte)(info.length);
            System.arraycopy(info,0,command,6,info.length);
            command[6 + info.length] = (byte)(filesize/0x7F);
            command[6 + info.length + 1] = (byte)(filesize - ((byte)(filesize/0x7F))*(byte)0x7F);

            System.out.println("// Write file info into the card");
            cmdAPDU = new CommandAPDU( command );
            displayAPDU(cmdAPDU);
            resp = this.sendAPDU( cmdAPDU, DISPLAY );

        } catch( Exception e ) {}

        try {
            DataInputStream br = new DataInputStream(new FileInputStream(filename));
            byte[] buffer = new byte[0xFF];
            int res = 0;

            do
            {
                try {
                    res = br.read(buffer, 1, 0x7F);
                } catch (Exception e) {
                    System.out.println( "Problem with buffer reader" );
                }
                buffer[0] = buffer_offset;
                byte[] cmd = { CLA, WRITEFILETOCARD, P1, P2, (byte)(res+1)};
                byte[] command = new byte[5+res+1];

                System.arraycopy(cmd,0,command,0,5);
                System.arraycopy(buffer,0,command,5,res+1);
                buffer_offset++;

                System.out.println("// Write file content into the card "+(short)(buffer_offset-1 & (short)0xFF)+"/"+filesize/0x7F+":"+res);
                cmdAPDU = new CommandAPDU( command );
                displayAPDU(cmdAPDU);
                resp = this.sendAPDU( cmdAPDU, DISPLAY );
                if( this.apdu2string( resp ).equals( "6F 00" ) )
                    break;
            }while(res == 0x7F && (short)(buffer_offset & (short) 0xFF) * 0x7F < filesize);
            br.close();
        } catch( Exception e ) {}

        System.out.println( "" );

    }


    void updateWritePIN() {
        CommandAPDU cmdAPDU;
        ResponseAPDU resp;
        try {
            byte[] pin = readKeyboard().getBytes();
            byte[] cmd = { CLA, UPDATEWRITEPIN, P1, P2, (byte)pin.length };
            byte[] command = new byte[5+pin.length];

            System.arraycopy(cmd,0,command,0,5);
            System.arraycopy(pin,0,command,5,pin.length);
            System.out.println("// Update writePIN into the card");
            cmdAPDU = new CommandAPDU( command );
            displayAPDU(cmdAPDU);
            resp = this.sendAPDU( cmdAPDU, DISPLAY );

        } catch( Exception e ) {}

        System.out.println( "" );
    }


    void updateReadPIN() {
        CommandAPDU cmdAPDU;
        ResponseAPDU resp;
        try {
            byte[] pin = readKeyboard().getBytes();
            byte[] cmd = { CLA, UPDATEREADPIN, P1, P2, (byte)pin.length };
            byte[] command = new byte[5+pin.length];

            System.arraycopy(cmd,0,command,0,5);
            System.arraycopy(pin,0,command,5,pin.length);
            System.out.println("// Update readPIN into the card");
            cmdAPDU = new CommandAPDU( command );
            displayAPDU(cmdAPDU);
            resp = this.sendAPDU( cmdAPDU, DISPLAY );

        } catch( Exception e ) {}

        System.out.println( "" );
    }


    void displayPINSecurity() {
        byte[] cmd_ = {CLA,DISPLAYPINSECURITY,P1,P2,(byte)0x01};
        CommandAPDU cmd = new CommandAPDU( cmd_ );
        System.out.println("Sending DISPLAYPINSECURITY command APDU, data expected...");
        ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
        byte[] bytes = resp.getBytes();
        String msg = "PIN security is ";
        if(bytes[0] == (byte)0x01)
            msg += "activated";
        else
            msg += "desactivated";

        System.out.println(msg);
    }


    void desactivateActivatePINSecurity() {
        CommandAPDU cmdAPDU;
        ResponseAPDU resp;
        try {
            byte[] cmd = { CLA, DESACTIVATEACTIVATEPINSECURITY, P1, P2 };

            System.out.println("// Deactivate/activate security into the card");
            cmdAPDU = new CommandAPDU( cmd );
            displayAPDU(cmdAPDU);
            resp = this.sendAPDU( cmdAPDU, DISPLAY );

        } catch( Exception e ) {}

        System.out.println( "" );
    }


    void enterReadPIN() {
        CommandAPDU cmdAPDU;
        ResponseAPDU resp;
        try {
            byte[] pin = readKeyboard().getBytes();
            byte[] cmd = { CLA, ENTERREADPIN, P1, P2, (byte)pin.length };
            byte[] command = new byte[5+pin.length];

            System.arraycopy(cmd,0,command,0,5);
            System.arraycopy(pin,0,command,5,pin.length);
            System.out.println("// Enter readPIN for validation");
            cmdAPDU = new CommandAPDU( command );
            displayAPDU(cmdAPDU);
            resp = this.sendAPDU( cmdAPDU, DISPLAY );

        } catch( Exception e ) {}

        System.out.println( "" );
    }

    void enterWritePIN() {
        CommandAPDU cmdAPDU;
        ResponseAPDU resp;
        try {
            byte[] pin = readKeyboard().getBytes();
            byte[] cmd = { CLA, ENTERWRITEPIN, P1, P2, (byte)pin.length };
            byte[] command = new byte[5+pin.length];

            System.arraycopy(cmd,0,command,0,5);
            System.arraycopy(pin,0,command,5,pin.length);
            System.out.println("// Enter writePIN for validation");
            cmdAPDU = new CommandAPDU( command );
            displayAPDU(cmdAPDU);
            resp = this.sendAPDU( cmdAPDU, DISPLAY );

        } catch( Exception e ) {}

        System.out.println( "" );

    }


    void readNameFromCard() {
        byte[] cmd_ = {CLA,READNAMEFROMCARD,P1,P2,(byte)0x1F};
        CommandAPDU cmd = new CommandAPDU( cmd_ );
        System.out.println("Sending READNAMEFROMCARD command APDU, data expected...");
        ResponseAPDU resp = this.sendAPDU( cmd, DISPLAY );
        byte[] bytes = resp.getBytes();
        String msg = "";
        for(int i=0; i<bytes.length-2;i++)
            msg += new StringBuffer("").append((char)bytes[i]);
        System.out.println(msg);
    }


    void writeNameToCard() {

        CommandAPDU cmdAPDU;
        ResponseAPDU resp;
        try {
            byte[] name = readKeyboard().getBytes();
            byte[] cmd = { CLA, WRITENAMETOCARD, P1, P2, (byte)name.length };
            byte[] command = new byte[5+name.length];

            System.arraycopy(cmd,0,command,0,5);
            System.arraycopy(name,0,command,5,name.length);
            System.out.println("// Write name into the card");
            cmdAPDU = new CommandAPDU( command );
            displayAPDU(cmdAPDU);
            resp = this.sendAPDU( cmdAPDU, DISPLAY );

        } catch( Exception e ) {}

        System.out.println( "" );

    }


    void exit() {
        loop = false;
    }


    void runAction( int choice ) {
        switch( choice ) {
            case 14: updateCardKey(); break;
            case 13: uncipherFileByCard(); break;
            case 12: cipherFileByCard(); break;
            case 11: cipherAndUncipherNameByCard(); break;
            case 10: readFileFromCard(); break;
            case 9: writeFileToCard(); break;
            case 8: updateWritePIN(); break;
            case 7: updateReadPIN(); break;
            case 6: displayPINSecurity(); break;
            case 5: desactivateActivatePINSecurity(); break;
            case 4: enterReadPIN(); break;
            case 3: enterWritePIN(); break;
            case 2: readNameFromCard(); break;
            case 1: writeNameToCard(); break;
            case 0: exit(); break;
            default: System.out.println( "unknown choice!" );
        }
    }


    String readKeyboard() {
        String result = null;

        try {
            BufferedReader input = new BufferedReader( new InputStreamReader( System.in ) );
            result = input.readLine();
        } catch( Exception e ) {}

        return result;
    }


    int readMenuChoice() {
        int result = 0;

        try {
            String choice = readKeyboard();
            result = Integer.parseInt( choice );
        } catch( Exception e ) {}

        System.out.println( "" );

        return result;
    }


    void printMenu() {
        System.out.println( "" );
        System.out.println( "14: update the DES key within the card" );
        System.out.println( "13: uncipher a file by the card" );
        System.out.println( "12: cipher a file by the card" );
        System.out.println( "11: cipher and uncipher a name by the card" );
        System.out.println( "10: read a file from the card" );
        System.out.println( "9: write a file to the card" );
        System.out.println( "8: update WRITE_PIN" );
        System.out.println( "7: update READ_PIN" );
        System.out.println( "6: display PIN security status" );
        System.out.println( "5: desactivate/activate PIN security" );
        System.out.println( "4: enter READ_PIN" );
        System.out.println( "3: enter WRITE_PIN" );
        System.out.println( "2: read a name from the card" );
        System.out.println( "1: write a name to the card" );
        System.out.println( "0: exit" );
        System.out.print( "--> " );
    }


    void mainLoop() {
        while( loop ) {
            printMenu();
            int choice = readMenuChoice();
            runAction( choice );
        }
    }


    public static void main( String[] args ) throws InterruptedException {
        new TheClient();
    }


}

