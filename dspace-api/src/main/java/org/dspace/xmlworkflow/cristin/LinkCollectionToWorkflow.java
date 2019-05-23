/**
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree and available
 * online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.cristin;

import org.apache.commons.io.IOUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ra
 *
 * prerequisite: Only the existing CRIStin and Inspera harvesting collections are mapped.
 *
 * This class reads the CRIStin and the Inspera harvesting collections from the database and
 * mapps the collections handle to the xmlworkflow named 'cristin' resp. 'inspera' The mapping
 * is stored in the config-file config/workflow.xml
 *
 *
 */
public class LinkCollectionToWorkflow {


    private static final HarvestedCollectionService harvestedCollectionService = HarvestServiceFactory.getInstance().getHarvestedCollectionService();

    public static void main(String[] args) throws SQLException {
        final String workflowFileName = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir") + File.separator +
                "config" + File.separator + "workflow.xml";

        StringBuilder stringBuilder = new StringBuilder();

        try {
            generateMapping(stringBuilder);

            FileInputStream inputStream = new FileInputStream(workflowFileName);
            String stringContentOfFile = IOUtils.toString(inputStream);
            inputStream.close();

            // --- Print the final file --- //
            String patternString = "(?s)(.*?)<workflow-map>(.*?)</workflow-map>(.*)";
            Pattern pattern = Pattern.compile(patternString);

            Matcher matcher = pattern.matcher(stringContentOfFile);
            if (matcher.find()) {
                String theBeginning = matcher.group(1).trim();
                String theEnd = matcher.group(3).trim();
                PrintWriter fw = new PrintWriter(workflowFileName, "UTF-8");
                
                fw.append(theBeginning);
                fw.append(stringBuilder.toString());
                fw.append(theEnd);
                fw.close();
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateMapping(StringBuilder stringBuilder) throws SQLException {
        stringBuilder.append("\n");
        stringBuilder.append("    <workflow-map>" + "\n");
        stringBuilder.append("        <name-map collection=\"default\" workflow=\"default\"/>" + "\n");
        stringBuilder.append("        <!--<name-map collection=\"123456789/4\" workflow=\"selectSingleReviewer\"/>-->" + "\n");
        stringBuilder.append("        <!--<name-map collection=\"123456789/5\" workflow=\"scoreReview\"/>-->" + "\n");

        findCollection(stringBuilder, "cristin");
        findCollection(stringBuilder, "inspera");
        stringBuilder.append("    </workflow-map>" + "\n");
        stringBuilder.append("\n    ");
    }

    private static void findCollection(StringBuilder themeSB, String collectionType) throws SQLException {
        final Context context = new Context();
        List<HarvestedCollection> harvestedCollections = harvestedCollectionService.findAll(context);
        for (HarvestedCollection harvestedCollection : harvestedCollections) {

            if (harvestedCollection == null || !collectionType.equalsIgnoreCase(harvestedCollection.getWorkflowProcess())) {
                continue;
            }

            final Collection collection = harvestedCollection.getCollection();
            if (collection == null) {
                continue;
            }

            List<Community> parentCommunities = ContentServiceFactory.getInstance().getCommunityService().getAllParents(context, collection);
            if (parentCommunities.size() > 0) {
                Community topCommunity = parentCommunities.get(parentCommunities.size() - 1);
                if (topCommunity == null) {
                    final String handle = collection.getHandle();
                    appendMapping(themeSB, topCommunity.getName(), handle, collectionType);
                }
            }
        }
    }

    private static void appendMapping(StringBuilder themeSB, String name, String handle, String collectionType) {
        System.out.println("Mapped to " + collectionType + " workflow: handle: " + handle + " Community: " + name);
        // <name-map collection="11250.1/8550594" workflow="[cristin/inspera]"/> <!-- Import from [CRIStin/Inspera] - (Top) Community name -->
        themeSB.append("        ")
                .append("<name-map collection=\"").append(handle).append("\" workflow=\"" + collectionType + "\"/>")
                .append(" <!-- Import from " + collectionType + " - ").append(name).append(" -->")
                .append("\n");

    }
}