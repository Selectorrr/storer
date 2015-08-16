package net.org.selector.storer.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by Selector on 16.08.2015.
 */
@Document(collection = "T_SEQUENCE")
public class SequenceId {
    @Id
    private String id;
    private long seq;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SequenceId that = (SequenceId) o;

        if (seq != that.seq) return false;
        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (int) (seq ^ (seq >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "SequenceId{" +
            "id='" + id + '\'' +
            ", seq=" + seq +
            '}';
    }
}
