/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.MetadataSchemaRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage MetadataSchema Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(MetadataSchemaRest.CATEGORY + "." + MetadataSchemaRest.NAME)
public class MetadataSchemaRestRepository extends DSpaceRestRepository<MetadataSchemaRest, Integer> {

    @Autowired
    MetadataSchemaService metadataSchemaService;

    @Override
    @PreAuthorize("permitAll()")
    public MetadataSchemaRest findOne(Context context, Integer id) {
        MetadataSchema metadataSchema = null;
        try {
            metadataSchema = metadataSchemaService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (metadataSchema == null) {
            return null;
        }
        return converter.toRest(metadataSchema, utils.obtainProjection());
    }

    @Override
    public Page<MetadataSchemaRest> findAll(Context context, Pageable pageable) {
        try {
            List<MetadataSchema> metadataSchemas = metadataSchemaService.findAll(context);
            return converter.toRestPage(metadataSchemas, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<MetadataSchemaRest> getDomainClass() {
        return MetadataSchemaRest.class;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected MetadataSchemaRest createAndReturn(Context context)
            throws AuthorizeException, SQLException {

        // parse request body
        MetadataSchemaRest metadataSchemaRest;
        try {
            metadataSchemaRest = new ObjectMapper().readValue(
                    getRequestService().getCurrentRequest().getHttpServletRequest().getInputStream(),
                    MetadataSchemaRest.class
            );
        } catch (IOException excIO) {
            throw new DSpaceBadRequestException("error parsing request body", excIO);
        }

        // validate fields
        if (isBlank(metadataSchemaRest.getPrefix())) {
            throw new UnprocessableEntityException("metadata schema name cannot be blank");
        }
        if (isBlank(metadataSchemaRest.getNamespace())) {
            throw new UnprocessableEntityException("metadata schema namespace cannot be blank");
        }

        // create
        MetadataSchema metadataSchema;
        try {
            metadataSchema = metadataSchemaService.create(
                    context, metadataSchemaRest.getPrefix(), metadataSchemaRest.getNamespace()
            );
            metadataSchemaService.update(context, metadataSchema);
        } catch (NonUniqueMetadataException e) {
            throw new UnprocessableEntityException("metadata schema "
                    + metadataSchemaRest.getPrefix() + "." + metadataSchemaRest.getNamespace() + " already exists");
        }

        // return
        return converter.toRest(metadataSchema, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {

        try {
            MetadataSchema metadataSchema = metadataSchemaService.find(context, id);

            if (metadataSchema == null) {
                throw new ResourceNotFoundException("metadata schema with id: " + id + " not found");
            }

            metadataSchemaService.delete(context, metadataSchema);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "error while trying to delete " + MetadataSchemaRest.NAME + " with id: " + id, e
            );
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected MetadataSchemaRest put(Context context, HttpServletRequest request, String apiCategory, String model,
                                     Integer id, JsonNode jsonNode) throws SQLException, AuthorizeException {

        MetadataSchemaRest metadataSchemaRest;
        try {
            metadataSchemaRest = new ObjectMapper().readValue(jsonNode.toString(), MetadataSchemaRest.class);
        } catch (JsonProcessingException e) {
            throw new UnprocessableEntityException("Cannot parse JSON in request body", e);
        }

        if (metadataSchemaRest == null || isBlank(metadataSchemaRest.getPrefix())) {
            throw new UnprocessableEntityException("metadata schema name cannot be blank");
        }
        if (isBlank(metadataSchemaRest.getNamespace())) {
            throw new UnprocessableEntityException("metadata schema namespace cannot be blank");
        }

        if (!Objects.equals(id, metadataSchemaRest.getId())) {
            throw new UnprocessableEntityException("ID in request doesn't match path ID");
        }

        MetadataSchema metadataSchema = metadataSchemaService.find(context, id);
        if (metadataSchema == null) {
            throw new ResourceNotFoundException("metadata schema with id: " + id + " not found");
        }

        metadataSchema.setName(metadataSchemaRest.getPrefix());
        metadataSchema.setNamespace(metadataSchemaRest.getNamespace());

        try {
            metadataSchemaService.update(context, metadataSchema);
            context.commit();
        } catch (NonUniqueMetadataException e) {
            throw new UnprocessableEntityException("metadata schema "
                    + metadataSchemaRest.getPrefix() + "." + metadataSchemaRest.getNamespace() + " already exists");
        }

        return converter.toRest(metadataSchema, utils.obtainProjection());
    }
}
