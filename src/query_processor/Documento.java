package query_processor;

import java.io.Serializable;

public class Documento implements Serializable, Comparable<Documento> {
    private String url;
    private String fileName;
    private int length;

    public Documento(String fileName, String url, int length) {
        this.url = url;
        this.fileName = fileName;
        this.length = length;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public int compareTo(Documento o) {
        int fileName = this.fileName.compareTo(o.fileName);
        return fileName == 0 ? this.fileName.compareTo(o.fileName) : fileName;
    }
}
