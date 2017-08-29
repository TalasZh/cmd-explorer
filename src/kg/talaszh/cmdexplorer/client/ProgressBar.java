package kg.talaszh.cmdexplorer.client;

/**
 * Created by talas on 8/28/17.
 */
public class ProgressBar {
    private long max;
    private long current;
    private String name;
    private long start;
    private long lastUpdate;

    public ProgressBar(int max, String name)
    {
        this.start = System.currentTimeMillis();
        this.name = name;
        this.max = max;
        System.out.println(this.name+":");
        this.printBar(false);
    }

    public void setVal(long i)
    {
        this.current += i;
        if((System.currentTimeMillis() - this.lastUpdate)>1000){
            this.lastUpdate = System.currentTimeMillis();
            this.printBar(false);
        }
    }

    public void finish()
    {
        System.out.println(String.format("Total: %,d Received: %,d", max, current));
        this.current = this.max;
        this.printBar(true);
    }

    private void printBar(boolean finished)
    {
        double numbar= Math.floor(20*(double)current/(double)max);
        String strbar = "";
        int ii = 0;
        for(ii = 0; ii < numbar; ii++){
            strbar += "=";
        }
        for(ii = (int)numbar; ii < 20; ii++){
            strbar += " ";
        }
        long elapsed = (System.currentTimeMillis() - this.start);
        int seconds = (int)(elapsed / 1000)%60;
        int minutes = (int)(elapsed / 1000)/60;
        String strend = String.format("%02d",minutes)+":"+String.format("%02d",seconds);

        String percentage = "";
        if (elapsed < 2000){
            percentage = "  0";
        }else{
            long ETAminutes = (current * 100)/max;
            percentage = String.format("%3d",ETAminutes);
        }
        if(finished){
            strend = "Finished: "+strend+"                   ";
        }else{
            strend = "Elapsed: "+strend+" Downloaded% : "+percentage+"%   ";
        }
        System.out.print("|"+strbar+"| "+strend);
        if(finished){
            System.out.print("\n");
        }else{
            System.out.print("\r");
        }
    }
}
