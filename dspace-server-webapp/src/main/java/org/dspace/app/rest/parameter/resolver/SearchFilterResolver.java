/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.parameter.resolver;

import java.lang.reflect.ParameterizedType;
import java.util.List;

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

    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.getParameterType().equals(SearchFilter.class) || isSearchFilterList(parameter);
    }

    public Object resolveArgument(final MethodParameter parameter, final ModelAndViewContainer mavContainer,
                                  final NativeWebRequest webRequest, final WebDataBinderFactory binderFactory) {
        List<SearchFilter> result = SearchFilterResolverUtil.resolveSearchFilters(webRequest);

        if (parameter.getParameterType().equals(SearchFilter.class)) {
            return result.isEmpty() ? null : result.get(0);
        } else {
            return result;
        }
    }

    private boolean isSearchFilterList(final MethodParameter parameter) {
        return parameter.getParameterType().equals(List.class)
            && parameter.getGenericParameterType() instanceof ParameterizedType
            && ((ParameterizedType) parameter.getGenericParameterType()).getActualTypeArguments()[0]
            .equals(SearchFilter.class);
    }
}
