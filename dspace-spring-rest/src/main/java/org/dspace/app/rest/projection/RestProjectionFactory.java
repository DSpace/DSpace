/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * Factory that can create projections of objects that are being converted to a REST model
 */
public class RestProjectionFactory<T> implements MethodInterceptor {

    private T delegate;
    private Class<?> projection;

    private RestProjectionFactory(T delegate, final Class<?> projection) {
        this.delegate = delegate;
        this.projection = projection;
    }

    public static <E> E createProjection(E wrapped, Class<?> projection) {
        return (E) Enhancer.create(wrapped.getClass(), new RestProjectionFactory(wrapped, projection));
    }

    @Override
    public Object intercept(final Object o, final Method method, final Object[] objects, final MethodProxy methodProxy) throws Throwable {
        Method m = findMethod(projection, method);
        if (m != null) {
            return method.invoke(delegate, objects);
        }
        return null;
    }

    private Method findMethod(Class<?> clazz, Method method) {
        try {
            return clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
