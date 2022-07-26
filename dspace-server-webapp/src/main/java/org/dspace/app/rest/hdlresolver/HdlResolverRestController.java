/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.hdlresolver;

import java.text.MessageFormat;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is public and is useful for handle resolving,
 * wheter a target handle identifier will be resolved into the
 * corresponding URL (if found), otherwise will respond a null string.
 * 
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.it)
 *
 */
@RestController
@RequestMapping(path = "/{hdlService:hdlResolver|resolve|listhandles|listprefixes}/")
public class HdlResolverRestController {
    static final String HDL_RESOLVER = "/hdlResolver/";
    static final String RESOLVE = "/resolve/";
    static final String LISTHANDLES = "/listhandles/";
    static final String LISTPREFIXES = "/listprefixes/";

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private HdlResolverService hdlResolverService;

    /**
     * REST GET Method used to find and retrieve the URL of a target Handle. It
     * should return only one item, if found, else a null body value.
     * 
     * Generate an <code>HttpStatus.BAD_REQUEST</code> 400 Error if the handle used
     * in path isn't valid.
     * 
     * The response type will be the same that is in the /xmlui/handleresolver/resolve
     * (<code>application/json;charset=UTF-8</code>)
     * 
     * @param request HttpServletRequest
     * @return One element List or <code>null</code> with status 200
     *         <code>HttpStatus.OK</code> or <code>HttpStatus.BAD_REQUEST</code> 400
     *         error
     */
    @GetMapping(
        value = "**",
        produces = "application/json;charset=UTF-8"
    )
    public ResponseEntity<String> getHandleUrlResolver(HttpServletRequest request, @PathVariable String hdlService) {
        HdlResolverDTO handleResolver =
               this.hdlResolverService.resolveBy(
                       request.getRequestURI(),
                       MessageFormat.format("{0}/{1}/", request.getContextPath(), hdlService)
               );
        if (!handleResolver.isValid()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(this.resolveToURL(request, handleResolver), HttpStatus.OK);
        }
    }

    /**
     * Maps the handle to a correct response.
     * 
     * @param request        HttpServletRequest
     * @param handleResolver HdlResolverDTO - Handle resolver
     * @return One element list using String if found, else null String.
     */
    private String resolveToURL(HttpServletRequest request, HdlResolverDTO handleResolver) {
        String json = "null";
        final String resolvedUrl = this.hdlResolverService.resolveToURL(ContextUtil.obtainContext(request),
                handleResolver);
        if (StringUtils.isNotEmpty(resolvedUrl)) {
            try {
                json = new ObjectMapper().writeValueAsString(List.of(resolvedUrl));
            } catch (JsonProcessingException e) {
                log.error("Error during conversion of response!", e);
            }
        }
        return json;
    }

}
