/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws.marshaller;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.UtilsXML;
import org.dspace.app.cris.util.UtilsXSD;
import org.jdom.Element;
import org.jdom.Namespace;

public class MarshallerDynamicNestedObject<T extends ACrisNestedObject<NP, NTP, P, TP>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>>
        implements Marshaller<T>
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(MarshallerDynamicNestedObject.class);

    private ApplicationService applicationService;

    private Class<TP> tpClazz;
    
    private String namespace;
    
    private String namespacePrefix;
    
    public String getNamespace()
    {
        return namespace;
    }
    
    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }
    
    public String getNamespacePrefix()
    {
        return namespacePrefix;
    }
    
    public void setNamespacePrefix(String namespacePrefix)
    {
        this.namespacePrefix = namespacePrefix;
    }
    
    public void setTpClazz(Class<TP> tpClazz)
    {
        this.tpClazz = tpClazz;
    }
    
    public Class<TP> getTpClazz()
    {
        return tpClazz;
    }
    
    public Element buildResponse(List<T> docList, long start, long hit,
            String type, String[] splitProjection, boolean seeHiddenValue, String responseRootName)

    {
        StringWriter writer = new StringWriter();

        String namespaceRoot = UtilsXSD.NAMESPACE_CRIS;
        
        Namespace echoNamespaceRoot = Namespace.getNamespace(UtilsXSD.NAMESPACE_PREFIX_CRIS,
                namespaceRoot);
        hit = docList.size();
        
        UtilsXML xml = new UtilsXML(writer, applicationService);
        xml.createPagination(hit, start, docList.size());
        xml.createType(type);
        xml.setSeeHiddenValue(seeHiddenValue);
        
        org.jdom.Document xmldoc = null;
        try
        {

            xmldoc = xml.createRoot(responseRootName, echoNamespaceRoot.getPrefix(),
                    echoNamespaceRoot.getURI());
        }
        catch (IOException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (ParserConfigurationException e)
        {
            log.error(e.getMessage(), e);
        }
        
        // build the response XML with JDOM
        Namespace echoNamespace = Namespace.getNamespace(namespacePrefix,
                namespace);
        
        Element child = new Element("crisobjects", echoNamespace.getPrefix(),echoNamespace.getURI());
        xmldoc.getRootElement().addContent(child);
                
        if (docList != null)
        {
            List<IContainable> tmp_containables = new LinkedList<IContainable>();
            List<IContainable> containables = new LinkedList<IContainable>();
            try
            {
                tmp_containables = applicationService
                        .newFindAllContainables(tpClazz);

                main: for (String projection : splitProjection)
                {
                    slave: for (IContainable cc : tmp_containables)
                    {
                        if (cc.getShortName().startsWith(projection))
                        {
                            containables.add(cc);
                        }
                    }
                }

                if (containables.isEmpty())
                {
                    throw new RuntimeException(
                            "Incoherent properties definitions, you looking for a projection not stable, contact administrator (web services contract fault)");
                }
            }
            catch (InstantiationException e)
            {
                log.error(e.getMessage(), e);
            }
            catch (IllegalAccessException e)
            {
                log.error(e.getMessage(), e);
            }
            for (T rp : docList)
            {
                try
                {
                    xml.write(rp, containables, child);
                }
                catch (SecurityException e)
                {
                    log.error(e.getMessage(), e);
                }
                catch (IllegalArgumentException e)
                {
                    log.error(e.getMessage(), e);
                }
                catch (IOException e)
                {
                    log.error(e.getMessage(), e);
                }
                catch (NoSuchFieldException e)
                {
                    log.error(e.getMessage(), e);
                }
                catch (IllegalAccessException e)
                {
                    log.error(e.getMessage(), e);
                }
                catch (InvocationTargetException e)
                {
                    log.error(e.getMessage(), e);
                }
                catch (TransformerException e)
                {
                    log.error(e.getMessage(), e);
                }
            }

        }
        return xmldoc.getRootElement();
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
}
