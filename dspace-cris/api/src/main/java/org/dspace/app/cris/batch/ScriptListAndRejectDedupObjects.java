/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.batch;

import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.deduplication.utils.DedupUtils;
import org.dspace.app.cris.deduplication.utils.DuplicateItemInfo;
import org.dspace.app.cris.model.dto.SimpleViewEntityDTO;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class ScriptListAndRejectDedupObjects
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptMergeCrisObjects.class);

    private static final Format FORMATTER = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    public static final String SCRIPT_NAME = ScriptListAndRejectDedupObjects.class.getSimpleName();

    /**
     * Batch script to list and reject dedup objects
     */
    public static void main(String[] args)
    {
        log.info("#### START " + SCRIPT_NAME + ": -----" + FORMATTER.format(new Date()) + " ----- ####");
        DedupUtils dedupUtils = new DSpace().getServiceManager().getServiceByName("dedupUtils", DedupUtils.class);
        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();

            Option helpOption = new Option("h", "help", false, "help");
            Option typeOption = new Option("t", "type", true, "object type");
            typeOption.setArgs(1);
            Option idOption = new Option("i", "id", true, "object id");
            idOption.setArgs(2);
            idOption.setValueSeparator(' ');
            Option compareOption = new Option("c", "compare", false, "compare two objects");
            Option rejectOption = new Option("r", "reject", false, "reject two objects");
            Option noteOption = new Option("n", "note", true, "reject note");

            Options options = new Options();
            options.addOption(helpOption);
            options.addOption(typeOption);
            options.addOption(idOption);
            options.addOption(compareOption);
            options.addOption(rejectOption);
            options.addOption(noteOption);

            CommandLine line = new PosixParser().parse(options, args);
            if (line.hasOption('h'))
            {
                printHelp(options);
            }

            String objectTypeString = line.getOptionValue('t');
            String[] objectIDsString = line.getOptionValues('i');
            boolean compare = line.hasOption('c');
            boolean reject = line.hasOption('r');
            String note = line.getOptionValue('n');

            if (objectTypeString == null)
            {
                printHelp(options);
            }

            if (compare && reject)
            {
                printHelp(options);
            }
            else if (compare || reject)
            {
                if (objectIDsString == null || objectIDsString.length != 2)
                {
                    printHelp(options);
                }
            }

            int objectType = Integer.parseInt(objectTypeString);
            int[] objectIDs = new int[] { -1 };
            if (objectIDsString != null)
            {
                objectIDs = new int[objectIDsString.length];
                for (int i = 0; i < objectIDs.length; i++)
                {
                    objectIDs[i] = Integer.parseInt(objectIDsString[i]);
                    if (!foundObject(context, dedupUtils, objectIDs[i], objectType))
                    {
                        System.out.println("ERROR: object with id=" + objectIDs[i] + " and type=" + objectType + " not found.");
                        System.exit(0);
                    }
                }
            }

            if (compare)
            {
                doCompareMode(dedupUtils, context, objectIDs[0], objectIDs[1], objectType);
            }
            else if (reject)
            {
                doRejectMode(dedupUtils, context, objectIDs[0], objectIDs[1], objectType, note);
            }
            else
            {
                doNormalMode(dedupUtils, context, objectIDs[0], objectType);
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        log.info("#### END " + SCRIPT_NAME + ": -----" + FORMATTER.format(new Date()) + " ----- ####");
        System.exit(0);
    }

    public static void printHelp(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp(SCRIPT_NAME + "\n", options, false);
        System.out.println(
                "\n\nUSAGE:\n"
                + " List duplicates: -t <object type> [-i <object id>]\n"
                + " Compare two objects: -c -t <object type> -i <first object id> <second object id>\n"
                + " Reject two duplicate objects: -r -t <object type> -i <first object id> <second object id> [-n <reject note>]");
        System.exit(0);
    }

    public static void printDuplicates(int parentObjectID, int parentObjectType, List<DuplicateItemInfo> duplicateItemInfos)
    {
        if (duplicateItemInfos.isEmpty())
        {
            System.out.println("object with id=" + parentObjectID + " and type=" + parentObjectType + " has no duplicates.");
            return;
        }
        else
        {
            System.out.println("object with id=" + parentObjectID + " and type=" + parentObjectType + " duplicate(s):");
        }

        for (int i = 0; i < duplicateItemInfos.size(); i++)
        {
            // print all possible informations about the duplicate
            SimpleViewEntityDTO entity = duplicateItemInfos.get(i).getDuplicateItem();
            System.out.println("  " + (i+1) + ") duplicate:");
            System.out.println("\tobjectID: " + entity.getEntityID());
            System.out.println("\tobjectType: " + entity.getEntityTypeID());

            for (Map.Entry<String, List<String>> entry : entity.getDuplicateItem().entrySet())
            {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty())
                {
                    for (int j = 0; j < values.size(); j++)
                    {
                        values.set(j, "\"" + values.get(j) + "\"");
                    }

                    System.out.println("\t" + entry.getKey() + ": " + StringUtils.join(values, ", "));
                }
            }

            if (i < duplicateItemInfos.size()-1)
            {
                System.out.println("  ----------------------------------------------");
            }
        }
    }

    public static void doNormalMode(DedupUtils dedupUtils, Context context, int objectID, int objectType)
            throws SQLException, SearchServiceException
    {
        if (objectID == -1)
        {
            Map<Integer, List<DuplicateItemInfo>> items = dedupUtils.findAllDuplicates(context, objectType, null, false);
            if (items.isEmpty())
            {
                System.out.println("objects with type=" + objectType + " have no duplicates.");
                return;
            }

            System.out.println("================================================");
            int i = 1;
            for (Map.Entry<Integer, List<DuplicateItemInfo>> item : items.entrySet())
            {
                System.out.print((i++) + ") ");
                printDuplicates(item.getKey(), objectType, item.getValue());
                System.out.println("================================================");
            }
        }
        else
        {
            List<DuplicateItemInfo> duplicateItemInfos = dedupUtils.getDuplicateByIDandType(context, objectID, objectType, false);
            printDuplicates(objectID, objectType, duplicateItemInfos);
        }
    }

    private static boolean areDuplicates(DedupUtils dedupUtils, Context context, int objectID1, int objectID2, int objectType)
            throws SQLException, SearchServiceException
    {
        List<DuplicateItemInfo> duplicateItemInfos = dedupUtils.getDuplicateByIDandType(context, objectID1, objectType, false);
        for (DuplicateItemInfo duplicateItemInfo : duplicateItemInfos)
        {
            SimpleViewEntityDTO entity = duplicateItemInfo.getDuplicateItem();
            if (objectID2 == entity.getEntityID())
            {
                return true;
            }
        }
        return false;
    }

    public static void doCompareMode(DedupUtils dedupUtils, Context context, int objectID1, int objectID2, int objectType)
            throws SQLException, SearchServiceException
    {
        System.out.println("object with id=" + objectID1 + " and type=" + objectType + " and "
                + "object with id=" + objectID2 + " and type=" + objectType
                + " are" + (areDuplicates(dedupUtils, context, objectID1, objectID2, objectType) ? "" : " not") + " duplicates.");
    }

    public static void doRejectMode(DedupUtils dedupUtils, Context context, int objectID1, int objectID2, int objectType, String note)
            throws SQLException, AuthorizeException, SearchServiceException
    {
        if (areDuplicates(dedupUtils, context, objectID1, objectID2, objectType))
        {
            if (note == null)
            {
                note = SCRIPT_NAME + " " + FORMATTER.format(new Date());
            }

            if (dedupUtils.rejectDups(context, objectID1, objectID2, objectType, true, note, false))
            {
                context.commit();
                System.out.println("object with id=" + objectID1 + " and type=" + objectType
                        + " and object with id=" + objectID2 + " and type=" + objectType
                        + " aren't duplicates anymore.");
            }
        }
        else
        {
            System.out.println("object with id=" + objectID1 + " and type=" + objectType + " and "
                    + "object with id=" + objectID2 + " and type=" + objectType
                    + " are not" + " duplicates.");
        }
    }

    private static boolean foundObject(Context context, DedupUtils dedupUtils, int id, int type)
            throws SQLException
    {
        if (type == Constants.ITEM)
        {
            if (DSpaceObject.find(context, type, id) == null)
            {
                return false;
            }
        }
        else
        {
            if (dedupUtils.getApplicationService().getEntityById(id, type) == null)
            {
                return false;
            }
        }
        return true;
    }

}
