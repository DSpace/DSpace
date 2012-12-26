/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.database;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.orm.dao.api.ICheckSumHistoryDao;
import org.dspace.orm.entity.CheckSumHistory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */

@Transactional
@Repository("ICheckSumHistoryDao")
public class CheckSumHistoryDao implements ICheckSumHistoryDao {
	private static Logger log = LogManager.getLogger(CheckSumHistory.class);
	
	@Resource(name="sessionFactory")
    SessionFactory sessionFactory;
	
	protected Session getSession() {
        return sessionFactory.getCurrentSession();
    }
	
	

    @Override
    public CheckSumHistory selectByKey(String key) {
        return (CheckSumHistory) getSession().get(CheckSumHistory.class, key);
    }
	
	@Override
    public Integer save(CheckSumHistory c) {
        Session session = getSession();
        Integer id = null;
        try {
            id = (Integer) session.save(c);
            log.debug(c.getClass().getSimpleName() + " saved");
        } catch (HibernateException e) {
            log.error(e.getMessage(), e);
        }
        return id;
    }
	
	@Override
    public boolean delete(CheckSumHistory c) {
        boolean result = false;
        Session session = getSession();
        try {
            session.delete(c);
            log.debug(c.getClass().getSimpleName() + " deleted");
            result = true;
        } catch (HibernateException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }
	
    @SuppressWarnings("unchecked")
    @Override
    public List<CheckSumHistory> selectAll() {
        return (List<CheckSumHistory>) getSession().createCriteria(
        		CheckSumHistory.class).list();
    }
}
