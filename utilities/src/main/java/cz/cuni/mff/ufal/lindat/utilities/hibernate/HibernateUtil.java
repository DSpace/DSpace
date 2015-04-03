package cz.cuni.mff.ufal.lindat.utilities.hibernate;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;

import cz.cuni.mff.ufal.lindat.utilities.units.Variables;

@SuppressWarnings({ "rawtypes", "deprecation" })
public class HibernateUtil {

	static Logger log = Logger.getLogger(HibernateUtil.class);

	private static SessionFactory sessionFactory;

	private Session session = null;
	private Transaction transaction = null;

	static void init() {

		if (Variables.databaseURL == null) {
			log.error("The databaseURL is not set, are the Variables initialized?");
			log.error("Initial SessionFactory creation failed.");
			return;
		}

		try {

			Configuration cfg = new Configuration();
			cfg.setProperty("hibernate.connection.url", Variables.databaseURL);
			cfg.setProperty("hibernate.connection.username",
					Variables.databaseUser);
			cfg.setProperty("hibernate.connection.password",
					Variables.databasePassword);
			cfg.setProperty("show_sql", "true");
			sessionFactory = cfg.configure().buildSessionFactory();

		} catch (Exception e) {
			log.error("Initial SessionFactory creation failed.", e);
		}

	}

	public HibernateUtil() {
	}

	public static SessionFactory getSessionFactory() {
		log.debug("Requesting Hibernate SessionFactory");
		if (sessionFactory == null) {
			init();
		}
		return sessionFactory;
	}

	public Session openSession() {
		try {
			SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
			this.session = sessionFactory.openSession();
			return session;
		} catch (Exception e) {
			log.error("Unable to open hibernate session", e);
		}
		return null;
	}

	public void closeSession() {
		try {
			this.session.close();
		} catch (HibernateException e) {
			e.printStackTrace();
		}
	}

	public Transaction startTransaction() {
		try {
			if(!session.isOpen()) {
				openSession();
			}
			transaction = session.beginTransaction();
		} catch (HibernateException e) {
			log.error("Failed to begin transaction.", e);
		}
		return transaction;
	}

	public void endTransaction() {
		try {
			transaction.commit();
		} catch (HibernateException e) {
			log.error("Transaction Failed.", e);
			transaction.rollback();
			closeSession();

			throw e;
		}
	}

	// public
	//
	//

	public void persist(Class clazz, GenericEntity transientInstance) {
		log.debug("persisting " + clazz.getSimpleName() + " instance");
		try {
			startTransaction();
			session.persist(transientInstance);
			endTransaction();
			log.debug("persist successful");
		} catch (RuntimeException re) {
			log.error("persist failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}
	
	public void update(Class clazz, GenericEntity instance) {
		log.debug("Updating " + clazz.getSimpleName() + " instance");
		try {
			startTransaction();
			session.update(instance);
			endTransaction();
			log.debug("update successful");
		} catch (RuntimeException re) {
			log.error("updte failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}


	public void saveOrUpdate(Class clazz, GenericEntity instance) {
		log.debug("attaching dirty " + clazz.getSimpleName() + " instance");
		try {
			startTransaction();
			session.saveOrUpdate(instance);
			endTransaction();
			log.debug("attach successful");
		} catch (RuntimeException re) {
			log.error("attach failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}

	public void delete(Class clazz, GenericEntity persistentInstance) {
		log.debug("deleting " + clazz.getSimpleName() + " instance");
		try {
			startTransaction();
			session.delete(persistentInstance);
			endTransaction();
			log.debug("delete successful");
		} catch (RuntimeException re) {
			log.error("delete failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}

	public void deleteById(Class clazz, int id) {
		GenericEntity instance = findById(clazz, id);
		delete(clazz, instance);
	}

	public void deleteByCriteria(Class clazz, Criterion... criterion) {
		log.debug("deleteting " + clazz.getSimpleName() + " instance");
		try {
			startTransaction();
			Criteria criteria = session.createCriteria(clazz);
			for (Criterion ct : criterion) {
				criteria.add(ct);
			}
			List<GenericEntity> items = (List<GenericEntity>) criteria.list();

			for (GenericEntity item : items) {
				session.delete(item);
			}

			endTransaction();
		} catch (RuntimeException re) {
			log.error("delete failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}

	public GenericEntity merge(Class clazz, GenericEntity detachedInstance) {
		log.debug("merging " + clazz.getSimpleName() + " instance");
		try {
			startTransaction();
			GenericEntity result = (GenericEntity)session.merge(detachedInstance);
			endTransaction();
			log.debug("merge successful");
			return result;
		} catch (RuntimeException re) {
			log.error("merge failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}

	public GenericEntity findById(Class clazz, int id) {
		log.debug("getting " + clazz.getSimpleName() + " instance with id: "
				+ id);
		try {
			startTransaction();
			GenericEntity instance = (GenericEntity)session.get(clazz, id);
			if (instance == null) {
				log.debug("get successful, no instance found");
			} else {
				log.debug("get successful, instance found");
			}
			endTransaction();
			return instance;
		} catch (RuntimeException re) {
			log.error("get failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}

	public List findByExample(Class clazz, GenericEntity instance) {
		log.debug("finding " + clazz.getSimpleName() + " instance by example");
		try {
			startTransaction();

			Example example = Example.create(instance).excludeNone();

			List results = session.createCriteria(instance.getClass())
					.add(example)
					.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();

			log.debug("find by example successful, result size: "
					+ results.size());
			endTransaction();
			return results;
		} catch (RuntimeException re) {
			log.error("find by example failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}

	public List findAll(Class clazz) {
		log.debug("finding all " + clazz.getSimpleName());
		try {
			startTransaction();
			List results = session.createCriteria(clazz)
					.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
			log.debug("find all successful, result size: " + results.size());
			endTransaction();
			return results;
		} catch (RuntimeException re) {
			log.error("find all failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}

	public List findByCriterie(Class clazz, Object ... criterion) {
		log.debug("finding " + clazz.getSimpleName() + " instance by criteria");
		try {
			startTransaction();
			Criteria criteria = session.createCriteria(clazz);

			ArrayList<Criterion> ct = new ArrayList<Criterion>();
			ArrayList<Order> od = new ArrayList<Order>();
			ProjectionList pj = Projections.projectionList();

			for (Object o : criterion) {
				if (o instanceof Criterion) {
					ct.add((Criterion) o);
				} else if (o instanceof Order) {
					od.add((Order) o);
				} else if (o instanceof Projection) {
					pj.add((Projection) o);
				}
			}

			for (Criterion c : ct) {
				criteria.add(c);
			}

			for (Order o : od) {
				criteria.addOrder(o);
			}

			if (pj.getLength() != 0)
				criteria.setProjection(pj);

			List results = criteria.setResultTransformer(
					Criteria.DISTINCT_ROOT_ENTITY).list();
			endTransaction();
			return results;
		} catch (RuntimeException re) {
			log.error("find by criteria failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}

	public List findByQuery(String query, Hashtable<String, Object> params) {
		log.debug("finding by query " + query);
		try {
			startTransaction();
			Query hql = session.createQuery(query);

			if (params != null) {
				for (String key : params.keySet()) {
					hql.setParameter(key, params.get(key));
				}
			}
			List results = hql.list();
			endTransaction();
			return results;
		} catch (RuntimeException re) {
			log.error("find by query failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}

	public int executeUpdate(String query, Hashtable<String, Object> params) {
		log.debug("executing update " + query);
		try {
			startTransaction();
			Query hql = session.createQuery(query);

			for (String key : params.keySet()) {
				hql.setParameter(key, params.get(key));
			}

			int n = hql.executeUpdate();
			endTransaction();

			return n;

		} catch (RuntimeException re) {
			log.error("execute update failed", re);
			transaction.rollback();
			closeSession();

			throw re;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		if(session.isOpen()) {
			closeSession();
		}
	}

}


