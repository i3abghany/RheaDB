package RheaDB.StorageManagement;

import BPlusTree.BPlusTree;
import RheaDB.Page;
import RheaDB.RowRecord;
import RheaDB.Table;

import java.io.*;
import java.util.HashMap;
import java.util.logging.*;

public class DiskManager {
    private final static Logger LOGGER = Logger.getLogger(DiskManager.class.getName());

    public static Page getPage(Table table, int idx) {
        String fullPath = getFullPath(table, idx);
        return deserializePage(fullPath);
    }

    public static void compactTable(Table table) {
        int numPages = table.getNumPages();

        for (int i = numPages; i > 0; i--) {
            Page p = deserializePage(getFullPath(table, i));
            if (p.isEmpty()) {
                deletePage(table, i);
            }
        }

        Page lastPage = deserializePage(getFullPath(table, numPages));
        for (int i = 1; i < numPages; i++) {
            Page p = deserializePage(getFullPath(table, i));
            if (!p.isFull()) {
                mergePages(p, lastPage);
                savePage(table, p);
                if (lastPage.isEmpty()) {
                    deletePage(table, numPages);
                    lastPage = deserializePage(getFullPath(table, i));
                } else {
                    savePage(table, lastPage);
                }
            }
        }
    }

    private static void mergePages(Page p, Page lastPage) {
        int rem = p.getMaxRows() - p.getNumberOfRows();
        int taken = (rem - lastPage.getNumberOfRows());
        taken = taken < 0 ? rem : taken;

        for (int i = 0; i < taken; i++) {
            stealRow(p, lastPage);
        }
    }

    private static void stealRow(Page p1, Page p2) {
        if (p1.isFull() || p2.isEmpty()) {
            return;
        }

        p1.addRecord(p2.popRow());
    }

    public static boolean deletePage(Table table, int idx) {
        String fullPath = getFullPath(table, idx);
        File pageFile = new File(fullPath);

        if (!pageFile.exists()) {
            return false;
        }

        if (!pageFile.delete()) {
            LOGGER.log(Level.SEVERE, "Could not delete database files... Exiting");
            return false;
        }
        table.popPage();
        return true;
    }

    private static String getFullPath(Table table, int idx) {
        return table.getPageDirectory() + File.separator + table.getName() + "_" + idx + ".db";
    }

    public static void savePage(Table table, Page page) {
        serializePage(page, getFullPath(table, page.getPageIdx()));
    }

    private static Page deserializePage(String fullPath) {
        Page page = null;
        try {
            File file = new File(fullPath);
            if (!file.exists()) {
                return null;
            }

            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            page = (Page) ois.readObject();

            ois.close();
            fis.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while serializing a page... Exiting.");
            System.exit(1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return page;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void serializePage(Page page, String fullPath) {
        try {
            File file = new File(fullPath);

            if (file.exists()) {
                boolean wasDeleted = file.delete();
                if (!wasDeleted) {
                    LOGGER.log(Level.SEVERE, "Could not access a page for deletion... Exiting.");
                    System.exit(1);
                }
            } else {
                file.getParentFile().mkdirs();
                boolean fileCreated = file.createNewFile();
                if (!fileCreated) {
                    LOGGER.log(Level.SEVERE, "Could not create page file... Exiting.");
                    System.exit(1);
                }
            }
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(page);

            oos.close();
            fos.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while serializing a page... Exiting.", e);
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, Table> readMetadata() throws IOException {
        HashMap<String, Table> map = new HashMap<>();
        File file = new File("." + File.separator + "data" + File.separator + "metadata.db");
        if (file.length() == 0)
            return new HashMap<>();
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        try {
            map = (HashMap<String, Table>) ois.readObject();
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Corrupted metadata file... Exiting.", e);
            System.exit(1);
        }
        return map;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void saveIndex(String fullPath, BPlusTree<?, RowRecord> tree) {
        try {
            File file = new File(fullPath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                boolean fileCreated = file.createNewFile();
                if (!fileCreated) {
                    LOGGER.log(Level.SEVERE, "Could not create index file... Exiting.");
                    System.exit(1);
                }
            }
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(tree);

            oos.close();
            fos.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while serializing index... Exiting.", e);
            System.exit(1);
        }
    }

    public static void deleteIndex(String fullPath) {
        File file = new File(fullPath);
        if (!file.exists()) {
            LOGGER.log(Level.SEVERE, "Could not find the index to delete.");
            System.exit(1);
        }

        if (!file.delete()) {
            LOGGER.log(Level.SEVERE, "Could not delete the index.");
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    public static BPlusTree<?, RowRecord> deserializeIndex(String fullPath) {
        BPlusTree<?, RowRecord> tree = null;
        try {
            File file = new File(fullPath);
            if (!file.exists()) {
                return null;
            }

            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            tree = (BPlusTree<?, RowRecord>) ois.readObject();

            ois.close();
            fis.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while deserializing"
                    + " index... Exiting.");
            System.exit(1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return tree;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void saveMetadata(HashMap<String, Table> map) {
        File file = new File("." + File.separator + "data" + File.separator + "metadata.db");
        if (!file.exists()) {
            boolean fileCreated = false;
            file.getParentFile().mkdirs();
            try {
                fileCreated = file.createNewFile();
            } catch (IOException ioException) {
                LOGGER.log(Level.SEVERE, "Could not instantiate metadata file... Exiting.");
                System.exit(1);
            }
            if (!fileCreated) {
                LOGGER.log(Level.SEVERE, "Could not instantiate metadata file... Exiting.");
                System.exit(1);
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(map);
        } catch (IOException ioException) {
            LOGGER.log(Level.SEVERE, "Could not open metadata file... Exiting", ioException);
            System.exit(1);
        }
    }
}
