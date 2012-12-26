/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.context;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.ContextV2;
import org.dspace.orm.dao.api.IEpersonDao;
import org.dspace.services.ContextService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class implements a DSpace Context Service.
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public class DSpaceContextService implements ContextService {
	private static final String CONTEXT_ATTR = "dspace.context";
	
	private SessionFactory sessionFactory;
	@Autowired(required=false) RequestService requestService;
	@Autowired(required=false) IEpersonDao epersonDao;

	@Override
	public Context newContext() {
		Session session = sessionFactory.openSession();
		session.beginTransaction();
		ContextV2 ctx;
		try {
			ctx = new ContextV2(session);

			if (requestService != null) {
				Request r = requestService.getCurrentRequest();
				if (r != null)  {// There is one request running on this thread!
					String userID = r.getSession().getUserId();
					if (userID != null && epersonDao != null)  
						ctx.setCurrentEperson(epersonDao.selectById(Integer.parseInt(userID)));
					r.setAttribute(CONTEXT_ATTR, ctx);
				}
			}
			return ctx;
		} catch (SQLException e) {
			// IMPORTANT! This never happens because ContextV2 in fact, do not throw any exception!
			return null;
		}
	}
	
	@Override
	public Context getContext() {
		if (requestService != null) {
			Request r = requestService.getCurrentRequest();
			if (r != null) {// There is one request running on this thread!
				Context ctx = (Context) r.getAttribute(CONTEXT_ATTR);
				if (ctx != null) return ctx;
			}
		}
		return newContext();
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}
