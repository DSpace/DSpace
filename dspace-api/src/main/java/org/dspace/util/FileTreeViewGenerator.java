/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
/**
 * Generate a tree view of the file in a bitstream
 *
 * @author longtv
 */
public class FileTreeViewGenerator {
    private FileTreeViewGenerator () {
    }

    public static List<FileInfo> parse(String data) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(data)));
        Element rootElement = document.getDocumentElement();
        NodeList nl = rootElement.getChildNodes();
        FileInfo root = new FileInfo("root");
        Node n = nl.item(0);
        do {
            String fileInfo = n.getFirstChild().getTextContent();
            String f[] = fileInfo.split("\\|");
            String fileName = "";
            String path = f[0];
            long size = Long.parseLong(f[1]);
            if (!path.endsWith("/")) {
                fileName = path.substring(path.lastIndexOf('/') + 1);
                if (path.lastIndexOf('/') != -1) {
                    path = path.substring(0, path.lastIndexOf('/'));
                } else {
                    path = "";
                }
            }
            FileInfo current = root;
            for (String p : path.split("/")) {
                if (current.sub.containsKey(p)) {
                    current = current.sub.get(p);
                } else {
                    FileInfo temp = new FileInfo(p);
                    current.sub.put(p, temp);
                    current = temp;
                }
            }
            if (!fileName.isEmpty()) {
                FileInfo temp = new FileInfo(fileName, humanReadableFileSize(size));
                current.sub.put(fileName, temp);
            }
        } while ((n = n.getNextSibling()) != null);
        return new ArrayList<>(root.sub.values());
    }
    public static String humanReadableFileSize(long bytes) {
        int thresh = 1024;
        if (Math.abs(bytes) < thresh) {
            return bytes + " B";
        }
        String units[] = {"kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        int u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while (Math.abs(bytes) >= thresh && u < units.length - 1);
        return bytes + " " + units[u];
    }
}
