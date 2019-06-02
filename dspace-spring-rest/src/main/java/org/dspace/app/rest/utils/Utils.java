/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.LinksRest;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Collection of utility methods
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class Utils {

    private static final Logger log = Logger.getLogger(Utils.class);

    @Autowired
    ApplicationContext applicationContext;

    @Autowired(required = true)
    private List<DSpaceObjectService<? extends DSpaceObject>> dSpaceObjectServices;


    public <T> Page<T> getPage(List<T> fullContents, Pageable pageable) {
        int total = fullContents.size();
        List<T> pageContent = null;
        if (pageable.getOffset() > total) {
            throw new PaginationException(total);
        } else {
            if (pageable.getOffset() + pageable.getPageSize() > total) {
                pageContent = fullContents.subList(pageable.getOffset(), total);
            } else {
                pageContent = fullContents.subList(pageable.getOffset(), pageable.getOffset() + pageable.getPageSize());
            }
            return new PageImpl<T>(pageContent, pageable, total);
        }
    }

    public Link linkToSingleResource(DSpaceResource r, String rel) {
        RestAddressableModel data = r.getContent();
        return linkToSingleResource(data, rel);
    }

    public Link linkToSingleResource(RestAddressableModel data, String rel) {
        return linkTo(data.getController(), data.getCategory(), data.getTypePlural()).slash(data)
                                                                                     .withRel(rel);
    }

    public Link linkToSubResource(RestAddressableModel data, String rel) {
        return linkToSubResource(data, rel, rel);
    }

    public Link linkToSubResource(RestAddressableModel data, String rel, String path) {
        return linkTo(data.getController(), data.getCategory(), data.getTypePlural()).slash(data).slash(path)
                                                                                     .withRel(rel);
    }

    public DSpaceRestRepository getResourceRepository(String apiCategory, String modelPlural) {
        String model = makeSingular(modelPlural);
        try {
            return applicationContext.getBean(apiCategory + "." + model, DSpaceRestRepository.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new RepositoryNotFoundException(apiCategory, model);
        }
    }

    public String[] getRepositories() {
        return applicationContext.getBeanNamesForType(DSpaceRestRepository.class);
    }

    public static String makeSingular(String modelPlural) {
        // The old dspace res package includes the evo inflection library which
        // has a plural() function but no singular function
        if (modelPlural.equals("communities")) {
            return CommunityRest.NAME;
        }
        if (modelPlural.equals("authorities")) {
            return AuthorityRest.NAME;
        }
        if (modelPlural.equals("resourcePolicies")) {
            return ResourcePolicyRest.NAME;
        }
        return modelPlural.replaceAll("s$", "");
    }

    /**
     * Retrieve the LinkRestRepository associated with a specific link from the
     * apiCategory and model specified in the parameters.
     *
     * @param apiCategory the apiCategory
     * @param modelPlural the model name in its plural form
     * @param rel         the name of the relation
     * @return
     */
    public LinkRestRepository getLinkResourceRepository(String apiCategory, String modelPlural, String rel) {
        String model = makeSingular(modelPlural);
        try {
            return applicationContext.getBean(apiCategory + "." + model + "." + rel, LinkRestRepository.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new RepositoryNotFoundException(apiCategory, model);
        }
    }

    /**
     * @param rel
     * @param domainClass
     * @return the LinkRest annotation corresponding to the specified rel in the
     * domainClass. Null if not found
     */
    public LinkRest getLinkRest(String rel, Class<RestAddressableModel> domainClass) {
        LinkRest linkRest = null;
        LinksRest linksAnnotation = domainClass.getDeclaredAnnotation(LinksRest.class);
        if (linksAnnotation != null) {
            LinkRest[] links = linksAnnotation.links();
            for (LinkRest l : links) {
                if (StringUtils.equals(rel, l.name())) {
                    linkRest = l;
                    break;
                }
            }
        }
        return linkRest;
    }

    /**
     * Build the canonical representation of a metadata key in DSpace. ie
     * <schema>.<element>[.<qualifier>]
     *
     * @param schema
     * @param element
     * @param object
     * @return
     */
    public String getMetadataKey(String schema, String element, String qualifier) {
        return org.dspace.core.Utils.standardize(schema, element, qualifier, ".");
    }

    /**
     * Create a temporary file from a multipart file upload
     * 
     * @param multipartFile
     *            the multipartFile representing the uploaded file. Please note that it is a complex object including
     *            additional information other than the binary like the orginal file name and the mimetype
     * @param prefixTempName
     *            the prefix to use to generate the filename of the temporary file
     * @param suffixTempName
     *            the suffic to use to generate the filename of the temporary file
     * @return the temporary file on the server
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static File getFile(MultipartFile multipartFile, String prefixTempName, String suffixTempName)
            throws IOException, FileNotFoundException {
        // TODO after change item-submission into
        String tempDir = (ConfigurationManager.getProperty("upload.temp.dir") != null)
                ? ConfigurationManager.getProperty("upload.temp.dir")
                : System.getProperty("java.io.tmpdir");
        File uploadDir = new File(tempDir);
        if (!uploadDir.exists()) {
            if (!uploadDir.mkdir()) {
                uploadDir = null;
            }
        }
        File file = File.createTempFile(prefixTempName + "-" + suffixTempName, ".temp", uploadDir);
        InputStream io = new BufferedInputStream(multipartFile.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        org.dspace.core.Utils.bufferedCopy(io, out);
        return file;
    }

    /**
     * Return the filename part from a multipartFile upload that could eventually contains the fullpath on the client
     * filesystem
     * 
     * @param multipartFile
     *            the file uploaded
     * @return the filename part of the file on the client filesystem
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static String getFileName(MultipartFile multipartFile)
            throws IOException, FileNotFoundException {
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename != null) {
            // split by \ or / as we don't know the client OS (Win, Linux)
            String[] parts = originalFilename.split("[\\/]");
            return parts[parts.length - 1];
        } else {
            return multipartFile.getName();
        }
    }

    /**
     * This method will construct a List of DSpaceObjects by executing the method
     * {@link Utils#readFromRequest(HttpServletRequest)} and fetching the List of Strings from the request.
     * The method will iterate over this list of Strings and parse the String to retrieve the UUID from it.
     * It will then look through all the DSpaceObjectServices to try and match this UUID to a DSpaceObject.
     * If one is found, this DSpaceObject is added to the List of DSpaceObjects that we will return.
     * @param context   The relevant DSpace context
     * @param request   The request out of which we'll create the List of DSpaceObjects
     * @return          The resulting list of DSpaceObjects that we parsed out of the request
     */
    public List<DSpaceObject> constructDSpaceObjectList(Context context, List<String> list) {
        List<DSpaceObject> dSpaceObjects = new LinkedList<>();
        for (String string : list) {
            if (string.endsWith("/")) {
                string = string.substring(0, string.length() - 1);
            }
            String uuid = string.substring(string.lastIndexOf('/') + 1);
            try {
                for (DSpaceObjectService dSpaceObjectService : dSpaceObjectServices) {
                    DSpaceObject dSpaceObject = dSpaceObjectService.find(context, UUIDUtils.fromString(uuid));
                    if (dSpaceObject != null) {
                        dSpaceObjects.add(dSpaceObject);
                        break;
                    }
                }
            } catch (SQLException e) {
                log.error("Could not find DSpaceObject for UUID: " + uuid, e);
            }

        }
        return dSpaceObjects;
    }

    /**
     * This method reads lines from the request's InputStream and will add this to a list of Strings.
     * @param request       The request from which the InputStream will be fetched
     * @return              A list of String constructed from the request's InputStream
     * @throws IOException  If something goes wrong
     */
    private List<String> readFromRequest(HttpServletRequest request) throws IOException {
        List<String> list = new LinkedList<>();
        Scanner scanner = new Scanner(request.getInputStream());

        try {

            while (scanner.hasNextLine()) {

                String line = scanner.nextLine();
                if (org.springframework.util.StringUtils.hasText(line)) {
                    list.add(line);
                }
            }

        } finally {
            scanner.close();
        }
        return list;
    }


    /**
     * This method will retrieve a list of DSpaceObjects from the Request by reading in the Request's InputStream
     * with a Scanner and searching the InputStream for UUIDs which will then be resolved to a DSpaceObject.
     * These will all be added to a list and returned by this method.
     * @param request       The request of which the InputStream will be used
     * @return              The list of DSpaceObjects that we could find in the InputStream
     * @throws IOException  If something goes wrong
     */
    public List<String> getStringListFromRequest(HttpServletRequest request) {
        List<String> list = null;
        try {
            list = readFromRequest(request);
        } catch (IOException e) {
            log.error("Something went wrong with reading in the inputstream from the request", e);
        }
        return list;
    }
}
