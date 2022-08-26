/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.testing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Simple class to transform a medline.xml file from PubMed into DSpace import package(s).
 *
 * <p>
 * This is a distinctly incomplete implementation - it doesn't even attempt to map a number of fields,
 * and has no means of customizing the mapping. More importantly, it makes assumptions in parsing the xml
 * that would be problematic for a production instance.
 *
 * <p>
 * However, it does use SAX parsing, which means it has no problems with handling a 1GB+ input file.
 * This means it is a good way to generate a large number of realistic import packages very quickly -
 * simply go to http://www.ncbi.nlm.nih.gov/pubmed and search for something that returns a lot of records
 * ('nature' returns over 300,000 for example). Download the results as a medline.xml (and yes, it will attempt
 * to download all 300,000+ into a single file), and then run this class over that file to spit out import packages
 * which can then be loaded into DSpace using ItemImport.
 */
public class PubMedToImport {
    private static final Logger log = LogManager.getLogger(PubMedToImport.class);

    private static File outputDir = null;

    /**
     * Default constructor
     */
    private PubMedToImport() { }

    public static void main(String args[]) {
        Options options = new Options();

        options.addOption(new Option("s", "source", true, "Source xml"));
        options.addOption(new Option("o", "output", true, "Output directory"));

        try {
            CommandLine cli = new DefaultParser().parse(options, args);

            String source = cli.getOptionValue("s");
            String output = cli.getOptionValue("o");

            if (!new File(source).exists()) {
                throw new IllegalArgumentException("Source file does not exist");
            }

            outputDir = new File(output);
            if (outputDir.exists()) {
                if (outputDir.list().length > 0) {
                    throw new IllegalStateException("Output directory must be empty");
                }
            } else {
                if (!outputDir.mkdirs()) {
                    throw new IllegalStateException("Unable to create output directory");
                }
            }

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse(source, new PubMedHandler());

        } catch (Exception e) {
            // ignore
        }
    }

    private static class PubMedHandler extends DefaultHandler {
        private static int recordCount = 1;
        private static List<MockMetadataValue> dcValues;

        private static StringBuilder value;
        private static StringBuilder lastName;
        private static StringBuilder firstName;

        private static boolean isCorrection = false;
        private static boolean isLastName = false;
        private static boolean isFirstName = false;

        private static void addDCValue(String element, String qualifier, String value) {
            if (dcValues == null) {
                dcValues = new ArrayList<>();
            }

            MockMetadataValue thisValue = new MockMetadataValue();
            thisValue.schema = "dc";
            thisValue.element = element;
            thisValue.qualifier = qualifier;
            thisValue.value = value;

            dcValues.add(thisValue);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
            if ("PubmedArticle".equals(qName)) {
                System.out.println("Starting record " + recordCount);
            } else if ("CommensCorrectionsList".equals(qName)) {
                isCorrection = true;
            } else if ("ForeName".equals(qName)) {
                isFirstName = true;
                firstName = new StringBuilder();
            } else if ("LastName".equals(qName)) {
                isLastName = true;
                lastName = new StringBuilder();
            } else {
                value = new StringBuilder();
            }

            super.startElement(uri, localName, qName, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (!isCorrection) {
                if ("PMID".equals(qName)) {
                    addDCValue("identifier", null, value.toString());
                } else if ("ISSN".equals(qName)) {
                    addDCValue("identifier", "issn", value.toString());
                } else if ("ArticleTitle".equals(qName)) {
                    addDCValue("title", null, value.toString());
                } else if ("AbstractText".equals(qName)) {
                    addDCValue("description", "abstract", value.toString());
                } else if ("PublicationType".equals(qName)) {
                    addDCValue("type", null, value.toString());
                } else if ("Author".equals(qName)) {
                    addDCValue("contributor", "author", lastName + ", " + firstName);
                } else if ("DescriptorName".equals(qName)) {
                    addDCValue("subject", "mesh", value.toString());
                }
            } else {
                if ("MedlineCitation".equals(qName)) {
                    isCorrection = false;
                }
            }

            if ("PubmedArticle".equals(qName)) {
                try {
                    writeItem();
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to export record", e);
                }
                System.out.println("Ending record " + recordCount);
                recordCount++;
            }

            isFirstName = false;
            isLastName = false;
            super.endElement(uri, localName, qName);
        }

        @Override
        public void characters(char[] chars, int start, int length) throws SAXException {
            if (isFirstName) {
                firstName.append(chars, start, length);
//                firstName = String.copyValueOf(chars, start, length);
            } else if (isLastName) {
                lastName.append(chars, start, length);
//                lastName = String.copyValueOf(chars, start, length);
            } else {
                value.append(chars, start, length);
//                value = String.copyValueOf(chars, start, length);
            }

            super.characters(chars, start, length);
        }

        private void writeItem() throws IOException {
            File itemDir = new File(outputDir, String.valueOf(recordCount));
            itemDir.mkdirs();

            new File(itemDir, "contents").createNewFile();

            Document doc = new Document();
            Element root = new Element("dublin_core");

            doc.setRootElement(root);

            for (MockMetadataValue dcValue : dcValues) {
                Element dcNode = new Element("dcvalue");

                dcNode.setAttribute("element", dcValue.element);

                if (!StringUtils.isEmpty(dcValue.qualifier)) {
                    dcNode.setAttribute("qualifier", dcValue.qualifier);
                }

                dcNode.setText(dcValue.value);

                root.addContent(dcNode);
            }


            File dc = new File(itemDir, "dublin_core.xml");
            XMLOutputter dcOutput = new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8"));
            OutputStream out = null;
            try {
                out = new BufferedOutputStream(new FileOutputStream(dc));
                dcOutput.output(doc, out);
            } finally {
                if (out != null) {
                    out.close();
                }
            }

            dcValues.clear();
        }
    }


    protected static class MockMetadataValue {
        public String schema;
        public String element;
        public String qualifier;
        public String value;
    }
}
