/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import org.apache.log4j.Logger;
import org.dspace.content.MetadataValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * MicrosoftTranslator translates stuff
 *
 * @author Kim Shepherd
 */
@Distributive
public abstract class AbstractTranslator extends AbstractCurationTask
{

    protected int status = Curator.CURATE_UNSET;

    protected final String PLUGIN_PREFIX = "translator";
    protected String authLangField = "dc.language";
    protected String authLang = "en";
    protected String[] toTranslate;
    protected String[] langs;

    protected String apiKey = "";

    private static Logger log = Logger.getLogger(AbstractTranslator.class);

    protected List<String> results = new ArrayList<String>();
    
    private final transient ConfigurationService configurationService
             = DSpaceServicesFactory.getInstance().getConfigurationService();


    @Override
    public void init(Curator curator, String taskId) throws IOException
    {
        super.init(curator, taskId);

        // Load configuration
        authLang = configurationService.getProperty("default.locale");
        authLangField = configurationService.getProperty(PLUGIN_PREFIX + ".field.language");
        String[] toTranslate = configurationService.getArrayProperty(PLUGIN_PREFIX + ".field.targets");
        String[] langs = configurationService.getArrayProperty(PLUGIN_PREFIX + ".language.targets");

        if(!(toTranslate.length > 0 && langs.length > 0))
        {
            status = Curator.CURATE_ERROR;
            results.add("Configuration error");
            setResult(results.toString());
            report(results.toString());

            return;
        }

        initApi();

    }

    @Override
    public int perform(DSpaceObject dso) throws IOException
    {

        if(dso instanceof Item)
        {
            Item item = (Item) dso;

            /*
             * We lazily set success here because our success or failure
             * is per-field, not per-item
             */

            status = Curator.CURATE_SUCCESS;

            String handle = item.getHandle();
            log.debug("Translating metadata for " + handle);

            List<MetadataValue> authLangs = itemService.getMetadataByMetadataString(item, authLangField);
            if(authLangs.size() > 0)
            {
                /* Assume the first... multiple
                  "authoritative" languages won't work */
                authLang = authLangs.get(0).getValue();
                log.debug("Authoritative language for " + handle + " is " + authLang);
            }

            for(String lang : langs)
            {
                lang = lang.trim();

                for(String field : toTranslate)
                {
                    boolean translated = false;
                    field = field.trim();
                    String[] fieldSegments = field.split("\\.");
                    List<MetadataValue> fieldMetadata = null;
                    
                    if(fieldSegments.length > 2) {
                        // First, check to see if we've already got this in the target language
                        List<MetadataValue> checkMetadata = itemService.getMetadata(item, fieldSegments[0], fieldSegments[1], fieldSegments[2], lang);
                        if(checkMetadata.size() > 0)
                        {
                            // We've already translated this, move along
                            log.debug(handle + "already has " + field + " in " + lang + ", skipping");
                            results.add(handle + ": Skipping " + lang + " translation " + "(" + field + ")");
                            translated = true;
                        }

                        // Let's carry on and get the authoritative version, then
                        fieldMetadata = itemService.getMetadata(item, fieldSegments[0], fieldSegments[1], fieldSegments[2], authLang);

                    }
                    else {
                        // First, check to see if we've already got this in the target language
                        List<MetadataValue> checkMetadata = itemService.getMetadata(item, fieldSegments[0], fieldSegments[1], null, lang);
                        if(checkMetadata.size() > 0)
                        {
                            // We've already translated this, move along
                            log.debug(handle + "already has " + field + " in " + lang + ", skipping");
                            results.add(handle + ": Skipping " + lang + " translation " + "(" + field + ")");
                            translated = true;
                        }

                        // Let's carry on and get the authoritative version, then
                        fieldMetadata = itemService.getMetadata(item, fieldSegments[0], fieldSegments[1], null, authLang);


                    }

                    if(!translated && fieldMetadata.size() > 0)
                    {
                        for(MetadataValue metadataValue : fieldMetadata) {
                            String value = metadataValue.getValue();
                            String translatedText = translateText(authLang, lang, value);
                            if(translatedText != null && !"".equals(translatedText))
                            {
                                try {
                                    // Add the new metadata
                                    if(fieldSegments.length > 2) {
                                        itemService.addMetadata(Curator.curationContext(), item, fieldSegments[0], fieldSegments[1], fieldSegments[2], lang, translatedText);
                                    }
                                    else {
                                        itemService.addMetadata(Curator.curationContext(), item, fieldSegments[0], fieldSegments[1], null, lang, translatedText);
                                    }

                                    itemService.update(Curator.curationContext(), item);
                                    results.add(handle + ": Translated " + authLang + " -> " + lang + " (" + field + ")");
                                }
                                catch(Exception e) {
                                    log.info(e.getLocalizedMessage());
                                    status = Curator.CURATE_ERROR;
                                }

                            }
                            else {
                                results.add(handle + ": Failed translation of " + authLang + " -> " + lang + "(" + field + ")");
                            }
                        }
                    }
                }
            }
        }

        processResults();
        return status;

    }

    protected void initApi() {
        /*
         * Override this method in your translator
         * Only needed to set key, etc.
         * apiKey = ConfigurationManager.getProperty(PLUGIN_PREFIX, "translator.api.key.[service]");
         *
         */
    }

    protected String translateText(String from, String to, String text) throws IOException {

        // Override this method in your translator
        return null;
    }

    private void processResults() throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Translation report: \n----------------\n");
        for(String result : results)
        {
            sb.append(result).append("\n");
        }
        setResult(sb.toString());
        report(sb.toString());

    }

}

