package RheaDB;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Table implements Serializable {
    private final String name;
    private final Vector<Attribute> attributeList;
    private final String pageDirectory;
    private int numPages;
    private final int maxTuplesPerPage;

    public Table(String name, Vector<Attribute> attributeList, String pageDirectory, int maxTuplesPerPage) {
        this.name = name;
        this.attributeList = attributeList;
        this.pageDirectory = pageDirectory;
        this.maxTuplesPerPage = maxTuplesPerPage;
        this.numPages = 0;
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
        this.attributeList.add(attribute);
    }

    public int getNumPages() {
        return numPages;
    }

    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }

    public Page getNewPage() {
        this.numPages++;
        return new Page(this.maxTuplesPerPage, this.numPages);
    }

    public Attribute getAttributeWithName(String name) {
        for (Attribute attribute : attributeList) {
            if (attribute.getName().equals(name))
                return attribute;
        }

        return null;
    }
}
