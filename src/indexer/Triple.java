package indexer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Triple implements Serializable {
    private int idDocumento;
    private int numOfOcurrences;

    Triple(int idDocumento, int numOfOcurrences) {
        this.idDocumento = idDocumento;
        this.numOfOcurrences = numOfOcurrences;
    }

    public int getNumOfOcurrences() {
        return numOfOcurrences;
    }
    public int getIdDocumento() {
        return idDocumento;
    }
    void incrementOcurrences() {
        numOfOcurrences++;
    }
}
