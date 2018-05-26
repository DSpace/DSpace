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

import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.PatchUnprocessableEntityException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

/**
 * This is the base class for any Rest Repository. It add a DSpaceContext to the
 * normal Spring Data Repository methods signature and assure that the
 * repository is able to wrap a DSpace Rest Object in a HAL Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public abstract class DSpaceRestRepository<T extends RestAddressableModel, ID extends Serializable>
    extends AbstractDSpaceRestRepository implements PagingAndSortingRepository<T, ID> {

    private static final Logger log = Logger.getLogger(DSpaceRestRepository.class);

    @Override
    public <S extends T> S save(S entity) {
        Context context = obtainContext();
        return save(context, entity);
    }

    protected <S extends T> S save(Context context, S entity) {
        try {
            //nothing to do default implementation commit transaction
            context.commit();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return entity;
    }

    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public T findOne(ID id) {
        Context context = obtainContext();
        return findOne(context, id);
    }

    public abstract T findOne(Context context, ID id);

    @Override
    public boolean exists(ID id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<T> findAll() {
        throw new RuntimeException("findAll MUST be paginated");
    }

    @Override
    public Iterable<T> findAll(Iterable<ID> ids) {
        throw new RuntimeException("findAll MUST be paginated");
    }

    @Override
    public long count() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void delete(ID id) {
        Context context = obtainContext();
        try {
            delete(context, id);
            context.commit();
        } catch (Exception e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
    }

    protected void delete(Context context, ID id) throws RepositoryMethodNotImplementedException {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public void delete(T entity) {
        // TODO Auto-generated method stub
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub

    }

    @Override
    public Iterable<T> findAll(Sort sort) {
        throw new RuntimeException("findAll MUST be paginated");
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        Context context = obtainContext();
        return findAll(context, pageable);
    }

    public abstract Page<T> findAll(Context context, Pageable pageable);

    public abstract Class<T> getDomainClass();

    public abstract DSpaceResource<T> wrapResource(T model, String... rels);

    public T createAndReturn() throws SQLException, AuthorizeException, HttpRequestMethodNotSupportedException {
        Context context = obtainContext();
        T entity = createAndReturn(context);
        context.commit();
        return entity;
    }

    protected T createAndReturn(Context context)
        throws SQLException, AuthorizeException, HttpRequestMethodNotSupportedException {
        throw new HttpRequestMethodNotSupportedException(RequestMethod.POST.toString());
    }

    public T upload(HttpServletRequest request, String apiCategory, String model, ID id, String extraField,
                    MultipartFile file) throws Exception {
        throw new RuntimeException("No implementation found; Method not allowed!");
    }

    public T patch(HttpServletRequest request, String apiCategory, String model, ID id, Patch patch)
        throws HttpRequestMethodNotSupportedException, PatchUnprocessableEntityException, PatchBadRequestException {
        Context context = obtainContext();
        try {
            patch(context, request, apiCategory, model, id, patch);
            context.commit();
        } catch (SQLException | AuthorizeException | DCInputsReaderException e) {
            throw new PatchUnprocessableEntityException(e.getMessage());
        }
        return findOne(id);
    }

    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, ID id,
                         Patch patch)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException, DCInputsReaderException {
        throw new RepositoryMethodNotImplementedException(apiCategory, model);
    }

    public T action(HttpServletRequest request, ID id) throws SQLException, IOException, AuthorizeException {
        Context context = obtainContext();
        T entity = action(context, request, id);
        context.commit();
        return entity;
    }

    protected T action(Context context, HttpServletRequest request, ID id)
        throws SQLException, IOException, AuthorizeException {
        throw new RuntimeException("No implementation found; Method not allowed!");
    }

    public Iterable<T> upload(HttpServletRequest request, MultipartFile uploadfile)
        throws SQLException, FileNotFoundException, IOException, AuthorizeException {
        Context context = obtainContext();
        Iterable<T> entity = upload(context, request, uploadfile);
        context.commit();
        return entity;
    }

    protected Iterable<T> upload(Context context, HttpServletRequest request, MultipartFile uploadfile)
        throws SQLException, FileNotFoundException, IOException, AuthorizeException {
        throw new RuntimeException("No implementation found; Method not allowed!");
    }

}
