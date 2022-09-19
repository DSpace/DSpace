/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.dspace.handle.external.ExternalHandleConstants.MAGIC_BEAN;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.RandomStringUtils;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.core.Context;
import org.dspace.handle.HandlePlugin;
import org.dspace.handle.external.Handle;
import org.dspace.handle.external.HandleRest;
import org.dspace.handle.service.HandleClarinService;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST Controller which manages the `external/Handle` which has the URL with the `@magicLindat` string
 */
@RestController
@RequestMapping("/api/services")
public class ExternalHandleRestRepository {

    private final String EXTERNAL_HANDLE_ENDPOINT_FIND_ALL = "handles/magic";
    private final String EXTERNAL_HANDLE_ENDPOINT_SHORTEN = "handles";
    private final String EXTERNAL_HANDLE_ENDPOINT_UPDATE = "handles";

    private String prefix;

    @Autowired
    private HandleClarinService handleClarinService;

    @Autowired
    private HandleService handleService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Get all Handles with url which contains the `@magicLindat` string then convert them to the `external/Handle`
     * and return in the List.
     */
    @RequestMapping(value = EXTERNAL_HANDLE_ENDPOINT_FIND_ALL, method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResponseEntity<List<HandleRest>> getHandles(HttpServletResponse response,
                                                       HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);

        // get all external handles - they have `@magicLindat` string in the url
        List<org.dspace.handle.Handle> magicHandles = this.handleClarinService.findAllExternalHandles(context);

        // create the external handles from the handles with magic URL
        List<Handle> externalHandles = this.handleClarinService.convertHandleWithMagicToExternalHandle(magicHandles);

        // convert external Handles to Handle Rest objects
        List<HandleRest> externalHandlesRest =
                this.handleClarinService.convertExternalHandleToHandleRest(externalHandles);

        return new ResponseEntity<>(new ArrayList<>(externalHandlesRest), HttpStatus.OK);
    }

    /**
     * From the Handle url create some `external/Handle` with short /handle/URL
     * @param handle
     * @param response
     * @param request
     * @return
     */
    @RequestMapping(value = EXTERNAL_HANDLE_ENDPOINT_SHORTEN, method = RequestMethod.POST,
            produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResponseEntity<Object> shortenHandle(@RequestBody Handle handle, HttpServletResponse response,
                                                   HttpServletRequest request) {
        if (validateHandle(handle)) {
            try {
                Context context = ContextUtil.obtainContext(request);

                if (Objects.isNull(context)) {
                    return new ResponseEntity<>("Context is null", HttpStatus.INTERNAL_SERVER_ERROR);
                }
                handle.submitdate = new DCDate(new Date()).toString();
                String subprefix = (isNotBlank(handle.subprefix)) ? handle.subprefix + "-" : "";
                String magicURL = handle.getMagicUrl();
                String hdl = createHandle(subprefix, magicURL, context);
                if (Objects.isNull(hdl)) {
                    return new ResponseEntity<>("Cannot create the shortened handle, try it again.",
                            HttpStatus.BAD_REQUEST);
                }
                context.complete();

                return new ResponseEntity<>(new Handle(hdl, magicURL), HttpStatus.OK);
            } catch (SQLException | AuthorizeException e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>("Cannot create handle because some parameter is null or the URL is not valid",
                HttpStatus.BAD_REQUEST);
    }


    // change only the URL
    @RequestMapping(value = EXTERNAL_HANDLE_ENDPOINT_UPDATE, method = RequestMethod.PUT,
            produces = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ResponseEntity<Object> updateHandle(@RequestBody Handle updatedHandle, HttpServletResponse response,
                                                   HttpServletRequest request) {
        Context context = ContextUtil.obtainContext(request);
        if (Objects.isNull(context)) {
            return new ResponseEntity<>("Context is null.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (validHandleUrl(updatedHandle.url)
                && isNotBlank(updatedHandle.getHandle()) && isNotBlank(updatedHandle.token)) {
            try {
                // remove canonical prefix from handle string because the handle is stored in the DB without
                // the canonical prefix
                String canonicalPrefix = HandlePlugin.getCanonicalHandlePrefix();
                String oldHandleStr = Objects.isNull(canonicalPrefix) ? updatedHandle.getHandle() :
                        updatedHandle.getHandle().replace(canonicalPrefix, "");

                // load Handle object from the DB
                org.dspace.handle.Handle oldHandle =
                        this.handleClarinService.findByHandle(context, oldHandleStr);

                if (Objects.isNull(oldHandle)) {
                    return new ResponseEntity<>("Cannot find the handle in the database.",
                            HttpStatus.BAD_REQUEST);
                }

                // create externalHandle based on the handle and the URL with the `@magicLindat` string
                Handle oldExternalHandle = new Handle(oldHandle.getHandle(), oldHandle.getUrl());

                oldExternalHandle.url  = updatedHandle.url;

                // generate new magicURL for the oldHandle
                oldHandle.setUrl(oldExternalHandle.getMagicUrl());

                // update handle in the DB
                this.handleClarinService.save(context, oldHandle);
                context.commit();

                // return updated external handle
                return new ResponseEntity<>(oldExternalHandle, HttpStatus.OK);
            } catch (SQLException | AuthorizeException e) {
                return new ResponseEntity<>("Cannot update the external handle because: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>("Cannot update the external handle.",
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Create new handle string with the size of `4` characters
     *
     * @param subprefix of the handle `<PREFIX>/<SUBPREFIX>-handle` e.g. `123/PMLTQ-54785632
     * @param url URL with `@magicLindat` string
     * @param context DSpace object context
     * @return shortened Handle string e.g. `5478`
     */
    private String createHandle(String subprefix, String url, Context context) throws SQLException, AuthorizeException {
        String handle;
        this.loadPrefix();

        // generate short handle
        String rnd = RandomStringUtils.random(4,true,true).toUpperCase();
        handle = prefix + "/" + subprefix + rnd;

        try {
            // check if exists such handle - it throws error if exists such handle
            this.handleClarinService.findByHandle(context, handle);
        } catch (PSQLException | PersistenceException e) {
            return null;
        }

        handleClarinService.createExternalHandle(context, handle, url);
        return handle;
    }

    private boolean validateHandle(Handle handle) {
        if (!validHandleUrl(handle.url)) {
            return false;
        }

        // handle parameters cannot be blank
        if (isBlank(handle.url) ||
                isBlank(handle.title) ||
                isBlank(handle.reportemail) ||
                isBlank(handle.subprefix)) {
            return false;
        }

        return true;
    }

    private boolean validHandleUrl(String url) {
        if (isBlank(url)) {
            return false;
        }
        if (url.contains(MAGIC_BEAN)) {
            return false;
        }
        try {
            final URL url_o = new URL(url);
            final String host = url_o.getHost();
            //whitelist host
            if (matchesAnyOf(host, "shortener.post.host.whitelist.regexps")) {
                return true;
            }
            //blacklist url
            if (matchesAnyOf(url, "shortener.post.url.blacklist.regexps")) {
                return false;
            }
            //blacklist host
            if (matchesAnyOf(host, "shortener.post.host.blacklist.regexps")) {
                return false;
            }
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    private boolean matchesAnyOf(String tested, String configPropertyWithPatterns) {
        final String[] patterns = this.configurationService.getArrayProperty(configPropertyWithPatterns);
        if (Objects.isNull(patterns)) {
            return false;
        }

        final String pattern = String.join(",", patterns);

        String[] list = pattern.split(";");
        for (String regexp : list) {
            if (tested.matches(regexp.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Load prefix from the configuration just once.
     */
    private void loadPrefix() {
        if (Objects.nonNull(this.prefix)) {
            return;
        }

        prefix = this.configurationService.getProperty("shortener.handle.prefix", "123456789");
    }

}
