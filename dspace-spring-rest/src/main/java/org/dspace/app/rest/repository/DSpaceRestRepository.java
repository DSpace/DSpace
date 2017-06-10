/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.Serializable;

import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * This is the base class for any Rest Repository. It add a DSpaceContext to the
 * normal Spring Data Repository methods signature and assure that the
 * repository is able to wrap a DSpace Rest Object in a HAL Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public abstract class DSpaceRestRepository<T extends RestModel, ID extends Serializable>
extends AbstractDSpaceRestRepository
		implements PagingAndSortingRepository<T, ID> {

	@Override
	public <S extends T> S save(S entity) {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub

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
}
