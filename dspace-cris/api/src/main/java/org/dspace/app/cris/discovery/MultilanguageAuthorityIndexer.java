package org.dspace.app.cris.discovery;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;

public class MultilanguageAuthorityIndexer implements SolrServiceIndexPlugin
{

    Logger log = Logger.getLogger(MultilanguageAuthorityIndexer.class);

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document)
    {
        if (dso instanceof Item)
        {
            try
            {
                Set<String> authorities = ChoiceAuthorityManager.getManager()
                        .getAuthorities();
                Item item = Item.find(context, dso.getID());
                
                for (String fieldKey : authorities)
                {
                    String value = item.getMetadata(fieldKey);
                    String locales = ConfigurationManager
                            .getProperty("webui.supported.locales");
                    if (!StringUtils.isEmpty(locales))
                    {
                        String[] localesArray = locales.split("\\s*,\\s*");
                        for (String locStr : localesArray)
                        {
                            Locale loc = new Locale(locStr);
                            indexAuthorityLabel(document,
                                    fieldKey, value,
                                    loc != null ? loc.getLanguage() : null);
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

    private void indexAuthorityLabel(SolrInputDocument doc,
            String fieldKey, String authority, String loc)
    {
        String indexValue = ChoiceAuthorityManager.getManager()
                .getLabel(fieldKey, authority, loc);
        indexValue = StringUtils.isEmpty(indexValue) ? authority : indexValue;

        doc.addField(fieldKey + "multilang_ac", indexValue);
        doc.addField(fieldKey + "multilang_keyword", indexValue);
    }
}
