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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.dspace.content.Item;

/**
 * Builder for accumulating a database query in fluent style.
 */
public class DatabaseQuery {
    private static final Logger log = Logger.getLogger(DatabaseQuery.class);

    private String query;
    private final List<Object> parameters;
    private final Context context;
    private Integer total;

    private String countQuery;
    private List<Object> countParameters;

    /**
     * Create a null query, to be filled in.
     *
     * @param context
     */
    public DatabaseQuery(Context context) {
        this.context = context;
        this.parameters = new ArrayList<>();
    }

    public String getQuery() {
        return query;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    /**
     * Set the query string to be used.
     *
     * @param query
     * @return a reference to this object.
     */
    public DatabaseQuery withQuery (String query) {
        this.query = query;
        return this;
    }

    /**
     * Add some parameters to the query parameter list.  Repeated calls accumulate.
     *
     * @param parameters
     * @return a reference to this object.
     */
    public DatabaseQuery withParameters (List<Object> parameters) {
        this.parameters.addAll(parameters);
        return this;
    }

    /**
     * Set the "count query" and its parameter list.
     *
     * @param query "count query", distinct from general query.  FIXME would
     *              someone please explain the difference.
     * @param parameters entire parameter list for {@code query}.
     * @return a reference to this object.
     */
    public DatabaseQuery withCountQuery (String query, List<Object> parameters) {
        this.countQuery = query;
        this.countParameters = parameters;
        return this;
    }

    public List<Item> execute()
    {
        // TODO run the query somehow
    }

    /**
     * Execute the "count query", which must return a single Integer result.
     *
     * @return the result of the count query.
     */
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
