/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif;

import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller for IIIF Presentation and Search API.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RestController
@RequestMapping("/iiif")
// Only enable this controller if "iiif.enabled=true"
@ConditionalOnProperty("iiif.enabled")
public class IIIFController {

    @Autowired
    IIIFServiceFacade iiifFacade;

    /**
     * The manifest response contains sufficient information for the client to initialize
     * itself and begin to display something quickly to the user. The manifest resource
     * represents a single object and any intellectual work or works embodied within that
     * object. In particular it includes the descriptive, rights and linking information
     * for the object. It then embeds the sequence(s) of canvases that should be rendered
     * to the user.
     *
     * Called with GET to retrieve the manifest for a single DSpace item.
     *
     * @param id DSpace Item uuid
     * @return manifest as JSON
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{id}/manifest")
    public String findOne(@PathVariable UUID id) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return iiifFacade.getManifest(context, id);
    }

    /**
     * Any resource in the Presentation API may have a search service associated with it.
     * The resource determines the scope of the content that will be searched. A service
     * associated with a manifest will search all of the annotations on canvases or other
     * objects below the manifest, a service associated with a particular range will only
     * search the canvases within the range, or a service on a canvas will search only
     * annotations on that particular canvas. The URIs for services associated with different
     * resources must be different to allow the client to use the correct one for the desired
     * scope of the search.
     *
     * This endpoint for searches within the manifest scope (by DSpace item uuid).
     *
     * @param id DSpace Item uuid
     * @param query query terms
     * @return AnnotationList as JSON
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{id}/manifest/search")
    public String searchInManifest(@PathVariable UUID id,
                                   @RequestParam(name = "q") String query) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return iiifFacade.searchInManifest(context, id, query);
    }

    /**
     * All resources can link to semantic descriptions of themselves via the seeAlso property.
     * These could be METS, ALTO, full text, or a schema.org descriptions.
     *
     * Since there's currently no reliable way to associate "seeAlso" links and individual
     * canvases (e.g. associate a single image with its ALTO file) the
     * scope is the entire manifest (or DSpace Item).
     *
     * @param id DSpace Item uuid
     * @return AnnotationList as JSON
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{id}/manifest/seeAlso")
    public String findSeeAlsoList(@PathVariable UUID id) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return iiifFacade.getSeeAlsoAnnotations(context, id);
    }

    /**
     * The canvas represents an individual page or view and acts as a central point for
     * laying out the different content resources that make up the display. This information
     * should be embedded within a sequence.
     *
     * This endpoint allows canvases to be dereferenced separately from the manifest. This
     * is an atypical use case.
     *
     * @param id DSpace Item uuid
     * @param cid canvas identifier
     * @return canvas as JSON
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{id}/canvas/{cid}")
    public String findCanvas(@PathVariable UUID id, @PathVariable String cid) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return iiifFacade.getCanvas(context, id, cid);
    }
}
