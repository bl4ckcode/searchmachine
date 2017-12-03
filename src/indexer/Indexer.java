package indexer;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import query_processor.Documento;

import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Indexer {
    private static final String[] stopwords = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i",
            "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "x", "w", "y", "z"};
    private static final Pattern urlPattern = Pattern.compile(
            "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                    + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                    + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private HashMap<Integer, Documento> documentoHashMap = new HashMap<>();
    private HashMap<String, Integer> vocabulario = new HashMap<>();
    private MVMap<Integer, ArrayList<Triple>> indiceInvertido;
    private MVStore s;

    @SuppressWarnings("unchecked")
    public Indexer(boolean fromCrawler) {
        s = new MVStore.Builder().fileName(
                fromCrawler ? ".." + File.separator + "invertedIndexCrawler.db"
                        : ".." + File.separator + "invertedIndexW10G.db").autoCommitDisabled().open();
        indiceInvertido = s.openMap(fromCrawler ? "invertedIndexCrawler" : "invertedIndexW10G");
    }

    private static String readFile(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();

        String line = reader.readLine();

        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = reader.readLine();
        }

        return sb.toString();
    }

    private static int hasTriple(ArrayList<Triple> triples, int idDocumento) {
        for (Triple triple : triples) {
            if (triple.getIdDocumento() == idDocumento)
                return triples.indexOf(triple);
        }

        return -1;
    }


    public void indexFromCrawler(HashMap<Integer, String> linksVisitados) {
        BufferedReader reader;
        int idDocumento = 0, documentosIndexados = 0, contadorDocumentosIndexados = 0,
                tamanhoMedioDocumentos = 0, posIndice = 0;
        File pastaArquivos = new File(".." + File.separator + "documentos" + File.separator + "crawler");
        String[] htmlFilenames = pastaArquivos.list();
        long startTimeForDocuments = System.currentTimeMillis();
        if (htmlFilenames != null) {
            try {
                for (String fileName : htmlFilenames) {
                    reader = new BufferedReader(new FileReader(".." + File.separator
                            + "documentos" + File.separator + "crawler" + File.separator + fileName));
                    Document document = Jsoup.parse(readFile(reader));
                    String texto = Jsoup.parse(document.toString()).text();
                    int tamanhoDocumento = texto.getBytes("UTF-8").length;
                    documentoHashMap.put(idDocumento, new Documento(fileName,
                            linksVisitados.get(
                                    Integer.parseInt(fileName.substring(0, fileName.indexOf(".")))),
                            tamanhoDocumento));

                    tamanhoMedioDocumentos += tamanhoDocumento;

                    String filteredText = texto
                            .replaceAll("\\.", "")
                            .replaceAll(",", "")
                            .replaceAll("!", "")
                            .replaceAll("\\?", "")
                            .replaceAll("[^\\p{IsAlphabetic}\\s]", "")
                            .toLowerCase();

                    //Remover acentos e preservar caractere
                    filteredText = Normalizer.normalize(filteredText, Normalizer.Form.NFD);
                    filteredText = filteredText.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

                    //Tokenizar
                    List<String> tokens = new ArrayList<>(Arrays.asList(filteredText.split("\\s+")));

                    //Remover stopwords
                    List<String> stopWordsList = new ArrayList<>(Arrays.asList(stopwords));
                    tokens.removeAll(stopWordsList);

                    for (String token : tokens) {
                        if (vocabulario.containsKey(token)) {
                            int posTriplas, posPreIndice = vocabulario.get(token);
                            ArrayList<Triple> triplas = indiceInvertido.get(posPreIndice);

                            if ((posTriplas = hasTriple(triplas, idDocumento)) == -1) {
                                triplas.add(new Triple(idDocumento, 1));
                                indiceInvertido.put(posPreIndice, triplas);
                                vocabulario.put(token, posPreIndice);
                            } else {
                                Triple triple = triplas.get(posTriplas);
                                triple.incrementOcurrences();
                                triplas.set(posTriplas, triple);
                                indiceInvertido.put(vocabulario.get(token), triplas);
                            }
                        } else {
                            ArrayList<Triple> triplas = new ArrayList<>();
                            Triple triple = new Triple(idDocumento, 1);
                            triplas.add(triple);
                            vocabulario.put(token, posIndice);
                            indiceInvertido.put(posIndice, triplas);
                            posIndice++;
                        }

                        s.commit();
                    }

                    idDocumento++;

                    contadorDocumentosIndexados++;
                    documentosIndexados++;


                    PrintWriter fileWriter = new PrintWriter(new FileWriter(".." + File.separator + "util.txt", false));
                    String line = "" + documentosIndexados + " "
                            + (tamanhoMedioDocumentos / documentosIndexados);
                    fileWriter.append(line);
                    fileWriter.close();


                    if (contadorDocumentosIndexados == 50) {
                        contadorDocumentosIndexados = 0;

                        System.out.println("Levou " +
                                (((System.currentTimeMillis() - startTimeForDocuments) / 1000) / 60)
                                + " minutos para indexar " + documentosIndexados + " documentos");
                    }
                }
            } catch (IOException ignored) {
            }

            s.close();

            try {
                //Salvar vocabulario para processador de consultas
                FileOutputStream fos = new FileOutputStream(".." + File.separator + "vocabulario.properties");
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(vocabulario);
                oos.close();

                fos = new FileOutputStream(".." + File.separator + "documentos.properties");
                oos = new ObjectOutputStream(fos);
                oos.writeObject(documentoHashMap);
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void indexWT10G() {
        BufferedReader reader;
        int posIndice = 0, documentosIndexados = 0, contadorDocumentosIndexados = 0, tamanhoMedioDocumentos = 0,
                idDocumento = 0;
        File pastaArquivos = new File(".." + File.separator + "documentos" + File.separator + "WT10G");
        File[] diretorios = pastaArquivos.listFiles();
        long startTimeForDocuments = System.currentTimeMillis();
        if (diretorios != null) {
            for (File diretorioAtual : diretorios) {
                String[] documentos = diretorioAtual.list();

                if (documentos != null) {
                    for (String fileName : documentos) {
                        try {
                            if (!fileName.equals(".DS_Store") && !fileName.equals("crawler")) {
                                reader = new BufferedReader(new FileReader(".." + File.separator + "documentos"
                                        + File.separator + "WT10G" + File.separator +
                                        diretorioAtual.getName() + File.separator + fileName));
                                Document document = Jsoup.parse(readFile(reader), "", Parser.xmlParser());

                                Elements docs = document.select("DOC");
                                for (Element doc : docs) {
                                    String fn = doc.getElementsByTag("DOCNO").text();
                                    String url = "";
                                    String dochdr = doc.getElementsByTag("DOCHDR").text();

                                    Matcher matcher = urlPattern.matcher(
                                            doc.getElementsByTag("DOCHDR").text());
                                    while (matcher.find()) {
                                        int matchStart = matcher.start(1);
                                        int matchEnd = matcher.end();

                                        url = dochdr.substring(matchStart, matchEnd);
                                    }

                                    StringBuilder textWithHtml = new StringBuilder();

                                    for (Node child : doc.childNodes()) {
                                        String childText = child.toString();
                                        if (!childText.contains("<DOCNO>")
                                                && !childText.contains("<DOCOLDNO>")
                                                && !childText.contains("<DOCHDR>"))
                                            textWithHtml.append(childText);
                                    }

                                    String texto = Jsoup.parse(textWithHtml.toString()).text();
                                    int tamanhoDocumento = texto.getBytes("UTF-8").length;
                                    documentoHashMap.put(idDocumento, new Documento(fn, url, tamanhoDocumento));

                                    tamanhoMedioDocumentos += tamanhoDocumento;

                                    String filteredText = texto.replaceAll("\\.", "")
                                            .replaceAll(",", "")
                                            .replaceAll("!", "")
                                            .replaceAll("\\?", "")
                                            .replaceAll("[^\\p{IsAlphabetic}\\s]", "")
                                            .toLowerCase();

                                    //Remover acentos e preservar caractere
                                    filteredText = Normalizer.normalize(filteredText, Normalizer.Form.NFD);
                                    filteredText = filteredText.
                                            replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

                                    //Tokenizar
                                    List<String> tokens = new ArrayList<>(Arrays.
                                            asList(filteredText.split("\\s+")));

                                    //Remover stopwords
                                    List<String> stopWordsList = new ArrayList<>(Arrays.asList(stopwords));
                                    tokens.removeAll(stopWordsList);

                                    for (String token : tokens) {
                                        if (vocabulario.containsKey(token)) {
                                            int posTriplas, posPreIndice = vocabulario.get(token);
                                            ArrayList<Triple> triplas = indiceInvertido.get(posPreIndice);

                                            if ((posTriplas = hasTriple(triplas, idDocumento)) == -1) {
                                                triplas.add(new Triple(idDocumento, 1));
                                                indiceInvertido.put(posPreIndice, triplas);
                                                vocabulario.put(token, posPreIndice);
                                            } else {
                                                Triple triple = triplas.get(posTriplas);
                                                triple.incrementOcurrences();
                                                triplas.set(posTriplas, triple);
                                                indiceInvertido.put(vocabulario.get(token), triplas);
                                            }
                                        } else {
                                            ArrayList<Triple> triplas = new ArrayList<>();
                                            Triple triple = new Triple(idDocumento, 1);
                                            triplas.add(triple);
                                            vocabulario.put(token, posIndice);
                                            indiceInvertido.put(posIndice, triplas);
                                            posIndice++;
                                        }

                                        s.commit();
                                    }

                                    idDocumento++;

                                    contadorDocumentosIndexados++;
                                    documentosIndexados++;

                                    PrintWriter fileWriter =
                                            new PrintWriter(new FileWriter(".."
                                                    + File.separator + "util.txt", false));
                                    String line = "" + documentosIndexados + " "
                                            + (tamanhoMedioDocumentos / documentosIndexados);
                                    fileWriter.write(line);
                                    fileWriter.close();

                                    if (contadorDocumentosIndexados == 50) {
                                        contadorDocumentosIndexados = 0;

                                        System.out.println("Levou " +
                                                (((System.currentTimeMillis() - startTimeForDocuments) / 1000) / 60)
                                                + " minutos para indexar " + documentosIndexados + " documentos");
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    s.close();

                    try {
                        //Salvar vocabulario para processador de consultas
                        FileOutputStream fos = new FileOutputStream(".." + File.separator
                                + "vocabulario.properties");
                        ObjectOutputStream oos = new ObjectOutputStream(fos);
                        oos.writeObject(vocabulario);
                        oos.close();

                        fos = new FileOutputStream(".." + File.separator + "documentos.properties");
                        oos = new ObjectOutputStream(fos);
                        oos.writeObject(documentoHashMap);
                        oos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.out.println("Não existem documentos para indexar!");
        }
    }

    public static void main(String[] args) {
        Indexer indexer = new Indexer(false);
        indexer.indexWT10G();
    }
}
