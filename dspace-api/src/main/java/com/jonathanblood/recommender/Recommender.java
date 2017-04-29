package com.jonathanblood.recommender;


import org.apache.log4j.Logger;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.jdbc.PostgreSQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.dspace.storage.rdbms.DatabaseManager;

import javax.sql.DataSource;
import java.util.List;

/**
 * Created by Jon on 05/07/15.
 */
public class Recommender
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(Recommender.class);

    public Recommender()
    {

    }

    public List getRecommendations(int itemId, int numberOfRecs)
    {
        DataSource dataSource = DatabaseManager.getDataSource();
        DataModel model = new PostgreSQLJDBCDataModel(dataSource, "ratings",
                "eperson_id", "dspace_object_id", "rating", null);
        LogLikelihoodSimilarity similarity = null;
        try
        {
            similarity = new LogLikelihoodSimilarity(model);
            GenericItemBasedRecommender recommender = new GenericItemBasedRecommender(model, similarity);
            List recommendations = recommender.mostSimilarItems(itemId, numberOfRecs);
            return recommendations;
        } catch (TasteException e)
        {
            log.error("Could not calculate recommendations", e);
        }
        return null;
    }
}
