public class Zug {
    String input;
    int startX;
    int endX;
    int startY;
    int endY;
    char promoteTo;
    public Zug(String e){
        input = e;
        processInput();
    }
    public Zug(int x1, int y1, int x2, int y2){
        startX = x1;
        startY = y1;
        endX = x2;
        endY = y2;
    }
    public Zug(int x1, int y1, int x2, int y2, char c){
        startX = x1;
        startY = y1;
        endX = x2;
        endY = y2;
        promoteTo = c;
    }
    private void processInput(){
        startX = input.charAt(0) - 'a';
        startY = 8 - Integer.parseInt(input.charAt(1)+ "");
        endX = input.charAt(2) - 'a';
        endY = 8 - Integer.parseInt(input.charAt(3)+ "");
        if(input.length() == 5){
            promoteTo = input.charAt(4);
        }
    }
    public String processZug(){
        String s = "";
        s += (char)(startX + 'a');
        s += 8 - startY;
        s += (char) (endX + 'a');
        s += 8 - endY;
        if(promoteTo == 'q' || promoteTo == 'n' || promoteTo == 'b' || promoteTo == 'r')
            s += promoteTo;
        return s;
    }
}
