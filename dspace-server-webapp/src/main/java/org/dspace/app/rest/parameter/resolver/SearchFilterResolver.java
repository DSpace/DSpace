/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.parameter.resolver;

import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.query.RestSearchOperator;
import org.dspace.app.rest.parameter.SearchFilter;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Custom Request parameter resolver to fill in {@link org.dspace.app.rest.parameter.SearchFilter} parameter objects
 * TODO UNIT TEST
 */
public class SearchFilterResolver implements HandlerMethodArgumentResolver {

    public static final String SEARCH_FILTER_PREFIX = "f.";
    public static final String FILTER_OPERATOR_SEPARATOR = ",";

    public static final List<String> ALLOWED_SEARCH_OPERATORS =
        RestSearchOperator.getListOfAllowedSearchOperatorStrings();

    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.getParameterType().equals(SearchFilter.class) || isSearchFilterList(parameter);
    }

    public Object resolveArgument(final MethodParameter parameter, final ModelAndViewContainer mavContainer,
                                  final NativeWebRequest webRequest, final WebDataBinderFactory binderFactory) {
        List<SearchFilter> result = new LinkedList<>();

        Iterator<String> parameterNames = webRequest.getParameterNames();
        while (parameterNames != null && parameterNames.hasNext()) {
            String parameterName = parameterNames.next();

            if (parameterName.startsWith(SEARCH_FILTER_PREFIX)) {
                String filterName = StringUtils.substringAfter(parameterName, SEARCH_FILTER_PREFIX);

                for (String value : webRequest.getParameterValues(parameterName)) {
                    String filterValue = StringUtils.substringBeforeLast(value, FILTER_OPERATOR_SEPARATOR);
                    String filterOperator = StringUtils.substringAfterLast(value, FILTER_OPERATOR_SEPARATOR);
                    this.checkIfValidOperator(filterOperator);
                    result.add(new SearchFilter(filterName, filterOperator, filterValue));
                }
            }
        }

        if (parameter.getParameterType().equals(SearchFilter.class)) {
            return result.isEmpty() ? null : result.get(0);
        } else {
            return result;
        }
    }

    private void checkIfValidOperator(String filterOperator) {
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

    private boolean isSearchFilterList(final MethodParameter parameter) {
        return parameter.getParameterType().equals(List.class)
            && parameter.getGenericParameterType() instanceof ParameterizedType
            && ((ParameterizedType) parameter.getGenericParameterType()).getActualTypeArguments()[0]
            .equals(SearchFilter.class);
    }
}
