package net.org.selector.storer.domain;

/**
 * Created by Selector on 04.05.2015.
 */
public class FileInfo {
    private String name;
    private String url;
    private String size;
    private String height;
    private String width;

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

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileInfo fileInfo = (FileInfo) o;

        if (name != null ? !name.equals(fileInfo.name) : fileInfo.name != null) return false;
        if (url != null ? !url.equals(fileInfo.url) : fileInfo.url != null) return false;
        if (size != null ? !size.equals(fileInfo.size) : fileInfo.size != null) return false;
        if (height != null ? !height.equals(fileInfo.height) : fileInfo.height != null) return false;
        return !(width != null ? !width.equals(fileInfo.width) : fileInfo.width != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (height != null ? height.hashCode() : 0);
        result = 31 * result + (width != null ? width.hashCode() : 0);
        return result;
    }
}
