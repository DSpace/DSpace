/*
 * Copyright 2019 Mark H. Wood.
 */

package org.dspace.app.authority;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
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

        OptionGroup verbs = new OptionGroup();
        verbs.addOption(new Option(OPT_DUMP,    "dump", false, "Export all records as XML on standard out"));
        verbs.addOption(new Option(OPT_RESTORE, "restore", false, "Import records from XML on standard in"));
        verbs.addOption(new Option(OPT_HELP,    "help", false, "Describe all options"));

        Options options = new Options();
        options.addOptionGroup(verbs);

        CommandLine command = null;
        try {
            command = new DefaultParser().parse(options, argv);
        } catch (ParseException e) {
            System.err.format("Unrecognized command:%n%s%n", e.getMessage());
            giveHelp(options);
            System.exit(1);
        }

        // Locate the 'authority' Solr core.
        URI collectionURI = null;
        try {
            collectionURI = new URI(CFG.getProperty("solr.server")).resolve("authority");
        } catch (URISyntaxException ex) {
            System.err.format("Could not build URL for the authority index:  {}",
                    ex.getMessage());
            System.exit(1);
        }

        // Interpret command.
        if (command.hasOption(OPT_HELP)) {
            giveHelp(options);
            System.exit(0);
        } else if (command.hasOption(OPT_DUMP)) {
            try (
                    HttpSolrClient solr = new HttpSolrClient.Builder(collectionURI.toString()).build();
                    Writer output = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
                    ) {
                dump(solr, output);
            }
        } else if (command.hasOption(OPT_RESTORE)) {
            try ( HttpSolrClient solr = new HttpSolrClient.Builder(collectionURI.toString()).build(); ) {
                restore(solr, System.in);
            }
        } else {
            System.err.println("Unknown command");
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
    static private void dump(HttpSolrClient solr, Writer output)
            throws IOException, SolrServerException, XMLStreamException {
        // Set up to write XML.
        XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance()
                .createXMLStreamWriter(output);
        xmlWriter.writeStartDocument("1.0", StandardCharsets.UTF_8.name());
        xmlWriter.writeStartElement("authority-dump");

        // Set up the query
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", "*.*");

        // For all Solr records.
        for (SolrDocument document : new SolrQueryWindow(solr, params)) {
            //    Start a record element.
            xmlWriter.writeStartElement("r");
            //    For all fields.
            for (String field : document.getFieldNames()) {
                //      For all values.
                for (Object value : document.getFieldValues(field)) {
                    xmlWriter.writeStartElement("f"); // Field
                    xmlWriter.writeAttribute("name", field);
                    xmlWriter.writeCharacters(value.toString());
                    xmlWriter.writeEndElement(); // Field (f)
                }
            }
            //    End the record element.
            xmlWriter.writeEndElement(); // Record (r)
        }

        // Close up XML.
        xmlWriter.writeEndElement(); // Document ("authority-dump")
        xmlWriter.close();

        // Finished!
    }

    /**
     * Read a valid authority dump document and create Solr records from it.
     * Uses ContentHandler to update Solr.
     *
     * @param solr connection to the authority core.
     * @param input the dump document.
     * @throws ParserConfigurationException passed through.
     * @throws SAXException passed through.
     * @throws IOException passed through.
     */
    private static void restore(HttpSolrClient solr, InputStream input)
            throws ParserConfigurationException, SAXException, IOException {
        // Load the schema.
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(Commands.class.getResource("authority-dump.xsd"));

        // Configure a parser factory.
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setSchema(schema);
        parserFactory.setValidating(true);

        // Create and run a parser.  ContentHandler will update Solr.
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(input, new ParseEventHandler(solr));
    }

    /**
     * Handle SAX content events and errors.
     */
    private static class ParseEventHandler
            extends DefaultHandler {
        private final HttpSolrClient solr;
        private final StringBuilder currentValue = new StringBuilder();
        private Locator locator;
        private SolrInputDocument solrInputDocument;
        private String currentField;

        /*
         * ContentHandler
         */

        /**
         * Save a reference to a connection to a Solr core/collection.
         *
         * @param solr Connection to Solr.
         */
        ParseEventHandler(HttpSolrClient solr) {
            this.solr = solr;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            if ("f".equals(localName)) {
                currentField = atts.getValue("name");
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if ("r".equals(localName)) {
                try {
                    solr.add(solrInputDocument);
                } catch (SolrServerException | IOException ex) {
                    throw new SAXException("Could not add a record", ex);
                }
            } else if ("f".equals(localName)) {
                solrInputDocument.addField(currentField, currentValue);
                currentValue.delete(0, currentValue.length());
                solrInputDocument.clear();
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
            throw new SAXException(String.format("At line %d column %d:  %s",
                    locator.getLineNumber(), locator.getColumnNumber(),
                    e.getMessage()));
        }

        @Override
        public void fatalError(SAXParseException e)
                throws SAXException {
            throw new SAXException(String.format("At line %d column %d:  %s",
                    locator.getLineNumber(), locator.getColumnNumber(),
                    e.getMessage()));
        }

        @Override
        public void warning(SAXParseException e)
                throws SAXException {
            throw new SAXException(String.format("At line %d column %d:  %s",
                    locator.getLineNumber(), locator.getColumnNumber(),
                    e.getMessage()));
        }
    }
}
