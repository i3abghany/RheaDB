package RheaDB.StorageManagement;

import BPlusTree.BPlusTree;
import RheaDB.Attribute;
import RheaDB.Page;
import RheaDB.RowRecord;
import RheaDB.Table;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class BufferPool {

    private final static int maxPagesInCache = 16;
    private final ConcurrentHashMap<PageIdentifier, Page> pageHashMap;
    private final ConcurrentHashMap<IndexIdentifier, BPlusTree<?, RowRecord>> indexHashMap;
    private final Set<PageIdentifier> dirtyPages;

    private record PageIdentifier(Table table, int pageIdx) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PageIdentifier that = (PageIdentifier) o;
            return pageIdx == that.pageIdx && table.getName().equals(that.table.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(table.getName(), pageIdx);
        }
    }

    public void commitAllPages() {
        Vector<PageIdentifier> dirtySnapshot = new Vector<>(dirtyPages);
        dirtySnapshot.forEach(pageIdentifier -> {
            Page page = pageHashMap.get(pageIdentifier);
            if (page != null) {
                flushPage(pageIdentifier, page);
            }
        });
    }

    private record IndexIdentifier(String tableName, String attributeName) {
    }

    public boolean deleteTable(Table table) {
        for (int i = table.getNumPages(); i > 0; i--) {
            deletePage(table, i);
        }

        table.getAttributeList()
                .stream()
                .filter(Attribute::getIsIndexed)
                .forEach(attr -> deleteIndex(table, attr));

        String pageDir = table.getPageDirectory();
        File indexDirectory = Paths.get(pageDir + File.separator + "index").toFile();
        indexHashMap.keySet().removeIf(key -> key.tableName.equals(table.getName()));
        indexDirectory.delete();
        Paths.get(pageDir).toFile().delete();

        return true;
    }

    public void updateTablePagesFromDisk(Table t) {
        Vector<Integer> pages = new Vector<>();
        pageHashMap.keySet().forEach((PageIdentifier pi) -> {
            if (pi.table == t) {
                pages.add(pi.pageIdx);
            }
        });

        pageHashMap.keySet().removeIf(p -> p.table == t);
        dirtyPages.removeIf(p -> p.table == t);
        for (int i : pages) {
            getPageFromStorage(t, i);
        }
    }

    public void commitTable(Table t) {
        Vector<PageIdentifier> dirtySnapshot = new Vector<>(dirtyPages);
        dirtySnapshot.forEach(pageIdentifier -> {
            if (pageIdentifier.table == t) {
                Page page = pageHashMap.get(pageIdentifier);
                if (page != null) {
                    flushPage(pageIdentifier, page);
                }
            }
        });
    }

    public BPlusTree<?, RowRecord> getIndex(Table table, Attribute attribute) {
        IndexIdentifier indexIdentifier = new IndexIdentifier(table.getName(), attribute.getName());
        BPlusTree<?, RowRecord> cachedIndex = indexHashMap.get(indexIdentifier);
        if (cachedIndex != null) {
            return cachedIndex;
        }

        String indexFullPath =
                table.getPageDirectory() + File.separator + "index" +
                        File.separator + attribute.getName() + ".idx";

        BPlusTree<?, RowRecord> deserializedIndex = DiskManager.deserializeIndex(indexFullPath);
        if (deserializedIndex != null) {
            indexHashMap.put(indexIdentifier, deserializedIndex);
        }
        return deserializedIndex;
    }

    public void saveIndex(Table table, Attribute attribute, BPlusTree<?, RowRecord> bPlusTree) {
        IndexIdentifier indexIdentifier = new IndexIdentifier(table.getName(), attribute.getName());
        String fullIndexPath = table.getPageDirectory() + File.separator +
                "index" + File.separator + attribute.getName() + ".idx";

        DiskManager.saveIndex(fullIndexPath, bPlusTree);
        indexHashMap.put(indexIdentifier, bPlusTree);
    }

    public void deleteIndex(Table table, Attribute attribute) {
        IndexIdentifier indexIdentifier = new IndexIdentifier(table.getName(), attribute.getName());
        String fullIndexPath = table.getPageDirectory() + File.separator +
                "index" + File.separator + attribute.getName() + ".idx";

        indexHashMap.remove(indexIdentifier);
        DiskManager.deleteIndex(fullIndexPath);
    }

    public void deletePage(Table table, int pageIdx) {
        PageIdentifier pageIdentifier = new PageIdentifier(table, pageIdx);
        pageHashMap.remove(pageIdentifier);
        dirtyPages.remove(pageIdentifier);
        boolean didDelete = DiskManager.deletePage(table, pageIdx);
        if (!didDelete) table.popPage();
    }

    public void updatePage(Table table, Page page) {
        PageIdentifier pageIdentifier = new PageIdentifier(table, page.getPageIdx());
        insertPage(table, page);
        dirtyPages.add(pageIdentifier);
    }

    public BufferPool() {
        pageHashMap = new ConcurrentHashMap<>();
        indexHashMap = new ConcurrentHashMap<>();
        dirtyPages = ConcurrentHashMap.newKeySet();
    }

    /**
     * Searches for the page in the hash table, if not existent, deserialize it.
     *
     * @param table   The table to which the page belong to.
     * @param pageIdx Page index in table that's used to construct an identifier.
     * @return The requested page.
     */
    public Page getPage(Table table, int pageIdx) {
        PageIdentifier pageIdentifier = new PageIdentifier(table, pageIdx);
        Page cachedPage = pageHashMap.get(pageIdentifier);
        return cachedPage != null ? cachedPage : getPageFromStorage(table, pageIdx);
    }

    /**
     * Deserializes the page and inserts it into the hash table.
     *
     * @param table   The table to which the page belong to.
     * @param pageIdx The index of the page that's use to construct an identifier.
     * @return Returns the page JIC the caller may want to search for it in the hash table.
     */
    private Page getPageFromStorage(Table table, int pageIdx) {
        Page page = DiskManager.getPage(table, pageIdx);
        return page == null ? null : insertPage(table, page);
    }

    public Page insertPage(Table table, Page page) {
        PageIdentifier pageIdentifier = new PageIdentifier(table, page.getPageIdx());

        Page previousPage = pageHashMap.get(pageIdentifier);
        if (previousPage != null) {
            pageHashMap.put(pageIdentifier, page);
            return page;
        }

        if (pageHashMap.size() >= maxPagesInCache) {
            PageIdentifier randKey = (PageIdentifier) pageHashMap.keySet().toArray()[0];
            Page evictedPage = pageHashMap.get(randKey);
            flushPage(randKey, evictedPage);
            pageHashMap.remove(randKey);
        }

        pageHashMap.put(pageIdentifier, page);
        return page;
    }

    private void flushPage(PageIdentifier pageIdentifier, Page page) {
        if (!dirtyPages.contains(pageIdentifier)) {
            return;
        }

        DiskManager.savePage(pageIdentifier.table, page);
        dirtyPages.remove(pageIdentifier);
    }
}
