package org.dspace.app.cris.discovery;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.utils.DSpace;

public class MultilanguageAuthorityIndexer implements SolrServiceIndexPlugin
{

    Logger log = Logger.getLogger(MultilanguageAuthorityIndexer.class);

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document, Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        if (dso instanceof Item)
        {
            try
            {
                List<String> authorities = MetadataAuthorityManager.getManager()
                        .getAuthorityMetadata();
                Item item = Item.find(context, dso.getID());

                for (String fieldKey : authorities)
                {
                    String makeFieldKey = ChoiceAuthorityManager.getManager()
                            .makeFieldKey(fieldKey);
                    if (ChoiceAuthorityManager.getManager()
                            .isChoicesConfigured(makeFieldKey))
                    {

                        boolean isMultilanguage = new DSpace()
                                .getConfigurationService().getPropertyAsType(
                                        "discovery.authority.multilanguage."
                                                + makeFieldKey,
                                        false);

                        if (isMultilanguage)
                        {
                            Metadatum[] metadatums = item
                                    .getMetadataByMetadataString(fieldKey);
                            for (Metadatum metadatum : metadatums)
                            {

                                String value = metadatum.value;
                                if (StringUtils.isNotBlank(value))
                                {
                                    String locales = ConfigurationManager
                                            .getProperty(
                                                    "webui.supported.locales");
                                    if (!StringUtils.isEmpty(locales))
                                    {
                                        String[] localesArray = locales
                                                .split("\\s*,\\s*");
                                        for (String locStr : localesArray)
                                        {
                                            Locale loc = new Locale(locStr);
                                            indexAuthorityLabel(document,
                                                    makeFieldKey, metadatum.authority,
                                                    loc != null
                                                            ? loc.getLanguage()
                                                            : null);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (SQLException e)
            {
                log.error(e.getMessage(), e);
            }
        }

    }

    private void indexAuthorityLabel(SolrInputDocument doc, String fieldKey,
            String authority, String loc)
    {
        String indexValue = ChoiceAuthorityManager.getManager()
                .getLabel(fieldKey, authority, loc);
        indexValue = StringUtils.isEmpty(indexValue) ? authority : indexValue;

        doc.addField(fieldKey + "multilang_ac", indexValue);
        doc.addField(fieldKey + "multilang_keyword", indexValue);
    }
}
