package org.dspace.app.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.IConverter;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;

public abstract class AConfigurableImporter<T> extends ASchedulableImporter
{
    // Patter to extract the converter name if any
    private static final Pattern converterPattern = Pattern
            .compile(".*\\((.*)\\)");

    private DCInputsReader dcInputsReader = null;

    /** Location of config file */
    private final String configFilePath = ConfigurationManager
            .getProperty("dspace.dir")
            + File.separator
            + "config"
            + File.separator + "crosswalks" + File.separator;

    private Properties importerProps = null;

    private IConverter nilConverter = new IConverter()
    {
        public String makeConversion(String value)
        {
            return value;
        }
    };

    public synchronized void init()
    {
        if (importerProps != null && dcInputsReader != null)
            return;
        try
        {
            dcInputsReader = new DCInputsReader();
        }
        catch (DCInputsReaderException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }

        // Read in configuration
        importerProps = new Properties();

        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(configFilePath
                    + getConfigurationFileName());
            importerProps.load(fis);
        }
        catch (Exception notfound)
        {
            throw new IllegalArgumentException(
                    "Impossibile leggere la configurazione per l'importer "
                            + getConfigurationFileName(), notfound);
        }
        finally
        {
            if (fis != null)
            {
                try
                {
                    fis.close();
                }
                catch (IOException ioe)
                {
                    log.error(ioe.getMessage(), ioe);
                }
            }
        }

    }

    protected String getConfigurationFileName()
    {
        return getPluginInstanceName() + ".properties";
    }

    protected Properties getImporterConfiguration()
    {
        init();
        return importerProps;
    }

    protected final String getTargetCollectionFormName(T itemToImport)
    {
        String targetCollection = getImporterConfiguration().getProperty(
                "target_collection." + getType(itemToImport),
                getImporterConfiguration().getProperty(
                        "target_collection.default"));
        if (targetCollection == null)
        {
            throw new RuntimeException("The type " + getType(itemToImport)
                    + " cannot be imported");
        }
        targetCollection = targetCollection.trim();
        return targetCollection;
    }

    protected Collection getCollection(Context context, Community community,
            Collection collection, T crossitem) throws SQLException,
            DCInputsReaderException
    {
        if (collection != null)
            return collection;

        String targetCollection = getTargetCollectionFormName(crossitem);
        // Show all collections
        Collection[] collections = Collection.findAuthorized(context,
                community, Constants.ADD);
        for (Collection coll : collections)
        {
            if (targetCollection.equals(dcInputsReader.getInputs(
                    coll.getHandle()).getFormName()))
            {
                return coll;
            }
        }
        throw new RuntimeException(
                "The type "
                        + getType(crossitem)
                        + " cannot be imported - no valid target collection has been found");
    }

    protected abstract String getType(T crossitem);

    protected final void removeInvalidMetadata(Context context,
            WorkspaceItem witem, String type)
    {
        Item item = witem.getItem();
        List<String[]> metadata = getCheckMetadataList(type);
        for (String[] md : metadata)
        {
            SanitizeImportedMetadata sim = (SanitizeImportedMetadata) PluginManager
                    .getNamedPlugin(SanitizeImportedMetadata.class, md[3]);
            sim.sanitize(context, item, md[0], md[1], md[2]);
        }
    }

    private List<String[]> getCheckMetadataList(String type)
    {
        List<String[]> result = new ArrayList<String[]>();
        for (Object key : getImporterConfiguration().keySet())
        {
            String keyStr = (String) key;
            if (keyStr.startsWith("sanitize.")
                    || keyStr.startsWith(type + ".sanitize."))
            {
                String md = keyStr.startsWith("sanitize.") ? keyStr
                        .substring(9) : keyStr.substring(type.length() + 10);

                String[] mdSplit = new String[4];

                String tmpSplit[] = md.split("\\.");
                if (tmpSplit.length == 3)
                {
                    mdSplit = new String[4];
                    mdSplit[0] = tmpSplit[0];
                    mdSplit[1] = tmpSplit[1];
                    mdSplit[2] = tmpSplit[2];
                }
                else if (tmpSplit.length == 2)
                {
                    mdSplit = new String[4];
                    mdSplit[0] = tmpSplit[0];
                    mdSplit[1] = tmpSplit[1];
                    mdSplit[2] = null;
                }

                mdSplit[3] = getImporterConfiguration().getProperty(keyStr);
                result.add(mdSplit);
            }
        }
        return result;
    }

    private IConverter getConverterForField(String name, String formName)
    {
        String md = getImporterConfiguration().getProperty(
                formName + "." + name,
                getImporterConfiguration().getProperty(name)).trim();
        Matcher converterMatcher = converterPattern.matcher(md);

        if (converterMatcher.matches())
        {
            String converterName = converterMatcher.group(1);
            IConverter converter = (IConverter) PluginManager.getNamedPlugin(
                    IConverter.class, converterName);
            if (converter == null)
            {
                throw new RuntimeException("Non � stato trovato il converter "
                        + converterName);
            }
            return converter;
        }
        else
        {
            return nilConverter;
        }
    }

    private String[] getMetadataForField(String name, String formName)
    {
        String md = getImporterConfiguration().getProperty(
                formName + "." + name,
                getImporterConfiguration().getProperty(name));

        if (md == null)
        {
            return null;
        }
        else
        {
            md = md.trim();
        }

        Matcher converterMatcher = converterPattern.matcher(md);

        if (converterMatcher.matches())
        {
            md = md.replaceAll("\\(" + converterMatcher.group(1) + "\\)", "");
        }

        String mdSplit[] = null;
        if (StringUtils.isNotBlank(md))
        {
            String tmpSplit[] = md.split("\\.");
            if (tmpSplit.length == 3)
            {
                mdSplit = new String[3];
                mdSplit[0] = tmpSplit[0];
                mdSplit[1] = tmpSplit[1];
                mdSplit[2] = tmpSplit[2];
            }
            else if (tmpSplit.length == 2)
            {
                mdSplit = new String[3];
                mdSplit[0] = tmpSplit[0];
                mdSplit[1] = tmpSplit[1];
                mdSplit[2] = null;
            }
        }
        return mdSplit;
    }

    protected final void extractMetadata(Context context, WorkspaceItem witem,
            String type)
    {
        List<String[]> metadata = getExtractMetadataList(type);
        Item item = witem.getItem();
        for (String[] md : metadata)
        {
            EnhanceImportedMetadata eim = (EnhanceImportedMetadata) PluginManager
                    .getNamedPlugin(EnhanceImportedMetadata.class, md[3]);
            eim.enhance(context, item, md[0], md[1], md[2]);
        }
    }

    protected final void addMetadata(Item item, String fieldName,
            String formName, String value)
    {
        if (StringUtils.isNotBlank(value))
        {
            String[] metadata = getMetadataForField(fieldName, formName);
            if (metadata != null)
            {
                IConverter converter = getConverterForField(fieldName, formName);
                String convertedValue = converter.makeConversion(value);
                if (convertedValue != null && !convertedValue.trim().isEmpty())
                {
                    item.addMetadata(metadata[0], metadata[1], metadata[2],
                            "en", convertedValue.trim());
                }
            }
        }
    }

    private List<String[]> getExtractMetadataList(String type)
    {
        List<String[]> result = new ArrayList<String[]>();
        for (Object key : getImporterConfiguration().keySet())
        {
            String keyStr = (String) key;
            if (keyStr.startsWith("enhance.")
                    || keyStr.startsWith(type + ".enhance."))
            {
                String md = keyStr.startsWith("enhance.") ? keyStr.substring(8)
                        : keyStr.substring(type.length() + 9);
                String[] mdSplit = new String[4];

                String tmpSplit[] = md.split("\\.");
                if (tmpSplit.length == 3)
                {
                    mdSplit = new String[4];
                    mdSplit[0] = tmpSplit[0];
                    mdSplit[1] = tmpSplit[1];
                    mdSplit[2] = tmpSplit[2];
                }
                else if (tmpSplit.length == 2)
                {
                    mdSplit = new String[4];
                    mdSplit[0] = tmpSplit[0];
                    mdSplit[1] = tmpSplit[1];
                    mdSplit[2] = null;
                }

                mdSplit[3] = getImporterConfiguration().getProperty(keyStr)
                        .trim();
                result.add(mdSplit);
            }
        }
        return result;
    }

}
