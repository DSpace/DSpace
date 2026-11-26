/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.parameter.resolver;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.query.RestSearchOperator;
import org.dspace.app.rest.parameter.SearchFilter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Utility class for resolving {@link SearchFilter}s from request parameters
 */
public class SearchFilterResolverUtil {
    public static final String SEARCH_FILTER_PREFIX = "f.";
    public static final String FILTER_OPERATOR_SEPARATOR = ",";

    public static final List<String> ALLOWED_SEARCH_OPERATORS =
        RestSearchOperator.getListOfAllowedSearchOperatorStrings();

    private SearchFilterResolverUtil() {
    }

    public static List<SearchFilter> resolveSearchFilters(NativeWebRequest webRequest) {
        return resolveSearchFilters(webRequest.getNativeRequest(HttpServletRequest.class));
    }

    public static List<SearchFilter> resolveSearchFilters(HttpServletRequest webRequest) {
        List<SearchFilter> result = new LinkedList<>();

        Enumeration<String> parameterNames = webRequest.getParameterNames();
        while (parameterNames != null && parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();

            if (parameterName.startsWith(SEARCH_FILTER_PREFIX)) {
                String filterName = StringUtils.substringAfter(parameterName, SEARCH_FILTER_PREFIX);

                for (String value : webRequest.getParameterValues(parameterName)) {
                    String filterValue = StringUtils.substringBeforeLast(value, FILTER_OPERATOR_SEPARATOR);
                    String filterOperator = StringUtils.substringAfterLast(value, FILTER_OPERATOR_SEPARATOR);
                    checkIfValidOperator(filterOperator);
                    result.add(new SearchFilter(filterName, filterOperator, filterValue));
                }
            }
        }

        return result;
    }

    private static void checkIfValidOperator(String filterOperator) {
        if (StringUtils.isNotBlank(filterOperator)) {
            if (!ALLOWED_SEARCH_OPERATORS.contains(filterOperator.trim())) {
                throw new UnprocessableEntityException(
                    "The operator can't be \"" + filterOperator + "\", must be the of one of: " +
                        String.join(", ", ALLOWED_SEARCH_OPERATORS));
            }
        } else {
            throw new UnprocessableEntityException(
                "The operator can't be empty, must be the one of: " + String.join(", ", ALLOWED_SEARCH_OPERATORS));
        }
    }
}
