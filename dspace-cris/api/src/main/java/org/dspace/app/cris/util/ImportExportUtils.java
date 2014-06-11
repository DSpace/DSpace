/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.util;

import it.cilea.osd.common.util.Utils;
import it.cilea.osd.common.utils.XMLUtils;
import it.cilea.osd.jdyna.dto.AnagraficaObjectDTO;
import it.cilea.osd.jdyna.dto.AnagraficaObjectWithTypeDTO;
import it.cilea.osd.jdyna.dto.ValoreDTO;
import it.cilea.osd.jdyna.model.AnagraficaObject;
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.util.AnagraficaUtils;
import it.cilea.osd.jdyna.value.EmbeddedLinkValue;
import it.cilea.osd.jdyna.widget.WidgetDate;
import it.cilea.osd.jdyna.widget.WidgetFile;
import it.cilea.osd.jdyna.widget.WidgetLink;
import it.cilea.osd.jdyna.widget.WidgetTesto;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.IExportableDynamicObject;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.jdyna.DecoratorRPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRestrictedField;
import org.dspace.app.cris.model.jdyna.ProjectAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.ProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPAdditionalFieldStorage;
import org.dspace.app.cris.model.jdyna.RPNestedObject;
import org.dspace.app.cris.model.jdyna.RPNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPNestedProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.ExtendedTabService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCPersonName;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Static class that provides export functionalities from the RPs database to
 * Excel. It defines also some constants useful for the import functionalities.
 * 
 * @author cilea
 * 
 */
public class ImportExportUtils
{

    private static final DateFormat dateFormat = new SimpleDateFormat(
            "dd-MM-yyyy_HH-mm-ss");

    /** log4j logger */
    private static Logger log = Logger.getLogger(ImportExportUtils.class);

    private static final String XPATH_ATTRIBUTE_SRC = "@"
            + UtilsXML.NAMEATTRIBUTE_SRC_LINK;

    private static final String XPATH_ATTRIBUTE_VIS = "@"
            + UtilsXML.NAMEATTRIBUTE_VISIBILITY;

    private static final String XPATH_ATTRIBUTE_RPID = "@"
            + UtilsXML.NAMEATTRIBUTE_RPID;

    private static final String XPATH_ATTRIBUTE_STAFFNO = "@"
            + UtilsXML.NAMEATTRIBUTE_STAFF_NO;

    private static final String XPATH_ELEMENT_ROOT = UtilsXML.ROOT_RESEARCHERS;

    private static final String XPATH_ELEMENT_RESEARCHER = UtilsXML.ELEMENT_RESEARCHER;

    private static final String XPATH_ATTRIBUTE_REMOTESRC = "@"
            + UtilsXML.NAMEATTRIBUTE_REMOTEURL;

    private static final String XPATH_ATTRIBUTE_MIME = "@"
            + UtilsXML.NAMEATTRIBUTE_MIMETYPE;

    public static final String[] XPATH_RULES = {
            "/" + XPATH_ELEMENT_ROOT + "/" + XPATH_ELEMENT_RESEARCHER,
            XPATH_ATTRIBUTE_STAFFNO, XPATH_ATTRIBUTE_VIS, XPATH_ATTRIBUTE_RPID,
            XPATH_ATTRIBUTE_SRC, XPATH_ATTRIBUTE_REMOTESRC,
            XPATH_ATTRIBUTE_MIME };

    /**
     * Defaul visibility, it is used when no visibility attribute and old value
     * founded
     */
    public static final String DEFAULT_VISIBILITY = "1";

    public static final String LABELCAPTION_VISIBILITY_SUFFIX = " visibility";

    public static final String IMAGE_SUBFOLDER = "image";

    public static final String CV_SUBFOLDER = "cv";

    /**
     * Default absolute path where find the contact data excel file to import
     */
    public static final String PATH_DEFAULT_XML = ConfigurationManager
            .getProperty("dspace.dir")
            + File.separatorChar
            + "rp-import/rpdata.xml";

    /**
     * Default absolute path where find the contact data excel file to import
     */
    public static final String GRANT_PATH_DEFAULT_XML = ConfigurationManager
            .getProperty("dspace.dir")
            + File.separatorChar
            + "rg-import/rpdata.xml";

    /**
     * Write in the output stream the researcher pages contact data as an excel
     * file. The format of the exported Excel file is suitable for re-import in
     * the system.
     * 
     * @param rps
     *            the researcher pages list to export
     * @param applicationService
     *            the applicationService
     * @param os
     *            the output stream, it will close directly when the method exit
     * @throws IOException
     * @throws WriteException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static void exportData(List<ResearcherPage> rps,
            ApplicationService applicationService, OutputStream os,
            List<IContainable> metadata) throws IOException, WriteException,
            IllegalArgumentException, IllegalAccessException,
            InvocationTargetException
    {

        WritableWorkbook workbook = Workbook.createWorkbook(os);
        WritableSheet sheet = workbook.createSheet("Sheet", 0);

        // create initial caption (other caption could be write field together)
        int x = 0;
        sheet.addCell(new Label(x++, 0, "staffNo"));
        sheet.addCell(new Label(x++, 0, "rp"));
        sheet.addCell(new Label(x++, 0, "rp url"));

        // row index
        int i = 1;
        for (ResearcherPage rp : rps)
        {
            int y = 0;
            sheet.addCell(new Label(0, i, ""));
            Label label = (Label) sheet.getCell(0, i);
            label.setString(rp.getSourceID());
            y++;
            sheet.addCell(new Label(1, i, ""));
            label = (Label) sheet.getCell(1, i);
            label.setString(ResearcherPageUtils.getPersistentIdentifier(rp));
            y++;
            sheet.addCell(new Label(2, i, ""));
            label = (Label) sheet.getCell(2, i);
            label.setString(ConfigurationManager.getProperty("dspace.url")
                    + "/cris/" + rp.getPublicPath() + "/"
                    + ResearcherPageUtils.getPersistentIdentifier(rp));

            for (IContainable containable : metadata)
            {
                if (containable instanceof DecoratorRPPropertiesDefinition)
                {
                    y = UtilsXLS.createCell(applicationService, y, i,
                            (DecoratorRPPropertiesDefinition) containable, rp,
                            sheet);
                }
                if (containable instanceof DecoratorRestrictedField)
                {
                    y = UtilsXLS.createCell(applicationService, y, i,
                            (DecoratorRestrictedField) containable, rp, sheet);
                }
            }

            i++;
        }
        // All sheets and cells added. Now write out the workbook
        workbook.write();
        workbook.close();
    }

    /**
     * 
     * Import xml files, matching validation with xsd builded at runtime
     * execution associate to list of dynamic fields and structural fields
     * 
     * @param input
     *            - XML file stream
     * @param dir
     *            - directory from read image/cv and write temporaries xsd and
     *            xml (this xsd validate actual xml)
     * @param applicationService
     *            - service
     * @param appendMode
     *            TODO
     * @throws Exception
     */
    public static void importResearchersXML(InputStream input, File dir,
            ApplicationService applicationService, Context dspaceContext,
            boolean status) throws Exception
    {

        File filexsd = null;
        File filexml = null;

        // build filexml
        String nameXML = "xml-" + dateFormat.format(new Date()) + ".xml";
        filexml = new File(dir, nameXML);
        filexml.createNewFile();
        FileOutputStream out = new FileOutputStream(filexml);
        Utils.bufferedCopy(input, out);
        out.close();

        List<IContainable> metadataALL = applicationService
                .findAllContainables(RPPropertiesDefinition.class);

        // create xsd and write up
        String nameXSD = "xsd-" + dateFormat.format(new Date()) + ".xsd";
        filexsd = new File(dir, nameXSD);
        filexsd.createNewFile();
        FileWriter writer = new FileWriter(filexsd);
        filexsd = generateXSD(writer, dir, metadataALL, filexsd, null);

        // create xsd validator
        SchemaFactory factory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaSource = new StreamSource(filexsd);

        Schema schema = factory.newSchema(schemaSource);
        // validate source xml to xsd
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(filexml));

        // parse xml to dom
        DocumentBuilder parser = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document document = parser.parse(filexml);

        // get from list of metadata dynamic field vs structural field
        List<RPPropertiesDefinition> realTPS = new LinkedList<RPPropertiesDefinition>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : metadataALL)
        {
            RPPropertiesDefinition rpPd = applicationService
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class, c.getShortName());
            if (rpPd != null)
            {
                realTPS.add(((DecoratorRPPropertiesDefinition) applicationService
                        .findContainableByDecorable(
                                RPPropertiesDefinition.class.newInstance()
                                        .getDecoratorClass(), c.getId()))
                        .getReal());
            }
            else
            {
                structuralField.add(c);
            }
        }

        List<RPPropertiesDefinition> realFillTPS = new LinkedList<RPPropertiesDefinition>();
        for (RPPropertiesDefinition r : realTPS)
        {
            NodeList e = document.getElementsByTagName(r.getShortName());
            if (e != null && e.getLength() > 0)
            {
                realFillTPS.add(r);
            }
        }

        List<IContainable> structuralFillField = new LinkedList<IContainable>();
        for (IContainable r : structuralField)
        {
            NodeList e = document.getElementsByTagName(r.getShortName());
            if (e != null && e.getLength() > 0)
            {
                structuralFillField.add(r);
            }
        }

        // import xml
        // XPath xpath = XPathFactory.newInstance().newXPath();
        // String xpathExpression = XPATH_RULES[0];
        // NodeList researchers = (NodeList) xpath.evaluate(xpathExpression,
        // document, XPathConstants.NODESET);

        List<Element> researchers = XMLUtils.getElementList(
                document.getDocumentElement(), "researcher");
        int rows_discarded = 0;
        int rows_imported = 0;
        log.info("Start import " + new Date());
        // foreach researcher element in xml
        for (int i = 0; i < researchers.size(); i++)
        {
            log.info("Number " + i + " of " + researchers.size());
            ResearcherPage researcher = null;
            try
            {
                Element node = researchers.get(i);

                // check if staffNo and rpid exists as attribute
                // String nodeId = (String) xpath.evaluate(XPATH_RULES[1], node,
                // XPathConstants.STRING);
                // String rpId = (String) xpath.evaluate(XPATH_RULES[3], node,
                // XPathConstants.STRING);
                String nodeId = node
                        .getAttribute(UtilsXML.NAMEATTRIBUTE_STAFF_NO);
                String rpId = node.getAttribute(UtilsXML.NAMEATTRIBUTE_RPID);
                ResearcherPage clone = null;
                // use dto to fill dynamic metadata
                AnagraficaObjectDTO dto = new AnagraficaObjectDTO();
                AnagraficaObjectDTO clonedto = new AnagraficaObjectDTO();
                boolean update = false; // if update a true then set field to
                                        // null
                                        // on case of empty element
                if (nodeId == null || nodeId.isEmpty())
                {
                    log.error("Researcher discarded ( staffNo not founded) [position researcher: "
                            + i + "]");
                    throw new RuntimeException(
                            "Researcher discarded (staffNo not founded whilst rpId is on xml) [position researcher: "
                                    + i + "]");

                }
                else
                {
                    // if there is rpid then try to get researcher by staffNo
                    // and
                    // set to null all structural metadata lists
                    log.info("Researcher staffNo : " + nodeId
                            + " / rp identifier : " + rpId);
                    if (rpId != null && !rpId.isEmpty())
                    {
                        researcher = applicationService
                                .getResearcherPageByStaffNo(nodeId);
                        if (researcher == null)
                        {
                            log.error("Researcher discarded (staffNo not founded whilst rpId is on xml) [position researcher: "
                                    + i + "]");
                            ;
                            throw new RuntimeException(
                                    "Researcher discarded (staffNo not founded whilst rpId is on xml) [position researcher: "
                                            + i + "]");
                        }
                        else
                        {
                            if (!rpId.equals(ResearcherPageUtils
                                    .getPersistentIdentifier(researcher)))
                            {
                                log.error("Researcher discarded (rpId don't match persistent identifier) [position researcher: "
                                        + i + "]");
                                throw new RuntimeException(
                                        "Researcher discarded (staffNo not founded whilst rpId is on xml) [position researcher: "
                                                + i + "]");
                            }
                        }
                        // clone dynamic data and structural on dto

                        clone = (ResearcherPage) researcher.clone();
                        RPAdditionalFieldStorage additionalTemp = new RPAdditionalFieldStorage();
                        clone.setDynamicField(additionalTemp);
                        additionalTemp.duplicaAnagrafica(researcher
                                .getDynamicField());
                        update = true;

                    }
                    else
                    {
                        // here there is perhaps a new researcher
                        researcher = applicationService
                                .getResearcherPageByStaffNo(nodeId);
                        if (researcher == null)
                        {
                            researcher = new ResearcherPage();
                            researcher.setSourceID(nodeId);
                            // added by Allen: all newly added researchers are
                            // inactive by default
                            // use -active in command line to change default
                            // status to active.
                            researcher.setStatus(status);

                            clone = (ResearcherPage) researcher.clone();
                            RPAdditionalFieldStorage additionalTemp = new RPAdditionalFieldStorage();
                            clone.setDynamicField(additionalTemp);
                            additionalTemp.duplicaAnagrafica(researcher
                                    .getDynamicField());
                        }
                        else
                        {
                            log.error("Researcher discarded (staffNo " + nodeId
                                    + " already exist) [position researcher: "
                                    + i + "]");
                            throw new RuntimeException(
                                    "Researcher discarded (staffNo "
                                            + nodeId
                                            + " already exist) [position researcher: "
                                            + i + "]");
                        }
                    }
                }

                AnagraficaUtils.fillDTO(dto, researcher.getDynamicField(),
                        realFillTPS);

                // one-shot fill and reverse to well-format clonedto and clean
                // empty
                // data
                AnagraficaUtils.fillDTO(clonedto, clone.getDynamicField(),
                        realFillTPS);

                AnagraficaUtils.reverseDTO(clonedto, clone.getDynamicField(),
                        realFillTPS);

                AnagraficaUtils.fillDTO(clonedto, clone.getDynamicField(),
                        realFillTPS);
                importDynAXML(applicationService, realFillTPS, node, dto,
                        clonedto, update);

                for (IContainable containable : structuralFillField)
                {
                    String shortName = containable.getShortName();
                    // xpathExpression = containable.getShortName();
                    if (containable instanceof DecoratorRestrictedField)
                    {
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
                                String nameSetter = m.getName().replaceFirst(
                                        "g", "s");
                                setter = researcher.getClass().getMethod(
                                        nameSetter, method.getReturnType());
                                break;
                            }
                        }
                        if (method.getReturnType().isAssignableFrom(List.class))
                        {

                            // NodeList nodeslist = (NodeList) xpath.evaluate(
                            // xpathExpression, node,
                            // XPathConstants.NODESET);
                            List<Element> nodeslist = XMLUtils.getElementList(
                                    node, shortName);

                            List<RestrictedField> object = (List<RestrictedField>) field;
                            List<RestrictedField> objectclone = new LinkedList<RestrictedField>();
                            objectclone.addAll(object);

                            for (int y = 0; y < nodeslist.size(); y++)
                            {
                                if (update == true && y == 0)
                                {
                                    object.clear();
                                }
                                Element nsublist = nodeslist.get(y);
                                String value = nsublist.getTextContent();

                                // String visibilityString = xpath.evaluate(
                                // XPATH_RULES[2], nsublist);
                                String visibilityString = nsublist
                                        .getAttribute(UtilsXML.NAMEATTRIBUTE_VISIBILITY);

                                if (value != null && !value.isEmpty())
                                {

                                    RestrictedField itemInCollection = new RestrictedField();

                                    Integer visibility = null;
                                    if (visibilityString != null
                                            && !visibilityString.isEmpty())
                                    {
                                        visibility = Integer
                                                .parseInt(visibilityString);
                                    }
                                    else if (update == false)
                                    {
                                        visibility = VisibilityConstants.PUBLIC;

                                    }
                                    else
                                    {
                                        visibility = checkOldVisibility(
                                                applicationService, value,
                                                objectclone, visibility);

                                    }

                                    // RestrictedField old = checkOldValue(
                                    // applicationService, value, object,
                                    // visibility);
                                    // if (old == null) {
                                    itemInCollection.setValue(value);
                                    if (visibility != null)
                                    {
                                        itemInCollection
                                                .setVisibility(visibility);
                                    }
                                    object.add(itemInCollection);
                                    setter.invoke(researcher, object);
                                    // }
                                }
                                // else {
                                // if (update == true
                                // && nodeslist.getLength() == 1) {
                                // setter.invoke(researcher,
                                // (List<RestrictedField>) null);
                                // }
                                // }

                            }

                        }
                        else
                        {
                            // Object control_value = xpath.evaluate(
                            // xpathExpression, node, XPathConstants.NODE);
                            Element control_value = XMLUtils.getSingleElement(
                                    node, shortName);
                            if (control_value != null)
                            {
                                // String value =
                                // xpath.evaluate(xpathExpression,
                                // node);
                                // String visibilityString = xpath.evaluate(
                                // xpathExpression + "/" + XPATH_RULES[2],
                                // node);

                                String value = XMLUtils.getElementValue(node,
                                        shortName);
                                String visibilityString = control_value
                                        .getAttribute(UtilsXML.NAMEATTRIBUTE_VISIBILITY);

                                if (!value.isEmpty())
                                {

                                    if (method.getReturnType().equals(
                                            String.class))
                                    {

                                        setter.invoke(researcher, value);
                                    }
                                    else
                                    {

                                        Integer visibility = null;
                                        if (visibilityString != null
                                                && !visibilityString.isEmpty())
                                        {
                                            visibility = Integer
                                                    .parseInt(visibilityString);
                                        }
                                        else if (!update)
                                        {
                                            visibility = VisibilityConstants.PUBLIC;

                                        }
                                        else
                                        {
                                            visibility = checkOldVisibility(
                                                    applicationService, value,
                                                    (RestrictedField) field,
                                                    visibility);

                                        }

                                        if (RestrictedField.class.equals(method
                                                .getReturnType()))
                                        {

                                            RestrictedField object = (RestrictedField) field;
                                            object.setValue(value);
                                            if (visibility != null)
                                            {
                                                object.setVisibility(visibility);
                                            }
                                            setter.invoke(researcher, object);
                                        }
                                    }

                                }
                                else
                                {
                                    if (update)
                                    {

                                        if (RestrictedField.class.equals(method
                                                .getReturnType()))
                                        {
                                            setter.invoke(researcher,
                                                    (RestrictedField) null);
                                        }

                                    }
                                }
                            }

                        }
                    }

                }

                AnagraficaUtils.reverseDTO(dto, researcher.getDynamicField(),
                        realFillTPS);

                EPerson dspaceUser = researcher.getDspaceUser();
                if (dspaceUser == null)
                {
                    // no dspace user we need to create it
                    try
                    {
                        EPerson emailUser = EPerson.findByEmail(dspaceContext,
                                researcher.getEmail().getValue());
                        if (emailUser != null)
                        {
                            throw new RuntimeException(
                                    "XML Row discarded STAFFNO : "
                                            + researcher.getSourceID()
                                            + " Find an eperson with email/netId '"
                                            + emailUser.getEmail()
                                            + "/"
                                            + emailUser.getNetid()
                                            + "' that not referred to the staffNo '"
                                            + researcher.getSourceID()
                                            + "' of researcher. Perhaps is it the same person?");
                        }
                        else
                        {
                            dspaceUser = EPerson.create(dspaceContext);
                            DCPersonName personalName = new DCPersonName(
                                    researcher.getFullName());
                            dspaceUser.setNetid(researcher.getSourceID());
                            dspaceUser.setFirstName(personalName
                                    .getFirstNames());
                            dspaceUser.setLastName(personalName.getLastName());
                            dspaceUser.setEmail(researcher.getEmail()
                                    .getValue());
                            dspaceUser.setLanguage("en");
                            dspaceUser.setCanLogIn(true);
                            dspaceUser.update();
                        }
                    }
                    catch (SQLException e)
                    {
                        throw new RuntimeException(
                                "XML Row discarded STAFFNO : "
                                        + researcher.getSourceID()
                                        + " Creation failure new eperson or researcher's mail has not been setted");
                    }
                    catch (AuthorizeException e)
                    {
                        throw new RuntimeException(
                                "XML Row discarded STAFFNO : "
                                        + researcher.getSourceID()
                                        + " Authorize failure");
                    }
                    dspaceContext.commit();
                }

                applicationService.saveOrUpdate(ResearcherPage.class,
                        researcher);

                log.info("Import researcher " + researcher.getSourceID()
                        + " (staffNo) / " + researcher.getId()
                        + " (id) - SUCCESS");
                rows_imported++;
            }
            catch (RuntimeException e)
            {
                log.error("Import researcher - FAILED " + e.getMessage(), e);
                rows_discarded++;
            }

        }

        log.info("Import researchers - end import additional files");

        log.info("Statistics: row ingested " + rows_imported + " on total of "
                + (researchers.size()) + " (" + rows_discarded
                + " row discarded)");
    }

    private static <TP extends PropertiesDefinition> void importDynAXML(
            ApplicationService applicationService, List<TP> realFillTPS,
            Element node, AnagraficaObjectDTO dto,
            AnagraficaObjectDTO clonedto, boolean update)
            throws XPathExpressionException
    {
        // foreach dynamic field read xml and fill on dto
        for (TP rpPD : realFillTPS)
        {

            // xpathExpression = rpPD.getShortName();
            String shortName = rpPD.getShortName();
            List<ValoreDTO> values = dto.getAnagraficaProperties().get(
                    shortName);
            List<ValoreDTO> oldValues = clonedto.getAnagraficaProperties().get(
                    shortName);
            if (rpPD.getRendering() instanceof WidgetTesto)
            {
                if (rpPD.isRepeatable())
                {

                    // NodeList nodeslist = (NodeList) xpath.evaluate(
                    // xpathExpression, node,
                    // XPathConstants.NODESET);

                    List<Element> nodeslist = XMLUtils.getElementList(node,
                            shortName);

                    for (int y = 0; y < nodeslist.size(); y++)
                    {
                        if (update == true && y == 0)
                        {
                            dto.getAnagraficaProperties().get(shortName)
                                    .clear();
                        }
                        Element nodetext = nodeslist.get(y);
                        String control_value = nodetext.getTextContent();
                        if (control_value != null && !control_value.isEmpty())
                        {
                            workOnText(applicationService, nodetext, rpPD,
                                    values, oldValues);
                        }
                        // else {
                        // if (update == true
                        // && nodeslist.getLength() == 1) {
                        // dto.getAnagraficaProperties()
                        // .get(shortName).clear();
                        // }
                        // }
                    }
                }
                else
                {
                    // Node nodeText = (Node) xpath.evaluate(
                    // xpathExpression, node, XPathConstants.NODE);
                    Element nodeText = XMLUtils.getSingleElement(node,
                            shortName);
                    String control_value = null;
                    try
                    {
                        control_value = nodeText.getTextContent();
                    }
                    catch (NullPointerException exc)
                    {
                        // nothing
                    }
                    if (control_value != null)
                    {
                        if (update == true)
                        {
                            dto.getAnagraficaProperties().get(shortName)
                                    .clear();
                        }
                        workOnText(applicationService, nodeText, rpPD, values,
                                oldValues);
                    }
                }
            }
            if (rpPD.getRendering() instanceof WidgetDate)
            {
                if (rpPD.isRepeatable())
                {
                    // NodeList nodeslist = (NodeList) xpath.evaluate(
                    // xpathExpression, node,
                    // XPathConstants.NODESET);
                    List<Element> nodeslist = XMLUtils.getElementList(node,
                            shortName);

                    for (int y = 0; y < nodeslist.size(); y++)
                    {
                        if (update == true && y == 0)
                        {
                            dto.getAnagraficaProperties().get(shortName)
                                    .clear();
                        }
                        Node nodeDate = nodeslist.get(y);
                        String control_value = nodeDate.getTextContent();
                        if (control_value != null && !control_value.isEmpty())
                        {
                            workOnDate(applicationService, node, rpPD, values,
                                    oldValues, nodeDate);
                        }
                        // else {
                        // if (update == true
                        // && nodeslist.getLength() == 1) {
                        // dto.getAnagraficaProperties()
                        // .get(shortName).clear();
                        // }
                        // }
                    }
                }
                else
                {
                    // Node nodeDate = (Node) xpath.evaluate(
                    // xpathExpression, node, XPathConstants.NODE);
                    Element nodeDate = XMLUtils.getSingleElement(node,
                            shortName);
                    String control_value = null;
                    try
                    {
                        control_value = nodeDate.getTextContent();
                    }
                    catch (NullPointerException exc)
                    {
                        // nothing
                    }
                    if (control_value != null)
                    {
                        if (update == true)
                        {
                            dto.getAnagraficaProperties().get(shortName)
                                    .clear();
                        }
                        workOnDate(applicationService, node, rpPD, values,
                                oldValues, nodeDate);
                    }
                }
            }
            if (rpPD.getRendering() instanceof WidgetLink)
            {

                if (rpPD.isRepeatable())
                {
                    // NodeList nodeslist = (NodeList) xpath.evaluate(
                    // xpathExpression, node,
                    // XPathConstants.NODESET);
                    List<Element> nodeslist = XMLUtils.getElementList(node,
                            shortName);

                    for (int y = 0; y < nodeslist.size(); y++)
                    {
                        if (update == true && y == 0)
                        {
                            dto.getAnagraficaProperties().get(shortName)
                                    .clear();
                        }
                        Element nodeLink = nodeslist.get(y);
                        String control_value = nodeLink.getTextContent();
                        if (control_value != null && !control_value.isEmpty())
                        {
                            workOnLink(applicationService, rpPD, values,
                                    oldValues, nodeLink);
                        }
                        // else {
                        // if (update == true
                        // && nodeslist.getLength() == 1) {
                        // dto.getAnagraficaProperties()
                        // .get(shortName).clear();
                        // }
                        // }
                    }
                }
                else
                {
                    // Node nodeLink = (Node) xpath.evaluate(
                    // xpathExpression, node, XPathConstants.NODE);
                    Element nodeLink = XMLUtils.getSingleElement(node,
                            shortName);
                    String control_value = null;
                    try
                    {
                        control_value = nodeLink.getTextContent();
                    }
                    catch (NullPointerException exc)
                    {
                        // nothing
                    }
                    if (control_value != null)
                    {
                        if (update == true)
                        {
                            dto.getAnagraficaProperties().get(shortName)
                                    .clear();
                        }
                        workOnLink(applicationService, rpPD, values, oldValues,
                                nodeLink);
                    }
                }
            }

            if (rpPD.getRendering() instanceof WidgetFile)
            {
                // TODO
            }
        }
    }

    @Deprecated
    public static File generateGrantXSD(Writer writer, File dir,
            List<IContainable> metadata, File filexsd, String[] elementsRoot)
            throws IOException, NoSuchFieldException, SecurityException,
            InstantiationException, IllegalAccessException
    {

        UtilsXSD xsd = new UtilsXSD(writer);
        xsd.createGrantXSD(metadata, elementsRoot, null, null);
        return filexsd;
    }

    public static File newGenerateGrantXSD(Writer writer, File dir,
            List<IContainable> metadata, File filexsd, String[] elementsRoot,
            String[] attributeMainRow, boolean[] attributeMainRowRequired)
            throws IOException, NoSuchFieldException, SecurityException,
            InstantiationException, IllegalAccessException
    {

        UtilsXSD xsd = new UtilsXSD(writer);
        xsd.createGrantXSD(metadata, elementsRoot, attributeMainRow,
                attributeMainRowRequired);
        return filexsd;
    }

    @Deprecated
    public static File generateXSD(Writer writer, File dir,
            List<IContainable> metadata, File filexsd, String[] elementsRoot)
            throws IOException, NoSuchFieldException, SecurityException,
            InstantiationException, IllegalAccessException
    {

        UtilsXSD xsd = new UtilsXSD(writer);
        xsd.createXSD(metadata, elementsRoot);
        return filexsd;
    }

    public static File generateSimpleTypeWithListOfAllMetadata(Writer writer,
            List<IContainable> metadata, File filexsd, String namespace,
            String fullNamespace, String name) throws IOException,
            SecurityException, NoSuchFieldException
    {
        UtilsXSD xsd = new UtilsXSD(writer);
        xsd.createSimpleTypeFor(metadata, namespace, fullNamespace, name);
        return filexsd;
    }

    public static File newGenerateXSD(Writer writer, File dir,
            List<IContainable> metadata, File filexsd, String[] elementsRoot,
            String namespace, String namespaceValue, String namespaceTarget,
            String[] attributeMainRow, boolean[] attributeMainRowRequired)
            throws IOException, NoSuchFieldException, SecurityException,
            InstantiationException, IllegalAccessException
    {

        UtilsXSD xsd = new UtilsXSD(writer);
        xsd.createXSD(metadata, elementsRoot, namespace, namespaceValue,
                namespaceTarget, attributeMainRow, attributeMainRowRequired);
        return filexsd;
    }

    private static <TP extends PropertiesDefinition> void workOnText(
            ApplicationService applicationService, Element node, TP rpPD,
            List<ValoreDTO> values, List<ValoreDTO> old)
            throws XPathExpressionException
    {
        if (node != null)
        {

            // String nodetext = node.getTextContent();
            // String vis = xpath.evaluate(XPATH_RULES[2], node);
            String nodetext = node.getTextContent();
            String vis = node.getAttribute(UtilsXML.NAMEATTRIBUTE_VISIBILITY);

            if (nodetext != null && !nodetext.isEmpty())
            {
                ValoreDTO valueDTO = new ValoreDTO(nodetext);
                if (vis == null || vis.isEmpty())
                {
                    // check old value
                    vis = checkOldVisibility(applicationService, rpPD, old,
                            nodetext, vis);
                }

                if (vis != null && !vis.isEmpty())
                {
                    valueDTO.setVisibility((Integer.parseInt(vis) == 1 ? true
                            : false));
                }

                // ValoreDTO oldValue = checkOldValue(applicationService, rpPD,
                // old, nodetext, valueDTO.getVisibility());
                // if(oldValue==null) {
                values.add(valueDTO);
                log.debug("Write text field " + rpPD.getShortName()
                        + " with value: " + nodetext + " visibility: "
                        + valueDTO.getVisibility());
                // }
            }
        }
    }

    private static <TP extends PropertiesDefinition> void workOnLink(
            ApplicationService applicationService, TP rpPD,
            List<ValoreDTO> values, List<ValoreDTO> old, Element nodeLink)
            throws XPathExpressionException
    {
        if (nodeLink != null)
        {

            // String nodetext = nodeLink.getTextContent();
            String nodetext = nodeLink.getTextContent();

            if (nodetext != null && !nodetext.isEmpty())
            {
                // String vis = xpath.evaluate(XPATH_RULES[2], node);
                String vis = nodeLink
                        .getAttribute(UtilsXML.NAMEATTRIBUTE_VISIBILITY);
                // if (vis != null && vis.isEmpty()) {
                // vis = xpath.evaluate(XPATH_RULES[2], nodeLink);
                // }
                // String src = xpath.evaluate(XPATH_RULES[4], node);
                String src = nodeLink
                        .getAttribute(UtilsXML.NAMEATTRIBUTE_SRC_LINK);
                // if (src != null && src.isEmpty()) {
                // src = xpath.evaluate(XPATH_RULES[4], nodeLink);
                // }

                nodetext += "|||" + src;

                if (vis == null || vis.isEmpty())
                {
                    // check old value
                    vis = checkOldVisibility(applicationService, rpPD, old,
                            nodetext, vis);
                }
                PropertyEditor pe = rpPD.getRendering().getPropertyEditor(
                        applicationService);
                pe.setAsText(nodetext);
                ValoreDTO valueDTO = new ValoreDTO(pe.getValue());
                if (vis != null && !vis.isEmpty())
                {
                    valueDTO.setVisibility((Integer.parseInt(vis) == 1 ? true
                            : false));
                }
                // ValoreDTO oldValue = checkOldValue(applicationService, rpPD,
                // old, nodetext, valueDTO.getVisibility());
                // if(oldValue==null) {
                values.add(valueDTO);
                log.debug("Write link field " + rpPD.getShortName()
                        + " with value" + nodetext + " visibility: "
                        + valueDTO.getVisibility());
                // }
            }
        }
    }

    private static <TP extends PropertiesDefinition> void workOnDate(
            ApplicationService applicationService, Element node, TP rpPD,
            List<ValoreDTO> values, List<ValoreDTO> old, Node nodeDate)
            throws XPathExpressionException
    {
        if (nodeDate != null)
        {
            // String nodetext = nodeDate.getTextContent();
            String nodetext = nodeDate.getTextContent();

            if (nodetext != null && !nodetext.isEmpty())
            {
                // String vis = xpath.evaluate(XPATH_RULES[2], node);
                String vis = node
                        .getAttribute(UtilsXML.NAMEATTRIBUTE_VISIBILITY);
                // if (vis != null) {
                // if (vis.isEmpty()) {
                // vis = xpath.evaluate(XPATH_RULES[2], nodeDate);
                // }
                // }

                if (vis == null || vis.isEmpty())
                {
                    // check old value
                    vis = checkOldVisibility(applicationService, rpPD, old,
                            nodetext, vis);
                }

                PropertyEditor pe = rpPD.getRendering().getPropertyEditor(
                        applicationService);
                pe.setAsText(nodetext);
                ValoreDTO valueDTO = new ValoreDTO(pe.getValue());
                if (vis != null && !vis.isEmpty())
                {
                    valueDTO.setVisibility((Integer.parseInt(vis) == 1 ? true
                            : false));
                }
                // ValoreDTO oldValue = checkOldValue(applicationService, rpPD,
                // old, nodetext, valueDTO.getVisibility());
                // if(oldValue==null) {
                values.add(valueDTO);
                log.debug("Write date field " + rpPD.getShortName()
                        + " with value: " + nodetext + " visibility: "
                        + valueDTO.getVisibility());
                // }
            }
        }
    }

    /**
     * 
     * Check old visibility on dynamic field
     * 
     * @param applicationService
     * @param rpPD
     * @param old
     * @param nodetext
     * @param vis
     * @return
     */
    private static <TP extends PropertiesDefinition> String checkOldVisibility(
            ApplicationService applicationService, TP rpPD,
            List<ValoreDTO> old, String nodetext, String vis)
    {
        PropertyEditor pe = rpPD.getRendering().getPropertyEditor(
                applicationService);

        boolean founded = false;
        for (ValoreDTO temp : old)
        {
            pe.setValue(temp.getObject());
            if (pe.getAsText().equals(nodetext))
            {
                vis = temp.getVisibility() ? "1" : "0";
                founded = true;
                break;
            }
        }
        return founded == true ? vis : DEFAULT_VISIBILITY;
    }

    private static Integer checkOldVisibility(
            ApplicationService applicationService, String value,
            List<RestrictedField> object, Integer vis)
    {
        boolean founded = false;
        for (RestrictedField f : object)
        {
            if (f.getValue().equals(value))
            {
                vis = f.getVisibility();
                founded = true;
                break;
            }
        }
        return founded == true ? vis : VisibilityConstants.PUBLIC;
    }

    private static Integer checkOldVisibility(
            ApplicationService applicationService, String value,
            RestrictedField field, Integer visibility)
    {

        return field.getValue().equals(value) ? field.getVisibility()
                : VisibilityConstants.PUBLIC;
    }

    /**
     * Export xml, it don't close or flush writer, format with
     * {@link XMLOutputter}, use use jdom for it.
     * 
     * @param writer
     * @param applicationService
     * @param metadata
     * @param researchers
     * @throws IOException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    @Deprecated
    public static void exportXML(Writer writer,
            ApplicationService applicationService, List<IContainable> metadata,
            List<ResearcherPage> researchers) throws IOException,
            SecurityException, IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException, InvocationTargetException,
            ParserConfigurationException, TransformerException
    {

        UtilsXML xml = new UtilsXML(writer, applicationService);
        org.jdom.Document xmldoc = xml.createRoot(null, null,
                "http://www.cilea.it/researcherpage/schemas");
        if (researchers != null)
        {
            for (ResearcherPage rp : researchers)
            {
                xml.writeRP(rp, metadata, xmldoc.getRootElement());
            }
            // Serialisation through XMLOutputter
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            out.output(xmldoc, writer);
        }
    }

    public static <TP extends PropertiesDefinition, P extends Property<TP>, AO extends AnagraficaObject<P, TP>, I extends IExportableDynamicObject<TP, P, AO>> void newExportXML(
            Writer writer, ApplicationService applicationService,
            List<IContainable> metadata, List<I> objects,
            String prefixNamespace, String namespace, String rootName)
            throws IOException, SecurityException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException,
            InvocationTargetException, ParserConfigurationException,
            TransformerException
    {

        UtilsXML xml = new UtilsXML(writer, applicationService);
        org.jdom.Document xmldoc = xml.createRoot(rootName, prefixNamespace,
                namespace);
        if (objects != null)
        {
            for (I rp : objects)
            {
                xml.write(rp, metadata, xmldoc.getRootElement());
            }
            // Serialisation through XMLOutputter
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            out.output(xmldoc, writer);
        }
    }

    /**
     * 
     * TODO 
     * 
     * Import RGs from RPs finded on database
     * 
     * @param applicationService
     * @param dspaceContext
     * @param status
     *            get only active or all rps
     * @param active
     *            set status true to newly rg
     * @param newly
     *            add only new project
     */
    public static void importGrants(ApplicationService applicationService,
            Context dspaceContext, boolean status, boolean active, boolean newly)
            throws Exception
    {

        List<ResearcherPage> rps = new LinkedList<ResearcherPage>();
        if (status)
        {
            rps = applicationService.getAllResearcherPageByStatus(status);
        }
        else
        {
            rps.addAll(applicationService.getList(ResearcherPage.class));
        }

        // extract grants from rps
        int newImported = 0;
        int editImported = 0;
        int discardImported = 0;
        int skipImported = 0;
        log.info("Start import " + new Date() + " mode(status/active/newly): "
                + status + "/" + active + "/" + newly);
        for (ResearcherPage rp : rps)
        {

            List<RPNestedObject> nestedObjects = ((ExtendedTabService) applicationService)
                    .getNestedObjectsByParentIDAndShortname(rp.getId(),
                            UtilsXML.GRANT_TAG_PROJECTS, RPNestedObject.class);

            if (nestedObjects != null)
            {
                for (RPNestedObject nestedObject : nestedObjects)
                {

                    List<RPNestedProperty> rpProperties = nestedObject
                            .getAnagrafica4view().get(
                                    UtilsXML.GRANT_TAG_PROJECTS);
                    for (RPNestedProperty rpp : rpProperties)
                    {

                        String projectcode = (String) (rpp.getValue()
                                .getObject());

                        Project rg = null;
                        // use dto to fill dynamic metadata
                        AnagraficaObjectDTO dtoRG = new AnagraficaObjectDTO();
                        AnagraficaObjectWithTypeDTO dtoNested = new AnagraficaObjectWithTypeDTO();
                        if (projectcode != null && !projectcode.isEmpty())
                        {
                            rg = applicationService
                                    .getResearcherGrantByCode(projectcode
                                            .trim());
                        }
                        else
                        {
                            log.error("Grant discarded ( projectCode not founded) [researcher: "
                                    + ResearcherPageUtils
                                            .getPersistentIdentifier(rp) + "]");
                            discardImported++;
                            continue;
                        }

                        // skip if only new grants mode and rg is found
                        if (newly && rg != null)
                        {
                            skipImported++;
                            continue;
                        }

                        // create new grants
                        if (rg == null)
                        {
                            log.info("Create new GRANT with code "
                                    + projectcode);
                            rg = new Project();
                            rg.setSourceID(projectcode);
                            rg.setStatus(active);
                            newImported++;
                        }
                        else
                        {
                            log.info("Edit GRANT with code " + projectcode);
                            editImported++;
                        }

                        List<RPNestedPropertiesDefinition> subTps = nestedObject.getTypo().getMask();
                        AnagraficaUtils
                                .fillDTO(dtoNested, nestedObject, subTps);

                        List<ProjectPropertiesDefinition> rgTps = applicationService
                                .getList(ProjectPropertiesDefinition.class);

                        for (String key : dtoNested.getAnagraficaProperties()
                                .keySet())
                        {
                            dtoRG.getAnagraficaProperties().put(
                                    key,
                                    dtoNested.getAnagraficaProperties()
                                            .get(key));
                        }

                        // get investigators/coninvestigator
                        List<ValoreDTO> investigatorDTO = dtoRG
                                .getAnagraficaProperties().get(
                                        UtilsXML.GRANT_TAG_INVESTIGATOR);
                        for (ValoreDTO vv : investigatorDTO)
                        {

                            EmbeddedLinkValue link = (EmbeddedLinkValue) vv
                                    .getObject();
                            if (link != null)
                            {
                                if (link.getValueLink() != null
                                        && !link.getValueLink().isEmpty())
                                {
                                    dtoRG.getAnagraficaProperties()
                                            .put("principalinvestigator",
                                                    dtoNested
                                                            .getAnagraficaProperties()
                                                            .get("principalinvestigator")
                                                            );

                                }
                                else
                                {
                                    dtoRG.getAnagraficaProperties()
                                    .put("extprincipalinvestigator",
                                            dtoNested
                                                    .getAnagraficaProperties()
                                                    .get("extprincipalinvestigator")
                                                    );
                                }
                            }
                            
                        }
                        List<ValoreDTO> coinvestigatorDTO = dtoRG
                                .getAnagraficaProperties().get(
                                        UtilsXML.GRANT_TAG_COINVESTIGATOR);
                        
                        for (ValoreDTO vv : coinvestigatorDTO)
                        {
                            
                            EmbeddedLinkValue link = (EmbeddedLinkValue) vv
                                    .getObject();
                            if (link != null)
                            {
                                if (link.getValueLink() != null
                                        && !link.getValueLink().isEmpty())
                                {
                                    dtoRG.getAnagraficaProperties()
                                    .put("coinvestigator",
                                            dtoNested
                                                    .getAnagraficaProperties()
                                                    .get("coinvestigator")
                                                    );
                                }
                                else
                                {
                                    dtoRG.getAnagraficaProperties()
                                    .put("extcoinvestigator",
                                            dtoNested
                                                    .getAnagraficaProperties()
                                                    .get("extcoinvestigator")
                                                    );

                                }
                            }
                            
                        }
                        
                        AnagraficaUtils.reverseDTO(dtoRG, rg, rgTps);
                        applicationService.saveOrUpdate(Project.class, rg);
                    }
                }
            }
        }
        log.info("Stats: total grants imported " + (newImported + editImported)
                + " new/edit:" + newImported + "/" + editImported
                + " discarded:" + discardImported + " "
                + (newly ? " skipped:" + skipImported : ""));
        log.info("### END IMPORT " + new Date());

    }

    /**
     * TODO
     * 
     * Import grant from xml file, matching validation with xsd builded at
     * runtime execution associate to list of dynamic fields
     * 
     * @param input
     *            - XML file stream
     * @param dir
     *            - directory to write temporaries xsd and xml (this xsd
     *            validate actual xml)
     * @param applicationService
     *            - service
     * @throws Exception
     */
    public static void importGrantsXML(InputStream input, File dir,
            ApplicationService applicationService, Context dspaceContext,
            boolean status) throws Exception
    {

        File filexsd = null;
        File filexml = null;

        // build filexml
        String nameXML = "xml-" + dateFormat.format(new Date()) + ".xml";
        filexml = new File(dir, nameXML);
        filexml.createNewFile();
        FileOutputStream out = new FileOutputStream(filexml);
        Utils.bufferedCopy(input, out);
        out.close();

        List<IContainable> metadataALL = applicationService
                .findAllContainables(ProjectPropertiesDefinition.class);

        // create xsd and write up
        String nameXSD = "xsd-" + dateFormat.format(new Date()) + ".xsd";
        filexsd = new File(dir, nameXSD);
        filexsd.createNewFile();
        FileWriter writer = new FileWriter(filexsd);
        filexsd = generateGrantXSD(writer, dir, metadataALL, filexsd,
                new String[] { "grants", "grant" });

        // create xsd validator
        SchemaFactory factory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaSource = new StreamSource(filexsd);

        Schema schema = factory.newSchema(schemaSource);
        // validate source xml to xsd
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(filexml));

        // parse xml to dom
        DocumentBuilder parser = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document document = parser.parse(filexml);

        List<ProjectPropertiesDefinition> realFillTPS = applicationService
                .getList(ProjectPropertiesDefinition.class);

        List<Element> grantxml = XMLUtils.getElementList(
                document.getDocumentElement(), "grant");
        int rows_discarded = 0;
        int rows_imported = 0;
        log.info("Start import " + new Date());
        // foreach researcher element in xml
        for (int i = 0; i < grantxml.size(); i++)
        {
            log.info("Number " + i + " of " + grantxml.size());
            Project grant = null;
            try
            {
                Element node = grantxml.get(i);

                // check if staffNo and rpid exists as attribute
                // String nodeId = (String) xpath.evaluate(XPATH_RULES[1], node,
                // XPathConstants.STRING);
                // String rpId = (String) xpath.evaluate(XPATH_RULES[3], node,
                // XPathConstants.STRING);
                String nodeId = node
                        .getAttribute(UtilsXML.GRANT_NAMEATTRIBUTE_CODE);
                String rgId = node
                        .getAttribute(UtilsXML.GRANT_NAMEATTRIBUTE_RGID);
                Project clone = null;
                // use dto to fill dynamic metadata
                AnagraficaObjectDTO dto = new AnagraficaObjectDTO();
                AnagraficaObjectDTO clonedto = new AnagraficaObjectDTO();
                boolean update = false; // if update a true then set field to
                                        // null
                                        // on case of empty element
                if (nodeId == null || nodeId.isEmpty())
                {
                    log.error("Grant discarded ( code not founded) [position grant: "
                            + i + "]");
                    throw new RuntimeException(
                            "Grant discarded (code not founded whilst rgId is on xml) [position grant: "
                                    + i + "]");

                }
                else
                {
                    // if there is rgid then try to get grant by code
                    // and
                    // set to null all structural metadata lists
                    log.info("Grant staffNo : " + nodeId
                            + " / rg identifier : " + rgId);
                    if (rgId != null && !rgId.isEmpty())
                    {
                        grant = applicationService
                                .getResearcherGrantByCode(nodeId);
                        if (grant == null)
                        {
                            log.error("Grant discarded (code not founded whilst rgId is on xml) [position grant: "
                                    + i + "]");
                            ;
                            throw new RuntimeException(
                                    "Grant discarded (code not founded whilst rgId is on xml) [position grant: "
                                            + i + "]");
                        }
                        else
                        {
                            if (!rgId.equals(grant.getId().toString()))
                            {
                                log.error("Grant discarded (rgId don't match persistent identifier) [position grant: "
                                        + i + "]");
                                throw new RuntimeException(
                                        "Grant discarded (rgId don't match persistent identifier) [position grant: "
                                                + i + "]");
                            }
                        }
                        // clone dynamic data and structural on dto

                        clone = (Project) grant.clone();
                        ProjectAdditionalFieldStorage additionalTemp = new ProjectAdditionalFieldStorage();
                        clone.setDynamicField(additionalTemp);
                        additionalTemp.duplicaAnagrafica(grant
                                .getDynamicField());
                        update = true;
                    }
                    else
                    {
                        // here there is perhaps a new grant
                        grant = applicationService
                                .getResearcherGrantByCode(nodeId);
                        if (grant == null)
                        {
                            grant = new Project();
                            grant.setSourceID(nodeId);
                            // use -active in command line to change default
                            // status to active.
                            grant.setStatus(status);

                            clone = (Project) grant.clone();
                            ProjectAdditionalFieldStorage additionalTemp = new ProjectAdditionalFieldStorage();
                            clone.setDynamicField(additionalTemp);
                            additionalTemp.duplicaAnagrafica(grant
                                    .getDynamicField());

                        }
                        else
                        {
                            log.error("Grant discarded (code " + nodeId
                                    + " already exist) [position grant: " + i
                                    + "]");
                            throw new RuntimeException("Grant discarded (code "
                                    + nodeId
                                    + " already exist) [position grant: " + i
                                    + "]");
                        }
                    }
                }

                AnagraficaUtils.fillDTO(dto, grant, realFillTPS);

                // one-shot fill and reverse to well-format clonedto and clean
                // empty
                // data
                AnagraficaUtils.fillDTO(clonedto, clone, realFillTPS);
                AnagraficaUtils.reverseDTO(clonedto, clone, realFillTPS);
                AnagraficaUtils.fillDTO(clonedto, clone, realFillTPS);

                importDynAXML(applicationService, realFillTPS, node, dto,
                        clonedto, update);
       
                AnagraficaUtils.reverseDTO(dto, grant, realFillTPS);

                applicationService.saveOrUpdate(Project.class, grant);

                log.info("Import grant " + grant.getSourceID() + " (code) / "
                        + grant.getId() + " (id) - SUCCESS");
                rows_imported++;
            }
            catch (RuntimeException e)
            {
                log.error("Import grant - FAILED " + e.getMessage(), e);
                rows_discarded++;
            }

        }

        log.info("Statistics: row ingested " + rows_imported + " on total of "
                + (grantxml.size()) + " (" + rows_discarded + " row discarded)");
    }

}
