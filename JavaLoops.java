import java.util.ArrayList;
public class JavaLoops{
    public static void main(String[] args){
        ArrayList<Integer> danger = new ArrayList<Integer>();
        for (int i = 0; i <= 20; i += 1){
            danger.add(i);
        }
        for (int i : danger){
            boolean x = true ? i%2 == 0 : false;
            if (x){
                System.out.println(i);
            }
        }
    }
}
