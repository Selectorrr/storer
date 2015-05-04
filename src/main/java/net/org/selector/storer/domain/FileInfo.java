package net.org.selector.storer.domain;

/**
 * Created by Selector on 04.05.2015.
 */
public class FileInfo {
    private String name;
    private String url;
    private String size;

    public FileInfo() {
    }

    public FileInfo(String name, String url, String size) {
        this.name = name;
        this.url = url;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileInfo fileInfo = (FileInfo) o;

        if (name != null ? !name.equals(fileInfo.name) : fileInfo.name != null) return false;
        if (url != null ? !url.equals(fileInfo.url) : fileInfo.url != null) return false;
        return !(size != null ? !size.equals(fileInfo.size) : fileInfo.size != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        return result;
    }
}
