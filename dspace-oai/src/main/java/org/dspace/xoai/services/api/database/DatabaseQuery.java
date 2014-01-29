/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.database;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseQuery {
    private static Logger log = Logger.getLogger(DatabaseQuery.class);

    private String query;
    private List<Object> parameters;
    private Context context;
    private Integer total;

    private String countQuery;
    private List<Object> countParameters;

    public DatabaseQuery(Context context) {
        this.context = context;
        this.parameters = new ArrayList<Object>();
    }

    public String getQuery() {
        return query;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public DatabaseQuery withQuery (String query) {
        this.query = query;
        return this;
    }

    public DatabaseQuery withParameters (List<Object> parameters) {
        this.parameters.addAll(parameters);
        return this;
    }

    public DatabaseQuery withCountQuery (String query, List<Object> parameters) {
        this.countQuery = query;
        this.countParameters = parameters;
        return this;
    }

    public int getTotal () {
        if (total == null) {
            try
            {
                total = DatabaseManager.querySingle(context, countQuery, countParameters).getIntColumn("count");
            }
            catch (SQLException e1)
            {
                log.error("Unable to retrieve number of items that match");
                total = -1;
            }
        }
        return total;
    }
}
