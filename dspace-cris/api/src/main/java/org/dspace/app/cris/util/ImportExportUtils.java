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
import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
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
import java.io.FileNotFoundException;
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

import jxl.Cell;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.importexport.IBulkChange;
import org.dspace.app.cris.importexport.IBulkChangeField;
import org.dspace.app.cris.importexport.IBulkChangeFieldLink;
import org.dspace.app.cris.importexport.IBulkChangeFieldValue;
import org.dspace.app.cris.importexport.IBulkChanges;
import org.dspace.app.cris.importexport.IBulkChangesService;
import org.dspace.app.cris.importexport.XmlBulkChanges;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.IExportableDynamicObject;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.RestrictedField;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.export.ExportConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
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
import org.dspace.utils.DSpace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
            + UtilsXML.NAMEATTRIBUTE_CRISID;

    private static final String XPATH_ATTRIBUTE_STAFFNO = "@"
            + UtilsXML.NAMEATTRIBUTE_SOURCEID;

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

	private static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
	ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>, ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>> ACO getCrisObject(
		ApplicationService applicationService, Class<ACO> objectTypeClass, IBulkChange change) throws InstantiationException, IllegalAccessException {
	String action = change.getAction();
	String crisID = change.getCrisID();
	String uuid = change.getUUID();
	String sourceRef = change.getSourceRef();
	String sourceId = change.getSourceID();
	
	ACO crisObject = null;
	
	if (StringUtils.isBlank(crisID) && StringUtils.isBlank(uuid) && StringUtils.isBlank(sourceRef) && StringUtils.isBlank(sourceId)) {
		if (!StringUtils.equalsIgnoreCase("delete", action)) {
			crisObject = objectTypeClass.newInstance();
		}
	}
	else {
		if (StringUtils.isNotBlank(crisID)) {
			crisObject = applicationService.getEntityByCrisId(crisID, objectTypeClass);
		}
		
		if (crisObject == null && StringUtils.isNotBlank(uuid)) {
			crisObject = (ACO) applicationService.getEntityByUUID(uuid);
		}
		
		if (crisObject == null && StringUtils.isNotBlank(sourceId)) {
			crisObject = applicationService.getEntityBySourceId(sourceRef, sourceId, objectTypeClass);
		}
	}
	
	if (StringUtils.isNotBlank(crisID)) {
		crisObject.setCrisID(crisID);
	}
	
	if (StringUtils.isNotBlank(uuid)) {
		crisObject.setUuid(uuid);
	}
	
	if (StringUtils.isNotBlank(sourceRef)) {
		crisObject.setSourceRef(sourceRef);
	}
	if (StringUtils.isNotBlank(sourceId)) {
		crisObject.setSourceID(sourceId);
	}
	return crisObject;
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
    public static <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
	ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void process(String format, InputStream input, File dir,
            ApplicationService applicationService, Context dspaceContext,
            boolean status, Class<TP> propDefClazz, Class<ACO> crisObjectClazz) throws Exception
    {

        List<IContainable> metadataALL = applicationService
                .findAllContainables(propDefClazz);
        
        DSpace dspace = new DSpace();
        IBulkChangesService importer = dspace.getServiceManager().getServiceByName(format, IBulkChangesService.class);
        IBulkChanges bulkChanges = importer.getBulkChanges(input, dir, crisObjectClazz, propDefClazz, metadataALL);

        processBulkChanges(applicationService, propDefClazz, crisObjectClazz,
				metadataALL, bulkChanges);
    }

	private static <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>, P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, 
	ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void processBulkChanges(
			ApplicationService applicationService, Class<TP> propDefClazz,
			Class<ACO> crisObjectClazz, List<IContainable> metadataALL,
			IBulkChanges bulkChanges) throws InstantiationException,
			IllegalAccessException, CloneNotSupportedException,
			XPathExpressionException, InvocationTargetException,
			NoSuchMethodException {
		// get from list of metadata dynamic field vs structural field
        List<TP> realTPS = new LinkedList<TP>();
        List<IContainable> structuralField = new LinkedList<IContainable>();
        for (IContainable c : metadataALL)
        {
            TP rpPd = applicationService
                    .findPropertiesDefinitionByShortName(
                    		propDefClazz, c.getShortName());
            if (rpPd != null)
            {
                realTPS.add((TP) ((ADecoratorPropertiesDefinition<TP>) applicationService
                        .findContainableByDecorable(
                        		propDefClazz.newInstance()
                                        .getDecoratorClass(), c.getId()))
                        .getReal());
            }
            else
            {
                structuralField.add(c);
            }
        }

        List<TP> realFillTPS = new LinkedList<TP>();
        for (TP r : realTPS)
        {
            if (bulkChanges.hasPropertyDefinition(r.getShortName())) {
                realFillTPS.add(r);
            }
        }

        List<IContainable> structuralFillField = new LinkedList<IContainable>();
        for (IContainable r : structuralField)
        {
        	if (bulkChanges.hasPropertyDefinition(r.getShortName())) {
                structuralFillField.add(r);
            }
        }

        // import xml
        // XPath xpath = XPathFactory.newInstance().newXPath();
        // String xpathExpression = XPATH_RULES[0];
        // NodeList researchers = (NodeList) xpath.evaluate(xpathExpression,
        // document, XPathConstants.NODESET);

        int rows_discarded = 0;
        int rows_imported = 0;
        log.info("Start import " + new Date());
        // foreach researcher element in xml
        for (int i = 0; i < bulkChanges.size(); i++)
        {
            log.info("Number " + i + " of " + bulkChanges.size());
            ACO researcher = null;
            try
            {
            	IBulkChange bulkChange = bulkChanges.getChanges(i);

                String sourceId = bulkChange.getSourceID();
                String sourceRef = bulkChange.getSourceRef();
                String crisID = bulkChange.getCrisID();
                String uuid = bulkChange.getUUID();
            	
                ACrisObject<P, TP, NP, NTP, ACNO, ATNO> clone = null;
                // use dto to fill dynamic metadata
                AnagraficaObjectDTO dto = new AnagraficaObjectDTO();
                AnagraficaObjectDTO clonedto = new AnagraficaObjectDTO();
                boolean update = false; // if update a true then set field to
                                        // null
                                        // on case of empty element

                // if there is rpid then try to get researcher by staffNo
                // and
                // set to null all structural metadata lists
                log.info("Researcher sourceRef: "+ sourceRef + " sourceID: " + sourceId
                        + " / crisID : " + crisID + " uuid: "+uuid);
                
                ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject = getCrisObject(applicationService, crisObjectClazz, bulkChange);

                clone = (ACrisObject<P, TP, NP, NTP, ACNO, ATNO>) researcher.clone();

                AnagraficaUtils.fillDTO(dto, researcher,
                        realFillTPS);

                // one-shot fill and reverse to well-format clonedto and clean
                // empty
                // data
                AnagraficaUtils.fillDTO(clonedto, clone,
                        realFillTPS);

                AnagraficaUtils.reverseDTO(clonedto, clone,
                        realFillTPS);

                AnagraficaUtils.fillDTO(clonedto, clone,
                        realFillTPS);
                importDynAXML(applicationService, realFillTPS, bulkChange, dto,
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
                            IBulkChangeField changeField = bulkChange.getFieldChanges(shortName);

                            List<RestrictedField> object = (List<RestrictedField>) field;
                            List<RestrictedField> objectclone = new LinkedList<RestrictedField>();
                            objectclone.addAll(object);

                            if (update == true)
                            {
                                object.clear();
                            }
                            for (int y = 0; y < changeField.size(); y++)
                            {
                               
                                IBulkChangeFieldValue nsublist = changeField.get(y);
                                String value = nsublist.getValue();

                                // String visibilityString = xpath.evaluate(
                                // XPATH_RULES[2], nsublist);
                                String visibilityString = nsublist.getVisibility();

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
                        	IBulkChangeField changeField = bulkChange.getFieldChanges(shortName);
                            if (changeField.size() == 1)
                            	
                            {
                            	IBulkChangeFieldValue changeFieldValue = changeField.get(0);
                                // String value =
                                // xpath.evaluate(xpathExpression,
                                // node);
                                // String visibilityString = xpath.evaluate(
                                // xpathExpression + "/" + XPATH_RULES[2],
                                // node);

                                String value = changeFieldValue.getValue();
                                String visibilityString = changeFieldValue.getVisibility();

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

                AnagraficaUtils.reverseDTO(dto, researcher,
                        realFillTPS);

                applicationService.saveOrUpdate(crisObjectClazz,
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
                + (bulkChanges.size()) + " (" + rows_discarded
                + " row discarded)");
	}


    private static <TP extends PropertiesDefinition> void importDynAXML(
            ApplicationService applicationService, List<TP> realFillTPS,
            IBulkChange bulkChange, AnagraficaObjectDTO dto,
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
            if (update == true)
            {
                dto.getAnagraficaProperties().get(shortName)
                        .clear();
            }
            if (rpPD.getRendering() instanceof WidgetTesto)
            {
                if (rpPD.isRepeatable())
                {

                    // NodeList nodeslist = (NodeList) xpath.evaluate(
                    // xpathExpression, node,
                    // XPathConstants.NODESET);

                    IBulkChangeField nodeslist = bulkChange.getFieldChanges(shortName);

                    for (int y = 0; y < nodeslist.size(); y++)
                    {
                        IBulkChangeFieldValue nodetext = nodeslist.get(y);
                        String control_value = nodetext.getValue();
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
                	IBulkChangeField nodeslist = bulkChange.getFieldChanges(shortName);
                    IBulkChangeFieldValue nodeText = nodeslist.get(0);
                    String control_value = null;
                    try
                    {
                        control_value = nodeText.getValue();
                    }
                    catch (NullPointerException exc)
                    {
                        // nothing
                    }
                    if (control_value != null)
                    {
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
                	IBulkChangeField nodeslist = bulkChange.getFieldChanges(shortName);

                    for (int y = 0; y < nodeslist.size(); y++)
                    {
                        if (update == true && y == 0)
                        {
                            dto.getAnagraficaProperties().get(shortName)
                                    .clear();
                        }
                        IBulkChangeFieldValue nodeDate = nodeslist.get(y);
                        String control_value = nodeDate.getValue();
                        if (StringUtils.isNotBlank(control_value))
                        {
                            workOnDate(applicationService, nodeDate, rpPD, values,
                                    oldValues);
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
                	IBulkChangeField nodeslist = bulkChange.getFieldChanges(shortName);
                	IBulkChangeFieldValue nodeDate = nodeslist.get(0);
                	
                    String control_value = null;
                    try
                    {
                        control_value = nodeDate.getValue();
                    }
                    catch (NullPointerException exc)
                    {
                        // nothing
                    }

                    workOnDate(applicationService, nodeDate, rpPD, values,
                    		oldValues);
                }
            }
            if (rpPD.getRendering() instanceof WidgetLink)
            {

                if (rpPD.isRepeatable())
                {
                    // NodeList nodeslist = (NodeList) xpath.evaluate(
                    // xpathExpression, node,
                    // XPathConstants.NODESET);
                	IBulkChangeFieldLink nodeslist = bulkChange.getFieldLinkChanges(shortName);

                    for (int y = 0; y < nodeslist.size(); y++)
                    {
                        if (update == true && y == 0)
                        {
                            dto.getAnagraficaProperties().get(shortName)
                                    .clear();
                        }
                        IBulkChangeFieldLinkValue nodeLink = nodeslist.get(y);
                        String control_value = nodeLink.getValue();
                        if (StringUtils.isNotBlank(control_value))
                        {
                            workOnLink(applicationService, rpPD, values,
                                    oldValues, nodeLink);
                        }
                    }
                }
                else
                {
                    // Node nodeLink = (Node) xpath.evaluate(
                    // xpathExpression, node, XPathConstants.NODE);
                	IBulkChangeFieldLink nodeslist = bulkChange.getFieldLinkChanges(shortName);
                	IBulkChangeFieldLinkValue nodeLink = nodeslist.get(0);
                    String control_value = nodeLink.getValue();
                    if (StringUtils.isNotBlank(control_value))
                    {
                        workOnLink(applicationService, rpPD, values,
                                oldValues, nodeLink);
                    }
                }
            }

            if (rpPD.getRendering() instanceof WidgetFile)
            {
                // TODO
            }
        }
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

    private static <TP extends PropertiesDefinition> void workOnText(
            ApplicationService applicationService, IBulkChangeFieldValue node, TP rpPD,
            List<ValoreDTO> values, List<ValoreDTO> old)
            throws XPathExpressionException
    {
        if (node != null)
        {

            // String nodetext = node.getTextContent();
            // String vis = xpath.evaluate(XPATH_RULES[2], node);
            String nodetext = node.getValue();
            String vis = node.getVisibility();

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
            List<ValoreDTO> values, List<ValoreDTO> old, IBulkChangeFieldLinkValue nodeLink)
            throws XPathExpressionException
    {
        if (nodeLink != null)
        {

            // String nodetext = nodeLink.getTextContent();
            String nodetext = nodeLink.getValue();

            if (nodetext != null && !nodetext.isEmpty())
            {
                // String vis = xpath.evaluate(XPATH_RULES[2], node);
                String vis = nodeLink.getVisibility();
                // if (vis != null && vis.isEmpty()) {
                // vis = xpath.evaluate(XPATH_RULES[2], nodeLink);
                // }
                // String src = xpath.evaluate(XPATH_RULES[4], node);
                String src = nodeLink.getLinkURL();
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
            ApplicationService applicationService, IBulkChangeFieldValue node, TP rpPD,
            List<ValoreDTO> values, List<ValoreDTO> old)
            throws XPathExpressionException
    {
        if (node != null)
        {
            // String nodetext = nodeDate.getTextContent();
            String nodetext = node.getValue();

            if (nodetext != null && !nodetext.isEmpty())
            {
                // String vis = xpath.evaluate(XPATH_RULES[2], node);
                String vis = node.getVisibility();
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
        org.jdom.Document xmldoc = xml.createRoot(UtilsXSD.RP_DEFAULT_ELEMENT[0], UtilsXSD.NAMESPACE_PREFIX_RP,
        		UtilsXSD.NAMESPACE_RP);
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

}

