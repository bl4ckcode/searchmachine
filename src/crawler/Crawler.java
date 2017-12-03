package crawler;

import indexer.Indexer;
import org.h2.mvstore.MVStore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class Crawler extends Thread {
    private static HashMap<Integer, String> visitedLinks = new HashMap<>();
    private static ArrayList<String> linksToVisit = new ArrayList<>();
    private static int idArquivo = 0;

    public Crawler() {
        //noinspection ResultOfMethodCallIgnored
        new File(".." + File.separator + "documentos" + File.separator + "crawler").mkdirs();

        //Sementes Iniciais
        linksToVisit.add("https://www.reddit.com/");
        linksToVisit.add("https://http://www.globo.com/");
    }

    public static void main(String[] args) {
        Crawler crawler = new Crawler();
        crawler.start();
    }

    @Override
    public void run() {
        RobotResolver robotResolver = new RobotResolver();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < linksToVisit.size(); i++) {
            String hyperlink = linksToVisit.get(i);
            linksToVisit.remove(hyperlink);

            try {
                //Garantir que nao vamos visitar links proibidos/já visitados
                if (!visitedLinks.containsValue(hyperlink)) {
                    if (robotResolver.robotSafe(new URL(hyperlink))) {
                        try {
                            Document document = Jsoup.connect(hyperlink).get();
                            //Remover as tags HTML e salvar apenas o texto
                            Elements links = document.select("a[href]");

                            links.parallelStream().forEach(link -> linksToVisit.add(link.attr("href")));

                            StringBuilder HTML = new StringBuilder(document.text());
                            new Thread(() -> {
                                try {
                                    PrintWriter writer = new PrintWriter(".." + File.separator + "documentos"
                                            + File.separator + "crawler" + File.separator
                                            + idArquivo + ".html", "UTF-8");
                                    HTML.append(document.text()).append("\n");
                                    writer.append(HTML);
                                    writer.close();
                                    idArquivo++;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        } catch (IOException ignored) {
                        }

                        visitedLinks.put(idArquivo, hyperlink);
                    }

                    if (visitedLinks.size() >= 50) {
                        System.out.println("Levou " +
                                (((System.currentTimeMillis() - startTime) / 1000) / 60)
                                + " minutos para coletar 1000 documentos");

                        Indexer indexer = new Indexer(true);
                        indexer.indexFromCrawler(visitedLinks);

                        System.exit(0);
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }
}
