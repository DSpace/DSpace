/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MultipartFile;

/**
 * This is the base class for any Rest Repository. It add a DSpaceContext to the
 * normal Spring Data Repository methods signature and assure that the
 * repository is able to wrap a DSpace Rest Object in a HAL Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public abstract class DSpaceRestRepository<T extends RestAddressableModel, ID extends Serializable>
    extends AbstractDSpaceRestRepository
    implements PagingAndSortingRepository<T, ID> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DSpaceRestRepository.class);

    //Trick to make inner-calls to ourselves that are checked by Spring security
    //See:
    // https://stackoverflow.com/questions/13564627/spring-aop-not-working-for-method-call-inside-another-method
    // https://docs.spring.io/spring/docs/4.3.18.RELEASE/spring-framework-reference/htmlsingle/#aop-understanding-aop-proxies
    @Autowired
    private DSpaceRestRepository<T, ID> thisRepository;

    @Override
    public <S extends T> S save(S entity) {
        Context context = null;
        try {
            context = obtainContext();
            S res = save(context, entity);
            context.commit();
            return res;
        } catch (AuthorizeException ex) {
            throw new RESTAuthorizationException(ex);
        } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Method to implement to support full update of a REST object. This is usually required by a PUT request.
     *
     * @param context
     *            the dspace context
     * @param entity
     *            the REST object to update
     * @return the new state of the REST object after persistence
     * @throws AuthorizeException
     * @throws RepositoryMethodNotImplementedException
     *             returned by the default implementation when the operation is not supported for the entity
     */
    protected <S extends T> S save(Context context, S entity) throws AuthorizeException,
        RepositoryMethodNotImplementedException {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    /**
     * Method to implement to support bulk update of a REST objects via a PUT request
     */
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    /**
     * Return a specific REST object
     *
     * @return the REST object identified by its ID
     */
    public T findOne(ID id) {
        Context context = obtainContext();
        return thisRepository.findOne(context, id);
    }

    /**
     * Method to implement to support retrieval of a specific REST object instance
     *
     * @param context
     *            the dspace context
     * @param id
     *            the rest object id
     * @return the REST object identified by its ID
     */
    public abstract T findOne(Context context, ID id);

    @Override
    /**
     * Return true if an object exist for the specified ID. The default implementation is inefficient as it retrieves
     * the actual object to state that it exists. This could lead to retrieve and inizialize lot of linked objects
     */
    public boolean exists(ID id) {
        return findOne(id) != null;
    }

    @Override
    /**
     * This method cannot be implemented we required all the find method to be paginated
     */
    public final Iterable<T> findAll() {
        throw new RuntimeException("findAll MUST be paginated");
    }

    @Override
    /**
     * This method could be implemented to support bulk retrieval of specific object by their IDs. Unfortunately, this
     * method doesn't allow pagination and it could be misused to retrieve thousand objects at once
     */
    public Iterable<T> findAll(Iterable<ID> ids) {
        throw new RuntimeException("findAll MUST be paginated");
    }

    @Override
    /**
     * This method return the number of object instances of the type managed by the repository class available in the
     * system
     */
    public long count() {
        // FIXME DS-4038
        return 0;
    }

    @Override
    /**
     * Delete the object identified by its ID
     */
    public void delete(ID id) {
        Context context = obtainContext();
        try {
            thisRepository.delete(context, id);
            context.commit();
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Method to implement to support delete of a single object instance
     *
     * @param context
     *            the dspace context
     * @param id
     *            the id of the rest object to delete
     * @throws AuthorizeException
     * @throws RepositoryMethodNotImplementedException
     *             returned by the default implementation when the operation is not supported for the entity
     */
    protected void delete(Context context, ID id) throws AuthorizeException, RepositoryMethodNotImplementedException {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    /**
     * Method to implement to allow delete of a specific entity instance
     */
    public void delete(T entity) {
        // TODO Auto-generated method stub
    }

    @Override
    /**
     * Method to implement to support bulk delete of multiple entity instances
     */
    public void delete(Iterable<? extends T> entities) {
        // TODO Auto-generated method stub

    }

    @Override
    /**
     * Method to implement to support bulk delete of ALL entity instances
     */
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

    @Override
    /**
     * This method cannot be implemented we required all the find method to be paginated
     */
    public final Iterable<T> findAll(Sort sort) {
        throw new RuntimeException("findAll MUST be paginated");
    }

    @Override
    /**
     * Provide access to the manage entity instances in a paginated way
     */
    public Page<T> findAll(Pageable pageable) {
        Context context = obtainContext();
        return thisRepository.findAll(context, pageable);
    }

    /**
     * Method to implement to support scroll of entity instances from the collection resource endpoin
     *
     * @param context
     *            the dspace context
     * @param pageable
     *            object embedding the requested pagination info
     * @return
     */
    public abstract Page<T> findAll(Context context, Pageable pageable);

    /**
     * The REST model supported by the repository
     */
    public abstract Class<T> getDomainClass();

    /**
     * Wrap the REST model in a REST HAL Resource
     *
     * @param model
     *            the rest model instance
     * @param rels
     *            the HAL links
     * @return the REST Resource
     */
    public abstract DSpaceResource<T> wrapResource(T model, String... rels);

    /**
     * Create and return a new instance. Data are usually retrieved from the thread bound http request
     *
     * @return the created REST object
     */
    public T createAndReturn() {
        Context context = null;
        try {
            context = obtainContext();
            T entity = thisRepository.createAndReturn(context);
            context.commit();
            return entity;
        } catch (AuthorizeException e) {
            throw new RESTAuthorizationException(e);
        } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Method to implement to support the creation of a new instance. Usually require to retrieve the http request from
     * the thread bound attribute
     *
     * @param context
     *            the dspace context
     * @return the created REST object
     * @throws AuthorizeException
     * @throws SQLException
     * @throws RepositoryMethodNotImplementedException
     *             returned by the default implementation when the operation is not supported for the entity
     */
    protected T createAndReturn(Context context)
            throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    /**
     * Method to implement to attach/upload a file to a specific REST object
     *
     * @param request
     *            the http request
     * @param apiCategory
     * @param model
     * @param id
     *            the ID of the target REST object
     * @param file
     *            the uploaded file
     * @return the new state of the REST object
     * @throws Exception
     */
    public T upload(HttpServletRequest request, String apiCategory, String model,
                                                     ID id, MultipartFile file) throws Exception {
        throw new RuntimeException("No implementation found; Method not allowed!");
    }

    /**
     * Apply a partial update to the REST object via JSON Patch
     *
     * @param request
     *            the http request
     * @param apiCategory
     * @param model
     * @param id
     *            the ID of the target REST object
     * @param patch
     * the JSON Patch (https://tools.ietf.org/html/rfc6902) operation
     * @return
     * @throws HttpRequestMethodNotSupportedException
     * @throws UnprocessableEntityException
     * @throws PatchBadRequestException
     */
    public T patch(HttpServletRequest request, String apiCategory, String model, ID id, Patch patch)
        throws HttpRequestMethodNotSupportedException, UnprocessableEntityException, PatchBadRequestException {
        Context context = obtainContext();
        try {
            thisRepository.patch(context, request, apiCategory, model, id, patch);
            context.commit();
        } catch (AuthorizeException ae) {
            throw new RESTAuthorizationException(ae);
        } catch (SQLException | DCInputsReaderException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return findOne(id);
    }

    /**
     * Method to implement to allow partial update of the REST object via JSON Patch
     *
     * @param request
     *            the http request
     * @param apiCategory
     * @param model
     * @param id
     *            the ID of the target REST object
     * @param patch
     *            the JSON Patch (https://tools.ietf.org/html/rfc6902) operation
     * @return the full new state of the REST object after patching
     * @throws HttpRequestMethodNotSupportedException
     * @throws UnprocessableEntityException
     * @throws PatchBadRequestException
     * @throws RepositoryMethodNotImplementedException
     *             returned by the default implementation when the operation is not supported for the entity
     *
     * @throws SQLException
     * @throws AuthorizeException
     * @throws DCInputsReaderException
     */
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, ID id,
                         Patch patch)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException, DCInputsReaderException {
        throw new RepositoryMethodNotImplementedException(apiCategory, model);
    }

    /**
     * Bulk create object instances from an uploaded file
     *
     * @param request
     *            the http request
     * @param uploadfile
     *            the file to process
     * @return the created objects
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws AuthorizeException
     */
    public Iterable<T> upload(HttpServletRequest request, MultipartFile uploadfile)
        throws SQLException, FileNotFoundException, IOException, AuthorizeException {
        Context context = obtainContext();
        Iterable<T> entity = upload(context, request, uploadfile);
        context.commit();
        return entity;
    }

    /**
     * Method to implement to support bulk creation of objects from a file
     *
     * @param request
     *            the http request
     * @param uploadfile
     *            the file to process
     * @return the created objects
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws AuthorizeException
     * @throws RepositoryMethodNotImplementedException
     */
    protected Iterable<T> upload(Context context, HttpServletRequest request,
            MultipartFile uploadfile)
        throws SQLException, FileNotFoundException, IOException, AuthorizeException {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    /**
     * Method to support updating a DSpace instance.
     *
     * @param request     the http request
     * @param apiCategory the API category e.g. "api"
     * @param model       the DSpace model e.g. "metadatafield"
     * @param uuid        the ID of the target REST object
     * @param jsonNode    the part of the request body representing the updated rest object
     * @return the updated REST object
     */
    public T put(HttpServletRequest request, String apiCategory, String model, ID uuid, JsonNode jsonNode) {
        Context context = obtainContext();
        try {
            thisRepository.put(context, request, apiCategory, model, uuid, jsonNode);
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update DSpace object " + model + " with id=" + uuid, e);
        } catch (AuthorizeException e) {
            throw new RuntimeException("Unable to perform PUT request as the " +
                                           "current user does not have sufficient rights", e);
        }
        return findOne(uuid);
    }

    /**
     * Implement this method in the subclass to support updating a DSpace instance.
     *
     * @param context     the dspace context
     * @param request     the http request
     * @param apiCategory the API category e.g. "api"
     * @param model       the DSpace model e.g. "metadatafield"
     * @param id          the ID of the target REST object
     * @param jsonNode    the part of the request body representing the updated rest object
     * @return the updated REST object
     * @throws AuthorizeException if the context user is not authorized to perform this operation
     * @throws SQLException when the database returns an error
     * @throws RepositoryMethodNotImplementedException
     *             returned by the default implementation when the operation is not supported for the entity
     */
    protected T put(Context context, HttpServletRequest request, String apiCategory, String model, ID id,
                         JsonNode jsonNode)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException {
        throw new RepositoryMethodNotImplementedException(apiCategory, model);
    }
}