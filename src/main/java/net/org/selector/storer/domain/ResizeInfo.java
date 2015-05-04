package net.org.selector.storer.domain;

import java.io.ByteArrayInputStream;

/**
 * Created by Selector on 05.05.2015.
 */
public class ResizeInfo {
    private String name;
    private String contentType;
    private ByteArrayInputStream byteArrayInputStream;
    private String height;
    private String width;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public ByteArrayInputStream getByteArrayInputStream() {
        return byteArrayInputStream;
    }

    public void setByteArrayInputStream(ByteArrayInputStream byteArrayInputStream) {
        this.byteArrayInputStream = byteArrayInputStream;
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

        ResizeInfo imageInfo = (ResizeInfo) o;

        if (name != null ? !name.equals(imageInfo.name) : imageInfo.name != null) return false;
        if (contentType != null ? !contentType.equals(imageInfo.contentType) : imageInfo.contentType != null)
            return false;
        if (byteArrayInputStream != null ? !byteArrayInputStream.equals(imageInfo.byteArrayInputStream) : imageInfo.byteArrayInputStream != null)
            return false;
        if (height != null ? !height.equals(imageInfo.height) : imageInfo.height != null) return false;
        return !(width != null ? !width.equals(imageInfo.width) : imageInfo.width != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + (byteArrayInputStream != null ? byteArrayInputStream.hashCode() : 0);
        result = 31 * result + (height != null ? height.hashCode() : 0);
        result = 31 * result + (width != null ? width.hashCode() : 0);
        return result;
    }
}
