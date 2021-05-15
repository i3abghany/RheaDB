import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BPlusTreeTest {
    @Test
    void insertionTest() {
        BPlusTree<Integer, Character> tree = new BPlusTree<>();
        int i = 1;
        for (char c = 'a'; c <= 'z'; c++)
            tree.insert(i++, c);

        for (i = 1; i <= 26; i++)
            Assertions.assertEquals(tree.find(i), (char)('a' + i - 1));
    }

    @Test
    void randomDeletionTest() {
        BPlusTree<Integer, Character> tree = new BPlusTree<>();
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
    void insertionAfterDeletionTest() {
        BPlusTree<Integer, Character> tree = new BPlusTree<>();
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
        BPlusTree<Integer, Character> tree = new BPlusTree<>();
        int i = 1;
        for (char c = 'a'; c <= 'z'; c++)
            tree.insert(i++, c);

        for (i = 1; i <= 26; i++)
            Assertions.assertTrue(tree.delete(i));

        for (i = 1; i <= 26; i++)
            Assertions.assertNull(tree.find(i));

        Assertions.assertTrue(tree.isEmpty());
    }
}
