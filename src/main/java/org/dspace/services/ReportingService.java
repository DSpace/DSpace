package org.dspace.services;

import java.util.List;
import java.util.Map;

import org.dspace.services.model.ObjectCount;

/**
 * @author mdiggory
 * 
 */
public interface ReportingService {
	/**
	 * @param query
	 * @param max
	 * @throws SolrServerException
	 */
	public void query(String query, int max);

	/**
	 * Query used to get values grouped by the date
	 * 
	 * @param query
	 *            the query to be used
	 * @param filterQuery
	 * @param max
	 *            the max number of values given back (in case of 10 the top 10
	 *            will be given)
	 * @param dateType
	 *            the type to be used (example: DAY, MONTH, YEAR)
	 * @param dateStart
	 *            the start date Format:(-3, -2, ..) the date is calculated
	 *            relatively on today
	 * @param dateEnd
	 *            the end date stop Format (-2, +1, ..) the date is calculated
	 *            relatively on today
	 * @param showTotal
	 *            a boolean determening whether the total amount should be given
	 *            back as the last element of the array
	 * @return and array containing our results @ * ...
	 */
	public ObjectCount[] queryFacetDate(String query, String filterQuery,
			int max, String dateType, String dateStart, String dateEnd,
			boolean showTotal);

	/**
	 * Query used to get values grouped by the given facetfield
	 * 
	 * @param query
	 *            the query to be used
	 * @param filterQuery
	 * @param facetField
	 *            the facet field on which to group our values
	 * @param max
	 *            the max number of values given back (in case of 10 the top 10
	 *            will be given)
	 * @param showTotal
	 *            a boolean determening whether the total amount should be given
	 *            back as the last element of the array
	 * @param facetQueries
	 * @return an array containing our results @ * ...
	 */
	public ObjectCount[] queryFacetField(String query, String filterQuery,
			String facetField, int max, boolean showTotal,
			List<String> facetQueries);

	/**
	 * @param query
	 * @param filterQuery
	 * @param facetQueries
	 * @return @
	 */
	public Map<String, Integer> queryFacetQuery(String query,
			String filterQuery, List<String> facetQueries);

	/**
	 * @param query
	 * @param oldFieldVals
	 * @param field
	 * @return
	 */
	public Map<String, List<String>> queryField(String query,
			List oldFieldVals, String field);

	/**
	 * @param query
	 * @param filterQuery
	 * @return @
	 */
	public ObjectCount queryTotal(String query, String filterQuery);

}
