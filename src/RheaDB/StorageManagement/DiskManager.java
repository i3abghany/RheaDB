package RheaDB.StorageManagement;

import BPlusTree.BPlusTree;
import BPlusTree.ValueList;
import RheaDB.AttributeType;
import RheaDB.Page;
import RheaDB.RowRecord;
import RheaDB.Table;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiskManager {
    private final static Logger LOGGER = Logger.getLogger(DiskManager.class.getName());
    private static final int PAGE_FILE_MAGIC = 0x52484541;
    private static final int PAGE_FILE_VERSION = 1;

    private static class IndexSnapshot implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final Vector<ValueList<?, RowRecord>> valueLists;

        private IndexSnapshot(Vector<ValueList<?, RowRecord>> valueLists) {
            this.valueLists = valueLists;
        }
    }

    public static Page getPage(Table table, int idx) {
        String fullPath = getFullPath(table, idx);
        return deserializePage(table, fullPath);
    }

    public static void compactTable(Table table) {
        int numPages = table.getNumPages();

        for (int i = numPages; i > 0; i--) {
            Page p = deserializePage(table, getFullPath(table, i));
            if (p.isEmpty()) {
                deletePage(table, i);
            }
        }

        Page lastPage = deserializePage(table, getFullPath(table, numPages));
        for (int i = 1; i < numPages; i++) {
            Page p = deserializePage(table, getFullPath(table, i));
            if (!p.isFull()) {
                mergePages(p, lastPage);
                savePage(table, p);
                if (lastPage.isEmpty()) {
                    deletePage(table, numPages);
                    lastPage = deserializePage(table, getFullPath(table, i));
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
        serializePage(table, page, getFullPath(table, page.getPageIdx()));
    }

    private static Page deserializePage(Table table, String fullPath) {
        try {
            File file = new File(fullPath);
            if (!file.exists()) {
                return null;
            }

            try (DataInputStream inputStream = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(file)))) {
                int magic = inputStream.readInt();
                if (magic != PAGE_FILE_MAGIC) {
                    throw new IOException("Invalid page file format for " + fullPath);
                }
                return deserializePageSnapshot(table, inputStream);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while serializing a page... Exiting.");
            System.exit(1);
        }
        return null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void serializePage(Table table, Page page, String fullPath) {
        try {
            File file = new File(fullPath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                boolean fileCreated = file.createNewFile();
                if (!fileCreated) {
                    LOGGER.log(Level.SEVERE, "Could not create page file... Exiting.");
                    System.exit(1);
                }
            }
            try (DataOutputStream outputStream = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(file, false)))) {
                outputStream.writeInt(PAGE_FILE_MAGIC);
                outputStream.writeInt(PAGE_FILE_VERSION);
                outputStream.writeInt(page.getPageIdx());
                outputStream.writeInt(page.getMaxRows());
                outputStream.writeInt(page.getNumberOfRows());

                for (RowRecord rowRecord : page.getRecords()) {
                    outputStream.writeInt(rowRecord.getPageId());
                    outputStream.writeInt(rowRecord.getRowId());

                    Vector<Object> values = rowRecord.getAttributeValues();
                    for (int i = 0; i < table.getAttributeList().size(); i++) {
                        writeTypedValue(outputStream, table.getAttributeList().get(i).getType(), values.get(i));
                    }
                }
            }
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
            oos.writeObject(new IndexSnapshot(flattenTree(tree)));

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
        try {
            File file = new File(fullPath);
            if (!file.exists()) {
                return null;
            }

            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            IndexSnapshot snapshot = (IndexSnapshot) ois.readObject();

            ois.close();
            fis.close();
            return rebuildTree(snapshot);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred while deserializing"
                    + " index... Exiting.");
            System.exit(1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Vector<ValueList<?, RowRecord>> flattenTree(BPlusTree<?, RowRecord> tree) {
        Vector<ValueList<?, RowRecord>> flattened = new Vector<>();
        for (ValueList valueList : tree.getAllValueLists()) {
            ValueList copy = new ValueList(valueList.getKey(), valueList.getOneValue());
            for (int i = 1; i < valueList.size(); i++) {
                copy.add(valueList.get(i));
            }
            flattened.add(copy);
        }
        return flattened;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BPlusTree<?, RowRecord> rebuildTree(IndexSnapshot snapshot) {
        BPlusTree tree = new BPlusTree();

        for (ValueList valueList : snapshot.valueLists) {
            for (Object rowRecord : valueList) {
                tree.insert(valueList.getKey(), rowRecord);
            }
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

    private static Page deserializePageSnapshot(Table table, DataInputStream inputStream) throws IOException {
        int version = inputStream.readInt();
        if (version != PAGE_FILE_VERSION) {
            throw new IOException("Unsupported page version " + version);
        }

        int pageIdx = inputStream.readInt();
        int maxRows = inputStream.readInt();
        int rowCount = inputStream.readInt();
        Page page = new Page(table.getName(), maxRows, pageIdx);

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            int pageId = inputStream.readInt();
            int rowId = inputStream.readInt();
            Vector<Object> values = new Vector<>();

            for (int attributeIdx = 0; attributeIdx < table.getAttributeList().size(); attributeIdx++) {
                AttributeType attributeType = table.getAttributeList().get(attributeIdx).getType();
                values.add(readTypedValue(inputStream, attributeType));
            }

            RowRecord rowRecord = new RowRecord(table.getAttributeList(), values);
            rowRecord.setPageId(pageId);
            rowRecord.setRowId(rowId);
            page.addRecord(rowRecord);
        }

        return page;
    }

    private static void writeTypedValue(DataOutputStream outputStream, AttributeType attributeType, Object value)
            throws IOException {
        outputStream.writeBoolean(value == null);
        if (value == null) {
            return;
        }

        switch (attributeType) {
            case INT -> outputStream.writeInt((Integer) value);
            case FLOAT -> outputStream.writeFloat((Float) value);
            case STRING -> writeString(outputStream, (String) value);
        }
    }

    private static Object readTypedValue(DataInputStream inputStream, AttributeType attributeType)
            throws IOException {
        boolean isNull = inputStream.readBoolean();
        if (isNull) {
            return null;
        }

        return switch (attributeType) {
            case INT -> inputStream.readInt();
            case FLOAT -> inputStream.readFloat();
            case STRING -> readString(inputStream);
        };
    }

    private static void writeString(DataOutputStream outputStream, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
    }

    private static String readString(DataInputStream inputStream) throws IOException {
        int length = inputStream.readInt();
        byte[] bytes = inputStream.readNBytes(length);
        if (bytes.length != length) {
            throw new EOFException("Unexpected end of page while reading string value.");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
