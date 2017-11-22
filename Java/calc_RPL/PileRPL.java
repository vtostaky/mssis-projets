import java.io.*;

public class PileRPL{
    private ObjEmp objTab[];
    private static int nbObj;
    // The buffered writer used for log mechanism: logs will either be displayed in std output, or in a file
    private BufferedWriter writer;
    // The output where the RPL pile will be displayed : std output or remotely
    private PrintStream printStream;

    public PileRPL(){
        new PileRPL(3);
    }

    public PileRPL(int taille){
        this.objTab = new ObjEmp[taille];
        this.nbObj = 0;
        this.writer = new BufferedWriter(new OutputStreamWriter(System.out));
        this.printStream = new PrintStream(System.out);
    }

    public PileRPL(BufferedWriter writer){
        new PileRPL(3);
        this.writer = writer;
    }

    public PileRPL(int taille, BufferedWriter writer, PrintStream printStream){
        this.objTab = new ObjEmp[taille];
        this.nbObj = 0;
        this.writer = writer;
        this.printStream = printStream;
    }

    /*
     * Pop 2 objects and add the first to the second one.
     * Push modified object.
     */
    public void addition(){
        ObjEmp obj1 = this.depile();
        ObjEmp obj2 = this.depile();
        try{ 
            obj2.addition(obj1);
            this.empile(obj2);
        }
        catch(NullPointerException nullPE){
            try{
                writer.write("Impossible to add : either obj1="+obj1+" or obj2="+obj2+ " is null\n");
                writer.flush();
            }
            catch(IOException e){
                printStream.println("I/O exception!!!");
            }
            if(obj2 != null)
                this.empile(obj2);
            if(obj1 != null)
                this.empile(obj1);
        }
    }

    /*
     * Pop 2 objects and substract the first to the second one.
     * Push modified object.
     */
    public void soustraction(){
        ObjEmp obj1 = this.depile();
        ObjEmp obj2 = this.depile();
        try{ 
            obj2.soustraction(obj1);
            this.empile(obj2);
        }
        catch(NullPointerException nullPE){
            try{
                writer.write("Impossible to substract : either obj1="+obj1+" or obj2="+obj2+ " is null\n");
                writer.flush();
            }
            catch(IOException e){
                printStream.println("I/O exception!!!");
            }
            if(obj2 != null)
                this.empile(obj2);
            if(obj1 != null)
                this.empile(obj1);
        }
    }

    /*
     * Pop 2 objects and divide the first one by the second one.
     * Push modified object.
     */
    public void division(){
        ObjEmp obj1 = this.depile();
        ObjEmp obj2 = this.depile();
        try{
            obj2.division(obj1);
            this.empile(obj2);
        }
        catch(NullPointerException nullPE){
            try{
                writer.write("Impossible to divide : either obj1="+obj1+" or obj2="+obj2+ " is null\n");
                writer.flush();
            }
            catch(IOException e){
                printStream.println("I/O exception!!!");
            }
            if(obj2 != null)
                this.empile(obj2);
            if(obj1 != null)
                this.empile(obj1);
        }
        catch(ArithmeticException zeroE){
            try{
                writer.write("Impossible to divide : obj1="+obj1+ " is 0\n");
                writer.flush();
            }
            catch(IOException e){
                printStream.println("I/O exception!!!");
            }
            this.empile(obj2);
            this.empile(obj1);
        }
    }

    /*
     * Pop 2 objects and multiply the second one with the first one.
     * Push modified object.
     */
    public void multiplication(){
        ObjEmp obj1 = this.depile();
        ObjEmp obj2 = this.depile();
        try{ 
            obj2.multiplication(obj1);
            this.empile(obj2);
        }
        catch(NullPointerException nullPE){
            try{
                writer.write("Impossible to multiply : either obj1="+obj1+" or obj2="+obj2+ " is null\n");
                writer.flush();
            }
            catch(IOException e){
                printStream.println("I/O exception!!!");
            }
            if(obj2 != null)
                this.empile(obj2);
            if(obj1 != null)
                this.empile(obj1);
        }
    } 

    /*
     * Method to add a new object to the stack.
     */
    public void empile(ObjEmp obj){
        try{
            this.objTab[this.nbObj] = obj;
            this.nbObj++;
        }
        catch(ArrayIndexOutOfBoundsException arrayOOBE){
            printStream.println("Stack is full, please use operators to free some space.");
            try{
                writer.write("Stack is full, please use operators to free some space.\n");
                writer.flush();
            }
            catch(IOException e){
                printStream.println("I/O exception!!!");
            }
        }
    }

    /*
     * Method to pop an object from the stack.
     */
    private ObjEmp depile(){
        ObjEmp obj = null;
        try{
            obj = this.objTab[this.nbObj-1];
            this.nbObj--;
        }
        catch(ArrayIndexOutOfBoundsException arrayOOBE){
            printStream.println("Stack is empty, please provide some integers to operate on.");
            try{
                writer.write("Stack is empty, please provide some integers to operate on.\n");
                writer.flush();
            }
            catch(IOException e){
                printStream.println("I/O exception!!!");
            }
        }
        return obj; 
    }

    /*
     * Method that give the status of the stack, to know if it's full or not.
     */
    public boolean status(){
        if(this.nbObj == 0)
        {
            try{
                writer.write("Stack is empty!\n",0,16);
            }
            catch(IOException e){
                printStream.println("I/O exception!!!");
            }
        }
        if(this.nbObj == this.objTab.length)
        {
            try{
                writer.write("Stack is full!\n",0,15);
            }
            catch(IOException e){
                printStream.println("I/O exception!!!");
            }
            return true;
        }
        return false;
    }

    /*
     * Method to convert the stack into a string, to display it easily.
     */
    public String toString(){
        String pileString = "";

        for(int i = 0; i < this.nbObj; i++)
        {
            pileString += i + ": " + this.objTab[i].toString() + "\n";
        }
        pileString += "End of Stack";
        return pileString;
    }
}

