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

@Component
public class WebSecurityExpressionEvaluator {

    private static final FilterChain EMPTY_CHAIN = (request, response) -> {
        throw new UnsupportedOperationException();
    };

    private final List<SecurityExpressionHandler> securityExpressionHandlers;

    public WebSecurityExpressionEvaluator(List<SecurityExpressionHandler> securityExpressionHandlers) {
        this.securityExpressionHandlers = securityExpressionHandlers;
    }

    public boolean evaluate(String securityExpression, HttpServletRequest request, HttpServletResponse response) {
        SecurityExpressionHandler handler = getFilterSecurityHandler();

        Expression expression = handler.getExpressionParser().parseExpression(securityExpression);

        EvaluationContext evaluationContext = createEvaluationContext(handler, request, response);

        return ExpressionUtils.evaluateAsBoolean(expression, evaluationContext);
    }

    @SuppressWarnings("unchecked")
    private EvaluationContext createEvaluationContext(SecurityExpressionHandler handler, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        FilterInvocation filterInvocation = new FilterInvocation(request, response, EMPTY_CHAIN);

        return handler.createEvaluationContext(authentication, filterInvocation);
    }

    private SecurityExpressionHandler getFilterSecurityHandler() {
        return securityExpressionHandlers.stream()
                                         .filter(handler -> FilterInvocation.class.equals(GenericTypeResolver
                                                                                              .resolveTypeArgument(handler.getClass(), SecurityExpressionHandler.class)))
                                         .findAny()
                                         .orElseThrow(() -> new IllegalStateException("No filter invocation security expression handler has been found! Handlers: " + securityExpressionHandlers.size()));
    }
}