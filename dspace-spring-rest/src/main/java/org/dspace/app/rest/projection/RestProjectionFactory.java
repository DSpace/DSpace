package org.dspace.app.rest.projection;

import org.dspace.content.Item;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * TODO TOM UNIT TEST
 */
public class RestProjectionFactory<T> implements MethodInterceptor {

    private T delegate;
    private Class<?> projection;

    public RestProjectionFactory(T delegate, final Class<?> projection) {
        this.delegate = delegate;
        this.projection = projection;
    }

    public static <E> E createProjection(E wrapped, Class<?> projection) {
        return (E) Enhancer.create(wrapped.getClass(), new RestProjectionFactory(wrapped, projection));
    }

    public Object intercept(final Object o, final Method method, final Object[] objects, final MethodProxy methodProxy) throws Throwable {
        Method m = findMethod(projection, method);
        if (m != null) {
            return method.invoke(delegate, objects);
        }
        return null;
    }

    private Method findMethod(Class<?> clazz, Method method) throws Throwable {
        try {
            return clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
