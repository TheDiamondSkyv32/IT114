
public class RecursionToLoop {
  
	// public static int sum(int num) {
	// 	if (num > 0) {
	// 		return num + sum(num - 1);
	// 	}
	// 	return 0;
    // }
    
    public static int sum(int num){
        int value = 0;
        for (int i = 0; i<=num; i++){
            value += i;
        }
        return value;
    }
	public static void main(String[] args) {
		System.out.println(sum(10));
	}
}