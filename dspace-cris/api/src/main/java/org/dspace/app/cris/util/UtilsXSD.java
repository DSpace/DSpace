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
import it.cilea.osd.jdyna.model.IContainable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.widget.WidgetDate;
import it.cilea.osd.jdyna.widget.WidgetLink;
import it.cilea.osd.jdyna.widget.WidgetTesto;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.jdyna.DecoratorProjectPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.DecoratorRestrictedField;
import org.dspace.core.ConfigurationManager;

/**
 * Utility class to create xsd validation.
 * 
 * @author Pascarelli Andrea
 * 
 */
public class UtilsXSD
{

    public static final String[] RP_DEFAULT_ELEMENT = new String[] {
            "researchers", "researcher" };

    public static final String[] GRANT_DEFAULT_ELEMENT = new String[] {
            "grants", "grant" };

    public static final String[] OU_DEFAULT_ELEMENT = new String[] {"orgunits", "orgunit"};

    public final String TYPE_STRINGDATE = "stringdate";

    public final String TYPE_STRING = "text";

    public final String TYPE_DATE = "date";

    public final String TYPE_ANYURI = "uri";

    public final String TYPE_NESTED = "nested";

    public final String TYPE_STRUCTURALMETADATA = "structuralmetadata";

    private Writer writer;

    public UtilsXSD(Writer writer)
    {
        this.writer = writer;
    }

    
    /**TODO
     * @param <I>
     * @param metadata
     * @param elements
     * @param attributeStringMainRows
     * @param attributeStringMainRowsRequired
     * @throws IOException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public <I extends IContainable> void createGrantXSD(List<I> metadata,
            String[] elements, String[] attributeStringMainRows, boolean[] attributeStringMainRowsRequired) throws IOException, SecurityException,
            NoSuchFieldException, InstantiationException,
            IllegalAccessException
    {

        String namespace = "grant:";

        if (elements == null)
        {

            elements = GRANT_DEFAULT_ELEMENT;

        }

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        // writer.write("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"unqualified\" attributeFormDefault=\"unqualified\">\n");

        writer.write("<xs:schema xmlns:grant=\"http://www.cilea.it/grant/schemas\" elementFormDefault=\"qualified\" targetNamespace=\"http://www.cilea.it/grant/schemas\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n");

        writer.write("<xs:element name=\"" + elements[0] + "\" type=\""
                + namespace + "GrantsValueList\">\n");
        writer.write("</xs:element>\n");

        writer.write("  <xs:complexType name=\"GrantsValueList\">\n");
        writer.write("      <xs:sequence>\n");
        writer.write("          <xs:element ref=\"" + namespace + elements[1]
                + "\" maxOccurs=\"unbounded\"/>\n");
        writer.write("      </xs:sequence>\n");
        writer.write("  </xs:complexType>\n");

        writer.write("<xs:element name=\"" + elements[1] + "\">\n");
        writer.write("	<xs:complexType>\n");
        writer.write("		<xs:group ref=\"" + namespace
                + "group\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>\n");        
        if (attributeStringMainRows != null)
        {
            int counter = 0;
            for (String attributeMainRow : attributeStringMainRows)
            {
                writer.write("      <xs:attribute name=\""
                        + attributeMainRow
                        + "\" type=\"xs:string\" "
                        + (attributeStringMainRowsRequired[counter] == true ? "use=\"required\""
                                : "") + "/>\n");
                counter++;
            }
        }
        else
        {
            writer.write("		<xs:attribute name=\"rgid\" type=\"xs:string\"/>\n");
            writer.write("		<xs:attribute name=\"code\" type=\"xs:string\" use=\"required\"/>\n");
        }
        writer.write("	</xs:complexType>\n");
        writer.write("</xs:element>\n");

        writer.write("<xs:group name=\"group\">\n");
        writer.write("	<xs:choice>\n");

        for (I containable : metadata)
        {
            if (containable instanceof DecoratorProjectPropertiesDefinition)
            {
                this.createRefElement(
                        (DecoratorProjectPropertiesDefinition) containable,
                        namespace);
            }
        }
        
        writer.write("	</xs:choice>\n");
        writer.write("</xs:group>\n");

        //TODO add nested manage

        writer.write("<xs:complexType name=\"" + TYPE_STRUCTURALMETADATA
                + "\">\n");
        writer.write("	<xs:simpleContent>\n");
        writer.write("		<xs:extension base=\"xs:string\">\n");
        writer.write("			<xs:attribute name=\"visibility\" type=\"" + namespace
                + "visibility\" />\n");
        writer.write("			<xs:attribute name=\"mime\" type=\"xs:string\" />\n");
        writer.write("			<xs:attribute name=\"remotesrc\" type=\"xs:anyURI\" />\n");
        writer.write("		</xs:extension>\n");
        writer.write("	</xs:simpleContent>\n");
        writer.write("</xs:complexType>\n");

        writer.write("<xs:complexType name=\"" + TYPE_STRING + "\">\n");
        writer.write("	<xs:simpleContent>\n");
        writer.write("		<xs:extension base=\"xs:string\">\n");
        writer.write("			<xs:attribute name=\"visibility\" type=\"" + namespace
                + "visibility\" />\n");
        writer.write("		</xs:extension>\n");
        writer.write("	</xs:simpleContent>\n");
        writer.write("</xs:complexType>\n");

        writer.write("<xs:simpleType name=\"" + TYPE_STRINGDATE + "\">\n");
        writer.write("		<xs:restriction base=\"xs:string\">\n");
        writer.write("			<xs:maxLength value=\"10\"></xs:maxLength>\n");
        writer.write("			<xs:pattern value=\"((0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-(19|20)\\d\\d)?\"/>\n");
        writer.write("		</xs:restriction>\n");
        writer.write("</xs:simpleType>\n");

        writer.write("<xs:complexType name=\"" + TYPE_DATE + "\">\n");
        writer.write("	<xs:simpleContent>\n");
        writer.write("		<xs:extension base=\"" + namespace + TYPE_STRINGDATE
                + "\">\n");
        writer.write("			<xs:attribute name=\"visibility\" type=\"" + namespace
                + "visibility\"/>\n");
        writer.write("		</xs:extension>\n");
        writer.write("	</xs:simpleContent>\n");
        writer.write("</xs:complexType>\n");

        writer.write("<xs:complexType name=\"" + TYPE_ANYURI + "\">\n");
        writer.write("	<xs:simpleContent>\n");
        writer.write("		<xs:extension base=\"xs:string\">\n");
        writer.write("			<xs:attribute name=\"src\" type=\"xs:anyURI\"/>\n");
        writer.write("			<xs:attribute name=\"visibility\" type=\"" + namespace
                + "visibility\"/>\n");
        writer.write("		</xs:extension>\n");
        writer.write("	</xs:simpleContent>\n");
        writer.write("</xs:complexType>\n");


        writer.write("<xs:simpleType name=\"visibility\">\n");
        writer.write("	<xs:restriction base=\"xs:integer\">\n");
        writer.write("		<xs:enumeration value=\"0\"/>\n");
        writer.write("		<xs:enumeration value=\"1\"/>\n");
        writer.write("	</xs:restriction>\n");
        writer.write("</xs:simpleType>\n");

        writer.write("</xs:schema>");

        writer.flush();
        writer.close();

    }

    @Deprecated
    public <I extends IContainable> void createXSD(List<I> metadata,
            String[] elements) throws IOException, SecurityException,
            NoSuchFieldException, InstantiationException,
            IllegalAccessException
    {
        createXSD(metadata, elements, "rp:", null, null, new String[] { "rpid",
                "staffNo" }, new boolean[] { false, true });
    }

    
    /**
     * TODO 
     * 
     * @param <I>
     * @param metadata
     * @param elements
     * @param namespace
     * @param namespaceValue
     * @param targetNamespace
     * @param attributeStringMainRows
     * @param attributeStringMainRowsRequired
     * @throws IOException
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public <I extends IContainable> void createXSD(List<I> metadata,
            String[] elements, String namespace, String namespaceValue,
            String targetNamespace, String[] attributeStringMainRows,
            boolean[] attributeStringMainRowsRequired) throws IOException,
            SecurityException, NoSuchFieldException, InstantiationException,
            IllegalAccessException
    {
        String namespaceDeclaration = namespace.substring(0,
                namespace.length() - 1);
        if (elements == null)
        {
            elements = RP_DEFAULT_ELEMENT;
        }

        if (namespaceValue == null || targetNamespace == null)
        {
            namespaceValue = "http://www.cilea.it/researcherpage/schemas";
            targetNamespace = "http://www.cilea.it/researcherpage/schemas";
        }

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        // writer.write("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"unqualified\" attributeFormDefault=\"unqualified\">\n");

        writer.write("<xs:schema xmlns:" + namespaceDeclaration + "=\""
                + namespaceValue
                + "\" elementFormDefault=\"qualified\" targetNamespace=\""
                + targetNamespace
                + "\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n");

        writer.write("<xs:element name=\"" + elements[0] + "\" type=\""
                + namespace + "ResearchersValueList\">\n");
        writer.write("</xs:element>\n");

        writer.write("    <xs:complexType name=\"ResearchersValueList\">\n");
        writer.write("      <xs:sequence>\n");
        writer.write("          <xs:element ref=\"" + namespace + elements[1]
                + "\" maxOccurs=\"unbounded\"/>\n");
        writer.write("      </xs:sequence>\n");
        writer.write("  </xs:complexType>\n");

        writer.write("<xs:element name=\"" + elements[1] + "\">\n");
        writer.write("	<xs:complexType>\n");
        writer.write("		<xs:group ref=\"" + namespace
                + "group\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>\n");
        int counter = 0;
        for (String attributeMainRow : attributeStringMainRows)
        {
            writer.write("		<xs:attribute name=\""
                    + attributeMainRow
                    + "\" type=\"xs:string\" "
                    + (attributeStringMainRowsRequired[counter] == true ? "use=\"required\""
                            : "") + "/>\n");
            counter++;
        }
        writer.write("	</xs:complexType>\n");
        writer.write("</xs:element>\n");

        writer.write("<xs:group name=\"group\">\n");
        writer.write("	<xs:choice>\n");

        for (I containable : metadata)
        {
            if (containable instanceof ADecoratorPropertiesDefinition)
            {
                this.createRefElement(
                        (ADecoratorPropertiesDefinition) containable,
                        namespace);
            }
            if (containable instanceof DecoratorRestrictedField)
            {
                this.createRefElement((DecoratorRestrictedField) containable,
                        namespace);
            }
        }

        writer.write("	</xs:choice>\n");
        writer.write("</xs:group>\n");

        //TODO add nested manage

        writer.write("<xs:complexType name=\"" + TYPE_STRUCTURALMETADATA
                + "\">\n");
        writer.write("	<xs:simpleContent>\n");
        writer.write("		<xs:extension base=\"xs:string\">\n");
        writer.write("			<xs:attribute name=\"visibility\" type=\"" + namespace
                + "visibility\" />\n");
        writer.write("			<xs:attribute name=\"mime\" type=\"xs:string\" />\n");
        writer.write("			<xs:attribute name=\"remotesrc\" type=\"xs:anyURI\" />\n");
        writer.write("		</xs:extension>\n");
        writer.write("	</xs:simpleContent>\n");
        writer.write("</xs:complexType>\n");

        writer.write("<xs:complexType name=\"" + TYPE_STRING + "\">\n");
        writer.write("	<xs:simpleContent>\n");
        writer.write("		<xs:extension base=\"xs:string\">\n");
        writer.write("			<xs:attribute name=\"visibility\" type=\"" + namespace
                + "visibility\" />\n");
        writer.write("		</xs:extension>\n");
        writer.write("	</xs:simpleContent>\n");
        writer.write("</xs:complexType>\n");

        writer.write("<xs:simpleType name=\"" + TYPE_STRINGDATE + "\">\n");
        writer.write("		<xs:restriction base=\"xs:string\">\n");
        writer.write("			<xs:maxLength value=\"10\"></xs:maxLength>\n");
        writer.write("			<xs:pattern value=\"((0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-(19|20)\\d\\d)?|((19|20)[0-9][0-9])-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])\"/>\n");
        writer.write("		</xs:restriction>\n");
        writer.write("</xs:simpleType>\n");

        writer.write("<xs:complexType name=\"" + TYPE_DATE + "\">\n");
        writer.write("	<xs:simpleContent>\n");
        writer.write("		<xs:extension base=\"" + namespace + TYPE_STRINGDATE
                + "\">\n");
        writer.write("			<xs:attribute name=\"visibility\" type=\"" + namespace
                + "visibility\"/>\n");
        writer.write("		</xs:extension>\n");
        writer.write("	</xs:simpleContent>\n");
        writer.write("</xs:complexType>\n");

        writer.write("<xs:complexType name=\"" + TYPE_ANYURI + "\">\n");
        writer.write("	<xs:simpleContent>\n");
        writer.write("		<xs:extension base=\"xs:string\">\n");
        writer.write("			<xs:attribute name=\"src\" type=\"xs:anyURI\"/>\n");
        writer.write("			<xs:attribute name=\"visibility\" type=\"" + namespace
                + "visibility\"/>\n");
        writer.write("		</xs:extension>\n");
        writer.write("	</xs:simpleContent>\n");
        writer.write("</xs:complexType>\n");

        writer.write("<xs:simpleType name=\"visibility\">\n");
        writer.write("<xs:restriction base=\"xs:integer\">\n");
        writer.write("<xs:enumeration value=\"0\"/>\n");
        writer.write("<xs:enumeration value=\"1\"/>\n");
        writer.write("</xs:restriction>\n");
        writer.write("</xs:simpleType>\n");

        writer.write("</xs:schema>");

        writer.flush();
        writer.close();

    }



    private <TP extends PropertiesDefinition> void createComplexType(
            String name, List<IContainable> elements, String namespace)
            throws IOException
    {
        writer.write("<xs:complexType name=\"" + name + TYPE_NESTED + "\">\n");
        writer.write("		<xs:group ref=\"" + namespace + "group" + name
                + "\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>\n");
        writer.write("</xs:complexType>\n");

        writer.write("<xs:group name=\"group" + name + "\">\n");
        writer.write("	<xs:choice>\n");
        for (IContainable containable : elements)
        {
            createSimpleType((TP) containable.getObject(), namespace);
        }
        writer.write("	</xs:choice>\n");
        writer.write("</xs:group>\n");
    }

    private <TP extends PropertiesDefinition> void createSimpleType(TP tp,
            String namespace) throws IOException
    {
        createRefElement(tp, tp.getRendering(), namespace);
    }

    // method to create type element
    private <TP extends PropertiesDefinition> void createRefElement(ADecoratorPropertiesDefinition<TP> decorator,
            String namespace) throws IOException
    {
        createRefElement(decorator.getReal(), decorator.getRendering(),
                namespace);
    }

    // method to create type element
    private void createRefElement(DecoratorProjectPropertiesDefinition decorator,
            String namespace) throws IOException
    {
        createRefElement(decorator.getReal(), decorator.getRendering(),
                namespace);
    }

    private void createRefElement(DecoratorRestrictedField decorator,
            String namespace) throws IOException, SecurityException,
            NoSuchFieldException
    {
        createRefSimpleElement(decorator.getReal(), TYPE_STRUCTURALMETADATA,
                !decorator.isMandatory(), decorator.getRepeatable(), namespace);
    }

    private <TP extends PropertiesDefinition, A extends AWidget> void createRefElement(
            TP tp, A rendering, String namespace) throws IOException
    {
        if (rendering instanceof WidgetLink)
        {
            createRefElement(tp, (WidgetLink) rendering, namespace);
        }
        else if (rendering instanceof WidgetTesto)
        {
            createRefElement(tp, (WidgetTesto) rendering, namespace);
        }
        else if (rendering instanceof WidgetDate)
        {
            createRefElement(tp, (WidgetDate) rendering, namespace);
        }     
        else
        {
            createRefSimpleElement(tp.getShortName(), TYPE_STRING,
                    !tp.isMandatory(), tp.isRepeatable(), namespace);
        }
    }

    private <TP extends PropertiesDefinition> void createRefElement(TP tp,
            WidgetLink rendering, String namespace) throws IOException
    {
        createRefSimpleElement(tp.getShortName(), TYPE_ANYURI,
                !tp.isMandatory(), tp.isRepeatable(), namespace);
    }

    private <TP extends PropertiesDefinition> void createRefElement(TP tp,
            WidgetTesto rendering, String namespace) throws IOException
    {
        createRefSimpleElement(tp.getShortName(), TYPE_STRING,
                !tp.isMandatory(), tp.isRepeatable(), namespace);
    }
  
    private <TP extends PropertiesDefinition> void createRefElement(TP tp,
            WidgetDate rendering, String namespace) throws IOException
    {
        createRefSimpleElement(tp.getShortName(), TYPE_DATE, !tp.isMandatory(),
                tp.isRepeatable(), namespace);
    }

    private void createRefSimpleElement(String shortname, String type,
            boolean nullable, boolean repeatable, String namespace)
            throws IOException
    {

        writer.write("		<xs:element name=\"" + shortname + "\" type=\""
                + namespace + type + "\" nillable=\"" + nullable + "\""
                + (nullable == false ? " minOccurs=\"1\"" : "")
                + (repeatable == true ? " maxOccurs=\"unbounded\"" : "")
                + "/>\n");

    }

    private <TP extends PropertiesDefinition> void createRefComplexElement(
            String shortname, String type, boolean nullable,
            boolean repeatable, String namespace) throws IOException
    {
        writer.write("		<xs:element name=\"" + shortname + "\" type=\""
                + namespace + shortname + type + "\" nillable=\"" + nullable
                + "\"" + (repeatable == true ? " maxOccurs=\"unbounded\"" : "")
                + "/>\n");
    }

    // method to create element
    private void createElement(DecoratorRPPropertiesDefinition decorator)
            throws IOException
    {
        createElement(decorator.getReal(), decorator.getRendering());
    }

    private void createElement(DecoratorRestrictedField decorator)
            throws IOException, SecurityException, NoSuchFieldException
    {
        String fieldsNullable = ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE, "researcherpage.containables.isnotnullable");
        boolean nullable = fieldsNullable.contains(decorator.getReal());
        createSimpleElement(decorator.getReal(), TYPE_STRING, nullable);
    }

    private <TP extends PropertiesDefinition, A extends AWidget> void createElement(
            TP tp, A rendering) throws IOException
    {
        if (rendering instanceof WidgetLink)
        {
            createElement(tp, (WidgetLink) rendering);
        }
        else if (rendering instanceof WidgetTesto)
        {
            createElement(tp, (WidgetTesto) rendering);
        }
        else if (rendering instanceof WidgetDate)
        {
            createElement(tp, (WidgetDate) rendering);
        }     
        else
        {
            createElement(tp, rendering);
        }
    }

    private <TP extends PropertiesDefinition> void createElement(TP tp,
            WidgetLink rendering) throws IOException
    {
        createSimpleElement(tp.getShortName(), TYPE_ANYURI, tp.isMandatory());
    }

    private <TP extends PropertiesDefinition> void createElement(TP tp,
            WidgetTesto rendering) throws IOException
    {
        createSimpleElement(tp.getShortName(), TYPE_STRING, tp.isMandatory());
    }

    private <TP extends PropertiesDefinition> void createElement(TP tp,
            WidgetDate rendering) throws IOException
    {
        createSimpleElement(tp.getShortName(), TYPE_DATE, tp.isMandatory());
    }

    private <TP extends PropertiesDefinition> void createSimpleElement(TP tp)
            throws IOException
    {
        if (tp.getRendering() instanceof WidgetTesto)
        {
            createSimpleElement(tp.getShortName(), TYPE_STRING,
                    tp.isMandatory());
        }
        if (tp.getRendering() instanceof WidgetDate)
        {
            createSimpleElement(tp.getShortName(), TYPE_DATE, tp.isMandatory());
        }
    }

    private void createSimpleElement(String shortname, String type,
            Boolean nullable) throws IOException
    {

        writer.write("	<xs:element name=\"" + shortname + "\" type=\"" + type
                + "\" nillable=\"" + nullable + "\"/>\n");

    }

    private <TP extends PropertiesDefinition> void createComplexElement(
            String name, List<IContainable> elements) throws IOException
    {
        writer.write("<xs:element name=\"" + name + "\">\n");
        writer.write("	<xs:complexType>\n");
        writer.write("		<xs:sequence>\n");

        for (IContainable containable : elements)
        {
            createSimpleElement((TP) containable.getObject());
        }

        writer.write("		</xs:sequence>\n");
        writer.write("	</xs:complexType>\n");
        writer.write("</xs:element>\n");

    }

    public Writer getWriter()
    {
        return writer;
    }

    public void setWriter(Writer writer)
    {
        this.writer = writer;
    }

    public <I extends IContainable, TP extends PropertiesDefinition> void createSimpleTypeFor(
            List<I> metadata, String namespace, String fullNamespace,
            String nameSimpleType) throws IOException, SecurityException,
            NoSuchFieldException
    {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<xs:schema xmlns:"
                + namespace.subSequence(0, namespace.length() - 1) + "=\""
                + fullNamespace
                + "\" elementFormDefault=\"qualified\" targetNamespace=\""
                + fullNamespace
                + "\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n");
        writer.write("  <xs:simpleType name=\"" + nameSimpleType + "\">\n");
        writer.write("      <xs:restriction base=\"xs:string\">\n");
        for (I containable : metadata)
        {
            writer.write("<xs:enumeration value=\""
                    + containable.getShortName() + "\" />\n");
        }
        writer.write("      </xs:restriction>\n");
        writer.write("  </xs:simpleType>\n");
        writer.write("</xs:schema>");

        writer.flush();
        writer.close();
    }
}
