package org.dspace.orm.dao.database;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.orm.dao.api.ICheckSumResultsDao;
import org.dspace.orm.entity.CheckSumResults;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @author João Melo <jmelo@lyncode.com>
 * 
 * @version $Revision$
 */

public abstract class CheckSumResultsDao implements ICheckSumResultsDao {
	private static Logger log = LogManager.getLogger(CheckSumResults.class);
	
	@Autowired
    SessionFactory sessionFactory;
	
	protected Session getSession() {
        return sessionFactory.getCurrentSession();
    }
	
	

    @Override
    public CheckSumResults selectByKey(String key) {
        return (CheckSumResults) getSession().get(CheckSumResults.class, key);
    }
	
	@Override
    public Integer save(CheckSumResults c) {
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
    public boolean delete(CheckSumResults c) {
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
    public List<CheckSumResults> selectAll() {
        return (List<CheckSumResults>) getSession().createCriteria(
        		CheckSumResults.class).list();
    }
}
