/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.authority;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * CLI for the authority subsystem.
 *
 * @author mhwood
 */
public class Commands {
    private static final ConfigurationService CFG = new DSpace().getConfigurationService();

    /** Null constructor for a CLI class. */
    private Commands() {}

    public static void main (String[] argv)
            throws IOException,
                   SolrServerException,
                   XMLStreamException,
                   ParserConfigurationException,
                   SAXException {
        // Parse the command
        final String OPT_DUMP    = "d";
        final String OPT_RESTORE = "r";
        final String OPT_HELP    = "h";
        final String OPT_CLEAR   = "c";

        OptionGroup verbs = new OptionGroup();
        verbs.addOption(new Option(OPT_DUMP, "dump", false,
                "Export all records as XML on standard out"));
        verbs.addOption(new Option(OPT_RESTORE, "restore", false,
                "Import records from XML on standard in"));
        verbs.addOption(new Option(OPT_HELP, "help", false,
                "Describe all options"));
        verbs.setRequired(true);

        Options options = new Options();
        options.addOptionGroup(verbs);
        options.addOption(OPT_CLEAR, "clear", false,
                "(only with --restore) delete all existing authority records.");

        CommandLine command = null;
        try {
            command = new DefaultParser().parse(options, argv);
        } catch (ParseException e) {
            System.err.format("Unrecognized command:%n%s%n", e.getMessage());
            giveHelp(options);
            System.exit(1);
        }

        // Command is 'help'?
        if (command.hasOption(OPT_HELP)) {
            giveHelp(options);
            System.exit(0);
        }

        // Locate the 'authority' Solr core.
        String collectionURL = CFG.getProperty("solr.authority.server");
        if (StringUtils.isBlank(collectionURL)) {
            System.err.println("Cannot continue:  solr.authority.server is not configured.");
            System.exit(1);
        }

        // Interpret command.
        if (command.hasOption(OPT_DUMP)) {
            if (command.hasOption(OPT_CLEAR)) {
                System.err.println("--clear is ignored by --dump");
            }
            HttpSolrServer solr = new HttpSolrServer(collectionURL);
            try ( Writer output = new OutputStreamWriter(System.out, StandardCharsets.UTF_8); ) {
                dump(solr, output);
            }
            solr.shutdown();
        } else if (command.hasOption(OPT_RESTORE)) {
            HttpSolrServer solr = new HttpSolrServer(collectionURL);
            if (command.hasOption(OPT_CLEAR)) {
                System.err.println("Clearing all authority records");
                solr.deleteByQuery("*:*", 0);
            }
            restore(solr, System.in);
            solr.shutdown();
        } else {
            System.err.println("Unknown command");
            giveHelp(options);
            System.exit(1);
        }

        System.exit(0);
    }

    private static void giveHelp(Options options) {
        new HelpFormatter().printHelp("dspace authority [--dump | --restore]", options);
    }

    /**
     * Serialize all records from a Solr core to a simple XML document.
     *
     * @param solr connection to Solr.  Should be focused on a single core/collection.
     * @param output XML document written here.
     * @throws IOException passed through.
     * @throws SolrServerException passed through.
     * @throws XMLStreamException passed through.
     */
    static private void dump(HttpSolrServer solr, Writer output)
            throws IOException, SolrServerException, XMLStreamException {
        // Set up to write XML.
        XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance()
                .createXMLStreamWriter(output);
        xmlWriter.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
        xmlWriter.writeStartElement(ParseEventHandler.ELEMENT_ROOT);

        // Set up the query
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", "*:*");

        // For all Solr records.
        for (SolrDocument document : new SolrQueryWindow(solr, params)) {
            //    Start a record element.
            xmlWriter.writeStartElement(ParseEventHandler.ELEMENT_RECORD);
            //    For all fields.
            for (String fieldName : document.getFieldNames()) {
                //      For all field values.
                for (Object fieldValue : document.getFieldValues(fieldName)) {
                    xmlWriter.writeStartElement(ParseEventHandler.ELEMENT_FIELD);
                    xmlWriter.writeAttribute(ParseEventHandler.ATTRIBUTE_NAME, fieldName);
                    xmlWriter.writeCharacters(fieldValue.toString());
                    xmlWriter.writeEndElement(); // Field (f)
                }
            }
            //    End the record element.
            xmlWriter.writeEndElement(); // Record (r)
        }

        // Close up XML.
        xmlWriter.writeEndElement(); // Root
        xmlWriter.close();

        // Finished!
    }

    /**
     * Read a valid authority dump document and create Solr documents from it.
     * Uses {@link ParseEventHandler} to update Solr.
     *
     * @param solr connection to the authority core.
     * @param input the dump document.
     * @throws ParserConfigurationException passed through.
     * @throws SAXException passed through.
     * @throws IOException passed through.
     */
    private static void restore(HttpSolrServer solr, InputStream input)
            throws ParserConfigurationException, SAXException, IOException {
        // Load the schema.
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(Commands.class.getResource("authority-dump.xsd"));

        // Configure a parser factory.
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setValidating(false); // No DTD validation
        parserFactory.setSchema(schema); // XML Schema validation
        parserFactory.setNamespaceAware(false); // No namespaces
        parserFactory.setXIncludeAware(false); // No XInclude processing

        // Create and run a parser.  ContentHandler will update Solr.
        SAXParser parser = parserFactory.newSAXParser();
        ParseEventHandler handler = new ParseEventHandler(solr);
        parser.parse(input, handler);

        System.out.format("%d authority records loaded.%n", handler.getNRecords());
    }

    /**
     * Handle SAX content events and errors when parsing a dump document.
     */
    private static class ParseEventHandler
            extends DefaultHandler {
        private final HttpSolrServer solr;
        private final StringBuilder currentValue = new StringBuilder();
        private Locator locator;
        private SolrInputDocument solrInputDocument;
        private String currentField;
        private long nRecords = 0;

        private static final String ELEMENT_ROOT = "authority-dump";
        private static final String ELEMENT_RECORD = "r";
        private static final String ELEMENT_FIELD = "f";
        private static final String ATTRIBUTE_NAME = "name";

        /**
         * Save a reference to a connection to a Solr core/collection.
         *
         * @param solr Connection to Solr.
         */
        ParseEventHandler(HttpSolrServer solr) {
            this.solr = solr;
        }

        /**
         * Get the current record count.
         * @return number of "records" (Solr documents) loaded so far.
         */
        long getNRecords() {
            return nRecords;
        }

        /*
         * ContentHandler
         */

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            if (ELEMENT_FIELD.equals(localName)) {
                currentField = atts.getValue(ATTRIBUTE_NAME);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if (ELEMENT_RECORD.equals(qName)) {
                try {
                    solr.add(solrInputDocument);
                    solrInputDocument.clear();
                    nRecords++;
                } catch (SolrServerException | IOException ex) {
                    throw new SAXException("Could not add a record", ex);
                }
            } else if (ELEMENT_FIELD.equals(qName)) {
                solrInputDocument.addField(currentField, currentValue);
                currentValue.delete(0, currentValue.length());
            } else if (ELEMENT_ROOT.equals(qName)) {
                // Do nothing
            } else {
                throw new SAXException(String.format("Unknown element '%s' at line %d column %d",
                        qName, locator.getLineNumber(), locator.getColumnNumber()));
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            currentValue.append(ch, start, length);
        }

        @Override
        public void endDocument()
                throws SAXException {
            try {
                solr.commit();
            } catch (SolrServerException | IOException ex) {
                throw new SAXException("Could not commit final documents", ex);
            }
        }

        /*
         * ErrorHandler
         */

        @Override
        public void error(SAXParseException e)
                throws SAXException {
            throw new SAXException(String.format("Error at line %d column %d:  %s",
                    locator.getLineNumber(), locator.getColumnNumber(),
                    e.getMessage()));
        }

        @Override
        public void fatalError(SAXParseException e)
                throws SAXException {
            throw new SAXException(String.format("Fatal error at line %d column %d:  %s",
                    locator.getLineNumber(), locator.getColumnNumber(),
                    e.getMessage()));
        }

        @Override
        public void warning(SAXParseException e)
                throws SAXException {
            throw new SAXException(String.format("Warning at line %d column %d:  %s",
                    locator.getLineNumber(), locator.getColumnNumber(),
                    e.getMessage()));
        }
    }
}
