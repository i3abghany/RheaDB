import BPlusTree.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class BPlusTreeTest {
    @Test
    void searchTest() {
        BPlusTree<Integer, Character> tree = new BPlusTree<Integer, Character>();
        int i = 1;
        for (char c = 'a'; c <= 'z'; c++)
            tree.insert(i++, c);

        for (i = 1; i <= 26; i++) {
            int finalI = i;
            Assertions.assertTrue(tree.find(i).stream().findAny().isPresent() &&
                    tree.find(i).stream().findFirst().stream().allMatch((x) -> x == ((char)('a' + finalI - 1))));
        }
    }

    @Test
    void duplicateInsertions() {
        BPlusTree<Integer, Character> tree = new BPlusTree<Integer, Character>();

        int i = 1;
        for (char c = 'a'; c <= 'z'; c++)
            tree.insert(i++, c);

        for (char c = 'a'; c <= 'z'; c++)
            tree.insert(i++, c);

        for (char c = 'a'; c <= 'z'; c++)
            tree.insert(i++, c);

        for (i = 1; i <= 26; i++) {
            int finalI = i;
            Assertions.assertTrue(tree.find(i).stream().findAny().isPresent() &&
                    tree.find(i).stream().allMatch((x) -> x == ((char)('a' + finalI - 1))));
        }

    }

    @Test
    void randomDeletionTest() {
        BPlusTree<Integer, Character> tree = new BPlusTree<Integer, Character>();
        int i = 1;
        for (char c = 'a'; c <= 'z'; c++)
            tree.insert(i++, c);

        Assertions.assertTrue(tree.delete(('t' - 'a')));
        Assertions.assertNull(tree.find(('t' - 'a')));

        Assertions.assertTrue(tree.delete(('c' - 'a')));
        Assertions.assertNull(tree.find(('c' - 'a')));


        Assertions.assertTrue(tree.delete(('j' - 'a')));
        Assertions.assertNull(tree.find(('j' - 'a')));

        Assertions.assertTrue(tree.delete(('d' - 'a')));
        Assertions.assertNull(tree.find(('d' - 'a')));

        Assertions.assertTrue(tree.delete(('u' - 'a')));
        Assertions.assertNull(tree.find(('u' - 'a')));

        Assertions.assertTrue(tree.delete(('r' - 'a')));
        Assertions.assertNull(tree.find(('r' - 'a')));

        Assertions.assertTrue(tree.delete(('k' - 'a')));
        Assertions.assertNull(tree.find(('k' - 'a')));

        Assertions.assertTrue(tree.delete(('m' - 'a')));
        Assertions.assertNull(tree.find(('m' - 'a')));

        Assertions.assertTrue(tree.delete(('n' - 'a')));
        Assertions.assertNull(tree.find(('n' - 'a')));
    }

    @Test
    void findAfterDelete() {
        BPlusTree<Integer, Character> tree = new BPlusTree<Integer, Character>();
        int i = 1;
        for (char c = 'a'; c <= 'z'; c++)
            tree.insert(i++, c);

        tree.delete(('m' - 'a'));
        Assertions.assertNull(tree.find(('m' - 'a')));

        tree.insert(('m' - 'a'), 'm');
        Assertions.assertNotNull(tree.find(('m' - 'a')));

        tree.delete(('d' - 'a'));
        Assertions.assertNull(tree.find(('d' - 'a')));

        tree.insert(('d' - 'a'), 'd');
        Assertions.assertNotNull(tree.find(('d' - 'a')));
    }

    @Test
    void fullDeletionTest() {
        BPlusTree<Integer, Character> tree = new BPlusTree<Integer, Character>();
        int i = 1;
        for (char c = 'a'; c <= 'z'; c++)
            tree.insert(i++, c);

        for (i = 1; i <= 26; i++)
            Assertions.assertTrue(tree.delete(i));

        for (i = 1; i <= 26; i++)
            Assertions.assertNull(tree.find(i));

        Assertions.assertTrue(tree.isEmpty());
    }

    @Test
    void millionUniqueSearch() {
        Set<Integer> set = new TreeSet<>();
        BPlusTree<Integer, Integer> tree = new BPlusTree<Integer, Integer>();
        Random rng = new Random();

        while (set.size() < 1000000) {
            int r = rng.nextInt();
            if (set.contains(r))
                continue;
            set.add(r);
            tree.insert(r, r);
        }

        for (Integer i : set) {
            ValueList<Integer, Integer> valueList = tree.find(i);
            Assertions.assertNotNull(valueList);
            Assertions.assertTrue(valueList.stream().allMatch((x) -> x.equals(i)));
        }
    }

    @Test
    void millionSearchWithDuplicates() {
        BPlusTree<Integer, Integer> tree = new BPlusTree<Integer, Integer>();
        ArrayList<Integer> keys = new ArrayList<>();
        Random rng = new Random();

        for (int i = 0; i < 1000000; i++) {
            int r = rng.nextInt();
            keys.add(r);
            tree.insert(r, r);
        }

        for (Integer a : keys) {
            ValueList<Integer, Integer> valueList = tree.find(a);
            Assertions.assertNotNull(valueList);
            Assertions.assertTrue(valueList.stream().allMatch((x) -> x.equals(a)));
        }
    }

    @Test
    void millionUniqueDeletion() {
        Set<Integer> set = new TreeSet<>();
        BPlusTree<Integer, Integer> tree = new BPlusTree<Integer, Integer>();
        Random rng = new Random();

        while (set.size() < 1000000) {
            int r = rng.nextInt();
            if (set.contains(r))
                continue;
            set.add(r);
            tree.insert(r, r);
        }

        for (Integer i : set) {
            Assertions.assertTrue(tree.delete(i));
        }

        for (Integer i : set) {
            Assertions.assertNull(tree.find(i));
        }
    }

    @Test
    void deletionFromEmptyTree() {
        BPlusTree<Integer, Integer> tree = new BPlusTree<Integer, Integer>();
        Assertions.assertFalse(tree.delete(0));
    }
}
