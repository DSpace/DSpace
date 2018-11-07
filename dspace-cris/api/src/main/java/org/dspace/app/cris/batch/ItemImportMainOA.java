/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class ItemImportMainOA
{

    /** logger */
    private static Logger log = Logger.getLogger(ItemImportMainOA.class);

    /** Email buffer **/
    private static StringBuffer sb = new StringBuffer();

    private static boolean sendEmail = true;

    private static boolean index = true;

    private static Level currentLevel = null;

    private static boolean silent = false;

    private static PrintStream out;

    private static PrintStream outSilent;

    private static final String BATCH_USER = "batchjob@%";

    private static final String QUERY_BATCH_USER = "SELECT * FROM EPERSON WHERE email like ?";

    public static void main(String[] args)
    {

        // create a context
        Context context = null;

        String header = "";

        recordEvent(new Date() + " - Start IMP_RECORD Framework \n");

        String legenda = "LEGENDA: \n" + " -a to build item \n"
                + " -r to update (add also the metadata list with the option \n"
                + " -m it will contains the list of metadata to clean, by default delete all metadata otherwise specifying only the dc.title it will obtain an append on the other metadata) \n"
                + " -b to invert the management of deletion or append of the bitstream (by default append on the item standard and remove on the update) \n"
                + " -d to delete item. \n Status change: \n"
                + " -p to workspace \n" + " -w to workflow step one \n"
                + " -y to workflow step two \n"
                + " -x to workflow step three \n" + " -g to withdrawn \n"
                + " -i to verbose the script \n" + " -z publish item";

        recordEvent(legenda);

        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();

            CommandLineParser parser = new PosixParser();

            Options options = new Options();
            options.addOption("p", "notifyAuthor", false,
                    "Send the email for the in archive event to the authors, coauthors, etc. - the workflow email are EVER disabled");
            options.addOption("E", "batch_user", true, "BatchJob User email");
            options.addOption("x", "noindex", false,
                    "Indexing disabled (improve performance)");
            options.addOption("n", "noemail", false,
                    "Summary EMail disabled (improve performance)");
            options.addOption("b", "delete_bitstream", false,
                    "Delete bitstream related to the item in the update phase");
            options.addOption("h", "help", false, "help");
            options.addOption("m", "metadata", true,
                    "List of metadata to remove first and after do an update [by default all metadata are delete, specifying only the dc.title it will obtain an append on the other metadata]; use this option many times on the single metadata e.g. -m dc.title -m dc.contributor.*");
            options.addOption("s", "switch", false,
                    "Invert the logic for the -m option, using the option -s only the metadata list with the option -m are saved (ad es. -m dc.description.provenance) the other will be delete");
            options.addOption("S", "silent", false,
                    "muted logs");
            options.addOption("t", "threads", true,
                    "Threads numbers (default 0, if omitted read by configuration)");
            options.addOption("q", "query", true,
                    "Find by query (work only in singlethread mode)");
            
            CommandLine line = parser.parse(options, args);
            if (line.hasOption('h'))
            {
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("ItemImportMainOA\n", options);
                System.exit(0);
            }

            if (!line.hasOption('p'))
            {
                Email.setSkipEmailSend(true);
            }

            if (line.hasOption('n'))
            {
                sendEmail = false;
            }

            if (line.hasOption('x'))
            {
                index = false;
            }

            if (line.hasOption('S'))
            {
                silent = true;
            }

            String query = null;
            if (line.hasOption('q'))
            {
                query = line.getOptionValue("q");
            }
            
            String[] metadataClean = null;
            if (line.hasOption('m'))
            {
                String[] optionValues = line.getOptionValues('m');
                if (!line.hasOption('s'))
                {
                    metadataClean = optionValues;
                }
                else
                {
                    List<String> mOptions = Arrays.asList(optionValues);
                    MetadataField[] mdfs = MetadataField.findAll(context);
                    metadataClean = new String[mdfs.length
                            - optionValues.length];
                    int idx = 0;
                    for (MetadataField mdf : mdfs)
                    {
                        String metadataFieldToString = metadataFieldToString(
                                context, mdf);
                        if (!mOptions.contains(metadataFieldToString))
                        {
                            metadataClean[idx] = metadataFieldToString;
                            idx++;
                        }
                    }
                }
            }

            String batchJob = "";
            if (line.hasOption('E'))
            {
                batchJob = line.getOptionValue('E');
            }
            else
            {
                // try to get user batchjob
                TableRow row_batchJob = DatabaseManager.querySingleTable(
                        context, "eperson", QUERY_BATCH_USER, BATCH_USER);
                if (row_batchJob != null)
                {
                    batchJob = row_batchJob.getStringColumn("email");

                    if (batchJob == null)
                    {
                        throw new RuntimeException("User batch job not found");
                    }
                }
                else
                {
                    throw new RuntimeException("User batch job not found");
                }
            }

            System.out.println(ConfigurationManager.getProperty("dspace.name"));

            if (silent)
            {
                powerOffLog();
            }

            AtomicInteger count = new AtomicInteger(1);

            AtomicInteger row_discarded = new AtomicInteger(0);

            int numOfThread = 0;
            String sql = "SELECT * FROM imp_record WHERE last_modified is NULL order by imp_id ASC";
            if(StringUtils.isNotBlank(query)) {
                //query mode work only in single thread
                sql = query;
            }
            else {
                numOfThread = getNumberOfThread(line);
            }
    
            if (numOfThread > 1)
            {
                TableRowIterator rows;
                sql = "SELECT a.*, (select count(imp_record_id) from imp_record b where b.imp_record_id=a.imp_record_id and b.last_modified is NULL ) as cardinality FROM imp_record a WHERE last_modified is NULL order by imp_id ASC";// and
                log.debug(sql);
                rows = DatabaseManager.query(context, sql);
                parallelizeOperationsTri(line, metadataClean, batchJob, count,
                         row_discarded, numOfThread, rows);
            }
            else
            {
                TableRowIterator rows = DatabaseManager.query(context, sql);

                AtomicReference<Context> ctxHolder = ItemImportThread
                            .initCtxHolder();
    
                while (rows.hasNext())
                {
                    TableRow row_data = rows.next();
                    multithreadedMain(line, metadataClean, batchJob, count,
                             row_data, row_discarded, ctxHolder);
                }
                rows.close();
            }

            context.complete();

            if (silent)
            {
                powerOnLog();
            }

            header += "Righe scartate " + row_discarded.intValue()
                    + " su un totale di " + (count.intValue() - 1) + " \n";

            recordEvent("Righe scartate " + row_discarded.intValue()
                    + " su un totale di " + (count.intValue() - 1));
            recordEvent("TERMINATA PROCEDURA " + new Date());
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        finally
        {
            // send mail
            if (sendEmail)
            {
                Email email;
                try
                {
                    email = Email.getEmail(I18nUtil.getEmailFilename(
                            I18nUtil.getDefaultLocale(), "log_item_import"));
                    email.addArgument(header);
                    email.addArgument(sb);
                    String recipient = ConfigurationManager
                            .getProperty("batch.recipient");
                    String customerRecipient = ConfigurationManager
                            .getProperty("batch.customer.recipient");

                    if (StringUtils.isNotEmpty(customerRecipient))
                    {
                        email.addRecipientTO(customerRecipient);
                    }

                    if (StringUtils.isNotEmpty(recipient))
                    {
                        email.addRecipientTO(recipient);
                    }
                    else
                    {
                        String alert = ConfigurationManager
                                .getProperty("alert.recipient");
                        if (StringUtils.isNotEmpty(alert))
                        {
                            email.addRecipientTO(alert);
                        }
                    }
                    email.send();
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), e);
                }
            }
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
    }

    private static void parallelizeOperationsTri(CommandLine line,
            String[] metadataClean, String batchJob, AtomicInteger count,
            AtomicInteger row_discarded, int numOfThread, TableRowIterator rows)
                    throws SQLException
    {
        Map<String, Integer> ledger = new HashMap<>();
        List<ItemImportThread> workers = new ArrayList<>(numOfThread);
        for (int i = 0; i < numOfThread; i++)
        {
            workers.add(new ItemImportThread(line, metadataClean, batchJob,
                    count, row_discarded, numOfThread * 3, i));
        }
        for (ItemImportThread worker : workers)
        {
            worker.start();
        }
        int i = 0;
        while (rows.hasNext())
        {
            TableRow row_data = rows.next();
            try
            {
                int cardinality = row_data.getIntColumn("cardinality");
                ItemImportThread worker = null;
                if (cardinality > 1)
                {
                    String impRecordId = row_data.getStringColumn("imp_record_id");
                    if (ledger.containsKey(
                            impRecordId))
                    {
                        worker = workers.get(ledger
                                .get(impRecordId));
                    }
                    else
                    {
                        ledger.put(impRecordId, i);
                        worker = workers.get(i);
                        i++;
                    }
                }
                else
                {
                    worker = workers.get(i);
                    i++;
                }
                worker.enqueue(row_data);
                if (i >= workers.size())
                {
                    i = 0;
                }
            }
            catch (Exception exc)
            {
                exc.printStackTrace();
            }
        }
        rows.close();
        for (ItemImportThread worker : workers)
        {
            worker.requireStop();
        }
    }

    private static void multithreadedMain(CommandLine line,
            String[] metadataClean, String batchJob, AtomicInteger count,
            TableRow row_data, AtomicInteger row_discarded,
            AtomicReference<Context> ctxHolder)
                    throws SQLException, AuthorizeException, IOException
    {
        Context subcontext = ctxHolder.get();

        if (!subcontext.isValid())
        {
            subcontext = new Context();
            if (!index)
            {
                subcontext.setDispatcher("noindex");
            }
            ctxHolder.set(subcontext);
        }

        int imp_id = 0;
        String record_id = null;
        int epersonId = 0;
        int collectionId = 0;
        int itemId = 0;
        String status = "";
        String operation = "";
        String op = "";
        String handle = null;
        String sourceref = null;

        List<String> argvTemp = new LinkedList<String>();

        // ID temporary import table
        imp_id = row_data.getIntColumn("imp_id");
        // ID external
        record_id = row_data.getStringColumn("imp_record_id");
        // ID of the user to attach the publication
        epersonId = row_data.getIntColumn("imp_eperson_id");
        // ID of the collection 
        collectionId = row_data.getIntColumn("imp_collection_id");
        // p = workspace, w = workflow step 1, y = workflow step 2, x =
        // workflow step 3, z = inarchive
        status = row_data.getStringColumn("status");
        // update, delete - the insert will be do with the update if no match in the table imp_record_to_item
        operation = row_data.getStringColumn("operation");

        // handle related to the item
        handle = row_data.getStringColumn("handle");
        
        sourceref = row_data.getStringColumn("imp_sourceref");
        
        if (!operation.equals(""))
        {

            if (epersonId > -1 && collectionId > -1)
            {

                EPerson ep = EPerson.find(subcontext, epersonId);
                if (ep == null)
                {
                    recordEvent("Errore, eperson non trovato: " + epersonId,
                            true);
                }

                else
                {

                    TableRow record_item = DatabaseManager.querySingleTable(subcontext, "imp_record_to_item", "select * from imp_record_to_item where imp_record_id = ? and imp_sourceref = ?", record_id, sourceref);
                    if (record_item != null)
                    {
                        itemId = record_item.getIntColumn("imp_item_id");
                    }

                    if (operation.equals("delete"))
                    {
                        op = "d";
                        argvTemp.add("-o " + itemId);
                    }
                    else
                    {
                        if (operation.equals("update") && record_item != null)
                        {
                            op = "r";
                            argvTemp.add("-o " + itemId);
                        }
                        else
                        {
                            op = "a"; // insert
                        }
                    }

                }

                argvTemp.add("-" + op);
                argvTemp.add("-e " + epersonId);
                argvTemp.add("-c " + collectionId);
                argvTemp.add("-i " + record_id);
                argvTemp.add("-R " + sourceref);

                if (status != null && status.length() != 0)
                {
                    argvTemp.add("-" + status);
                }
                argvTemp.add("-E " + batchJob);
                argvTemp.add("-I " + imp_id);
                if (handle != null)
                    argvTemp.add("-k " + handle);

                if (metadataClean != null)
                {
                    for (String mc : metadataClean)
                    {
                        argvTemp.add("-m " + mc);
                    }
                }

                if (line.hasOption('b')
                        || (line.hasOption('j') && !line.hasOption('u')))
                {
                    argvTemp.add("-b");
                }

                String[] argv = argvTemp.toArray(new String[argvTemp.size()]);
                String log_error_or_not = "OK";
                try
                {
                    // ##--> Import record
                    recordEvent(
                            "--> Record: " + record_id + " ...\n"
                                    + "parameters: -o item_id operation_flag -e submitter -c collection -i id record change_status_flag -E batchjob -I imp_id -m clear metadata",
                            false, true);
                    String valueinfo = "values: ";
                    for (String arg : argv)
                    {
                        valueinfo += arg;
                        valueinfo += " ";
                    }

                    recordEvent(valueinfo);

                    int item_id = ItemImportOA.impRecord(subcontext, argv);
                    subcontext.commit();
                    Object oldItemfromCache = subcontext.fromCache(Item.class,
                            item_id);
                    if (oldItemfromCache != null)
                    {
                        subcontext.removeCached(oldItemfromCache, item_id);
                    }
                }
                catch (Exception e)
                {
                    if (subcontext != null && subcontext.isValid())
                    {
                        subcontext.abort();
                    }
                    log_error_or_not = "ERROR :" + e.getMessage();
                    row_discarded.incrementAndGet();
                }

                recordEvent("Record id --> " + record_id + " importato: "
                        + log_error_or_not, false, true);
                count.incrementAndGet();

            }
            else
            {
                recordEvent("Errore durante il caricamento del record: "
                        + record_id + ". Eperson o Collection mancanti.", true);
            }
        }
        else
        {
            recordEvent("Errore durante il caricamento del record: " + record_id
                    + ". Nessuna Operation definita.", true);
        }
    }

    private static String metadataFieldToString(Context context,
            MetadataField mdf) throws Exception
    {
        String toString = MetadataSchema.find(context, mdf.getSchemaID())
                .getName() + "." + mdf.getElement();
        if (StringUtils.isNotBlank(mdf.getQualifier()))
        {
            toString += "." + mdf.getQualifier();
        }
        return toString;
    }

    private static void recordEvent(String textString)
    {
        recordEvent(textString, false, !silent);
    }

    private static void recordEvent(String textString, boolean error)
    {
        recordEvent(textString, error, !silent);
    }

    private static void recordEvent(String textString, boolean error,
            boolean forceLog)
    {
        if (out == null)
        {
            out = System.out;
        }

        if (forceLog)
        {
            out.println(Thread.currentThread().getName() + "==> " + textString);
        }
        else
        {
            System.out
                    .println(Thread.currentThread().getName() + "==> " + textString);
        }

        if (sendEmail)
        {
            sb.append("\n" + textString + " \n");
        }

        if (error)
        {
            log.error(textString);
        }
        else
        {
            if (!silent)
            {
                log.info(textString);
            }
        }
    }

    private static void powerOffLog()
    {
        currentLevel = Logger.getRootLogger().getLevel();
        List<Logger> loggers = Collections
                .<Logger> list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for (Logger logger : loggers)
        {
            logger.setLevel(Level.OFF);
        }

        if (out == null)
        {
            out = System.out;
        }

        outSilent = new PrintStream(new OutputStream()
        {
            @Override
            public void write(int b) throws IOException
            {
            }
        });
        System.setOut(outSilent);
    }

    private static void powerOnLog()
    {

        List<Logger> loggers = Collections
                .<Logger> list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for (Logger logger : loggers)
        {
            logger.setLevel(currentLevel);
        }

        System.setOut(out);
    }

    private static int getNumberOfThread(CommandLine line)
    {
        int numThreads = ConfigurationManager
                .getIntProperty("batch.framework.itemimport.threads", 0);
        if (line.hasOption("t"))
        {
            numThreads = Integer.parseInt(line.getOptionValue('t', "0"));
        }
        return numThreads;
    }

    private static class ItemImportThread extends Thread
    {
        private CommandLine line;

        private String[] metadataClean;

        private String batchJob;

        private AtomicInteger count;

        private AtomicInteger row_discarded;

        private AtomicReference<Context> ctxHolder;

        private BlockingQueue<TableRow> localQueue;

        private CyclicBarrier barrier;

        private AtomicBoolean haveToStop = new AtomicBoolean();

        private int id;

        private ItemImportThread(CommandLine line, String[] metadataClean,
                String batchJob, AtomicInteger count,
                AtomicInteger row_discarded, int capacity, int id)
                        throws SQLException
        {
            this.line = line;
            this.metadataClean = metadataClean;
            this.batchJob = batchJob;
            this.count = count;
            this.row_discarded = row_discarded;
            this.ctxHolder = initCtxHolder();
            this.localQueue = new ArrayBlockingQueue<>(capacity);
            this.barrier = new CyclicBarrier(2);
            this.id = id;
        }

        private static AtomicReference<Context> initCtxHolder()
                throws SQLException
        {
            AtomicReference<Context> ctxHolder = new AtomicReference<>();
            Context initSubcontext = new Context();
            if (!index)
            {
                initSubcontext.setDispatcher("noindex");
            }
            ctxHolder.set(initSubcontext);
            return ctxHolder;
        }

        public void requireStop()
        {
            this.haveToStop.set(true);
            try
            {
                this.barrier.await();
            }
            catch (InterruptedException | BrokenBarrierException e)
            {
                e.printStackTrace();
            }
        }

        public void enqueue(TableRow row_data)
        {
            try
            {
                this.localQueue.put(row_data);
            }
            catch (Exception exc)
            {
                log.error(exc.getMessage(), exc);
            }
        }

        @Override
        public void run()
        {

            try
            {
                while (!this.haveToStop.get())
                {
                    TableRow row_data = this.localQueue.poll(1,
                            TimeUnit.SECONDS);
                    if (row_data != null)
                    {
                        ItemImportMainOA.multithreadedMain(line, metadataClean,
                                batchJob, count, row_data, row_discarded,
                                ctxHolder);
                    }
                }
            }
            catch (Exception exc)
            {
                log.error(exc.getMessage(), exc);
                throw new RuntimeException(exc.getMessage(), exc);
            }
            finally
            {

                Context context = ctxHolder.get();
                if (context != null && context.isValid())
                {
                    try
                    {
                        context.complete();
                    }
                    catch (SQLException e)
                    {
                        e.printStackTrace();
                    }
                }
                try
                {
                    this.barrier.await();
                }
                catch (InterruptedException | BrokenBarrierException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
