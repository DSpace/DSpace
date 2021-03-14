/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.repository.RelationshipRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This will be the entry point for the api/core/relationships endpoint with additional paths to it
 */
@RestController
@RequestMapping("/api/core/relationships")
public class RelationshipRestController {

    @Autowired
    private RelationshipRestRepository relationshipRestRepository;

    @Autowired
    Utils utils;

    /**
     * Method to change the left item of a relationship with a given item in the body
     * @return The modified relationship
     */
    @RequestMapping(method = RequestMethod.PUT, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT + "/leftItem",
            consumes = {"text/uri-list"})
    public RelationshipRest updateRelationshipLeft(@PathVariable Integer id, HttpServletResponse response,
                                                   HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        return relationshipRestRepository.put(context, "/api/core/relationships/", id,
                                              utils.getStringListFromRequest(request), false);
    }

    /**
     * Method to change the right item of a relationship with a given item in the body
     * @return The modified relationship
     */
    @RequestMapping(method = RequestMethod.PUT, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT + "/rightItem",
            consumes = {"text/uri-list"})
    public RelationshipRest updateRelationshipRight(@PathVariable Integer id, HttpServletResponse response,
                                                    HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        return relationshipRestRepository.put(context, "/api/core/relationships/", id,
                                              utils.getStringListFromRequest(request), true);
    }
}