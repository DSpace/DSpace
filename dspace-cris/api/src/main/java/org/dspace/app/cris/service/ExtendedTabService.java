/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.service;

import it.cilea.osd.jdyna.model.AccessLevelConstants;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.AbstractEditTab;
import it.cilea.osd.jdyna.web.AbstractTab;
import it.cilea.osd.jdyna.web.Box;
import it.cilea.osd.jdyna.web.TabService;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.DecoratorRestrictedField;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.core.ConfigurationManager;

public class ExtendedTabService<H extends Box<Containable>, D extends AbstractTab<H>, T extends AbstractEditTab<H, D>>
        extends TabService
{

    public static Map<String, List<IContainable>> cacheStructuredMetadata = new HashMap<String, List<IContainable>>();

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    protected void findOtherContainables(List<IContainable> containables)
    {
        findOtherContainables(containables, RPPropertiesDefinition.class.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void findOtherContainables(List<IContainable> containables,
            String extraPrefixConfiguration)
    {
        String dspaceProperty = extraPrefixConfiguration + ".containables";
        log.debug("Get from configuration additional containables object : "
                + dspaceProperty);
        String confContainables = ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE, dspaceProperty);
        if (confContainables != null && !confContainables.isEmpty())
        {
            String[] listConfContainables = confContainables.split(",");
            for (String containable : listConfContainables)
            {
                DecoratorRestrictedField drf = new DecoratorRestrictedField();
                drf.setReal(containable.trim());
                extractIsMandatory(containable, drf);
                extractIsRepeatable(containable, drf);
                extractAccessLevel(containable, drf);
                containables.add(drf);
            }
        }
    }

    @Deprecated
    private void extractIsMandatory(String containable,
            DecoratorRestrictedField drf)
    {
        extractIsMandatory(containable, drf, RPPropertiesDefinition.class.getName());
    }

    private void extractIsMandatory(String containable,
            DecoratorRestrictedField drf, String extraPrefixConfiguration)
    {
        String fieldsNotNullable = ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE,extraPrefixConfiguration
                        + ".containables.structural.mandatory");
        boolean notnullable = fieldsNotNullable.contains(containable);
        drf.setMandatory(notnullable);
    }

    @Deprecated
    private void extractAccessLevel(String containable,
            DecoratorRestrictedField drf)
    {
        extractAccessLevel(containable, drf, RPPropertiesDefinition.class.getName());
    }

    private void extractAccessLevel(String containable,
            DecoratorRestrictedField drf, String extraPrefixConfiguration)
    {
        String fieldsAccessLevelHIGH = ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE,extraPrefixConfiguration
                        + ".containables.box.staticfields.visibility.high");
        boolean accessLevel = fieldsAccessLevelHIGH.contains(containable);
        if (accessLevel)
        {
            drf.setAccessLevel(AccessLevelConstants.HIGH_ACCESS);
            return;
        }
        String fieldsAccessLevelSTANDARD = ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE,extraPrefixConfiguration
                        + ".containables.box.staticfields.visibility.standard");
        accessLevel = fieldsAccessLevelSTANDARD.contains(containable);
        if (accessLevel)
        {
            drf.setAccessLevel(AccessLevelConstants.STANDARD_ACCESS);
            return;
        }
        String fieldsAccessLevelADMIN = ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE,extraPrefixConfiguration
                        + ".containables.box.staticfields.visibility.admin");
        accessLevel = fieldsAccessLevelADMIN.contains(containable);
        if (accessLevel)
        {
            drf.setAccessLevel(AccessLevelConstants.ADMIN_ACCESS);
            return;
        }
        String fieldsAccessLevelLOW = ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE,extraPrefixConfiguration
                        + ".containables.box.staticfields.visibility.low");
        accessLevel = fieldsAccessLevelLOW.contains(containable);
        if (accessLevel)
        {
            drf.setAccessLevel(AccessLevelConstants.LOW_ACCESS);
            return;
        }
        drf.setAccessLevel(AccessLevelConstants.ADMIN_ACCESS);
    }

    private void extractIsRepeatable(String containable,
            DecoratorRestrictedField drf)
    {
        Method[] methods = ResearcherPage.class.getMethods();
        Method method = null;

        for (Method m : methods)
        {
            if (m.getName().toLowerCase()
                    .equals("get" + containable.trim().toLowerCase()))
            {
                method = m;
                break;
            }
        }
        if (method != null
                && method.getReturnType().isAssignableFrom(List.class))
        {
            drf.setRepeatable(true);
        }
        else
        {
            drf.setRepeatable(false);
        }
    }

    @Override
    @Deprecated
    public void findOtherContainablesInBoxByConfiguration(String holderName,
            List<IContainable> containables)
    {
        findOtherContainablesInBoxByConfiguration(holderName, containables,
                "researcherpage");
    }

    public void findOtherContainablesInBoxByConfiguration(String holderName,
            List<IContainable> containables, String extraPrefixConfiguration)
    {
        String boxName = StringUtils.deleteWhitespace(holderName).trim()
                .toLowerCase();

        String dspaceProperty = extraPrefixConfiguration + ".containables.box."
                + boxName;

        if (cacheStructuredMetadata.containsKey(dspaceProperty))
        {
            containables.addAll(cacheStructuredMetadata.get(dspaceProperty));
        }
        else
        {            
            log.debug("Get from configuration additional containables object : "
                    + dspaceProperty);
            String confContainables = ConfigurationManager
                    .getProperty(CrisConstants.CFG_MODULE,dspaceProperty);
            List<IContainable> tmp = new LinkedList<IContainable>();
            if (confContainables != null && !confContainables.isEmpty())
            {
                String[] listConfContainables = confContainables.split(",");
                for (String containable : listConfContainables)
                {
                    DecoratorRestrictedField drf = new DecoratorRestrictedField();
                    drf.setReal(containable.trim());
                    extractIsMandatory(containable, drf);
                    extractIsRepeatable(containable, drf);
                    extractAccessLevel(containable, drf);
                    tmp.add(drf);
                    
                }
                containables.addAll(tmp);                
            }            
            cacheStructuredMetadata.put(dspaceProperty, tmp);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void getOtherContainableOnCreation(List<IContainable> containables)
    {
        // customization code
    }

}