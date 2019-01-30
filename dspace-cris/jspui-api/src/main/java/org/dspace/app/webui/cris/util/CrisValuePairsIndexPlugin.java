/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.discovery.CrisServiceIndexPlugin;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.webui.cris.web.tag.PropertyDefinitionI18NWrapper;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.services.ConfigurationService;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.web.tag.JDynATagLibraryFunctions;
import it.cilea.osd.jdyna.widget.WidgetCheckRadio;

/**
 * 
 * @author Luigi Andrea Pascarelli
 *
 */
public class CrisValuePairsIndexPlugin implements CrisServiceIndexPlugin,
        SolrServiceIndexPlugin
{

    private static final Logger log = Logger
            .getLogger(CrisValuePairsIndexPlugin.class);

    private ApplicationService applicationService;

    private ConfigurationService configurationService;

    private Map<String, DCInputsReader> dcInputsReader = new HashMap<>();

    private String separator;

    private void init() throws DCInputsReaderException
    {
        if (separator == null)
        {
            separator = configurationService
                    .getProperty("discovery.solr.facets.split.char");
            if (separator == null)
            {
                separator = SolrServiceImpl.FILTER_SEPARATOR;
            }
        }
        
        if(dcInputsReader.isEmpty()) {
            for (Locale locale : I18nUtil.getSupportedLocales())
            {
                dcInputsReader.put(locale.getLanguage(),
                    new DCInputsReader(I18nUtil.getInputFormsFileName(locale)));
            }
        }

    }

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
            SolrInputDocument document,
            Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        try
        {
            init();
        }
        catch (DCInputsReaderException e)
        {
            log.error(e.getMessage(), e);
        }
        if (crisObject != null)
        {
            Map<String, String> authorityMapToWrite = new HashMap<String, String>();
            for (Locale locale : I18nUtil.getSupportedLocales())
            {
                String language = locale.getLanguage();
                String schema = "cris" + crisObject.getPublicPath();
                List<TP> allPropertiesDefinition = applicationService
                        .getAllPropertiesDefinitionWithRadioCheckDropdown(
                                crisObject.getClassPropertiesDefinition());
                for (TP pd : allPropertiesDefinition)
                {
                    List<P> storedP = crisObject.getAnagrafica4view()
                            .get(pd.getShortName());
                    for (P stored_value : storedP)
                    {
                        String field = schema + "."
                                + stored_value.getTypo().getShortName();
                        String displayVal = JDynATagLibraryFunctions.getCheckRadioDisplayValue(PropertyDefinitionI18NWrapper.getWidgetCheckRadioWrapper((WidgetCheckRadio) pd.getRendering(), pd.getAnagraficaHolderClass().getSimpleName(), pd.getShortName(), locale).getStaticValues(),
                                stored_value.toString());
                        if (StringUtils.isBlank(displayVal))
                        {
                            displayVal = stored_value.toString();
                        }
                        String prefixedDisplayVal = language + "_" + displayVal;
                        
                        buildSearchFilter(document, searchFilters,
                                stored_value.toString(), field, field,
                                displayVal, prefixedDisplayVal, authorityMapToWrite);
                        
                    }
                }
            }
            for(String indexFieldName : authorityMapToWrite.keySet()) {
                document.addField(indexFieldName, authorityMapToWrite.get(indexFieldName));
            }
        }
    }

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACNO crisObject, SolrInputDocument document,
            Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        // TODO manage nested
        // try
        // {
        // init();
        // }
        // catch (DCInputsReaderException e)
        // {
        // log.error(e.getMessage(), e);
        // }
        // if (crisObject != null)
        // {
        // ICrisObject<P, TP> parent = (ICrisObject<P, TP>) crisObject
        // .getParent();
        // String confName = "ncris" + parent.getPublicPath();
        // String schema = confName + crisObject.getTypo().getShortName();
        // List<NTP> allPropertiesDefinition = applicationService
        // .getAllPropertiesDefinitionWithRadioCheckDropdown(
        // crisObject.getClassPropertiesDefinition());
        // for (NTP pd : allPropertiesDefinition)
        // {
        // List<NP> storedP = crisObject.getAnagrafica4view()
        // .get(pd.getShortName());
        // for (NP stored_value : storedP)
        // {
        // String field = schema + "."
        // + stored_value.getTypo().getShortName();
        // String displayVal = getCheckRadioDisplayValue(
        // (((WidgetCheckRadio) pd.getRendering())
        // .getStaticValues()),
        // stored_value.toString());
        // document.removeField(field + "_authority");
        // document.addField(field + "_authority", stored_value);
        // document.removeField(field);
        // document.addField(field, displayVal);
        // buildSearchFilter(document, searchFilters,
        // stored_value.toString(), field, field, displayVal);
        // }
        // }
        // }
    }

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document,
            Map<String, List<DiscoverySearchFilter>> searchFilters)
    {
        try
        {
            init();
        }
        catch (DCInputsReaderException e)
        {
            log.error(e.getMessage(), e);
        }
        if (dso != null)
        {
            if (dso.getType() == Constants.ITEM)
            {
                Item item = (Item) dso;
                try
                {
                    Map<String, String> authorityMapToWrite = new HashMap<String, String>();
                    for (String language : dcInputsReader.keySet())
                    {
                        DCInputSet dcInputSet = dcInputsReader.get(language)
                                .getInputs(
                                        item.getOwningCollection().getHandle());

                        for (int i = 0; i < dcInputSet.getNumberPages(); i++)
                        {
                            DCInput[] dcInput = dcInputSet.getPageRows(i, false,
                                    false);
                            for (DCInput myInput : dcInput)
                            {
                                if (StringUtils
                                        .isNotBlank(myInput.getPairsType()))
                                {
                                    for (Metadatum metadatum : item.getMetadata(
                                            myInput.getSchema(),
                                            myInput.getElement(),
                                            myInput.getQualifier(), Item.ANY))
                                    {
                                        String stored_value = metadatum.value;
                                        String displayVal = myInput
                                                .getDisplayString(null,
                                                        stored_value);
                                        String prefixedDisplayVal = language
                                                + "_" + displayVal;
                                        if (StringUtils.isBlank(displayVal))
                                        {
                                            displayVal = stored_value;
                                        }
                                        String unqualifiedField = myInput
                                                .getSchema() + "."
                                                + myInput.getElement() + "."
                                                + Item.ANY;
                                        
                                        buildSearchFilter(document,
                                                searchFilters,
                                                stored_value.toString(),
                                                metadatum.getField(),
                                                unqualifiedField, displayVal,
                                                prefixedDisplayVal, authorityMapToWrite);
                                        
                                    }
                                }
                            }
                        }
                    }
                    for(String indexFieldName : authorityMapToWrite.keySet()) {
                        document.addField(indexFieldName, authorityMapToWrite.get(indexFieldName));
                    }
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }

            }
        }
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    private void buildSearchFilter(SolrInputDocument document,
            Map<String, List<DiscoverySearchFilter>> searchFilters,
            String stored_value, String field, String unqualifiedField,
            String displayVal, String prefixedDisplayVal, Map<String, String> authorityMapToWrite)
    {
        if (searchFilters.containsKey(field))
        {
            List<DiscoverySearchFilter> searchFilterConfigs = searchFilters
                    .get(field);
            if (searchFilterConfigs == null)
            {
                searchFilterConfigs = searchFilters
                        .get(unqualifiedField + "." + Item.ANY);
            }

            for (DiscoverySearchFilter searchFilter : searchFilterConfigs)
            {
                document.addField(searchFilter.getIndexFieldName() + "_keyword",
                        prefixedDisplayVal + SolrServiceImpl.AUTHORITY_SEPARATOR
                                + stored_value);
                document.addField(searchFilter.getIndexFieldName() + "_ac",
                        prefixedDisplayVal.toLowerCase() + separator
                                + displayVal);
                document.addField(searchFilter.getIndexFieldName() + "_acid",
                        prefixedDisplayVal.toLowerCase() + separator
                                + displayVal
                                + SolrServiceImpl.AUTHORITY_SEPARATOR
                                + stored_value);
                document.addField(searchFilter.getIndexFieldName() + "_filter",
                        prefixedDisplayVal.toLowerCase() + separator + displayVal
                                + SolrServiceImpl.AUTHORITY_SEPARATOR
                                + stored_value);
                authorityMapToWrite.put(searchFilter.getIndexFieldName() + "_authority", stored_value);
            }
        }
    }

    public ConfigurationService getConfigurationService()
    {
        return configurationService;
    }

    public void setConfigurationService(
            ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }

    public String getSeparator()
    {
        return separator;
    }

    public void setSeparator(String separator)
    {
        this.separator = separator;
    }
    
}
