package RheaDB.StorageManagement;

import BPlusTree.BPlusTree;
import RheaDB.Attribute;
import RheaDB.Page;
import RheaDB.Table;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

public class BufferPool {
    private final static int maxPagesInCache = 16;
    private final HashMap<PageIdentifier, Page> pageHashMap;

    public BPlusTree getIndex(Table table, Attribute attribute) {
        String indexFullPath =
                table.getPageDirectory() + File.separator + "index" +
                File.separator + attribute.getName() + ".idx";

        return DiskManager.deserializeIndex(indexFullPath);
    }

    public void savePage(Table table, Page lastPage) {
        DiskManager.savePage(table, lastPage);
    }

    public void saveIndex(Table table, Attribute attribute, BPlusTree bPlusTree) {
        String fullIndexPath = table.getPageDirectory() + File.separator +
                "index" + File.separator + attribute.getName() + ".idx";

        DiskManager.saveIndex(fullIndexPath, bPlusTree);
    }

    public void deleteIndex(Table table, Attribute attribute) {
        String fullIndexPath = table.getPageDirectory() + File.separator +
                "index" + File.separator + attribute.getName() + ".idx";

        DiskManager.deleteIndex(fullIndexPath);
    }

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

    public BufferPool() {
        this.pageHashMap = new HashMap<>();
    }

    public Page getPage(Table table, int pageIdx) {
        PageIdentifier pageIdentifier = new PageIdentifier(table.getName(), pageIdx);
        if (!this.pageHashMap.containsKey(pageIdentifier))
            return insertPage(table, pageIdx);
        else
            return pageHashMap.get(pageIdentifier);
    }

    private Page insertPage(Table table, int pageIdx) {
        Page page = DiskManager.getPage(table, pageIdx);
        PageIdentifier pageIdentifier = new PageIdentifier(table.getName(), pageIdx);

        if (pageHashMap.size() >= maxPagesInCache) {
            PageIdentifier randKey = (PageIdentifier) this.pageHashMap.keySet().toArray()[0];
            pageHashMap.remove(randKey);
        }

        pageHashMap.put(pageIdentifier, page);
        return page;
    }
}
