package org.dspace.app.rest.parameter.resolver;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.parameter.SearchFilter;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Custom Request parameter resolver to fill in {@link org.dspace.app.rest.parameter.SearchFilter} parameter objects
 * TODO UNIT TEST
 */
public class SearchFilterResolver implements HandlerMethodArgumentResolver {

    public static final String SEARCH_FILTER_PREFIX = "f.";
    public static final String FILTER_OPERATOR_SEPARATOR = ",";

    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.getParameterType().equals(SearchFilter.class) ||
                (parameter.getParameterType().equals(List.class) && parameter.getNestedParameterType().equals(SearchFilter.class));
    }

    public Object resolveArgument(final MethodParameter parameter, final ModelAndViewContainer mavContainer, final NativeWebRequest webRequest, final WebDataBinderFactory binderFactory) throws Exception {
        List<SearchFilter> result = new LinkedList<>();

        Iterator<String> parameterNames = webRequest.getParameterNames();
        while (parameterNames != null && parameterNames.hasNext()) {
            String parameterName = parameterNames.next();
            
            if(parameterName.startsWith(SEARCH_FILTER_PREFIX)) {
                String filterName = StringUtils.substringAfter(parameterName, SEARCH_FILTER_PREFIX);

                for (String value : webRequest.getParameterValues(parameterName)) {
                    String filterValue = StringUtils.substringBeforeLast(value, FILTER_OPERATOR_SEPARATOR);
                    String filterOperator = StringUtils.substringAfterLast(value, FILTER_OPERATOR_SEPARATOR);

                    result.add(new SearchFilter(filterName, filterOperator, filterValue));
                }
            }
        }

        if(parameter.getParameterType().equals(SearchFilter.class)) {
            return result.isEmpty() ? null : result.get(0);
        } else {
            return result;
        }
    }
}
