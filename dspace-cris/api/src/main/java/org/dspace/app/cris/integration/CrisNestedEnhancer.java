/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import it.cilea.osd.jdyna.model.ANestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ICrisObject;
import org.dspace.app.cris.service.ApplicationService;

public class CrisNestedEnhancer extends CrisEnhancer
{
    private ApplicationService applicationService;

    private Class nestedClazz;

    @Override
    public <P extends Property<TP>, TP extends PropertiesDefinition> List<P> getProperties(
            ICrisObject<P, TP> cris, String qualifier)
    {
        String path = qualifiers2path.get(qualifier);
        String[] splitted = path.split("\\.", 2);
        List<ANestedObject> anos = applicationService
                .getNestedObjectsByParentIDAndShortname(cris.getId(), alias,
                        nestedClazz);
        List result = new ArrayList();
        if (anos != null)
        {
            for (ANestedObject ano : anos)
            {
                List<? extends Property> props = ano.getAnagrafica4view().get(
                        splitted[0]);
                if (props != null)
                {
                    for (Property p : props)
                    {
                        if (splitted.length == 2)
                        {
                            if (p.getObject() instanceof ACrisObject)
                            {
                                List tmp = super.calculateProperties(
                                        (ACrisObject) p.getObject(),
                                        splitted[1]);
                                if (tmp != null)
                                {
                                    result.addAll(tmp);
                                }
                            }
                        }
                        else
                        {
                            result.add(p);
                        }
                    }
                }
            }
            return result;
        }
        else
        {
            return null;
        }
    }

    public void setNestedClazz(Class nestedClazz)
    {
        this.nestedClazz = nestedClazz;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
}
