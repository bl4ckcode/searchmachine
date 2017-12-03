package query_processor;

import indexer.Triple;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryProcessor implements Interface.ConsultaListener {
    private static MVMap<Integer, ArrayList<Triple>> indiceInvertido;
    private static HashMap<Integer, Documento> documentoHashMap;
    private static HashMap<String, Integer> vocabulario;

    @SuppressWarnings("unchecked")
    @Override
    public void consultaClickListener(String consulta, javax.swing.JEditorPane painelTexto) {
        try {
            consulta = consulta.replaceAll("\\.", "")
                    .replaceAll(",", "")
                    .replaceAll("!", "")
                    .replaceAll("\\?", "")
                    .replaceAll("[^\\p{IsAlphabetic}\\s]", "")
                    .toLowerCase();

            List<String> termosConsulta = Arrays.asList(consulta.split("\\s+"));

            BufferedReader bufferedReader = new BufferedReader(new FileReader(".."
                    + File.separator + "util.txt"));
            String[] linha = bufferedReader.readLine().split("\\s+");
            int numDocumentos = Integer.parseInt(linha[0]), averageDocLength = Integer.parseInt(linha[1]);
            BM25 bm25 = new BM25();

            HashMap<String, ArrayList<Triple>> map = new HashMap<>();
            for (String termo : termosConsulta) {
                if (vocabulario.containsKey(termo)) {
                    map.put(termo, indiceInvertido.get(vocabulario.get(termo)));
                }
            }

            HashMap<Documento, Double> ranking = new HashMap<>();
            for (String key : map.keySet()) {
                ArrayList<Triple> triples = map.get(key);
                double idf = Math.log(numDocumentos / triples.size() - 1);
                for (Triple tripla : triples) {
                    Documento documento = documentoHashMap.get(tripla.getIdDocumento());
                    if (ranking.containsKey(documento)) {
                        ranking.put(documento, ranking.get(documento) +
                                bm25.score(tripla.getNumOfOcurrences(), numDocumentos,
                                        documento.getLength(), averageDocLength, getQueryFrequency(key, consulta), idf));
                    } else {
                        ranking.put(documento, bm25.score(tripla.getNumOfOcurrences(), numDocumentos,
                                documento.getLength(), averageDocLength, getQueryFrequency(key, consulta), idf));
                    }
                }
            }

            ranking = orderHashMap(ranking);

            ArrayList<Documento> keys = new ArrayList<>(ranking.keySet());
            StringBuilder text = new StringBuilder();
            for (int i = ranking.size() - 1; i >= 0; i--) {
                Documento documento = keys.get(i);
                String fileName = "<br>" + documento.getFileName();
                String url = " <a href=" + documento.getUrl() + "\">" + documento.getUrl() + "</a></br>";
                text.append(fileName).append("                ").append(url);
            }

            painelTexto.setText(text.toString());
            painelTexto.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static LinkedHashMap<Documento, Double> orderHashMap(HashMap<Documento, Double> originalHashMap) {
        LinkedHashMap<Documento, Double> sortedHashMapByKeys = new LinkedHashMap<>();
        TreeMap<Documento, Double> originalTreeMap = new TreeMap<>(originalHashMap);

        for (Map.Entry<Documento, Double> map : originalTreeMap.entrySet()) {
            sortedHashMapByKeys.put(map.getKey(), map.getValue());
        }

        LinkedHashMap<Double, Documento> reversedOfSortedLinkedHashMap = new LinkedHashMap<>();
        for (Map.Entry<Documento, Double> map : sortedHashMapByKeys.entrySet()) {
            reversedOfSortedLinkedHashMap.put(map.getValue(), map.getKey());
        }

        LinkedHashMap<Documento, Double> finalMap = new LinkedHashMap<>();
        TreeMap<Double, Documento> treeMapOfReversedOfSortedLinkedHashMap = new TreeMap<>(reversedOfSortedLinkedHashMap);
        for (Map.Entry<Double, Documento> map : treeMapOfReversedOfSortedLinkedHashMap.entrySet()) {
            finalMap.put(map.getValue(), map.getKey());
        }

        return finalMap;
    }

    private int getQueryFrequency(String termo, String consulta) {
        int i = 0;
        Pattern p = Pattern.compile(termo);
        Matcher m = p.matcher(consulta);

        while (m.find()) {
            i++;
        }

        return i;
    }

    private void initInterface() {
        Interface itfc = new Interface(this);
        itfc.setExtendedState(Frame.MAXIMIZED_BOTH);
        itfc.setLocationRelativeTo(null);
        itfc.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        int flag = Integer.parseInt(args[0]);

        if (flag == 0) {
            MVStore s = MVStore.open(".." + File.separator + "invertedIndexCrawler.db");
            indiceInvertido = s.openMap("invertedIndexCrawler");
        } else {
            MVStore s = MVStore.open(".." + File.separator + "invertedIndexW10G.db");
            indiceInvertido = s.openMap("invertedIndexW10G");
        }

        try {
            FileInputStream fis = new FileInputStream(".." + File.separator + "vocabulario.properties");
            ObjectInputStream ois = new ObjectInputStream(fis);
            vocabulario = (HashMap) ois.readObject();
            ois.close();

            fis = new FileInputStream(".." + File.separator + "documentos.properties");
            ois = new ObjectInputStream(fis);
            documentoHashMap = (HashMap) ois.readObject();
            ois.close();

            QueryProcessor queryProcessor = new QueryProcessor();
            queryProcessor.initInterface();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
