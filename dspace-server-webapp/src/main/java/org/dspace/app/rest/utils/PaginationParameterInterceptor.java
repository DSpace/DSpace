/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rejects invalid "page" and "size" request parameters on endpoints that accept a {@link Pageable}, as required by
 * the REST contract: "page" must be zero or greater and "size" must be greater than zero. Without this check, Spring
 * Data silently replaces invalid values (negative, zero or non-numeric) with the defaults and the request succeeds.
 */
public class PaginationParameterInterceptor implements HandlerInterceptor {

    public static final String PAGE_PARAMETER = "page";
    public static final String SIZE_PARAMETER = "size";

    /**
     * Check the "page" and "size" parameters if the handler method takes a {@link Pageable}.
     *
     * @throws DSpaceBadRequestException if "page" or "size" is not an integer or is below its minimum value
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (handler instanceof HandlerMethod handlerMethod && acceptsPageable(handlerMethod)) {
            checkParameter(request, PAGE_PARAMETER, 0);
            checkParameter(request, SIZE_PARAMETER, 1);
        }
        return true;
    }

    private boolean acceptsPageable(HandlerMethod handlerMethod) {
        return Arrays.stream(handlerMethod.getMethodParameters())
                     .anyMatch(parameter -> Pageable.class.isAssignableFrom(parameter.getParameterType()));
    }

    private void checkParameter(HttpServletRequest request, String name, int minimum) {
        String value = request.getParameter(name);
        if (StringUtils.isBlank(value)) {
            return;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new DSpaceBadRequestException("The '" + name + "' parameter must be an integer, but was: " + value);
        }
        if (parsed < minimum) {
            throw new DSpaceBadRequestException(
                "The '" + name + "' parameter must be " + minimum + " or greater, but was: " + value);
        }
    }
}
