/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.LinksRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    /**
     * The default page size, if unspecified in the request.
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * The maximum number of embed levels to allow.
     */
    private static final int EMBED_MAX_LEVELS = 2;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    RequestService requestService;

    @Autowired(required = true)
    private List<DSpaceObjectService<? extends DSpaceObject>> dSpaceObjectServices;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    @Autowired
    private ConverterService converter;

    /** Cache to support fast lookups of LinkRest method annotation information. */
    private Map<Method, Optional<LinkRest>> linkAnnotationForMethod = new HashMap<>();

    public <T> Page<T> getPage(List<T> fullContents, @Nullable Pageable optionalPageable) {
        Pageable pageable = getPageable(optionalPageable);
        int total = fullContents.size();
        List<T> pageContent = null;
        if (pageable == null) {
            pageable = new PageRequest(0, DEFAULT_PAGE_SIZE);
        }
        if (pageable.getOffset() > total) {
            throw new PaginationException(total);
        } else {
            if (pageable.getOffset() + pageable.getPageSize() > total) {
                pageContent = fullContents.subList(Math.toIntExact(pageable.getOffset()), total);
            } else {
                pageContent = fullContents.subList(Math.toIntExact(pageable.getOffset()),
                        Math.toIntExact(pageable.getOffset()) + pageable.getPageSize());
            }
            return new PageImpl<T>(pageContent, pageable, total);
        }
    }

    /**
     * Convenience method to get a default pageable instance if needed.
     *
     * @param optionalPageable the existing pageable instance, may be null.
     * @return the existing instance if it is not null, a default pageable instance otherwise.
     */
    public Pageable getPageable(@Nullable Pageable optionalPageable) {
        return optionalPageable != null ? optionalPageable : new PageRequest(0, DEFAULT_PAGE_SIZE);
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
        if (modelPlural.equals("resourcepolicies")) {
            return ResourcePolicyRest.NAME;
        }
        if (StringUtils.equals(modelPlural, "processes")) {
            return ProcessRest.NAME;
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
     * @param restClass
     * @return the LinkRest annotation corresponding to the specified rel in the rest class, or null if not found.
     */
    public LinkRest getClassLevelLinkRest(String rel, Class<? extends RestAddressableModel> restClass) {
        Optional<LinkRest> optionalLinkRest = getLinkRests(restClass).stream().filter((linkRest) ->
                rel.equals(linkRest.name())).findFirst();
        return optionalLinkRest.isPresent() ? optionalLinkRest.get() : null;
    }

    /**
     * Build the canonical representation of a metadata key in DSpace. ie
     * <schema>.<element>[.<qualifier>]
     *
     * @param schema
     * @param element
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
     * This method will construct a List of BitstreamFormats out of a request.
     * It will call the {@link Utils#getStringListFromRequest(HttpServletRequest)} method to retrieve a list of links
     * out of the request.
     * The method will iterate over this list of links and parse the links to retrieve the integer ID from it.
     * It will then retrieve the BitstreamFormat corresponding to this ID.
     * If one is found, this BitstreamFormat is added to the List of BitstreamFormats that we will return.
     *
     * @param request   The request out of which we'll create the List of BitstreamFormats
     * @param context   The relevant DSpace context
     * @return          The resulting list of BitstreamFormats that we parsed out of the request
     */
    public List<BitstreamFormat> constructBitstreamFormatList(HttpServletRequest request, Context context) {

        return getStringListFromRequest(request).stream()
                .map(link -> {
                    if (link.endsWith("/")) {
                        link = link.substring(0, link.length() - 1);
                    }
                    return link.substring(link.lastIndexOf('/') + 1);
                })
                .map(id -> {
                    try {
                        return bitstreamFormatService.find(context, parseInt(id));
                    } catch (SQLException | NumberFormatException e) {
                        log.error("Could not find bitstream format for id: " + id, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * This method will construct a List of DSpaceObjects by executing the method
     * {@link Utils#readFromRequest(HttpServletRequest)} and fetching the List of Strings from the request.
     * The method will iterate over this list of Strings and parse the String to retrieve the UUID from it.
     * It will then look through all the DSpaceObjectServices to try and match this UUID to a DSpaceObject.
     * If one is found, this DSpaceObject is added to the List of DSpaceObjects that we will return.
     * @param context   The relevant DSpace context
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

    public <T extends HALResource> T toResource(RestModel restObject) {
        return converter.toResource(restObject);
    }

    /**
     * Gets the alphanumerically sorted union of multiple string arrays.
     *
     * @param arrays the string arrays.
     * @return the sorted union of them, with no duplicate values.
     */
    public String[] getSortedUnion(String[]... arrays) {
        Set<String> set = new TreeSet<>();
        for (String[] array : arrays) {
            for (String string : array) {
                set.add(string);
            }
        }
        return set.toArray(arrays[0]);
    }

    /**
     * Gets the method with the given name in the given class.
     *
     * @param clazz the class.
     * @param name the method name.
     * @return the first method found with the given name.
     * @throws IllegalArgumentException if no such method is found.
     */
    public Method requireMethod(Class clazz, String name) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException("No such method in " + clazz + ": " + name);
    }

    /**
     * Gets the projection requested by the current servlet request, or {@link DefaultProjection} if none
     * is specified.
     *
     * @return the requested or default projection, never {@code null}.
     * @throws IllegalArgumentException if the request specifies an unknown projection name.
     */
    public Projection obtainProjection() {
        String projectionName = requestService.getCurrentRequest().getServletRequest().getParameter("projection");
        return converter.getProjection(projectionName);
    }

    /**
     * Adds embeds or links for all class-level LinkRel annotations for which embeds or links are allowed.
     *
     * @param halResource the resource.
     */
    public void embedOrLinkClassLevelRels(HALResource<RestAddressableModel> halResource) {
        Projection projection = halResource.getContent().getProjection();
        getLinkRests(halResource.getContent().getClass()).stream().forEach((linkRest) -> {
            Link link = linkToSubResource(halResource.getContent(), linkRest.name());
            if (projection.allowEmbedding(halResource, linkRest)) {
                embedRelFromRepository(halResource, linkRest.name(), link, linkRest);
                halResource.add(link); // unconditionally link if embedding was allowed
            } else if (projection.allowLinking(halResource, linkRest)) {
                halResource.add(link);
            }
        });
    }

    private List<LinkRest> getLinkRests(Class<? extends RestAddressableModel> restClass) {
        List<LinkRest> list = new ArrayList<>();
        LinksRest linksAnnotation = restClass.getDeclaredAnnotation(LinksRest.class);
        if (linksAnnotation != null) {
            list.addAll(Arrays.asList(linksAnnotation.links()));
        }
        return list;
    }

    /**
     * Embeds a rel whose value comes from a {@link LinkRestRepository}, if the maximum embed level has not
     * been exceeded yet.
     * <p>
     * The embed will be skipped if 1) the link repository reports that it is not embeddable or 2) the returned
     * value is null and the LinkRest annotation has embedOptional = true.
     * </p><p>
     * Implementation note: The caller is responsible for ensuring that the projection allows the embed
     * before calling this method.
     * </p>
     *
     * @param resource the resource from which the embed will be made.
     * @param rel the name of the rel.
     * @param link the link.
     * @param linkRest the LinkRest annotation (must have method defined).
     * @throws RepositoryNotFoundException if the link repository could not be found.
     * @throws IllegalArgumentException if the method specified by the LinkRest could not be found in the
     * link repository.
     * @throws RuntimeException if any other problem occurs when trying to invoke the method.
     */
    void embedRelFromRepository(HALResource<? extends RestAddressableModel> resource,
                                        String rel, Link link, LinkRest linkRest) {
        if (resource.getContent().getEmbedLevel() == EMBED_MAX_LEVELS) {
            return;
        }
        Projection projection = resource.getContent().getProjection();
        LinkRestRepository linkRepository = getLinkResourceRepository(resource.getContent().getCategory(),
                resource.getContent().getType(), rel);
        if (linkRepository.isEmbeddableRelation(resource.getContent(), rel)) {
            Method method = requireMethod(linkRepository.getClass(), linkRest.method());
            Object contentId = getContentIdForLinkMethod(resource.getContent(), method);
            try {
                Object linkedObject = method.invoke(linkRepository, null, contentId, null, projection);
                resource.embedResource(rel, wrapForEmbedding(resource, linkedObject, link));
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException) e.getTargetException();
                } else {
                    throw new RuntimeException(e);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Adds embeds (if the maximum embed level has not been exceeded yet) for all properties annotated with
     * {@code @LinkRel} or whose return types are {@link RestAddressableModel} subclasses.
     */
    public void embedMethodLevelRels(HALResource<? extends RestAddressableModel> resource) {
        if (resource.getContent().getEmbedLevel() == EMBED_MAX_LEVELS) {
            return;
        }
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(
                    resource.getContent().getClass()).getPropertyDescriptors()) {
                Method readMethod = pd.getReadMethod();
                String propertyName = pd.getName();
                if (readMethod != null && !"class".equals(propertyName)) {
                    embedMethodLevelRel(resource, readMethod, propertyName);
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the LinkRest annotation for the given method, if any.
     *
     * @param readMethod the method.
     * @return the annotation, or {@code null} if not found.
     */
    public @Nullable LinkRest findLinkAnnotation(Method readMethod) {
        Optional<LinkRest> optional = linkAnnotationForMethod.get(readMethod);
        if (optional == null) {
            LinkRest linkRest = AnnotationUtils.findAnnotation(readMethod, LinkRest.class);
            optional = linkRest != null ? Optional.of(linkRest) : Optional.empty();
            linkAnnotationForMethod.put(readMethod, optional);
        }
        return optional.isPresent() ? optional.get() : null;
    }

    /**
     * Adds an embed for the given property read method. If the @LinkRel annotation is present and
     * specifies a method name, the value will come from invoking that method in the appropriate link
     * rest repository. Otherwise, the value will come from invoking the method directly on the wrapped
     * rest object.
     *
     * @param resource the resource from which the embed will be made.
     * @param readMethod the property read method.
     * @param propertyName the property name, which will be used as the rel/embed name unless the @LinkRel
     *                     annotation is present and specifies a different name.
     */
    private void embedMethodLevelRel(HALResource<? extends RestAddressableModel> resource,
                                     Method readMethod,
                                     String propertyName) {
        String rel = propertyName;
        LinkRest linkRest = findLinkAnnotation(readMethod);
        try {
            if (linkRest != null) {
                if (!resource.getContent().getProjection().allowEmbedding(resource, linkRest)) {
                    return; // projection disallows this optional method-level embed
                }
                if (StringUtils.isNotBlank(linkRest.name())) {
                    rel = linkRest.name();
                }
                Link link = linkToSubResource(resource.getContent(), rel);
                if (StringUtils.isBlank(linkRest.method())) {
                    Object linkedObject = readMethod.invoke(resource.getContent());
                    resource.embedResource(rel, wrapForEmbedding(resource, linkedObject, link));
                } else {
                    embedRelFromRepository(resource, rel, link, linkRest);
                }
            } else if (RestAddressableModel.class.isAssignableFrom(readMethod.getReturnType())) {
                RestAddressableModel linkedObject = (RestAddressableModel) readMethod.invoke(resource.getContent());
                resource.embedResource(rel, linkedObject == null ? null :
                        wrapForEmbedding(resource, linkedObject, linkToSubResource(resource.getContent(), rel)));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wraps the given linked object (retrieved from a link repository or link method on the rest item)
     * in an object that is appropriate for embedding, if needed. Does not perform the actual embed; the
     * caller is responsible for that.
     *
     * @param resource the resource from which the embed will be made.
     * @param linkedObject the linked object.
     * @param link the link, which is used if the linked object is a list or page, to determine the self link
     *             and embed property name to use for the subresource.
     * @return the wrapped object, which will have an "embed level" one greater than the given parent resource.
     */
    private Object wrapForEmbedding(HALResource<? extends RestAddressableModel> resource,
                                    Object linkedObject, Link link) {
        int childEmbedLevel = resource.getContent().getEmbedLevel() + 1;
        if (linkedObject instanceof RestAddressableModel) {
            RestAddressableModel restObject = (RestAddressableModel) linkedObject;
            restObject.setEmbedLevel(childEmbedLevel);
            return converter.toResource(restObject);
        } else if (linkedObject instanceof Page) {
            // The first page has already been constructed by a link repository and we only need to wrap it
            Page<RestAddressableModel> page = (Page<RestAddressableModel>) linkedObject;
            return new EmbeddedPage(link.getHref(), page.map((restObject) -> {
                restObject.setEmbedLevel(childEmbedLevel);
                return converter.toResource(restObject);
            }), null, link.getRel());
        } else if (linkedObject instanceof List) {
            // The full list has been retrieved and we need to provide the first page for embedding
            List<RestAddressableModel> list = (List<RestAddressableModel>) linkedObject;
            if (list.size() > 0) {
                PageImpl<RestAddressableModel> page = new PageImpl(
                        list.subList(0, list.size() > DEFAULT_PAGE_SIZE ? DEFAULT_PAGE_SIZE : list.size()),
                        new PageRequest(0, DEFAULT_PAGE_SIZE), list.size());
                return new EmbeddedPage(link.getHref(),
                        page.map((restObject) -> {
                            restObject.setEmbedLevel(childEmbedLevel);
                            return converter.toResource(restObject);
                        }),
                        list, link.getRel());
            } else {
                PageImpl<RestAddressableModel> page = new PageImpl(list);
                return new EmbeddedPage(link.getHref(), page, list, link.getRel());
            }
        } else {
            return linkedObject;
        }
    }

    /**
     * Gets an object representing the id of the wrapped object, whose runtime time matches the second
     * (id) argument of the given link method. This is necessary because it is possible for the rest
     * object's id to be a string while the domain object's id may be a uuid or numeric type.
     *
     * @param linkMethod the link method.
     * @return the id, which may be a UUID, Integer, or Long.
     */
    private Object getContentIdForLinkMethod(RestAddressableModel restObject, Method linkMethod) {
        Object contentId = ((BaseObjectRest) restObject).getId();
        Class requiredIdType = linkMethod.getParameterTypes()[1];
        if (!requiredIdType.isAssignableFrom(contentId.getClass())) {
            if (requiredIdType.equals(UUID.class)) {
                contentId = UUID.fromString(contentId.toString());
            } else if (requiredIdType.equals(Integer.class)) {
                contentId = Integer.parseInt(contentId.toString());
            } else if (requiredIdType.equals(Long.class)) {
                contentId = Long.parseLong(contentId.toString());
            } else {
                throw new IllegalArgumentException("Cannot cast " + restObject.getClass()
                        + " id type " + contentId.getClass() + " to id type required by "
                        + linkMethod.getDeclaringClass() + "#" + linkMethod.getName() + ": " + requiredIdType);
            }
        }
        return contentId;
    }
}
