public class Koordinaten {
    public int x, y;

    public Koordinaten(int x, int y){
        this.x = x;
        this.y = y;
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Koordinaten k)) return false;
        return k.x == this.x && k.y == this.y;
    }
    @Override
    public int hashCode(){
        return x*31+y;
    }
}
