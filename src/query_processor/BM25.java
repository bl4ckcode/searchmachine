package query_processor;

public class BM25 {
    BM25() {
    }

    /**     * Uses BM25 to compute a weight for a term in a document.
     *
     * @param d         The term frequency in the document
     * @param docLength the document's length
     * @return the score assigned to a document with the given
     * tf and docLength, and other preset parameters
     */
    public final double score(double d, double weigth, float docLength, double averageDocumentLength) {
        double b = 0.35;
        double k_1 = 1.2d;
        double K = k_1 * ((1 - b) + b * docLength / averageDocumentLength) + d;
        K = ((k_1 + 1f) * d / K);
        return K * weigth;
    }

    public final double score(double tf,
                              double numberOfDocuments,
                              double docLength,
                              double averageDocumentLength,
                              double queryFrequency,
                              double documentFrequency) {
        double b = 0.35;
        double k_1 = 1.2d;
        double K = k_1 * ((1 - b) + ((b * docLength) / averageDocumentLength));
        double weight = (((k_1 + 1d) * tf) / (K + tf));    //first part
        double k_3 = 8d;
        weight = weight * (((k_3 + 1) * queryFrequency) / (k_3 + queryFrequency));    //second part

        // multiply the weight with idf
        return weight * Math.log((numberOfDocuments - documentFrequency + 0.5d) / (documentFrequency + 0.5d));
    }
}