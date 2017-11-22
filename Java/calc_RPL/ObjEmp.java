public class ObjEmp{
    protected int valeur;

    public ObjEmp(int v){
        this.valeur = v;
    }

    public int getValeur(){
        return this.valeur;
    }

    public String toString(){
        return ""+this.valeur;
    }

    public void addition(ObjEmp obj){
        this.valeur += obj.getValeur();
    }

    public void soustraction(ObjEmp obj){
        this.valeur -= obj.getValeur();
    }

    public void division(ObjEmp obj) throws ArithmeticException {
        if(obj.getValeur() == 0)
            throw new ArithmeticException("cannot divide by 0");
        else
            this.valeur /= obj.getValeur();
    }

    public void multiplication(ObjEmp obj){
        this.valeur *= obj.getValeur();
    }
}
