package org.datadryad.authority;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.datadryad.api.DryadFunderConcept;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.content.authority.Term;
import org.dspace.core.Context;

import java.net.URL;
import java.util.*;

public final class ImportFunder2Authority {
    /**
     * DSpace Context object
     */
    private Context context;
    private static Properties crossrefFieldNameMap;

    static {
        crossrefFieldNameMap = new Properties();
        crossrefFieldNameMap.setProperty("funder.identifier", "uri");
        crossrefFieldNameMap.setProperty("organization.fullName", "value");
        crossrefFieldNameMap.setProperty("funder.altLabel", "other_names");
        crossrefFieldNameMap.setProperty("funder.country", "country");
    }
    /**
     * For invoking via the command line.  If called with no command line arguments,
     * it will negotiate with the user for the administrator details
     *
     * @param argv command-line arguments
     */
    public static void main(String[] argv)
            throws Exception {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        ImportFunder2Authority ca = new ImportFunder2Authority();
        options.addOption("t", "test", true, "test mode");

        CommandLine line = parser.parse(options, argv);
        ca.importAuthority(line.getOptionValue("t"));
    }


    /**
     * constructor, which just creates and object with a ready context
     *
     * @throws Exception
     */
    private ImportFunder2Authority()
            throws Exception {
        context = new Context();
    }

    /**
     * Create the administrator with the given details.  If the user
     * already exists then they are simply upped to administrator status
     *
     * @throws Exception
     */
    private void importAuthority(String test)
            throws Exception {
        // Of course we aren't an administrator yet so we need to
        // circumvent authorisation
        context.turnOffAuthorisationSystem();

        try {
            Date date = new Date();
            Scheme funderScheme = Scheme.findByIdentifier(context, "Funder");
            if (funderScheme == null) {
                funderScheme = Scheme.create(context, "Funder");
                funderScheme.setLastModified(context, date);
                funderScheme.setCreated(context, date);
                funderScheme.setLang(context, "en");
                funderScheme.setStatus(context, "Published");
            }


            context.commit();
            //Get all funders in fundref:
            System.out.println("starting");
            try {
                URL url = new URL("http://search.crossref.org/funders?format=json");
                ObjectMapper m = new ObjectMapper();
                JsonNode rootNode = m.readTree(url.openStream());
                if (rootNode.isArray()) {
                    Iterator<JsonNode> funders = rootNode.elements();
                    while (funders.hasNext()) {
                        JsonNode funderNode = funders.next();
                        Concept aConcept = funderScheme.createConcept(context);
                        aConcept.setLastModified(context, date);
                        aConcept.setCreated(context, date);
                        aConcept.setLang(context, "en");
                        aConcept.setStatus(context, Concept.Status.ACCEPTED.name());
                        aConcept.setTopConcept(context, true);
                        context.commit();
                        DryadFunderConcept funderConcept = new DryadFunderConcept(context, aConcept);

                        HashMap<String, String> funderHash = new HashMap<String, String>();
                        Set<String> propertyKeys = crossrefFieldNameMap.stringPropertyNames();
                        for (String key : propertyKeys) {
                            String val = crossrefFieldNameMap.getProperty(key);
                            JsonNode jsonNode = funderNode.get(val);
                            if (jsonNode != null) {
                                if (jsonNode.isArray()) {
                                    if (key.equals("funder.altLabel")) {
                                        // this is the only multi-value one
                                        Iterator<JsonNode> names = jsonNode.elements();
                                        while (names.hasNext()) {
                                            String name = names.next().textValue();
                                            funderConcept.addAltLabel(name);
                                        }
                                    }
                                } else {
                                    aConcept.addMetadata(context, key, jsonNode.textValue());
                                    funderHash.put(key, jsonNode.textValue());
                                }
                            }
                        }
                        Term aTerm = aConcept.createTerm(context, funderHash.get("organization.fullName"), Term.prefer_term);
                        System.out.println("added " + funderHash.get("organization.fullName"));
                    }
                }
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement element : e.getStackTrace()) {
                    sb.append(element.toString());
                    sb.append("\n");
                }
                String s = sb.toString();
                System.out.println("Exception:\n" + s);
            }

        } catch (Exception e) {
            System.out.print(e);
            System.out.print(e.getStackTrace());
        }

        context.complete();

        System.out.println("Authority imported");
    }
}
