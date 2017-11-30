/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.utils.servicemanager;

import java.lang.ref.WeakReference;


/**
 * A holder which is designed to make it easy to hold onto a reference 
 * to a class which is outside of our ClassLoader and not cause it to 
 * not be able to reload happily.  The reference to the
 * object that is held onto here needs to not be the only one in the 
 * system or this object will be garbage collected before it probably 
 * should be.  That is generally outside the purview of the service 
 * manager system though.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class ProviderHolder<T> {

    private WeakReference<T> providerReference = null;

    /**
     * Default constructor.
     */
    public ProviderHolder() {}

    /**
     * Create the holder with a provider already in it.
     * @param provider the provider to place in the holder to start
     */
    public ProviderHolder(T provider) {
        setProvider(provider);
    }

    /**
     * Gets the provider out of this holder.
     * Use the {@link #getProviderOrFail()} method if you want a method
     * that never returns null.
     *
     * @return the provider if it is still available OR null if none is set or no longer available
     */
    public T getProvider() {
        T t = null;
        if (this.providerReference != null) {
            t = this.providerReference.get();
        }
        return t;
    }

    /**
     * Gets the provider out of this holder if it still exists.
     * Will not return null (unlike the {@link #getProvider()} method).
     *
     * @return the provider if it is available
     * @throws ProviderNotFoundException if there is none available
     */
    public T getProviderOrFail() {
        T t = getProvider();
        if (t == null) {
            throw new ProviderNotFoundException("Could not get provider from this holder, none available");
        }
        return t;
    }

    /**
     * Stores a provider in this holder in a ClassLoader safe way.
     * 
     * @param provider the provider to store.  If this is null then the current provider is cleared.
     */
    public void setProvider(T provider) {
        if (provider == null) {
            if (this.providerReference != null) {
                this.providerReference.clear();
            }
            this.providerReference = null;
        } else {
            this.providerReference = new WeakReference<T>(provider);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        T provider = getProvider();
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ProviderHolder<T> other = (ProviderHolder<T>) obj;
        T provider = getProvider();
        T otherProvider = other.getProvider();
        if (provider == null || otherProvider == null) {
            return false;
        } else if (provider.equals(otherProvider)) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        T provider = getProvider();
        return "ph:" + (provider == null ? null : provider.getClass() + ":" + provider) + ": " + super.toString();
    }
    
}
