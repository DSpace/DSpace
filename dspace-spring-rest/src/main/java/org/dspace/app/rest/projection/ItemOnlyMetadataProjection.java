package org.dspace.app.rest.projection;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

/**
 * TODO TOM UNIT TEST
 */
public class ItemOnlyMetadataProjection extends Item implements MethodInterceptor {

    private Item delegate;

    public ItemOnlyMetadataProjection(Item delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<MetadataValue> getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public boolean isArchived() {
        return delegate.isArchived();
    }

    @Override
    public boolean isWithdrawn() {
        return delegate.isWithdrawn();
    }

    @Override
    public boolean isDiscoverable() {
        return delegate.isDiscoverable();
    }

    @Override
    public Date getLastModified() {
        return delegate.getLastModified();
    }

    public static Item wrap(Item wrapped) {
        return (Item) Enhancer.create(Item.class, new ItemOnlyMetadataProjection(wrapped));
    }

    public Object intercept(final Object o, final Method method, final Object[] objects, final MethodProxy methodProxy) throws Throwable {
        Method m = findMethod(this.getClass(), method);
        if (m != null) {
            return m.invoke(this, objects);
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
