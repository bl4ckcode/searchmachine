package crawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public class RobotResolver {
    private String userAgent;
    private String rule;

    RobotResolver() {
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        result.append(this.getClass().getName()).append(" Object {").append(NEW_LINE);
        result.append("   userAgent: ").append(this.userAgent).append(NEW_LINE);
        result.append("   rule: ").append(this.rule).append(NEW_LINE);
        result.append("}");
        return result.toString();
    }

    boolean robotSafe(URL url) {
        String strHost = url.getHost();
        final String DISALLOW = "Disallow";

        String strRobot = "http://" + strHost + "/robots.txt";
        URL urlRobot;
        try {
            urlRobot = new URL(strRobot);
        } catch (MalformedURLException e) {
            return false;
        }

        String strCommands;
        try {
            if(urlRobot.openStream() != null) {
                InputStream urlRobotStream = urlRobot.openStream();
                if (urlRobotStream != null) {
                    byte b[] = new byte[1000];
                    int numRead = urlRobotStream.read(b);
                    strCommands = new String(b, 0, numRead == -1 ? 0 : numRead);
                    while (numRead != -1) {
                        numRead = urlRobotStream.read(b);
                        if (numRead != -1) {
                            String newCommands = new String(b, 0, numRead);
                            //noinspection StringConcatenationInLoop
                            strCommands += newCommands;
                        }
                    }
                    urlRobotStream.close();

                    if (strCommands.contains(DISALLOW)) {
                        String[] split = strCommands.split("\n");
                        ArrayList<RobotResolver> robotRules = new ArrayList<>();
                        String mostRecentUserAgent = null;
                        for (String aSplit : split) {
                            String line = aSplit.trim();
                            if (line.toLowerCase().startsWith("user-agent")) {
                                int start = line.indexOf(":") + 1;
                                int end = line.length();
                                mostRecentUserAgent = line.substring(start, end).trim();
                            } else if (line.startsWith(DISALLOW)) {
                                if (mostRecentUserAgent != null) {
                                    RobotResolver r = new RobotResolver();
                                    r.userAgent = mostRecentUserAgent;
                                    int start = line.indexOf(":") + 1;
                                    int end = line.length();
                                    r.rule = line.substring(start, end).trim();
                                    robotRules.add(r);
                                }
                            }
                        }

                        for (RobotResolver robotRule : robotRules) {
                            String path = url.getPath();
                            if (robotRule.rule.length() == 0) return true;
                            if (Objects.equals(robotRule.rule, "/")) return false;
                            if (robotRule.rule.length() <= path.length()) {
                                String pathCompare = path.substring(0, robotRule.rule.length());
                                if (pathCompare.equals(robotRule.rule)) return false;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
