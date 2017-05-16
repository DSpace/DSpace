/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ICrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.DynamicNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.SolrServiceSearchPlugin;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.widget.WidgetCheckRadio;

/**
 * 
 * @author Luigi Andrea Pascarelli
 *
 */
public class CrisValuePairsIndexPlugin implements CrisServiceIndexPlugin,
        SolrServiceIndexPlugin, SolrServiceSearchPlugin
{

    private static final Logger log = Logger
            .getLogger(CrisValuePairsIndexPlugin.class);

    private ApplicationService applicationService;

    private DCInputsReader dcInputsReader;

    private void init() throws DCInputsReaderException
    {
        if (dcInputsReader == null)
        {
            dcInputsReader = new DCInputsReader();
        }
    }

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
            SolrInputDocument document)
    {
        if (crisObject != null)
        {
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
                    String displayVal = getCheckRadioDisplayValue(
                            (((WidgetCheckRadio) pd.getRendering())
                                    .getStaticValues()),
                            stored_value.toString());
                    document.addField(field + "_authority", stored_value);
                    document.addField(field, displayVal);
                }
            }
        }
    }

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
            ACNO crisObject, SolrInputDocument document)
    {
        if (crisObject != null)
        {
            ICrisObject<P, TP> parent = (ICrisObject<P, TP>) crisObject
                    .getParent();
            String confName = "ncris" + parent.getPublicPath();
            String schema = confName + crisObject.getTypo().getShortName();
            List<NTP> allPropertiesDefinition = applicationService
                    .getAllPropertiesDefinitionWithRadioCheckDropdown(
                            crisObject.getClassPropertiesDefinition());
            for (NTP pd : allPropertiesDefinition)
            {
                List<NP> storedP = crisObject.getAnagrafica4view()
                        .get(pd.getShortName());
                for (NP stored_value : storedP)
                {
                    String field = schema + "."
                            + stored_value.getTypo().getShortName();
                    String displayVal = getCheckRadioDisplayValue(
                            (((WidgetCheckRadio) pd.getRendering())
                                    .getStaticValues()),
                            stored_value.toString());
                    document.addField(field + "_authority", stored_value);
                    document.addField(field, displayVal);
                }
            }
        }
    }

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document)
    {
        if (dso != null)
        {
            if (dso.getType() == Constants.ITEM)
            {
                Item item = (Item) dso;
                try
                {
                    init();
                    DCInputSet dcInputSet = dcInputsReader
                            .getInputs(item.getOwningCollection().getHandle());

                    for (int i = 0; i < dcInputSet.getNumberPages(); i++)
                    {
                        DCInput[] dcInput = dcInputSet.getPageRows(i, false,
                                false);
                        for (DCInput myInput : dcInput)
                        {
                            if (StringUtils.isNotBlank(myInput.getPairsType()))
                            {
                                for (Metadatum metadatum : item.getMetadata(myInput.getSchema(),
                                        myInput.getElement(),
                                        myInput.getQualifier(), Item.ANY))
                                {
                                    String stored_value = metadatum.value;
                                    String displayVal = myInput
                                            .getDisplayString(null,
                                                    stored_value);
                                    document.addField(
                                            metadatum.getField() + "_authority",
                                            stored_value);
                                    document.addField(metadatum.getField(),
                                            displayVal);
                                    document.addField(myInput.getPairsType(),
                                            displayVal);
                                }
                            }
                        }
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

    @Override
    public void additionalSearchParameters(Context context,
            DiscoverQuery discoveryQuery, SolrQuery solrQuery)
    {
        try
        {
            init();
        }
        catch (DCInputsReaderException e)
        {
            log.error(e.getMessage(), e);
        }
        Set<String> result = new HashSet<String>();
        Iterator<String> iterator = dcInputsReader.getPairsNameIterator();
        while (iterator.hasNext())
        {
            result.add(iterator.next());
        }

        additionalSearchParameter(RPPropertiesDefinition.class);
        additionalSearchParameter(ProjectPropertiesDefinition.class);
        additionalSearchParameter(OUPropertiesDefinition.class);
        additionalSearchParameter(DynamicPropertiesDefinition.class);
        additionalSearchParameter(RPNestedPropertiesDefinition.class);
        additionalSearchParameter(ProjectNestedPropertiesDefinition.class);
        additionalSearchParameter(OUNestedPropertiesDefinition.class);
        additionalSearchParameter(DynamicNestedPropertiesDefinition.class);

        for (String rr : result)
        {
            solrQuery.addField(rr);
        }

    }

    private <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> Set<String> additionalSearchParameter(
            Class<TP> clazz)
    {
        Set<String> result = new HashSet<String>();
        List<TP> allPropertiesDefinitionRP = applicationService
                .getAllPropertiesDefinitionWithRadioCheckDropdown(clazz);
        for (TP pds : allPropertiesDefinitionRP)
        {
            result.add(pds.getShortName());
        }
        return result;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public static String getCheckRadioDisplayValue(String staticValues,
            String identifierValue)
    {
        String[] resultTmp = staticValues.split("\\|\\|\\|");
        for (String rr : resultTmp)
        {
            String displayValue = rr;
            String identifyingValue = rr;
            if (rr.contains("###"))
            {
                identifyingValue = rr.split("###")[0];
                displayValue = rr.split("###")[1];
            }
            if (identifyingValue.equals(identifierValue))
            {
                return displayValue;
            }
        }
        return null;
    }
}
