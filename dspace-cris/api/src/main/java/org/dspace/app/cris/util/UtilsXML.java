/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.util;

import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.utils.ExportUtils;
import it.cilea.osd.jdyna.value.EmbeddedFile;
import it.cilea.osd.jdyna.value.EmbeddedLinkValue;
import it.cilea.osd.jdyna.widget.WidgetDate;
import it.cilea.osd.jdyna.widget.WidgetFile;
import it.cilea.osd.jdyna.widget.WidgetLink;
import it.cilea.osd.jdyna.widget.WidgetPointer;
import it.cilea.osd.jdyna.widget.WidgetTesto;

import java.beans.PropertyEditor;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.IExportableDynamicObject;
import org.dspace.app.cris.model.IRestrictedField;
import org.dspace.app.cris.model.Investigator;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedFieldFile;
import org.dspace.app.cris.model.RestrictedFieldLocalOrRemoteFile;
import org.dspace.app.cris.model.jdyna.DecoratorRPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRestrictedField;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.ConfigurationManager;
import org.jdom.Document;
import org.jdom.Element;

/**
 * Utility class to create researchers export functionalities.
 * 
 * @author Pascarelli Andrea
 * 
 */
public class UtilsXML
{

    private Type type;

    private Pagination pagination;

    private boolean seeHiddenValue = false;

    public static final String ROOT_RESEARCHERS = "researchers";

    public static final String ELEMENT_RESEARCHER = "researcher";

    public static final String NAMEATTRIBUTE_LOCAL_FILE = "local";
    public static final String NAMEATTRIBUTE_DELETE_FILE = "delete";
    
    public static final String NAMEATTRIBUTE_SRC_LINK = "src";

    public static final String NAMEATTRIBUTE_VISIBILITY = "visibility";

    public static final String NAMEATTRIBUTE_CRISID = "crisID";

    public static final String NAMEATTRIBUTE_SOURCEID = "sourceID";
    
    public static final String NAMEATTRIBUTE_SOURCEREF = "sourceRef";
    
    public static final String NAMEATTRIBUTE_UUID = "uuid";

    public static final String NAMEATTRIBUTE_MIMETYPE = "mime";

    public static final String NAMEATTRIBUTE_REMOTEURL = "remotesrc";

    public static final String NAMEATTRIBUTE_FILEEXTENSION = "filext";

    public static final String GRANT_NAMEATTRIBUTE_RGID = "rgid";

    public static final String GRANT_NAMEATTRIBUTE_RPID = "rpkey";

    public static final String GRANT_NAMEATTRIBUTE_CODE = "code";

    public static final String GRANT_ELEMENT_INVESTIGATOR = "investigator";

    public static final String GRANT_ELEMENT_COINVESTIGATORS = "coInvestigators";

    public static final String GRANT_ELEMENT_COINVESTIGATOR = "coInvestigator";

    public static final String GRANT_TAG_PROJECTS = "projects";

    public static final String GRANT_TAG_PROJECTSCODE = "projectcode";

    public static final String GRANT_TAG_INVESTIGATOR = "investigators";

    public static final String GRANT_TAG_COINVESTIGATOR = "coinvestigators";

    private static final String NAMEATTRIBUTE_CRIS_POINTER_AUTHORITY = "authority";

    private static final String NAMEATTRIBUTE_CRIS_POINTER_UUID = "uuid";

    private static final String NAMEATTRIBUTE_CRIS_POINTER_TYPE = "type";

    private Writer writer;

    private ApplicationService applicationService;

    private Document xmldoc;

    public Document getXmldoc()
    {
        return xmldoc;
    }

    public void setXmldoc(Document xmldoc)
    {
        this.xmldoc = xmldoc;
    }

    public UtilsXML(Writer writer, ApplicationService applicationService)
    {
        this.writer = writer;
        this.applicationService = applicationService;
    }

    public Writer getWriter()
    {
        return writer;
    }

    public void setWriter(Writer writer)
    {
        this.writer = writer;
    }

    public Document createRoot() throws IOException,
            ParserConfigurationException
    {
        return createRoot(null, null, null);
    }

    public Document createRoot(String rootName, String prefixNamespace,
            String namespace) throws IOException, ParserConfigurationException
    {
        if (rootName == null)
        {
            rootName = ROOT_RESEARCHERS;
        }
        Element root = null;
        if (namespace == null)
        {
            root = new Element(rootName);
        }
        else if (prefixNamespace != null)
        {
            root = new Element(rootName, prefixNamespace, namespace);
        }
        else
        {
            root = new Element(rootName, namespace);
        }

        if (this.pagination != null)
        {
            root.setAttribute("hit", "" + this.pagination.getHit());
            root.setAttribute("start", "" + this.pagination.getStart());
            root.setAttribute("rows", "" + this.pagination.getRows());
        }
        if (this.type != null)
        {
            root.setAttribute("type", this.getType().getType());
        }
        xmldoc = new Document();
        xmldoc.addContent(root);
        return xmldoc;
    }

    public <I extends IContainable> void writeRP(ResearcherPage rp,
            List<I> metadata, Element root) throws IOException,
            SecurityException, NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException,
            TransformerException
    {
        List<String> attributes = new LinkedList<String>();
        List<String> valuesAttributes = new LinkedList<String>();
        attributes.add(NAMEATTRIBUTE_SOURCEID);
        valuesAttributes.add(rp.getSourceID());
        attributes.add(NAMEATTRIBUTE_SOURCEREF);
        valuesAttributes.add(rp.getSourceRef());
        attributes.add(NAMEATTRIBUTE_CRISID);
        valuesAttributes.add(ResearcherPageUtils.getPersistentIdentifier(rp));
        attributes.add(NAMEATTRIBUTE_UUID);
        valuesAttributes.add(rp.getUuid());
        Element element = ExportUtils.createCustomPropertyWithCustomAttributes(
                root, ELEMENT_RESEARCHER, attributes, valuesAttributes);

        for (I containable : metadata)
        {
            if (containable instanceof DecoratorRPPropertiesDefinition)
            {
                this.createElement(
                        (DecoratorRPPropertiesDefinition) containable, rp,
                        element);
            }
            if (containable instanceof DecoratorRestrictedField)
            {
                this.createElement((DecoratorRestrictedField) containable, rp,
                        element);
            }
        }

    }

    private <TP extends PropertiesDefinition, P extends Property<TP>, AS extends AnagraficaSupport<P, TP>> void createElement(
            ADecoratorPropertiesDefinition<TP> decorator,
            IExportableDynamicObject<TP, P, AS> rp, Element element)
            throws IOException
    {
        createElement(decorator.getReal(), decorator.getRendering(), rp,
                element);
    }

    private <TP extends PropertiesDefinition, P extends Property<TP>, AS extends AnagraficaSupport<P, TP>> void createElement(
            DecoratorRestrictedField decorator,
            IExportableDynamicObject<TP, P, AS> researcher, Element element)
            throws IOException, SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException,
            InvocationTargetException
    {
        String shortName = decorator.getShortName();
        Method[] methods = researcher.getClass().getMethods();
        Object field = null;
        Method method = null;
        Method setter = null;
        for (Method m : methods)
        {
            if (m.getName().toLowerCase()
                    .equals("get" + shortName.toLowerCase()))
            {
                field = m.invoke(researcher, null);
                method = m;
                break;
            }
        }
        if (method.getReturnType().isAssignableFrom(List.class))
        {
            Element coinvestigators = null;
            if (decorator.getShortName().equals("coInvestigators"))
            {
                coinvestigators = new Element("coInvestigators",
                        element.getNamespacePrefix(), element.getNamespaceURI());
                element.addContent(coinvestigators);
            }

            for (IRestrictedField rr : (List<IRestrictedField>) field)
            {
                if (decorator.getShortName().equals("coInvestigators"))
                {
                    List<String> attributes = new LinkedList<String>();
                    List<String> valuesAttributes = new LinkedList<String>();
                    Investigator invest = (Investigator) rr;
                    if (invest.getIntInvestigator() != null)
                    {
                        attributes.add("rpkey");
                        valuesAttributes.add(invest.getIntInvestigator()
                                .getValuePublicIDAttribute());
                    }
                    ExportUtils.createCoinvestigator(coinvestigators,
                            decorator.getReal(),
                            ((IRestrictedField) rr).getValue(), attributes,
                            valuesAttributes);

                }
                else
                {
                    createSimpleElement(decorator.getReal(),
                            (IRestrictedField) rr, element);
                }

            }

        }
        else if (method.getReturnType().isAssignableFrom(String.class))
        {
            createSimpleElement(decorator.getReal(), (String) field, element);
        }
        else
        {
            if (RestrictedFieldLocalOrRemoteFile.class.isAssignableFrom(method
                    .getReturnType()))
            {
                createSimpleElement(decorator.getReal(),
                        (RestrictedFieldLocalOrRemoteFile) field, element);
            }
            else if (RestrictedFieldFile.class.isAssignableFrom(method
                    .getReturnType()))
            {
                createSimpleElement(decorator.getReal(),
                        (RestrictedFieldFile) field, element);
            }
            else
            {
                createSimpleElement(decorator.getReal(),
                        (IRestrictedField) field, element);
            }
        }

    }

    private void createSimpleElement(String real, String field, Element element)
            throws IOException
    {
        if (field != null && !field.isEmpty())
        {
            ExportUtils.createCustomValue(element, real, field);
        }
    }

    private void createSimpleElement(String real,
            IRestrictedField restrictedField, Element element)
            throws IOException
    {
        if (restrictedField.getValue() != null
                && !restrictedField.getValue().isEmpty()
                && (restrictedField.getVisibility() == 1 || (restrictedField
                        .getVisibility() == 0 && isSeeHiddenValue())))
        {
            List<String> attributes = new LinkedList<String>();
            List<String> valuesAttributes = new LinkedList<String>();
            attributes.add(NAMEATTRIBUTE_VISIBILITY);
            valuesAttributes.add(restrictedField.getVisibility().toString());
            if (restrictedField instanceof RestrictedFieldFile)
            {
                attributes.add(NAMEATTRIBUTE_MIMETYPE);
                valuesAttributes.add(((RestrictedFieldFile) restrictedField)
                        .getMimeType());
            }
            if (restrictedField instanceof RestrictedFieldLocalOrRemoteFile)
            {
                attributes.add(NAMEATTRIBUTE_REMOTEURL);
                valuesAttributes
                        .add(((RestrictedFieldLocalOrRemoteFile) restrictedField)
                                .getRemoteUrl());
            }

            ExportUtils.createCustomValueWithCustomAttributes(element, real,
                    restrictedField.getValue(), attributes, valuesAttributes);
        }
    }

    private void createSimpleElement(String real,
            RestrictedFieldFile restrictedField, Element element)
            throws IOException
    {
        if (restrictedField.getValue() != null
                && !restrictedField.getValue().isEmpty()
                && (restrictedField.getVisibility() == 1 || (restrictedField
                        .getVisibility() == 0 && isSeeHiddenValue())))
        {
            List<String> attributes = new LinkedList<String>();
            List<String> valuesAttributes = new LinkedList<String>();
            attributes.add(NAMEATTRIBUTE_VISIBILITY);
            valuesAttributes.add(restrictedField.getVisibility().toString());

            attributes.add(NAMEATTRIBUTE_MIMETYPE);
            valuesAttributes.add(((RestrictedFieldFile) restrictedField)
                    .getMimeType());

            ExportUtils.createCustomValueWithCustomAttributes(element, real,
                    restrictedField.getValue(), attributes, valuesAttributes);
        }
    }

    private void createSimpleElement(String real,
            RestrictedFieldLocalOrRemoteFile restrictedField, Element element)
            throws IOException
    {
        if (restrictedField.getValue() != null
                && !restrictedField.getValue().isEmpty()
                && (restrictedField.getVisibility() == 1 || (restrictedField
                        .getVisibility() == 0 && isSeeHiddenValue())))
        {
            List<String> attributes = new LinkedList<String>();
            List<String> valuesAttributes = new LinkedList<String>();
            attributes.add(NAMEATTRIBUTE_VISIBILITY);
            valuesAttributes.add(restrictedField.getVisibility().toString());

            attributes.add(NAMEATTRIBUTE_MIMETYPE);
            valuesAttributes.add(((RestrictedFieldFile) restrictedField)
                    .getMimeType());

            attributes.add(NAMEATTRIBUTE_REMOTEURL);
            valuesAttributes
                    .add(((RestrictedFieldLocalOrRemoteFile) restrictedField)
                            .getRemoteUrl());

            ExportUtils.createCustomValueWithCustomAttributes(element, real,
                    restrictedField.getValue(), attributes, valuesAttributes);
        }
    }

    private <A extends AWidget, TP extends PropertiesDefinition, P extends Property<TP>, AS extends AnagraficaSupport<P, TP>> void createElement(
            TP tp, A rendering, IExportableDynamicObject<TP, P, AS> rp,
            Element element) throws IOException
    {

        createSimpleElement(tp.getShortName(), rp.getAnagraficaSupport()
                .getProprietaDellaTipologia(tp), element);

    }

    private <TP extends PropertiesDefinition, P extends Property<TP>> void createSimpleElement(
            String shortName, List<P> proprietaDellaTipologia, Element element)
            throws IOException
    {

        for (P prop : proprietaDellaTipologia)
        {
            PropertyEditor pe = prop.getTypo().getRendering()
                    .getPropertyEditor(applicationService);

            if (prop.getObject() != null
                    && (prop.getVisibility() == 1 || (prop.getVisibility() == 0 && isSeeHiddenValue())))
            {
                pe.setValue(prop.getObject());
                if (prop.getTypo().getRendering() instanceof WidgetTesto)
                {
                    List<String> attributes = new LinkedList<String>();
                    List<String> valuesAttributes = new LinkedList<String>();
                    attributes.add(NAMEATTRIBUTE_VISIBILITY);
                    valuesAttributes.add(prop.getVisibility().toString());
                    ExportUtils.createCustomValueWithCustomAttributes(element,
                            shortName, pe.getAsText(), attributes,
                            valuesAttributes);
                }
                if (prop.getTypo().getRendering() instanceof WidgetDate)
                {
                    List<String> attributes = new LinkedList<String>();
                    List<String> valuesAttributes = new LinkedList<String>();
                    attributes.add(NAMEATTRIBUTE_VISIBILITY);
                    valuesAttributes.add(prop.getVisibility().toString());
                    ExportUtils.createCustomValueWithCustomAttributes(element,
                            shortName, pe.getAsText(), attributes,
                            valuesAttributes);
                }
                if (prop.getTypo().getRendering() instanceof WidgetLink)
                {
                    EmbeddedLinkValue link = (EmbeddedLinkValue) pe.getValue();
                    List<String> attributes = new LinkedList<String>();
                    List<String> valuesAttributes = new LinkedList<String>();
                    attributes.add(NAMEATTRIBUTE_VISIBILITY);
                    valuesAttributes.add(prop.getVisibility().toString());
                    attributes.add(NAMEATTRIBUTE_SRC_LINK);
                    valuesAttributes.add(link.getValueLink());
                    ExportUtils.createCustomValueWithCustomAttributes(element,
                            shortName, link.getDescriptionLink(), attributes,
                            valuesAttributes);

                }
                if (prop.getTypo().getRendering() instanceof WidgetPointer)
                {
                    ACrisObject aCrisObject = (ACrisObject) pe.getValue();

                    List<String> attributes = new LinkedList<String>();
                    List<String> valuesAttributes = new LinkedList<String>();
                    attributes.add(NAMEATTRIBUTE_VISIBILITY);
                    valuesAttributes.add(prop.getVisibility().toString());
                    attributes.add(NAMEATTRIBUTE_CRIS_POINTER_AUTHORITY);
                    valuesAttributes.add(aCrisObject.getCrisID());
                    attributes.add(NAMEATTRIBUTE_CRIS_POINTER_UUID);
                    valuesAttributes.add(aCrisObject.getUuid());
                    attributes.add(NAMEATTRIBUTE_CRIS_POINTER_TYPE);
                    valuesAttributes.add(aCrisObject.getType() + "");
                    ExportUtils.createCustomValueWithCustomAttributes(element,
                            shortName, aCrisObject.getName(), attributes,
                            valuesAttributes);

                }
                if (prop.getTypo().getRendering() instanceof WidgetFile)
                {
                    EmbeddedFile file = (EmbeddedFile) pe.getValue();
                    WidgetFile widget = (WidgetFile) (prop.getTypo()
                            .getRendering());
                    List<String> attributes = new LinkedList<String>();
                    List<String> valuesAttributes = new LinkedList<String>();
                    attributes.add(NAMEATTRIBUTE_VISIBILITY);
                    valuesAttributes.add(prop.getVisibility().toString());

                    attributes.add(NAMEATTRIBUTE_MIMETYPE);
                    valuesAttributes.add(file.getMimeFile());

                    attributes.add(NAMEATTRIBUTE_FILEEXTENSION);
                    valuesAttributes.add(file.getExtFile());

                    ExportUtils.createCustomValueWithCustomAttributes(
                            element,
                            shortName,
                            ConfigurationManager.getProperty("dspace.url") + "/"
                                    + widget.getServletPath() + "/"
                                    + file.getFolderFile() + "/?filename="
                                    + file.getValueFile() + "."
                                    + file.getExtFile(), attributes,
                            valuesAttributes);

                }
            }
        }

    }

    public <I extends IContainable, TP extends PropertiesDefinition, P extends Property<TP>, AS extends AnagraficaSupport<P, TP>> void write(
            IExportableDynamicObject<TP, P, AS> rp, List<I> metadata,
            Element root) throws IOException, SecurityException,
            NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException,
            TransformerException
    {
        List<String> attributes = new LinkedList<String>();
        List<String> valuesAttributes = new LinkedList<String>();

        attributes.add(rp.getNamePublicIDAttribute());
        valuesAttributes.add(rp.getValuePublicIDAttribute());
        attributes.add(rp.getNameIDAttribute());
        valuesAttributes.add(rp.getValueIDAttribute());
        attributes.add(rp.getNameBusinessIDAttribute());
        valuesAttributes.add(rp.getValueBusinessIDAttribute());
        attributes.add(rp.getNameTypeIDAttribute());
        valuesAttributes.add(rp.getValueTypeIDAttribute());

        Element element = ExportUtils.createCustomPropertyWithCustomAttributes(
                root, rp.getNameSingleRowElement(), attributes,
                valuesAttributes);

        for (I containable : metadata)
        {
            if (containable instanceof ADecoratorPropertiesDefinition)
            {
                this.createElement(
                        (ADecoratorPropertiesDefinition<TP>) containable, rp,
                        element);
            }
            if (containable instanceof DecoratorRestrictedField)
            {
                this.createElement((DecoratorRestrictedField) containable, rp,
                        element);
            }
        }

    }

    public Pagination createPagination(long hit, long start, int rows)
    {
        this.pagination = new Pagination(hit, start, rows);
        return pagination;
    }

    public Type createType(String type)
    {
        this.setType(new Type(type));
        return this.type;
    }

    public void setPagination(Pagination pagination)
    {
        this.pagination = pagination;
    }

    public Pagination getPagination()
    {
        return pagination;
    }

    public void setSeeHiddenValue(boolean seeHiddenValue)
    {
        this.seeHiddenValue = seeHiddenValue;
    }

    public boolean isSeeHiddenValue()
    {
        return seeHiddenValue;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public Type getType()
    {
        return type;
    }

    class Pagination
    {

        private long hit;

        private long start;

        private int rows;

        public Pagination(long hit2, long start2, int rows2)
        {
            this.hit = hit2;
            this.start = start2;
            this.rows = rows2;
        }

        public long getHit()
        {
            return hit;
        }

        public void setHit(long hit)
        {
            this.hit = hit;
        }

        public long getStart()
        {
            return start;
        }

        public void setStart(long start)
        {
            this.start = start;
        }

        public int getRows()
        {
            return rows;
        }

        public void setRows(int rows)
        {
            this.rows = rows;
        }

    }

    class Type
    {
        private String type;

        public Type(String t)
        {
            this.type = t;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        public String getType()
        {
            return type;
        }

    }
}
