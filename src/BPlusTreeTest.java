import java.util.Random;
import java.util.TreeMap;

public class BPlusTreeTest {
    public static void main(String args) {
        vsTreeMapPerf();
    }

    private static void vsTreeMapPerf() {
        BPlusTree<Integer, String> mp = new BPlusTree<>();
        TreeMap<Integer, String> tm = new TreeMap<>();
        Random rng = new Random();

        long t1 = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            mp.insert(rng.nextInt(1090000000), "THis is a fairly long string...");
            try {
                mp.find(rng.nextInt(1090000000));
            } catch (Exception ignored) { }
        }
        System.out.println(System.nanoTime() - t1);

        t1 = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            tm.put(rng.nextInt(1090000000), "THis is a fairly long string...");
            tm.get(rng.nextInt(1090000000));
        }
        System.out.println(System.nanoTime() - t1);
    }
}
