/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import org.apache.log4j.Logger;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;

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

    int status = Curator.CURATE_UNSET;

    private static final String PLUGIN_PREFIX = "translator";
    private static String authLangField = "dc.language";
    private static String authLang = "en";
    private static String[] toTranslate;
    private static String[] langs;

    private static String apiKey = "";

    private static Logger log = Logger.getLogger(AbstractTranslator.class);

    private List<String> results = new ArrayList<String>();


    @Override
    public void init(Curator curator, String taskId) throws IOException
    {
        super.init(curator, taskId);

        // Load configuration
        authLang = ConfigurationManager.getProperty("default.locale");
        authLangField = ConfigurationManager.getProperty(PLUGIN_PREFIX, "translate.field.language");
        String toTranslateStr = ConfigurationManager.getProperty(PLUGIN_PREFIX, "translate.field.targets");
        String langsStr = ConfigurationManager.getProperty(PLUGIN_PREFIX, "translate.language.targets");
        toTranslate = toTranslateStr.split(",");
        langs = langsStr.split(",");

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

            Metadatum[] authLangs = item.getMetadataByMetadataString(authLangField);
            if(authLangs.length > 0)
            {
                /* Assume the first... multiple
                  "authoritative" languages won't work */
                authLang = authLangs[0].value;
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
                    Metadatum[] fieldMetadata = null;
                    
                    if(fieldSegments.length > 2) {
                        // First, check to see if we've already got this in the target language
                        Metadatum[] checkMetadata = item.getMetadata(fieldSegments[0], fieldSegments[1], fieldSegments[2], lang);
                        if(checkMetadata.length > 0)
                        {
                            // We've already translated this, move along
                            log.debug(handle + "already has " + field + " in " + lang + ", skipping");
                            results.add(handle + ": Skipping " + lang + " translation " + "(" + field + ")");
                            translated = true;
                        }

                        // Let's carry on and get the authoritative version, then
                        fieldMetadata = item.getMetadata(fieldSegments[0], fieldSegments[1], fieldSegments[2], authLang);

                    }
                    else {
                        // First, check to see if we've already got this in the target language
                        Metadatum[] checkMetadata = item.getMetadata(fieldSegments[0], fieldSegments[1], null, lang);
                        if(checkMetadata.length > 0)
                        {
                            // We've already translated this, move along
                            log.debug(handle + "already has " + field + " in " + lang + ", skipping");
                            results.add(handle + ": Skipping " + lang + " translation " + "(" + field + ")");
                            translated = true;
                        }

                        // Let's carry on and get the authoritative version, then
                        fieldMetadata = item.getMetadata(fieldSegments[0], fieldSegments[1], null, authLang);


                    }

                    if(!translated && fieldMetadata.length > 0)
                    {
                        for(Metadatum metadataValue : fieldMetadata) {
                            String value = metadataValue.value;
                            String translatedText = translateText(authLang, lang, value);
                            if(translatedText != null && !"".equals(translatedText))
                            {
                                // Add the new metadata
                                if(fieldSegments.length > 2) {
                                    item.addMetadata(fieldSegments[0], fieldSegments[1], fieldSegments[2], lang, translatedText);
                                }
                                else {
                                    item.addMetadata(fieldSegments[0], fieldSegments[1], null, lang, translatedText);
                                }

                                try {
                                    item.update();
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
         * apiKey = ConfigurationManager.getProperty(PLUGIN_PREFIX, "translate.api.key.[service]");
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

