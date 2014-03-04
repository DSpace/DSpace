/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * 
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.conversion;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class MetadataRDFMapping {
    
    private static final Logger log = Logger.getLogger(MetadataRDFMapping.class);
    
    protected final String name;
    protected final Pattern fulfills;
    protected final List<Resource> results;
    
    protected MetadataRDFMapping(String name, Pattern fulfills, List<Resource> results)
    {
        this.name = name;
        this.fulfills = fulfills;
        this.results = results;
    }
            
    
    public static MetadataRDFMapping getMetadataRDFMapping(
            Resource mappingResource, String dsoIdentifier)
    {
        // For better log message: try to get the uri of this mapping.
        String uri = null;
        if (mappingResource.getURI() != null)
        {
            uri = " (" + mappingResource.getURI() + ")";
        }
        
        if (log.isDebugEnabled())
        {
            if (uri.equals(""))
            {
                log.debug("Processing blank node MetadataRDFMapping.");
            }
            else
            {
                log.debug("Processing MetadataRDFMapping" + uri + ".");
            }
        }
        
        // Parse the property DMRM.metadataName
        RDFNode nameNode;
        try
        {
            nameNode = getSingularProperty(mappingResource, DMRM.metadataName);
        }
        catch (IllegalArgumentException ex)
        {
            log.error("The Property 'metadataName' exists multiple times in one "
                    + "DSpaceMetadataRDFMapping, ignoring it" + uri + ".");
            return null;
        }

        if (nameNode == null)
        {
            log.error("Cannot find property 'metadataName', ignoring mapping" + uri + ".");
            return null;
        }
        if (!nameNode.isLiteral())
        {
            log.error("Property 'metadataName' is not a literal, ignoring mapping" 
                    + uri + ".");
            return null;
        }
        String name = nameNode.asLiteral().getLexicalForm();
        log.debug("Found mapping name '" + name + "'.");
        
        // Parse the property condition, if it exists. 
        RDFNode conditionNode;
        try
        {
            conditionNode = getSingularProperty(mappingResource, DMRM.condition);
        }
        catch (IllegalArgumentException ex)
        {
            log.error("There are multiple properties 'condition' in one "
                    + "DSpaceMetadataRDFMapping, ignoring it" + uri + ".");
            return null;
        }
        String regex = null;
        Pattern condition = null;
        if (conditionNode != null)
        {
            if (conditionNode.isLiteral())
            {
                regex = conditionNode.asLiteral().getLexicalForm();
                log.debug("Found property condition '" + regex + "'.");
            } else {
                log.error("Property 'condition' is not a literal, ignoring "
                        + "mapping" + uri + ".");
                return null;
            }
        } else {
            // there is no property "condition". As this property is optional 
            // there is nothing to be done here.
            log.debug("Didn't find a property \"condition\".");
        }
        if (regex != null)
        {
            try
            {
                condition = Pattern.compile(regex);
            }
            catch (PatternSyntaxException ex)
            {
                log.error("Property 'condition' does not specify a valid java "
                        + "regex pattern. Will ignore mapping" + uri + ".", ex);
                return null;
            }
        }
        
        // parse all properties DMRM.creates.
        List<Resource> results = new ArrayList<>();
        StmtIterator mappingIter = mappingResource.listProperties(DMRM.creates);
        if (!mappingIter.hasNext())
        {
            log.warn("No 'creates' property in a DSpaceMetadataRDFMapping, "
                    + "ignonring it" + uri + ".");
            return null;
        }
        while (mappingIter.hasNext())
        {
            RDFNode result = mappingIter.nextStatement().getObject();
            if (!result.isResource())
            {
                log.error("Mapping result" + uri + " is a Literal not a resource. "
                        + "Ignoring mapping.");
                return null;
            }
            results.add(result.asResource());
        }
        
        // create mapping
        return new MetadataRDFMapping(name, condition, results);
    }
    
    public boolean matchesName(String name)
    {
        return StringUtils.equalsIgnoreCase(this.name, name);
    }
    
    public boolean fulfills(String value)
    {
        // if fulfills exists, we have to check the field value
        if (this.fulfills == null)
        {
            return true;
        }
        
        if (!this.fulfills.matcher(value).matches())
        {
            log.debug("Value '" + value + "' does not match regex '" + fulfills.toString() + "'.");
            return false;
        } else {
            return true;
        }
        
        //return this.fulfills.matcher(value).matches();
    }
    
    public void convert(String value, String lang, String dsoIRI, Model m)
    {
        log.debug("Using convertion for field " + name + " on value: " + value 
                + " for " + dsoIRI + ".");
        // run over all results
        for (Iterator<Resource> iter = this.results.iterator() ; iter.hasNext() ; )
        {
            try {
                compileResult(m, iter.next(), dsoIRI, name, value, lang);
            } catch (MetadataMappingException ex) {
                log.error(ex.getMessage() + " Will ignore this mapping result.");
            }
        }
    }

    protected void compileResult(Model m, Resource result, 
            String dsoIRI, String name, String value, String lang) throws MetadataMappingException
    {
        // for better debug messages.
        String uri = "";
        if (result.isURIResource()) uri = " (" + result.getURI() + ")";
        
        // check the subject
        RDFNode subjectNode;
        try
        {
            subjectNode = getSingularProperty(result, DMRM.subject);
        }
        catch (IllegalArgumentException ex)
        {
            throw new MetadataMappingException("There are multiple 'subject' "
                    + "properties in a mapping result" + uri + ".");
        }
        if (subjectNode == null)
        {
            throw new MetadataMappingException("Mapping result" + uri 
                    + " does not have a subject.");
        }
        if (!subjectNode.isResource())
        {
            throw new MetadataMappingException("Subject of a result" + uri 
                    + " is a Literal not a URIResource.");
        }
        
        log.debug("Found subject: " + subjectNode.toString());
        
        // check the predicate
        RDFNode predicateNode;
        try
        {
            predicateNode = getSingularProperty(result, DMRM.predicate);
        }
        catch (IllegalArgumentException ex)
        {
            throw new MetadataMappingException("There are multiple 'predicate' "
                    + "properties in a mapping result" + uri + ".");
        }
        if (predicateNode == null)
        {
            throw new MetadataMappingException("Mapping result" + uri 
                    + " does not have a predicate.");
        }
        if (!predicateNode.isResource())
        {
            throw new MetadataMappingException("Predicate of a result" + uri 
                    + " is a Literal not a URIResource.");
        }
        log.debug("Found predicate: " + predicateNode.toString());
        
        RDFNode objectNode;
        try
        {
            objectNode = getSingularProperty(result, DMRM.object);
        }
        catch (IllegalArgumentException ex)
        {
            throw new MetadataMappingException("There are multiple 'object' "
                    + "properties in a mapping result" + uri + ".");
        }
        if (objectNode == null)
        {
            throw new MetadataMappingException("Mapping result" + uri 
                    + " does not have a object.");
        }
        log.debug("Found object: " + objectNode.toString());
        
        Resource subject = parseSubject(m, subjectNode.asResource(), 
                dsoIRI, name, value);
        if (subject == null)
        {
            throw new MetadataMappingException("Cannot parse subject of a "
                    + "reified statement " + uri + ".");
        }
        Property predicate = parsePredicate(m, predicateNode.asResource(), 
                dsoIRI, name, value);
        if (predicate == null)
        {
            throw new MetadataMappingException("Cannot parse predicate of a "
                    + "reified statement " + uri + ".");
        }
        RDFNode object = parseObject(m, objectNode, dsoIRI, name, value, lang);
        if (object == null)
        {
            throw new MetadataMappingException("Cannot parse object of a "
                    + "reified statement " + uri + ".");
        }
        
        m.add(subject, predicate, object);
    }
    
    protected Resource parseSubject(Model m, Resource subject, String dsoIRI, 
            String name, String value)
    {
        if (subject.hasProperty(RDF.type, DMRM.ResourceGenerator))
        {
            String generatedIRI = parseResourceGenerator(subject, value, dsoIRI);
            if (generatedIRI == null)
            {
                log.debug("Generated subject IRI is null.");
                return null;
            }
            
            log.debug("Subject ResourceGenerator generated '" + generatedIRI + "'.");
            return m.createResource(generatedIRI);
        }
        
        return subject;
    }

    protected Property parsePredicate(Model m, Resource predicate, String dsoIRI,
            String name, String value)
    {
        if (predicate.hasProperty(RDF.type, DMRM.ResourceGenerator))
        {
            String generatedIRI = parseResourceGenerator(predicate, value, dsoIRI);
            if (generatedIRI == null)
            {
                log.debug("Generated predicate IRI is null.");
                return null;
            }
            
            log.debug("Property ResourceGenerator generated '" + generatedIRI + "'.");
            return m.createProperty(generatedIRI);
        }
        String uri = predicate.getURI();
        if (uri == null)
        {
            log.debug("A result predicate is blank node, but not a "
                    + "ResourceGenerator. Ingoring this result.");
            return null;
        }
        return m.createProperty(uri);
    }

    protected RDFNode parseObject(Model m, RDFNode objectNode, String dsoIRI,
            String name, String value, String lang)
    {
        if (objectNode.isLiteral()) return objectNode;
        
        Resource object = objectNode.asResource();
        
        if (object.hasProperty(RDF.type, DMRM.LiteralGenerator))
        {
            Literal literalValue = parseLiteralGenerator(m, object, value, lang);
            if (literalValue == null) return null;
            return literalValue;
        }
        
        if (object.hasProperty(RDF.type, DMRM.ResourceGenerator))
        {
            String generatedIRI = parseResourceGenerator(object, value, dsoIRI);
            if (generatedIRI == null)
            {
                log.debug("Generated predicate IRI is null.");
                return null;
            }
            
            log.debug("Property ResourceGenerator generated '" + generatedIRI + "'.");
            return m.createProperty(generatedIRI);
        }
        
        if (object.isAnon())
        {
            Resource blank = m.createResource();
            StmtIterator iter = object.listProperties();
            while (iter.hasNext())
            {
                Statement stmt = iter.nextStatement();
                Property predicate = stmt.getPredicate();
                // iterate recursive over the object of a blank node.
                blank.addProperty(predicate,
                        parseObject(m, stmt.getObject(), dsoIRI, name, value, lang));
            }
            return blank;
        }
            
        // object is not a literal, is not a blank node, is neither a 
        // IRIGenerator nor a LiteralGenerator => it must be a Resource => use 
        // it as it is.
        return object;
    }
    
    protected String parseResourceGenerator(Resource resourceGenerator, 
            String value, String dsoIRI)
    {
        if (resourceGenerator.isURIResource() 
                && resourceGenerator.equals(DMRM.DSpaceObjectIRI))
        {
            return dsoIRI;
        }

        return parseValueProcessor(resourceGenerator, value);
    }
    
    protected Literal parseLiteralGenerator(Model m, Resource literalGenerator, 
            String value, String lang)
    {
        if (literalGenerator.isURIResource() 
                && literalGenerator.equals(DMRM.DSpaceValue))
        {
            return m.createLiteral(value);
        }
        
        String modifiedValue = parseValueProcessor(literalGenerator, value);
        if (modifiedValue == null) return null;
        
        // check if we should produce a typed literal
        // Up the RDF spec lang tags are not significant on typed literals, so 
        // we can ignore them if we have a typed literal.
        try
        {
            RDFNode literalTypeNode = getSingularProperty(literalGenerator, DMRM.literalType);
            if (literalTypeNode != null)
            {
                if (literalTypeNode.isURIResource())
                {
                    return m.createTypedLiteral(modifiedValue, 
                            literalTypeNode.asResource().getURI());
                } else {
                    log.warn("A LiteralGenerator has a property 'literalType' that "
                            + "either is a blank node or a Literal. Ignoring it.");
                }
            }
        }
        catch (IllegalArgumentException ex)
        {
            log.error("A LiteralGenerator has multiple properties "
                    + "'literalType'. Will ignore them.");
        }
        
        
        // check if a language tag should be generated
        String languageTag = null;
        try
        {
            RDFNode langNode = getSingularProperty(literalGenerator, DMRM.literalLanguage);
            if (langNode != null)
            {
                if (langNode.isLiteral())
                {
                    languageTag = langNode.asLiteral().getLexicalForm();
                } else {
                    log.warn("Found a property 'literalLanguage', but its "
                            + "object is not a literal! Ignoring it.");
                }
            }
        }
        catch (IllegalArgumentException ex)
        {
            log.warn("A LiteralGenerator has multiple properties "
                    + "'literalLanguage'. Will ignore them.");
        }
        
        try {
            RDFNode dspaceLangNode = getSingularProperty(literalGenerator, 
                    DMRM.dspaceLanguageTag);
            if (dspaceLangNode != null)
            {
                boolean useDSpaceLang = false;
                if (dspaceLangNode.isLiteral())
                {
                    try {
                        useDSpaceLang = dspaceLangNode.asLiteral().getBoolean();
                    }
                    catch (Exception ex)
                    {
                        /*
                         * nothing to do here.
                         *
                         * this is for sure not the best coding style, but the 
                         * one that works best here as jena throws some undeclared 
                         * RuntimeExceptions if the detection of the boolean fails.
                         */
                    }
                }
                if (useDSpaceLang && !StringUtils.isEmpty(lang))
                {
                    if (lang.indexOf("_") == 2)
                    {
                        languageTag = lang.replaceFirst("_", "-");
                    } else {
                        languageTag = lang;
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            log.error("A LiteralGenerator has multiple properties "
                    + "'dspaceLanguageTag'. Will ignore them.");
        }
        
        if (languageTag != null) return m.createLiteral(modifiedValue, languageTag);

        return m.createLiteral(modifiedValue);
    }
    
    protected String parseValueProcessor(Resource valueProcessor, String value)
    {
        // look if there's a modifier.
        RDFNode modifierNode;
        try
        {
            modifierNode = getSingularProperty(valueProcessor, DMRM.modifier);
        }
        catch (IllegalArgumentException ex)
        {
            log.error("The ResourceGenerator of a mapping result has "
                    + "multiple 'modifier' properties, skipping this result.");
            return null;
        }
        if (modifierNode != null)
        {
            // in case there is a modifier find its matcher, its replacement and
            // modifies the value

            if (!modifierNode.isResource())
            {
                log.error("The modifier of a result is a Literal not an Resource! "
                        + "Ingoring this result.");
                return null;
            }
            Resource modifier = modifierNode.asResource();

            RDFNode matcherNode;
            try
            {
                matcherNode = getSingularProperty(modifier, DMRM.matcher);
            }
            catch (IllegalArgumentException ex)
            {
                log.error("The modifier of a mapping result has multiple "
                        + "'matcher' properties. Ignoring this result.");
                return null;
            }
            if (matcherNode == null)
            {
                log.error("Found a modifier property to a result, but no "
                        + "matcher property! Ignoring this result!");
                return null;
            }
            if (!matcherNode.isLiteral())
            {
                log.error("A matcher of a result modifier is not a Literal! "
                        + "Ignoring this result.");
                return null;
            }

            // get the replacement string
            RDFNode replacementNode;
            try
            {
                replacementNode = getSingularProperty(modifier, DMRM.replacement);
            }
            catch (IllegalArgumentException ex)
            {
                log.error("The modifier of a mapping result has multiple "
                        + "'replacement' properties. Ignoring this result.");
                return null;
            }
            if (replacementNode == null)
            {
                log.error("Found a modifier property to a result, but no "
                        + "replacement property! Ignoring this result!");
                return null;
            }
            if (!replacementNode.isLiteral())
            {
                log.error("A replacement of a result modifier is not a Literal! "
                        + "Ignoring this result.");
                return null;
            }

            String matcher = matcherNode.asLiteral().getLexicalForm();
            String replacement = replacementNode.asLiteral().getLexicalForm();
            try
            {
                Pattern pattern = Pattern.compile(matcher);
                String modifiedValue = pattern.matcher(value).replaceAll(replacement);

                log.debug("Found matcher '" + matcher + "'.\n"
                    + "Found replacement '" + replacement + "'.\n"
                    + "modified '" + value + "' => '" + modifiedValue + "'.");

                value = modifiedValue;
            }
            catch (PatternSyntaxException ex)
            {
                log.error("Property 'matcher' of a ValueModifider didn't specify a "
                        + "valid java regex pattern. Will ignore this result.", ex);
                return null;
            }
        }
        
        // in case there is a modifier, we modified the value. Insert the
        // (possibly modified) value in the pattern
        RDFNode patternNode;
        try
        {
            patternNode = getSingularProperty(valueProcessor, DMRM.pattern);
        }
        catch (IllegalArgumentException ex)
        {
            log.error("The ValueProcessor of a mapping result has "
                    + "multiple 'pattern' properties, skipping this result.");
            return null;
        }
        if (patternNode == null)
        {
            log.debug("Cannot find the property 'pattern' of a "
                    + "ValueProcessor, will use \"$DSpaceValue\".");
            patternNode = valueProcessor.getModel().createLiteral("$DSpaceValue");
        }
        if (!patternNode.isLiteral())
        {
            log.error("A 'pattern' property of a ValueProcessor is not a "
                    + "Literal! Skipping this result.");
            return null;
        }
        String pattern = patternNode.asLiteral().getLexicalForm();
        String result = pattern.replace("$DSpaceValue", value);
        log.debug("Found pattern " + pattern + ".\n"
                + "Created result: " + result);
        return result;

    }
    
    protected static RDFNode getSingularProperty(Resource r, Property p)
            throws IllegalArgumentException
    {
        List<Statement> stmts = r.listProperties(p).toList();
        if (stmts.isEmpty())
        {
            return null;
        }
        if (stmts.size() > 1)
        {
            throw new IllegalArgumentException("Property '" + p.getURI()
                    + "' exists multiple times.");
        }
        return stmts.get(0).getObject();
    }
}