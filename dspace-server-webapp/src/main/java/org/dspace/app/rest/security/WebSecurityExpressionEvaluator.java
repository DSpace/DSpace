/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.GenericTypeResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;
import org.springframework.stereotype.Component;

/**
 * This class will contain the logic to allow us to evaluate an expression given through a String.
 * This will be used by the {@link org.dspace.app.rest.converter.ConverterService} for parsing
 * the {@link org.springframework.security.access.prepost.PreAuthorize} annotations used on the findOne
 * methods of RestRepositories. A String will be given to the evaluate method and that String will then
 * be parsed and a boolean will be returned based on the condition in the String.
 * For example: "hasPermission(#id, 'ITEM', 'READ')" is such a String
 * This will be evaluated and if the current user has the permission to read an item with the given id,
 * a true will be returned, if not it'll be false.
 * This works on all the methods in {@link org.springframework.security.access.expression.SecurityExpressionRoot}
 */
@Component
public class WebSecurityExpressionEvaluator {

    private static final FilterChain EMPTY_CHAIN = (request, response) -> {
        throw new UnsupportedOperationException();
    };

    private final List<SecurityExpressionHandler> securityExpressionHandlers;

    /**
     * Constructor for this class that sets all the {@link SecurityExpressionHandler} objects in a list
     * @param securityExpressionHandlers    The {@link SecurityExpressionHandler} for this class
     */
    public WebSecurityExpressionEvaluator(List<SecurityExpressionHandler> securityExpressionHandlers) {
        this.securityExpressionHandlers = securityExpressionHandlers;
    }

    /**
     * This method will have to be used to evaluate the String given. It'll parse the String and resolve
     * it to a method in {@link org.springframework.security.access.expression.SecurityExpressionRoot}
     * and evaluate it to then return a boolean
     * @param securityExpression    The String that resembles the expression that has to be parsed
     * @param request               The current request
     * @param response              The current response
     * @param id                    The id for the Object that is the subject of the permission
     * @return                      A boolean indicating whether the currentUser adheres to the
     *                              permissions in the securityExpression String or not
     */
    public boolean evaluate(String securityExpression, HttpServletRequest request, HttpServletResponse response,
                            String id) {
        SecurityExpressionHandler handler = getFilterSecurityHandler();

        Expression expression = handler.getExpressionParser().parseExpression(securityExpression);

        EvaluationContext evaluationContext = createEvaluationContext(handler, request, response);
        evaluationContext.setVariable("id", id);
        return ExpressionUtils.evaluateAsBoolean(expression, evaluationContext);
    }

    @SuppressWarnings("unchecked")
    private EvaluationContext createEvaluationContext(SecurityExpressionHandler handler, HttpServletRequest request,
                                                      HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        FilterInvocation filterInvocation = new FilterInvocation(request, response, EMPTY_CHAIN);

        return handler.createEvaluationContext(authentication, filterInvocation);
    }

    private SecurityExpressionHandler getFilterSecurityHandler() {
        return securityExpressionHandlers.stream()
                                         .filter(handler ->
                                                     FilterInvocation.class.equals(
                                                         GenericTypeResolver.resolveTypeArgument(handler.getClass(),
                                                                                     SecurityExpressionHandler.class)))
                                         .findAny()
                                         .orElseThrow(() -> new IllegalStateException("No filter invocation security" +
                                          " expression handler has been found! Handlers: " +
                                            securityExpressionHandlers.size()));
    }
}