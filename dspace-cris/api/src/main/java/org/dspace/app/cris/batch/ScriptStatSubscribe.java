/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;

import jxl.CellView;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.cris.model.StatSubscription;
import org.dspace.app.cris.statistics.SummaryStatBean;
import org.dspace.app.cris.statistics.service.StatSubscribeService;
import org.dspace.app.cris.statistics.util.StatsConfig;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.SolrLogger;
import org.dspace.utils.DSpace;

/**
 * Class defining methods for sending statistics update to users
 * 
 * @author Andrea Bollini
 * @version $Revision: 3762 $
 */
public class ScriptStatSubscribe
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptStatSubscribe.class);

    /**
     * Process subscriptions. Should be run only one time to day, otherwise
     * duplicate notification could be send.
     * 
     * @param service
     * 
     * @param context
     *            DSpace context object
     * @param test
     * @throws SearchServiceException
     */
    public static void processDaily(DSpace dspace,
            StatSubscribeService service, Context context, int freq,
            boolean test) throws SQLException, IOException
    {
        EPerson currentEPerson = null;
        List<StatSubscription> rpSubscriptions = service
                .getAllStatSubscriptionByFreq(freq);
        for (StatSubscription rpSubscription : rpSubscriptions)
        {
            // Does this row relate to the same e-person as the last?
            if ((currentEPerson == null)
                    || (rpSubscription.getEpersonID() != currentEPerson.getID()))
            {
                // New e-person. Send mail for previous e-person
                if (currentEPerson != null)
                {
                    try
                    {
                        sendEmail(context, service, currentEPerson,
                                rpSubscriptions, freq, test);
                    }
                    catch (MessagingException me)
                    {
                        log.error("Failed to send stat subscription to eperson_id="
                                + currentEPerson.getID());
                        log.error(me);
                    }
                }

                currentEPerson = EPerson.find(context,
                        rpSubscription.getEpersonID());
                rpSubscriptions = new ArrayList<StatSubscription>();
            }
            rpSubscriptions.add(rpSubscription);
        }
        // Process the last person
        if (currentEPerson != null)
        {
            try
            {
                sendEmail(context, service, currentEPerson, rpSubscriptions,
                        freq, test);
            }
            catch (MessagingException me)
            {
                log.error("Failed to send stat subscription to eperson_id="
                        + currentEPerson.getID());
                log.error(me);
            }
        }
    }

    /**
     * Sends an email to the given e-person with statistics details of the
     * subscribed objects.
     * 
     * @param context
     *            DSpace context object
     * @param eperson
     *            eperson to send to
     * @param statSubscriptions
     *            List of DSpace Objects
     * @param test
     * @throws IOException
     */
    public static void sendEmail(Context context, StatSubscribeService service,
            EPerson eperson, List<StatSubscription> statSubscriptions,
            int freq, boolean test) throws MessagingException, IOException
    {
        String sfreq = null;
        switch (freq)
        {
        case StatSubscription.FREQUENCY_DAILY:
            sfreq = "daily";
            break;

        case StatSubscription.FREQUENCY_WEEKLY:
            sfreq = "weekly";
            break;

        case StatSubscription.FREQUENCY_MONTHLY:
            sfreq = "monthly";
            break;

        default:
            throw new IllegalArgumentException("Unknow frequency: " + freq);
        }

        // Get a resource bundle according to the eperson language preferences
        Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
        
        String tmpfile = ConfigurationManager.getProperty(
                SolrLogger.CFG_STAT_MODULE, "subscribe-stat.tmpdir")
                + File.separator
                + "stat-"
                + sfreq
                + eperson.getID()
                + "_"
                + Thread.currentThread().getId() + ".xls";
        File file = new File(tmpfile);
        OutputStream os = null;
        try
        {
            os = new FileOutputStream(tmpfile);

            WritableWorkbook workbook = Workbook.createWorkbook(os);

            int r = 0;
            WritableSheet sheet = null;
            int sheetNumber = 0;

            int oldType = -1;
            for (StatSubscription statSub : statSubscriptions)
            {
                boolean changedType = false;
                if (oldType != statSub.getTypeDef())
                {
                    changedType = true;
                    r = 0;
                }
                oldType = statSub.getTypeDef();
                SummaryStatBean statdetails = getStatBean(context, service,
                        statSub);

                r++;
                if (statdetails == null)
                    continue;

                SummaryStatBean.StatDataBean statDataBean = statdetails
                        .getData().get(0);

                if (changedType)
                {
                    sheet = workbook.createSheet(I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.sheet." + statdetails.getType()),
                            sheetNumber);
                    WritableFont labelFont = new WritableFont(
                            WritableFont.ARIAL, 10, WritableFont.BOLD);
                    WritableCellFormat cfobj = new WritableCellFormat(labelFont);
                    sheet.addCell(new Label(
                            0,
                            0,
                            I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.type"),
                            cfobj));
                    sheet.addCell(new Label(
                            1,
                            0,
                            I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.name"),
                            cfobj));
                    sheet.addCell(new Label(
                            2,
                            0,
                            I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.url"),
                            cfobj));
                    sheet.addCell(new Label(
                            3,
                            0,
                            I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.totalView"),
                            cfobj));
                    sheet.addCell(new Label(4, 0, I18nUtil
                            .getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe."
                                    + sfreq + "View"), cfobj));

                    int headerCell = 4;
                    if (statDataBean.isShowSelectedObjectDownload())
                    {
                        sheet.addCell(new Label(
                                ++headerCell,
                                0,
                                I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.totalDownload"),
                                cfobj));
                        sheet.addCell(new Label(++headerCell, 0, I18nUtil
                                .getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe."
                                        + sfreq + "Download"), cfobj));
                    }
                    for (String topKey : statDataBean
                            .getPeriodAndTotalTopView().keySet())
                    {
                        if (!statDataBean.getPeriodAndTotalTopView()
                                .get(topKey).isEmpty())
                        {
                            sheet.addCell(new Label(
                                    ++headerCell,
                                    0,
                                    I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.total"
                                            + topKey + "View"), cfobj));
                            sheet.addCell(new Label(
                                    ++headerCell,
                                    0,
                                    I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe."
                                            + sfreq + topKey + "View"), cfobj));
                        }
                    }
                    for (String topKey : statDataBean
                            .getPeriodAndTotalTopDownload().keySet())
                    {
                        if (!statDataBean.getPeriodAndTotalTopDownload()
                                .get(topKey).isEmpty())
                        {
                            sheet.addCell(new Label(
                                    ++headerCell,
                                    0,
                                    I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.total"
                                            + topKey + "Download"), cfobj));
                            sheet.addCell(new Label(
                                    ++headerCell,
                                    0,
                                    I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe."
                                            + sfreq + topKey + "Download"), cfobj));
                        }
                    }
                    for (int i = 0; i < headerCell; i++)
                    {
                        final CellView view = sheet.getColumnView(i);
                        view.setAutosize(true);
                        sheet.setColumnView(i, view);
                    }
                    sheetNumber++;
                }
                sheet.addCell(new Label(0, r, I18nUtil
                        .getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.type."
                                + statdetails.getType())));
                sheet.addCell(new Label(1, r, statdetails.getObjectName()));
                sheet.addCell(new Label(2, r, statdetails.getObjectURL()));
                if (statDataBean.getTotalSelectedView() == -1)
                {
                    sheet.addCell(new Label(3, r, I18nUtil
                            .getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.na")));
                }
                else
                {
                    sheet.addCell(new Label(3, r, new Long(statDataBean
                            .getTotalSelectedView()).toString()));
                }

                if (statDataBean.getPeriodSelectedView() == -1)
                {
                    sheet.addCell(new Label(4, r, I18nUtil
                            .getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.na")));
                }
                else
                {
                    sheet.addCell(new Label(4, r, new Long(statDataBean
                            .getPeriodSelectedView()).toString()));
                }

                int countTopCell = 4;
                if (statDataBean.isShowSelectedObjectDownload())
                {
                    if (statDataBean.getTotalSelectedDownload() == -1)
                    {
                        sheet.addCell(new Label(
                                ++countTopCell,
                                r,
                                I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.na")));
                    }
                    else
                    {
                        sheet.addCell(new Label(++countTopCell, r, new Long(
                                statDataBean.getTotalSelectedDownload())
                                .toString()));
                    }
                    if (statDataBean.getPeriodSelectedDownload() == -1)
                    {
                        sheet.addCell(new Label(
                                ++countTopCell,
                                r,
                                I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.na")));
                    }
                    else
                    {
                        sheet.addCell(new Label(++countTopCell, r, new Long(
                                statDataBean.getPeriodSelectedDownload())
                                .toString()));
                    }

                }
                for (String topKey : statDataBean.getPeriodAndTotalTopView()
                        .keySet())
                {
                    List<Long> tmpList = statDataBean
                            .getPeriodAndTotalTopView().get(topKey);
                    if (!tmpList.isEmpty())
                    {
                        if (tmpList.get(1) == null)
                        {
                            sheet.addCell(new Label(
                                    ++countTopCell,
                                    r,
                                    I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.na")));
                        }
                        else
                        {
                            sheet.addCell(new Label(++countTopCell, r,
                                    new Long(tmpList.get(1)).toString()));
                        }
                    }
                    if (!tmpList.isEmpty())
                    {
                        if (tmpList.get(0) == null)
                        {
                            sheet.addCell(new Label(
                                    ++countTopCell,
                                    r,
                                    I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.na")));
                        }
                        else
                        {
                            sheet.addCell(new Label(++countTopCell, r,
                                    new Long(tmpList.get(0)).toString()));
                        }
                    }
                }
                for (String topKey : statDataBean
                        .getPeriodAndTotalTopDownload().keySet())
                {
                    List<Long> tmpList = statDataBean
                            .getPeriodAndTotalTopDownload().get(topKey);
                    if (!tmpList.isEmpty())
                    {
                        if (tmpList.get(1) == null)
                        {
                            sheet.addCell(new Label(
                                    ++countTopCell,
                                    r,
                                    I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.na")));
                        }
                        else
                        {
                            sheet.addCell(new Label(++countTopCell, r,
                                    new Long(tmpList.get(1)).toString()));
                        }
                    }
                    if (!tmpList.isEmpty())
                    {
                        if (tmpList.get(0) == null)
                        {
                            sheet.addCell(new Label(
                                    ++countTopCell,
                                    r,
                                    I18nUtil.getMessage("org.dspace.app.cris.batch.ScriptStatSubscribe.na")));
                        }
                        else
                        {
                            sheet.addCell(new Label(++countTopCell, r,
                                    new Long(tmpList.get(0)).toString()));
                        }
                    }
                }

            }
            workbook.write();
            workbook.close();
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            throw new MessagingException(
                    "Failed to create the stat report attachment", e);
        }
        finally
        {
            if (os != null)
            {
                os.close();
            }
        }

        if (test)
        {
            System.out
                    .println("After check remove the follow file: " + tmpfile);
        }
        else
        {
            Email email = Email.getEmail(I18nUtil
                    .getEmailFilename(supportedLocale, "statsubscription-"
                            + sfreq));
            email.addRecipient(eperson.getEmail());
            email.addArgument(ConfigurationManager.getProperty("dspace.url")
                    + "/cris/tools/stats/subscription/unsubscribe?clear=all");
            email.addArgument(ConfigurationManager.getProperty("dspace.url")
                    + "/cris/tools/stats/subscription/list.htm");
            email.addAttachment(file, "stat-" + sfreq + "-update.xls");
            email.send();
            file.delete();
        }
        log.info(LogManager.getHeader(context, "sent_statsubscription",
                "eperson_id=" + eperson.getID() + ", freq=" + sfreq));
    }

    private static SummaryStatBean getStatBean(Context context,
            StatSubscribeService service, StatSubscription statSub)
            throws SolrServerException, SQLException
    {
        try
        {
            return service.getStatBean(context, statSub.getUid(),
                    statSub.getTypeDef(), statSub.getFreq(), 1);
        }
        catch (IllegalArgumentException e)
        {
            log.warn("Found invalid StatSubscription - StatSubscriptionID: "
                    + statSub.getId(), e);
            return null;
        }
    }

    /**
     * Method for invoking subscriptions via the command line
     * 
     * @param argv
     *            command-line arguments, none used yet
     */
    public static void main(String[] argv)
    {
        log.info("#### START STAT SUBSCRIBE: -----" + new Date()
                + " ----- ####");
        String usage = "org.dspace.app.cris.batch.ScriptStatSubscribe [-t] or nothing to send out subscriptions.";

        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        {
            Option opt = new Option("m", "mode", true,
                    "mode (1: daily, 7: weekly, 30: monthly)");
            opt.setRequired(true);
            options.addOption(opt);
        }

        {
            Option opt = new Option("t", "test", false, "Run test session");
            opt.setRequired(false);
            options.addOption(opt);
        }

        {
            Option opt = new Option("h", "help", false,
                    "Print this help message");
            opt.setRequired(false);
            options.addOption(opt);
        }

        try
        {
            line = new PosixParser().parse(options, argv);
        }
        catch (Exception e)
        {
            // automatically generate the help statement
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }

        String sfreq = line.getOptionValue('m');

        int freq = 0;

        try
        {
            freq = Integer.parseInt(sfreq);
        }
        catch (NumberFormatException nfe)
        {
            // ... do nothing as we deal with this next freq = 0

        }

        switch (freq)
        {
        case StatSubscription.FREQUENCY_DAILY:
        case StatSubscription.FREQUENCY_WEEKLY:
        case StatSubscription.FREQUENCY_MONTHLY:
            // ok
            break;
        default:
            System.out.println("Mode MUST be one of 1, 7 or 30");
            System.exit(1);
            break;
        }

        if (line.hasOption("h"))
        {
            // automatically generate the help statement
            formatter.printHelp(usage, options);
            System.exit(1);
        }

        boolean test = line.hasOption("t");

        if (test)
            log.setLevel(Level.DEBUG);
        Context context = null;
        try
        {
            DSpace dspace = new DSpace();
            StatSubscribeService service = dspace.getServiceManager()
                    .getServiceByName("statSubscribeService",
                            StatSubscribeService.class);

            context = new Context();
            processDaily(dspace, service, context, freq, test);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                // Nothing is actually written
                context.abort();
            }
        }
        log.info("#### END: -----" + new Date() + " ----- ####");
        // System.exit(0);
    }
}
