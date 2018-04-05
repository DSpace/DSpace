/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.storage;

import com.hp.hpl.jena.rdf.model.Model;
import java.util.List;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public interface RDFStorage {
    /**
     * Don't use this method directly, use 
     * {@link org.dspace.rdf.RDFUtil#convert(org.dspace.core.Context, org.dspace.content.DSpaceObject) RDFizer.convert(...)}
     * to convert and store DSpaceObjets.
     * @param uri Identifier for this DSO 
     * ({@link org.dspace.rdf.RDFUtil#generateIdentifier(org.dspace.core.Context, org.dspace.content.DSpaceObject) RDFizer.generateIdentifier(...)}).
     * You can load this model by using this URI.
     * @param model The model to store.
     * @see org.dspace.rdf.RDFizer#RDFizer
     */
    public void store(String uri, Model model);
    
    /**
     * Don't use this method directly, use
     * {@link org.dspace.rdf.RDFUtil#loadModel(String) RDFizer.loadModel(...)} instead.
     * @param uri
     * @return the model
     */
    public Model load(String uri);
    
    public void delete(String uri);
    
    public void deleteAll();
    
    public List<String> getAllStoredGraphs();
}
