/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.batch;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.dspace.batch.ImpRecord;
import org.dspace.batch.ImpRecordToItem;
import org.dspace.batch.service.ImpRecordService;
import org.dspace.batch.service.ImpRecordToItemService;
import org.dspace.batch.service.ImpServiceFactory;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class ItemImportMainOA {

    /** logger */
    private static Logger log = Logger.getLogger(ItemImportMainOA.class);

    /** Email buffer **/
    private static final String BATCH_USER = "batchjob@%";

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    public static void main(String[] argv) {
        Context context = null;

        try {
            context = new Context();
            new ItemImportMainOA().doAll(context, argv);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException();
        } finally {
            if (context != null && context.isValid()) {
                context.abort();
            }
        }
    }

    private Level currentLevel = Level.INFO;

    public void doAll(Context context, String[] args) {
        String header = "";

        String legenda = "LEGENDA: \n" + " -a to build item \n"
                + " -r to update (add also the metadata list with the option \n"
                + " -m it will contains the list of metadata to clean, by default delete all metadata"
                + " otherwise specifying only the dc.title it will obtain an append on the other metadata) \n"
                + " -b to invert the management of deletion or append of the bitstream"
                + " (by default append on the item standard and remove on the update) \n"
                + " -d to delete item. \n Status change: \n" + " -p send submission back to workspace \n"
                + " -w send submission through collection's workflow \n" + " -g set item in withdrawn state \n"
                + " -i to verbose the script \n" + " -z reinstate a withdrawn item \n";

        StringBuffer sb = new StringBuffer();
        CommandOptions commandOptions = new CommandOptions();
        commandOptions.setSendEmail(true);
        commandOptions.setIndex(true);
        commandOptions.setSilent(false);

        try {
            context.turnOffAuthorisationSystem();

            CommandLineParser parser = new PosixParser();

            Options options = new Options();
            options.addOption("p", "notifyAuthor", false,
                    "Send the email for the in archive event to the authors, coauthors, etc."
                            + " - the workflow email are EVER disabled");
            options.addOption("E", "batch_user", true, "BatchJob User email");
            options.addOption("x", "noindex", false, "Indexing disabled (improve performance)");
            options.addOption("n", "noemail", false, "Summary EMail disabled (improve performance)");
            options.addOption("b", "delete_bitstream", false,
                    "Delete bitstream related to the item in the update phase");
            options.addOption("h", "help", false, "help");
            options.addOption("m", "metadata", true,
                    "List of metadata to remove first and after do an update [by default all metadata are delete,"
                            + " specifying only the dc.title it will obtain an append on the other metadata];"
                            + " use this option many times on the single metadata"
                            + " e.g. -m dc.title -m dc.contributor.*");
            options.addOption("s", "switch", false,
                    "Invert the logic for the -m option, using the option -s only the metadata list"
                            + " with the option -m are saved (ad es. -m dc.description.provenance)"
                            + " the other will be delete");
            options.addOption("S", "silent", false, "muted logs");
            options.addOption("t", "threads", true, "Threads numbers (default 0, if omitted read by configuration)");
//            options.addOption("q", "query", true, "Find by query (work only in singlethread mode)");

            CommandLine line = parser.parse(options, args);
            if (line.hasOption('h')) {
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("ItemImportMainOA\n", options);
                System.exit(0);
            }

            if (!line.hasOption('p')) {
                commandOptions.setSendEmail(false);
            }

            if (line.hasOption('n')) {
                commandOptions.setSendEmail(false);
            }

            if (line.hasOption('x')) {
                commandOptions.setIndex(false);
            }

            if (line.hasOption('S')) {
                commandOptions.setSilent(true);
            }

            // TODO: add custom query
//            String query = null;
//            if (line.hasOption('q')) {
//                query = line.getOptionValue("q");
//            }
            recordEvent(commandOptions, sb, new Date() + " - Start IMP_RECORD Framework \n");
            recordEvent(commandOptions, sb, legenda);

            String[] metadataClean = null;
            if (line.hasOption('m')) {
                String[] optionValues = line.getOptionValues('m');
                if (!line.hasOption('s')) {
                    metadataClean = optionValues;
                } else {
                    List<String> mOptions = Arrays.asList(optionValues);
                    List<MetadataField> mdfs = getMetadataFieldService().findAll(context);
                    metadataClean = new String[mdfs.size() - optionValues.length];
                    int idx = 0;
                    for (MetadataField mdf : mdfs) {
                        String metadataFieldToString = metadataFieldToString(context, mdf);
                        if (!mOptions.contains(metadataFieldToString)) {
                            metadataClean[idx] = metadataFieldToString;
                            idx++;
                        }
                    }
                }
            }

            String batchJob = "";
            if (line.hasOption('E')) {
                batchJob = line.getOptionValue('E');
            } else {
                // try to get user batchjob
                EPerson row_batchJob = getEPersonService().findByEmail(context, BATCH_USER);
                if (row_batchJob != null) {
                    batchJob = row_batchJob.getEmail();

                    if (batchJob == null) {
                        throw new RuntimeException("User batch job not found");
                    }
                } else {
                    throw new RuntimeException("User batch job not found");
                }
            }

            System.out.println(configurationService.getProperty("dspace.name"));

            if (commandOptions.getSilent()) {
                powerOffLog();
            }

            AtomicInteger count = new AtomicInteger(0);
            AtomicInteger row_discarded = new AtomicInteger(0);

            int numOfThread = 0;
            List<ImpRecord> impRecords = getImpRecordService().searchNewRecords(context);
            numOfThread = getNumberOfThread(line);

            if (numOfThread > 1) {
                parallelizeOperationsTri(context, commandOptions, sb, line, metadataClean, batchJob, count,
                        row_discarded, numOfThread, impRecords);
            } else {
                AtomicReference<Context> ctxHolder = ItemImportThread.initCtxHolder(commandOptions);

                for (ImpRecord row_data : impRecords) {
                    multithreadedMain(commandOptions, sb, line, metadataClean, batchJob, count, row_data, row_discarded,
                            ctxHolder);
                }
            }

            if (commandOptions.getSilent()) {
                powerOnLog();
            }

            String countDone = "Rows done " + count.intValue() + " on a total of " + impRecords.size();
            String countDiscarded = "Rows discarded " + row_discarded.intValue() + " on a total of "
                    + impRecords.size();
            header += countDone + " \n" + countDiscarded + " \n";

            recordEvent(commandOptions, sb, countDone + " \n" + countDiscarded + " \n");
            recordEvent(commandOptions, sb, "DONE " + new Date());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            // send mail
            if (commandOptions.getSendEmail()) {
                Email email;
                try {
                    email = Email.getEmail(I18nUtil.getEmailFilename(I18nUtil.getDefaultLocale(), "log_item_import"));
                    email.addArgument(header);
                    email.addArgument(sb);
                    String recipient = configurationService.getProperty("batch.recipient");
                    String customerRecipient = configurationService.getProperty("batch.customer.recipient");

                    if (StringUtils.isNotEmpty(customerRecipient)) {
                        email.addRecipient(customerRecipient);
                    }

                    if (StringUtils.isNotEmpty(recipient)) {
                        email.addRecipient(recipient);
                    } else {
                        String alert = configurationService.getProperty("alert.recipient");
                        if (StringUtils.isNotEmpty(alert)) {
                            email.addRecipient(alert);
                        }
                    }
                    email.send();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private void parallelizeOperationsTri(Context context, CommandOptions commandOptions, StringBuffer sb,
            CommandLine line, String[] metadataClean, String batchJob, AtomicInteger count, AtomicInteger row_discarded,
            int numOfThread, List<ImpRecord> rows) throws SQLException {
        Map<String, Integer> ledger = new HashMap<>();

        List<ItemImportThread> workers = new ArrayList<>(numOfThread);
        for (int i = 0; i < numOfThread; i++) {
            workers.add(new ItemImportThread(commandOptions, line, metadataClean, batchJob, count, row_discarded,
                    numOfThread * 3, i));
        }
        for (ItemImportThread worker : workers) {
            worker.start();
        }
        int i = 0;
        for (ImpRecord row_data : rows) {
            try {
                int cardinality = getImpRecordService().countNewImpRecords(context, row_data);
                ItemImportThread worker = null;
                if (cardinality > 1) {
                    String impRecordId = row_data.getImpRecordId();
                    if (ledger.containsKey(impRecordId)) {
                        worker = workers.get(ledger.get(impRecordId));
                    } else {
                        ledger.put(impRecordId, i);
                        worker = workers.get(i);
                        i++;
                    }
                } else {
                    worker = workers.get(i);
                    i++;
                }
                worker.enqueue(row_data);
                if (i >= workers.size()) {
                    i = 0;
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        for (ItemImportThread worker : workers) {
            worker.requireStop();

            sb.append(worker.getStringBuffer());
        }
    }

    private void multithreadedMain(CommandOptions commandOptions, StringBuffer sb, CommandLine line,
            String[] metadataClean, String batchJob, AtomicInteger count, ImpRecord row_data,
            AtomicInteger row_discarded, AtomicReference<Context> ctxHolder)
            throws SQLException, AuthorizeException, IOException {
        Context subcontext = ctxHolder.get();

        if (!subcontext.isValid()) {
            subcontext = new Context();
            if (!commandOptions.getIndex()) {
                subcontext.setDispatcher("noindex");
            }
            ctxHolder.set(subcontext);
        }

        int imp_id = 0;
        String record_id = null;
        UUID epersonId = null;
        UUID collectionId = null;
        UUID itemId = null;
        String status = "";
        String operation = "";
        String op = "";
        String handle = null;
        String sourceref = null;

        List<String> argvTemp = new LinkedList<String>();

        // ID temporary import table
        imp_id = row_data.getImpId();
        // ID external
        record_id = row_data.getImpRecordId();
        // ID of the user to attach the publication
        epersonId = row_data.getImpEpersonUuid();
        // ID of the collection
        collectionId = row_data.getImpCollectionUuid();
        // p = workspace, w = workflow
        status = row_data.getStatus();
        // update, delete - the insert will be do with the update if no match in the
        // table imp_record_to_item
        operation = row_data.getOperation();

        // handle related to the item
        handle = row_data.getHandle();

        sourceref = row_data.getImpSourceref();

        if (operation != null && !operation.equals("")) {

            if (epersonId != null && collectionId != null) {

                EPerson ep = getEPersonService().find(subcontext, epersonId);

                if (ep == null) {
                    recordEvent(commandOptions, sb, "Error, eperson not found: " + epersonId, true);
                } else {
                    ImpRecordToItem record_item = getImpRecordToItemService().findByPK(subcontext, record_id);
                    if (record_item != null && StringUtils.equals(sourceref, record_item.getImpSourceref())) {
                        itemId = record_item.getImpItemId();
                    }

                    if (operation.equals("delete")) {
                        op = "d";
                        argvTemp.add("-o " + itemId);
                    } else {
                        if (operation.equals("update") && record_item != null) {
                            op = "r";
                            argvTemp.add("-o " + itemId);
                        } else {
                            op = "a"; // insert
                        }
                    }
                }

                argvTemp.add("-" + op);
                argvTemp.add("-e " + epersonId);
                argvTemp.add("-c " + collectionId);
                argvTemp.add("-i " + record_id);
                argvTemp.add("-R " + sourceref);

                if (status != null && status.length() != 0) {
                    argvTemp.add("-" + status);
                }
                argvTemp.add("-E " + batchJob);
                argvTemp.add("-I " + imp_id);
                if (handle != null) {
                    argvTemp.add("-k " + handle);
                }

                if (metadataClean != null) {
                    for (String mc : metadataClean) {
                        argvTemp.add("-m " + mc);
                    }
                }

                if (line.hasOption('b') || (line.hasOption('j') && !line.hasOption('u'))) {
                    argvTemp.add("-b");
                }

                String[] argv = argvTemp.toArray(new String[argvTemp.size()]);
                String log_error_or_not = "OK";
                try {
                    // ##--> Import record
                    recordEvent(commandOptions, sb,
                            "--> Record: " + record_id + " ...\n"
                                    + "parameters: -o item_id operation_flag -e submitter -c collection"
                                    + " -i id record change_status_flag -E batchjob -I imp_id -m clear metadata",
                            false);
                    String valueinfo = "values: ";
                    for (String arg : argv) {
                        valueinfo += arg;
                        valueinfo += " ";
                    }

                    recordEvent(commandOptions, sb, valueinfo);

                    UUID item_id = new ItemImportOA().impRecord(subcontext, argv);
                    subcontext.commit();
                } catch (Exception e) {
                    if (subcontext != null && subcontext.isValid()) {
                        subcontext.abort();
                    }
                    log_error_or_not = "ERROR :" + e.getMessage();
                    row_discarded.incrementAndGet();
                }

                recordEvent(commandOptions, sb, "ID: " + imp_id + " Record id: " + record_id +
                        " imported: " + log_error_or_not, false);
                count.incrementAndGet();

            } else {
                recordEvent(commandOptions, sb,
                        "Error while loading entry with id: " + imp_id + " record: " + record_id +
                        ". Eperson or Collection missing.",
                        true);
                row_discarded.incrementAndGet();
            }
        } else {
            recordEvent(commandOptions, sb,
                    "Error while loading entry with id: " + imp_id + " record: " + record_id +
                    ". No operation defined.", true);
            row_discarded.incrementAndGet();
        }
    }

    private static String metadataFieldToString(Context context, MetadataField mdf) throws Exception {
        String toString = mdf.getMetadataSchema().getName() + "." + mdf.getElement();
        if (StringUtils.isNotBlank(mdf.getQualifier())) {
            toString += "." + mdf.getQualifier();
        }
        return toString;
    }

    private void recordEvent(CommandOptions commandOptions, StringBuffer sb, String textString) {
        recordEvent(commandOptions, sb, textString, false);
    }

    private void recordEvent(CommandOptions commandOptions, StringBuffer sb, String textString, boolean error) {
        if (commandOptions.getSendEmail() && sb != null) {
            sb.append("\n" + textString + " \n");
        }

        if (error) {
            log.error(textString);
        } else {
            if (!commandOptions.getSilent()) {
                log.info(textString);
            }
        }

        if (!commandOptions.getSilent()) {
            System.out.println(Thread.currentThread().getName() + "==> " + textString);
        }
    }

    private void powerOffLog() {
        currentLevel = Logger.getRootLogger().getLevel();
        List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for (Logger logger : loggers) {
            logger.setLevel(Level.OFF);
        }
    }

    private void powerOnLog() {

        List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for (Logger logger : loggers) {
            logger.setLevel(currentLevel);
        }
    }

    private int getNumberOfThread(CommandLine line) {
        int numThreads = configurationService.getIntProperty("batch.framework.itemimport.threads", 0);
        if (line.hasOption("t")) {
            numThreads = Integer.parseInt(line.getOptionValue('t', "0"));
        }
        return numThreads;
    }

    private class CommandOptions {
        private boolean index;
        private boolean silent;
        private boolean sendEmail;

        public boolean getIndex() {
            return index;
        }

        public void setIndex(boolean index) {
            this.index = index;
        }

        public boolean getSilent() {
            return silent;
        }

        public void setSilent(boolean silent) {
            this.silent = silent;
        }

        public boolean getSendEmail() {
            return sendEmail;
        }

        public void setSendEmail(boolean sendEmail) {
            this.sendEmail = sendEmail;
        }
    }

    private static class ItemImportThread extends Thread {
        private CommandLine line;

        private String[] metadataClean;

        private String batchJob;

        private AtomicInteger count;

        private AtomicInteger row_discarded;

        private AtomicReference<Context> ctxHolder;

        private BlockingQueue<ImpRecord> localQueue;

        private CyclicBarrier barrier;

        private AtomicBoolean haveToStop = new AtomicBoolean();

        private int id;

        private CommandOptions commandOptions;

        private StringBuffer sb = new StringBuffer();

        private StringBuffer getStringBuffer() {
            return this.sb;
        }

        private ItemImportThread(CommandOptions commandOptions, CommandLine line, String[] metadataClean,
                String batchJob, AtomicInteger count, AtomicInteger row_discarded, int capacity, int id)
                throws SQLException {
            this.line = line;
            this.metadataClean = metadataClean;
            this.batchJob = batchJob;
            this.count = count;
            this.row_discarded = row_discarded;
            this.ctxHolder = initCtxHolder(commandOptions);
            this.localQueue = new ArrayBlockingQueue<>(capacity);
            this.barrier = new CyclicBarrier(2);
            this.commandOptions = commandOptions;
        }

        private static AtomicReference<Context> initCtxHolder(CommandOptions commandOptions) throws SQLException {
            AtomicReference<Context> ctxHolder = new AtomicReference<>();
            Context initSubcontext = new Context();
            if (!commandOptions.getIndex()) {
                initSubcontext.setDispatcher("noindex");
            }
            ctxHolder.set(initSubcontext);
            return ctxHolder;
        }

        public void requireStop() {
            this.haveToStop.set(true);
            try {
                this.barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }

        public void enqueue(ImpRecord row_data) {
            try {
                this.localQueue.put(row_data);
            } catch (Exception exc) {
                log.error(exc.getMessage(), exc);
            }
        }

        @Override
        public void run() {

            try {
                while (!this.haveToStop.get()) {
                    ImpRecord row_data = this.localQueue.poll(1, TimeUnit.SECONDS);
                    if (row_data != null) {
                        new ItemImportMainOA().multithreadedMain(commandOptions, sb, line, metadataClean, batchJob,
                                count, row_data, row_discarded, ctxHolder);
                    }
                }
            } catch (Exception exc) {
                log.error(exc.getMessage(), exc);
                throw new RuntimeException(exc.getMessage(), exc);
            } finally {

                Context context = ctxHolder.get();
                if (context != null && context.isValid()) {
                    try {
                        context.complete();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    this.barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private MetadataFieldService getMetadataFieldService() {
        return ContentServiceFactory.getInstance().getMetadataFieldService();
    }

    private EPersonService getEPersonService() {
        return EPersonServiceFactory.getInstance().getEPersonService();
    }

    private ImpRecordService getImpRecordService() {
        return ImpServiceFactory.getInstance().getImpRecordService();
    }

    private ImpRecordToItemService getImpRecordToItemService() {
        return ImpServiceFactory.getInstance().getImpRecordToItemService();
    }
}
