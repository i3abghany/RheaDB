package RheaDB.StorageManagement;

import BPlusTree.BPlusTree;
import RheaDB.Attribute;
import RheaDB.Page;
import RheaDB.RowRecord;
import RheaDB.Table;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

public class BufferPool {
    private final static int maxPagesInCache = 16;
    private final HashMap<PageIdentifier, Page> pageHashMap;

    private record PageIdentifier(String tableName, int pageIdx) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PageIdentifier that = (PageIdentifier) o;
            return pageIdx == that.pageIdx && tableName.equals(that.tableName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableName, pageIdx);
        }
    }

    public BPlusTree<?, RowRecord> getIndex(Table table, Attribute attribute) {
        String indexFullPath =
                table.getPageDirectory() + File.separator + "index" +
                File.separator + attribute.getName() + ".idx";

        return DiskManager.deserializeIndex(indexFullPath);
    }

    public void savePage(Table table, Page lastPage) {
        DiskManager.savePage(table, lastPage);
    }

    public void saveIndex(Table table, Attribute attribute, BPlusTree<?, RowRecord> bPlusTree) {
        String fullIndexPath = table.getPageDirectory() + File.separator +
                "index" + File.separator + attribute.getName() + ".idx";

        DiskManager.saveIndex(fullIndexPath, bPlusTree);
    }

    public void deleteIndex(Table table, Attribute attribute) {
        String fullIndexPath = table.getPageDirectory() + File.separator +
                "index" + File.separator + attribute.getName() + ".idx";

        DiskManager.deleteIndex(fullIndexPath);
    }

    public void deletePage(Table table, int pageIdx) {
        PageIdentifier pageIdentifier = new PageIdentifier(table.getName(), pageIdx);
        pageHashMap.remove(pageIdentifier);
        DiskManager.deletePage(table, pageIdx);
    }

    public void updatePage(Table table, Page page) {
        deletePage(table, page.getPageIdx());
        DiskManager.savePage(table, page);
        insertPage(table, page);
    }

    public BufferPool() {
        pageHashMap = new HashMap<>();
    }

    /**
     * Searches for the page in the hash table, if not existent, deserialize it.
     * @param table The table to which the page belong to.
     * @param pageIdx Page index in table that's used to construct an identifier.
     * @return The requested page.
     */
    public Page getPage(Table table, int pageIdx) {
        PageIdentifier pageIdentifier = new PageIdentifier(table.getName(), pageIdx);
        if (!pageHashMap.containsKey(pageIdentifier))
            return insertPage(table, pageIdx);
        else
            return pageHashMap.get(pageIdentifier);
    }

    /**
     * Deserializes the page and inserts it into the hash table.
     * @param table The table to which the page belong to.
     * @param pageIdx The index of the page that's use to construct an identifier.
     * @return Returns the page JIC the caller may want to search for it in the hash table.
     */
    private Page insertPage(Table table, int pageIdx) {
        Page page = DiskManager.getPage(table, pageIdx);
        return page == null ? null : insertPage(table, page);
    }

    private Page insertPage(Table table, Page page) {
        PageIdentifier pageIdentifier = new PageIdentifier(table.getName(), page.getPageIdx());

        if (pageHashMap.size() >= maxPagesInCache) {
            PageIdentifier randKey = (PageIdentifier) pageHashMap.keySet().toArray()[0];
            pageHashMap.remove(randKey);
        }

        pageHashMap.put(pageIdentifier, page);
        return page;
    }
}
