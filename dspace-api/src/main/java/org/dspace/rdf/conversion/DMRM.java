/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * 
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.conversion;
 
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Schema for DSpace Metadata RDF Mappings.
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 * @see <a href="http://digital-repositories.org/ontologies/dspace-metadata-mapping/0.2.0">http://digital-repositories.org/ontologies/dspace-metadata-mapping/0.2.0</a>
 */
public class DMRM {
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://digital-repositories.org/ontologies/dspace-metadata-mapping/0.2.0#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @return Namespace URI
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );

    /** <p>Represents the mapping of a DSpace metadata value to an RDF equivalent.</p> */
    public static final Resource DSpaceMetadataRDFMapping = m_model.createResource(NS + "DSpaceMetadataRDFMapping");
    
    /** <p>A reified statement that describes the result of the DSpaceMetadataRDFMapping.</p> */
    public static final Resource Result = m_model.createResource(NS + "Result");
    
    /** <p>Processes a metadata value into an RDF value or an IRI.</p> */
    public static final Resource ValueProcessor = m_model.createResource(NS + "ValueProcessor");
    
    /** <p>A regular expression to be used with java, composed of a matching and a replaying expression.</p> */
    public static final Resource ValueModifier = m_model.createResource( NS + "ValueModifier" );
    
    /** <p>Generates a literal depending on a DSpace metadata value.</p> */
    public static final Resource LiteralGenerator = m_model.createResource(NS + "LiteralGenerator");
    
    /** <p>Generates an IRI used for a rdfs:Resource depending on the converted DSpace Object and one of its metadata values.</p> */
    public static final Resource ResourceGenerator = m_model.createResource(NS + "ResourceGenerator");
    
    /** <p>Placeholder for the IRI of the DSpace Object that gets converted.</p> */
    public static final Resource DSpaceObjectIRI = m_model.createResource( NS + "DSpaceObjectIRI" );
    
    /** <p>Shortcut to generate a Literal containing an unchanged metadata value.</p> */
    public static final Resource DSpaceValue = m_model.createResource(NS + "DSpaceValue");
    
    /** <p>Specifies the RDF to generate for a specified matadata.</p> */
    public static final Property creates = m_model.createProperty( NS + "creates" );
    
    /** <p>The subject of a DSpace metadata RDF mapping result.</p> */
    public static final Property subject = m_model.createProperty( NS + "subject" );
    
    /** <p>The predicate of a DSpace metadata RDF mapping result.</p> */
    public static final Property predicate = m_model.createProperty( NS + "predicate" );
    
    /** <p>The object of a DSpace metadata RDF mapping result.</p> */
    public static final Property object = m_model.createProperty( NS + "object" );
    
    /** <p>The name of the metadata to convert (e.g. dc.title).</p> */
    public static final Property metadataName = m_model.createProperty( NS + "metadataName" );
    
    /** <p>A regex that the metadata value has to fulfill if the mapping should become active.</p> */
    public static final Property condition = m_model.createProperty( NS + "condition" );
    
    /** <p>Information how the metadata value should be modified before it is inserted in the pattern.</p> */
    public static final Property modifier = m_model.createProperty( NS + "modifier" );
    
    /** <p>A regex that matches those subsequences of a metadata value, that should be replaced.</p> */
    public static final Property matcher = m_model.createProperty( NS + "matcher" );
    
    /** <p>A regex that replaces previously matched subsequences of a metadata value.</p> */
    public static final Property replacement = m_model.createProperty( NS + "replacement" );
    
    /** <p>A pattern that contains $DSpaceValue as placeholder for the metadata value.</p> */
    public static final Property pattern = m_model.createProperty( NS + "pattern" );
    
    /** <p>Defines the datatype a generated literal gets.</p> */
    public static final Property literalType = m_model.createProperty( NS + "literalType" );
    
    /** <p>Defines the language a literal uses. Maybe overridden by #dspaceLanguageTag.</p> */
    public static final Property literalLanguage = m_model.createProperty( NS + "literalLanguage" );
    
    /** <p>Defines to use the language tag of a DSpace metadata value.</p> */
    public static final Property dspaceLanguageTag = m_model.createProperty( NS + "dspaceLanguageTag");
}
