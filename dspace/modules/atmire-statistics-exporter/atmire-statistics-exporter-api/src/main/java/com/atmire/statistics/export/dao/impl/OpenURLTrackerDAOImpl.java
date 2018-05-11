package com.atmire.statistics.export.dao.impl;

import com.atmire.statistics.export.OpenURLTracker;
import com.atmire.statistics.export.dao.OpenURLTrackerDAO;
import org.dspace.core.AbstractHibernateDAO;

/**
 * Created by jonas - jonas@atmire.com on 09/02/17.
 */
public class OpenURLTrackerDAOImpl extends AbstractHibernateDAO<OpenURLTracker> implements OpenURLTrackerDAO {


    protected OpenURLTrackerDAOImpl(){
        super();
    }

}
