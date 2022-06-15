/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.hdlresolver;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.utils.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.it)
 *
 */
@RestController
@RequestMapping(HdlResolverRestController.BASE_PATH)
public class HdlResolverRestController {
    static final String BASE_PATH = "/hdlResolver/";

    @Autowired
    private HdlResolverService hdlResolverService;

    /**
     * REST Get Method used to find and retrieve the URL of a target Handle. It
     * should return only one item, if found, else a null body value.
     * 
     * Generate an <code>HttpStatus.BAD_REQUEST</code> 400 Error if the handle used
     * in path isn't valid.
     * 
     * @param request HttpServletRequest
     * @return One element List or <code>null</code> with status 200
     *         <code>HttpStatus.OK</code> or <code>HttpStatus.BAD_REQUEST</code> 400
     *         error
     */
    @GetMapping("**")
    public ResponseEntity<String> getHandleUrlResolver(HttpServletRequest request) {
        HdlResolverDTO handleResolver = this.hdlResolverService.resolveBy(request.getRequestURI(),
                request.getContextPath() + HdlResolverRestController.BASE_PATH);
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
        final String resolvedUrl = this.hdlResolverService.resolveToURL(ContextUtil.obtainContext(request),
                handleResolver);
        if (StringUtils.isNotEmpty(resolvedUrl)) {
            return new Gson().toJson(List.of(resolvedUrl));
        } else {
            return "null";
        }
    }

}
