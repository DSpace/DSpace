package org.dspace.app.rest.spel;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.BeanReference;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.Operator;
import org.springframework.expression.spel.ast.TypeReference;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ExpressionValidator {
    private final ExpressionParser parser = new SpelExpressionParser();

    public void validate(String expression, Class<?> expressionRoot) throws ExpressionValidationException, ParseException {
        SpelExpression exp = (SpelExpression) parser.parseExpression(expression);
        if (expressionRoot != null) {
            SpelNode node = exp.getAST();
            handle(node, expressionRoot);
        }
    }

    private void handle(SpelNode node, Class<?> expressionRoot) throws ExpressionValidationException{
        if (node instanceof MethodReference) {
            verify((MethodReference) node, expressionRoot);
        } else if (node instanceof Operator) {
            Operator operator = (Operator) node;
            handle(operator.getLeftOperand(), expressionRoot);
            handle(operator.getRightOperand(), expressionRoot);
        } else if (node != null) {
            for(int i=0; i<node.getChildCount(); i++) {
                SpelNode child = node.getChild(i);
                if (child instanceof VariableReference ||
                    child instanceof TypeReference ||
                    child instanceof BeanReference) {
                    // stop inspecting if it's a variable or type reference.
                    // We can handle this later if we get smart about resolving these
                    break;
                }
                handle(child, expressionRoot);
            }
        }
    }

    private void verify(MethodReference node, Class<?> expressionRoot) throws ExpressionValidationException {
        String methodName = node.getName();
        int args = node.getChildCount();
        Method[] methods = expressionRoot.getDeclaredMethods();
        for(Method m : methods) {
            if (m.getName().equals(methodName)) {
                // exact match on the args
                if (args == m.getParameterCount()) {
                    try {
                        SecurityExpressionRoot.class.getConstructor(Authentication.class).newInstance(
                            SecurityContextHolder.getContext().getAuthentication());
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                Class<?>[] parameterTypes = m.getParameterTypes();
                if (m.getName().equals(methodName) &&
                    parameterTypes != null &&
                    parameterTypes.length>=1 &&
                    parameterTypes[parameterTypes.length-1].isArray()) {
                    // allow the number of params to be one less or >= the reported length
                    if(args == m.getParameterCount()-1 || args >= m.getParameterCount()) {
                        return;
                    }
                }
            }
        }
        // if we get here, then we were unable to match the method call
        String pattern = "Unable to match method %s with %d params";
        throw new ExpressionValidationException(String.format(pattern, methodName, args));
    }

}
