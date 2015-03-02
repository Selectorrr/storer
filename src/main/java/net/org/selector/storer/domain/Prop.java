package net.org.selector.storer.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * SLitvinov on 02.03.2015.
 */
@Document(collection = "T_FILES_PROPS")
public class Prop {
    @Id
    private String key;
    private Object value;

    public Prop() {
    }

    public Prop(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Prop prop = (Prop) o;

        if (key != null ? !key.equals(prop.key) : prop.key != null) return false;
        if (value != null ? !value.equals(prop.value) : prop.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Prop{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
