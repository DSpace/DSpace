/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import it.cilea.osd.jdyna.model.AnagraficaObject;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ICrisObject;

public class CrisEnhancer
{
    protected String alias;
    
    private Class<? extends AnagraficaObject> clazz;
    
    protected Map<String, String> qualifiers2path;

    public Set<String> getQualifiers()
    {
        return qualifiers2path.keySet();
    }

    public <P extends Property<TP>, TP extends PropertiesDefinition> List<P> getProperties(
            ICrisObject<P, TP> cris, String qualifier)
    {
        String path = qualifiers2path.get(qualifier);
        return calculateProperties(cris, alias + "." + path);
    }

    protected <P extends Property<TP>, TP extends PropertiesDefinition> List<P> calculateProperties(
            AnagraficaSupport<P, TP> cris, String path)
    {
        String[] splitted = path.split("\\.", 2);
        List result = new ArrayList();
        if (splitted.length == 2)
        {
            List<P> props = cris.getAnagrafica4view().get(splitted[0]);
            if (props != null)
            {
                for (P prop : props)
                {
                    if (prop.getObject() instanceof ACrisObject)
                    {
                        List tmp = calculateProperties(
                                (ACrisObject) prop.getObject(), splitted[1]);
                        if (tmp != null)
                        {
                            result.addAll(tmp);
                        }
                    }
                }
                return result;
            }
            return null;
        }
        else
        {
            return cris.getAnagrafica4view().get(splitted[0]);
        }
    }

    public void setClazz(Class<? extends AnagraficaObject> clazz)
    {
        this.clazz = clazz;
    }
    
    public Class<? extends AnagraficaObject> getClazz()
    {
        return clazz;
    }    
    
    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public Map<String, String> getQualifiers2path()
    {
        return qualifiers2path;
    }

    public void setQualifiers2path(Map<String, String> qualifiers2path)
    {
        this.qualifiers2path = qualifiers2path;
    }

}
