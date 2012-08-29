package org.dspace.app.importer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.eperson.EPerson;

public abstract class ASchedulableImporter extends SelfNamedPlugin implements
        Importer
{

    /** log4j logger */
    protected static Logger log = Logger.getLogger(ASchedulableImporter.class);

    public ImportResultBean ingest(String data, Community community,
            EPerson eperson)
    {
        return ingest(data, community, null, eperson);
    }

    public ImportResultBean ingest(String data, Collection collection,
            EPerson eperson)
    {
        return ingest(data, null, collection, eperson);
    }

    public ImportResultBean ingest(String data, EPerson eperson)
    {
        return ingest(data, null, null, eperson);
    }

    public ImportResultBean ingest(String data, Community community,
            Collection collection, EPerson eperson)
    {
        ImportResultBean result = new ImportResultBean();
        int tot = getTotal(data);

        // see whether configurated max number of import items excedeed total
        // value
        int limit = ConfigurationManager.getIntProperty(
                "importer.max-limitimport." + getPluginInstanceName(), 20);

        if (tot > limit)
        {
            throw new ImporterException(
                    "Max limited items import excedeed - LIMIT=" + limit
                            + " REQUESTITEMS=" + tot, limit, tot,
                    getPluginInstanceName());
        }

        boolean scheduled = false;
        List<SingleImportResultBean> results = null;
        if (ConfigurationManager.getIntProperty("importer.max-sync."
                + getPluginInstanceName(), -1) != -1
                && ConfigurationManager.getIntProperty("importer.max-sync."
                        + getPluginInstanceName()) < tot)
        {
            scheduled = true;
            Thread go = new ThreadImporter(eperson.getID(), community,
                    collection, tot, data);
            go.start();
        }
        else
        {
            results = processData(data, community, collection, eperson);
        }
        result.setTot(tot);
        result.setScheduled(scheduled);
        result.setDetails(results);
        return result;
    }

    protected abstract int getTotal(String data);

    protected abstract List<SingleImportResultBean> processData(String data,
            Community community, Collection collection, EPerson eperson);

    public void sendResultEmail(ImportResultBean result, EPerson eperson)
            throws IOException, MessagingException
    {
        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(
                I18nUtil.getEPersonLocale(eperson), "itemimport-result"));
        email.addArgument(result.getTot());
        email.addArgument(result.getSuccess());
        email.addArgument(result.getWarning());
        email.addArgument(result.getFailure());
        email.addArgument(I18nUtil.getMessage("jsp.tools.import-item.plugin."
                + getPluginInstanceName()));
        StringBuffer sb = new StringBuffer();
        if (result.getDetails() != null)
        {
            sb.append(I18nUtil
                    .getMessage("jsp.tools.import-item.details.importIdentifier")
                    + "\t"
                    + I18nUtil
                            .getMessage("jsp.tools.import-item.details.status")
                    + "\t"
                    + I18nUtil
                            .getMessage("jsp.tools.import-item.details.message")
                    + "\n");
            for (SingleImportResultBean r : result.getDetails())
            {
                sb.append(r.getImportIdentifier()
                        + "\t"
                        + I18nUtil
                                .getMessage("jsp.tools.import-item.details.status"
                                        + r.getStatus()) + "\t"
                        + r.getMessage() + "\n");
            }
        }
        email.addArgument(sb.toString());
        email.addRecipient(eperson.getEmail());
        email.send();
    }

    class ThreadImporter extends Thread
    {
        private int epersonid;

        private int tot;

        private String importData;

        private Community com;

        private Collection col;

        public ThreadImporter(int epersonid, Community community,
                Collection collection, int tot, String importData)
        {
            this.tot = tot;
            this.epersonid = epersonid;
            this.importData = importData;
            this.com = community;
            this.col = collection;
        }

        public void run()
        {
            Context context = null;
            try
            {
                context = new Context();
                EPerson eperson = EPerson.find(context, epersonid);
                ImportResultBean result = new ImportResultBean();
                List<SingleImportResultBean> results = processData(importData,
                        com, col, eperson);
                result.setTot(tot);
                result.setScheduled(true);
                result.setDetails(results);
                sendResultEmail(result, eperson);
            }
            catch (Exception e)
            {
                try
                {
                    log.error(e.getMessage(), e);
                    sendFailureEmail(e);
                }
                catch (Exception e1)
                {
                    // non posso fare nulla...
                }
            }
            finally
            {
                if (context != null && context.isValid())
                {
                    context.abort();
                }
            }
        }

        private void sendFailureEmail(Exception exception) throws IOException,
                MessagingException
        {
            String alertRecipient = ConfigurationManager
                    .getProperty("alert.recipient");
            if (StringUtils.isNotBlank(alertRecipient))
            {
                Email email = ConfigurationManager.getEmail(I18nUtil
                        .getEmailFilename(I18nUtil.getDefaultLocale(),
                                "itemimport-error"));
                email.addArgument(getPluginInstanceName());
                email.addArgument(new Date());
                String stackTrace;

                if (exception != null)
                {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                }
                else
                {
                    stackTrace = "No exception";
                }
                email.addArgument(stackTrace);
                email.addRecipient(alertRecipient);
                email.send();
            }
        }

    }
}
