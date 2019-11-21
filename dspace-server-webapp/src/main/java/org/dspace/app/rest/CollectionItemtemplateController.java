/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.repository.CollectionRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController takes care of the creation and retrieval of Collection's Item templates
 * This class will receive the UUID of a Collection and it'll perform actions on its nested objects
 */
@RestController
@RequestMapping("/api/" + CollectionRest.CATEGORY + "/" + CollectionRest.PLURAL_NAME
    + CollectionItemtemplateController.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/itemtemplate")
public class CollectionItemtemplateController {

    /**
     * Regular expression in the request mapping to accept UUID as identifier
     */
    protected static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID =
        "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}";

    @Autowired
    private Utils utils;

    @Autowired
    private CollectionRestRepository collectionRestRepository;

    @Autowired
    private CollectionService collectionService;

    /**
     * This method will create an Item and add it as a template to a Collection.
     *
     * Example:
     * <pre>
     * {@code
     * curl http://<dspace.server.url>/api/core/collections/51715dd3-5590-49f2-b227-6a663c849921/itemtemplate
     *  -XPOST -H 'Content-Type: Content-Type:application/json' \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...' \
     *  --data '{
     *        "metadata": {
     *          "dc.type": [
     *            {
     *              "value": "Journal Article",
     *              "language": "en",
     *              "authority": null,
     *              "confidence": -1
     *            }
     *          ]
     *        },
     *        "inArchive": false,
     *        "discoverable": false,
     *        "withdrawn": false,
     *        "type": "item"
     *      }'
     * }
     * </pre>
     * @param request   The request as described above
     * @param uuid      The UUID of the Collection for which the template item should be made
     * @param itemBody  The new item
     * @return          The created template
     * @throws SQLException
     * @throws AuthorizeException
     */
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ResourceSupport> createTemplateItem(HttpServletRequest request,
                                                              @PathVariable UUID uuid,
                                                              @RequestBody(required = false) JsonNode itemBody)
            throws SQLException, AuthorizeException {

        if (itemBody == null) {
            throw new BadRequestException("The new item should be included as json in the body of this request");
        }

        Context context = ContextUtil.obtainContext(request);
        Collection collection = getCollection(context, uuid);

        ItemRest inputItemRest;
        try {
            ObjectMapper mapper = new ObjectMapper();
            inputItemRest = mapper.readValue(itemBody.toString(), ItemRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("Error parsing request body", e1);
        }

        ItemRest templateItem = collectionRestRepository.createTemplateItem(context, collection, inputItemRest);
        context.commit();

        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null,
                new ItemResource(templateItem, utils));
    }

    /**
     * This method gets the template Item based on the owning Collection
     *
     * <pre>
     * {@code
     * curl http://<dspace.server.url>/api/core/collections/51715dd3-5590-49f2-b227-6a663c849921/itemtemplate
     *  -XGET \
     *  -H 'Authorization: Bearer eyJhbGciOiJI...'
     * }
     * </pre>
     * @param request
     * @param uuid      The UUID of the Collection from which you want the template item
     * @return          The template item from the Collection in the request
     * @throws SQLException
     */
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'READ')")
    @RequestMapping(method = RequestMethod.GET)
    public ItemResource getTemplateItem(HttpServletRequest request, @PathVariable UUID uuid)
            throws SQLException {

        Context context = ContextUtil.obtainContext(request);
        Collection collection = getCollection(context, uuid);
        ItemRest templateItem = collectionRestRepository.getTemplateItem(collection);

        return new ItemResource(templateItem, utils);
    }

    private Collection getCollection(Context context, UUID uuid) throws SQLException {
        Collection collection = collectionService.find(context, uuid);
        if (collection == null) {
            throw new ResourceNotFoundException(
                "The given uuid did not resolve to a collection on the server: " + uuid);
        }
        return collection;
    }
}
