package RheaDB;

import java.io.Serializable;
import java.util.Vector;

public class Table implements Serializable {
    private final String name;
    private final Vector<Attribute> attributeList;
    private final String pageDirectory;
    private int numPages;
    private final int maxTuplesPerPage;

    public Table(String name, Vector<Attribute> attributeList,
                 String pageDirectory, int maxTuplesPerPage) {
        this.name = name;
        this.attributeList = attributeList;
        this.pageDirectory = pageDirectory;
        this.maxTuplesPerPage = maxTuplesPerPage;
        this.numPages = 0;
    }

    public void popPage() {
        assert numPages > 0;
        this.numPages--;
    }

    public Table(String name, String pageDirectory, int maxTuplesPerPage) {
        this(name, new Vector<>(), pageDirectory, maxTuplesPerPage);
    }

    public Vector<Attribute> getAttributeList() {
        return attributeList;
    }

    public String getName() {
        return name;
    }

    public String getPageDirectory() {
        return pageDirectory;
    }

    public void addAttribute(Attribute attribute) {
        attributeList.add(attribute);
    }

    public int getNumPages() {
        return numPages;
    }

    public Page getNewPage() {
        numPages++;
        return new Page(name, maxTuplesPerPage, numPages);
    }

    public Attribute getAttributeWithName(String name) {
        return attributeList
                .stream()
                .filter(a -> a.getName().equals(name))
                .findAny()
                .orElse(null);
    }
}
