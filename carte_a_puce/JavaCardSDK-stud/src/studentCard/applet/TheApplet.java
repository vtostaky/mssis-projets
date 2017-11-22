package applet;


import javacard.framework.*;




public class TheApplet extends Applet {


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

    /**
     *      * SW bytes for PIN verification failure
     *           */
    final static short SW_VERIFICATION_FAILED = 0x6300;

    /**
     *      * SW bytes for PIN validation required
     *           */
    final static short SW_PIN_VERIFICATION_REQUIRED = 0x6301;

    final static short NVRSIZE      = (short)1024;
    static byte[] NVR               = new byte[NVRSIZE];
    OwnerPIN writePIN;
    OwnerPIN readPIN;
    boolean security;

    protected TheApplet() {
        this.register();
        byte[] pincode = {(byte)0x30,(byte)0x30,(byte)0x30,(byte)0x30}; // PIN code "0000"
        writePIN = new OwnerPIN((byte)3,(byte)8);               // 3 tries 8=Max Size
        writePIN.update(pincode,(short)0,(byte)4);              // from pincode, offset 0, length 4
        readPIN = new OwnerPIN((byte)3,(byte)8);                // 3 tries 8=Max Size
        readPIN.update(pincode,(short)0,(byte)4);               // from pincode, offset 0, length 4
        security = false;
    }


    public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
        new TheApplet();
    } 


    public boolean select() {
        return true;
    } 


    public void deselect() {
    }

    public void process(APDU apdu) throws ISOException {
        if( selectingApplet() == true )
            return;

        byte[] buffer = apdu.getBuffer();

        switch( buffer[1] )     {
            case UPDATECARDKEY: updateCardKey( apdu ); break;
            case UNCIPHERFILEBYCARD: uncipherFileByCard( apdu ); break;
            case CIPHERFILEBYCARD: cipherFileByCard( apdu ); break;
            case CIPHERANDUNCIPHERNAMEBYCARD: cipherAndUncipherNameByCard( apdu ); break;
            case READFILEFROMCARD: readFileFromCard( apdu ); break;
            case WRITEFILETOCARD: writeFileToCard( apdu ); break;
            case UPDATEWRITEPIN: updateWritePIN( apdu ); break;
            case UPDATEREADPIN: updateReadPIN( apdu ); break;
            case DISPLAYPINSECURITY: displayPINSecurity( apdu ); break;
            case DESACTIVATEACTIVATEPINSECURITY: desactivateActivatePINSecurity( apdu ); break;
            case ENTERREADPIN: enterReadPIN( apdu ); break;
            case ENTERWRITEPIN: enterWritePIN( apdu ); break;
            case READNAMEFROMCARD: readNameFromCard( apdu ); break;
            case WRITENAMETOCARD: writeNameToCard( apdu ); break;
            default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    void updateCardKey( APDU apdu ) {
    }


    void uncipherFileByCard( APDU apdu ) {
    }


    void cipherFileByCard( APDU apdu ) {
    }


    void cipherAndUncipherNameByCard( APDU apdu ) {
    }


    void readFileFromCard( APDU apdu ) {
    }


    void writeFileToCard( APDU apdu ) {
    }


    void updateWritePIN( APDU apdu ) {
        byte[] pincode = apdu.getBuffer();
        if (security && (! writePIN.isValidated()) )
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        else
        {
            writePIN.update(pincode,(short)1,(byte)pincode[0]);     // from pincode, offset 1, length pincode[0]
        }
    }


    void updateReadPIN( APDU apdu ) {
        byte[] pincode = apdu.getBuffer();
        if (security && (! readPIN.isValidated()) )
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        else
        {
            readPIN.update(pincode,(short)1,(byte)pincode[0]);  // from pincode, offset 1, length pincode[0]
        }
    }


    void displayPINSecurity( APDU apdu ) {
        byte[] buffer = apdu.getBuffer();
        if(security)
            buffer[0] = 0x01;
        else
            buffer[0] = 0x00;
        apdu.setOutgoingAndSend( (short)0, (byte)0x01);
    }


    void desactivateActivatePINSecurity( APDU apdu ) {
        if(security == false)
            security = true;
        else
            security = false;
    }


    void enterReadPIN( APDU apdu ) {
        apdu.setIncomingAndReceive();
        byte[] buffer = apdu.getBuffer();
        if( !readPIN.check( buffer, (byte)5, buffer[4] ) ) 
            ISOException.throwIt( SW_VERIFICATION_FAILED );
    }


    void enterWritePIN( APDU apdu ) {
        apdu.setIncomingAndReceive();
        byte[] buffer = apdu.getBuffer();
        if( !writePIN.check( buffer, (byte)5, buffer[4] ) ) 
            ISOException.throwIt( SW_VERIFICATION_FAILED );
    }


    void readNameFromCard( APDU apdu ) {
        byte[] buffer = apdu.getBuffer();
        if (security && (! readPIN.isValidated()) )
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        else
        {
            Util.arrayCopy(NVR, (short)1, buffer, (short)0, NVR[0]);
            apdu.setOutgoingAndSend( (short)0, NVR[0]);
        }
    }


    void writeNameToCard( APDU apdu ) {
        byte[] buffer = apdu.getBuffer();
        if ( security && (! writePIN.isValidated()) )
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        else
        {
            apdu.setIncomingAndReceive();  
            Util.arrayCopy(buffer,(short)4,NVR,(short)0,(short)(buffer[4]+(short)1));
        }
    }


}
