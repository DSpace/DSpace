/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.utils.servicemanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.dspace.kernel.ServiceManager;
import org.dspace.kernel.mixins.OrderedService;


/**
 * This class stores a list of providers in a specific order 
 * (determined by insertion order and the {@link OrderedService} 
 * settings).
 * <p>
 * Should be thread safe.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class ProviderStack<T> {

    protected final List<ProviderHolder<T>> providers;

    /**
     * Default empty constructor.
     * <p>
     * This is mostly only useful if you are planning to add new 
     * providers later.  You should probably use the other contructors.
     */
    public ProviderStack() {
        providers = Collections.synchronizedList(new ArrayList<ProviderHolder<T>>());
    }

    /**
     * Construct a provider holder with all currently known providers of 
     * the given type.
     *
     * @param serviceManager the system service manager
     * @param providerType the interface type of the providers we care about
     */
    public ProviderStack(ServiceManager serviceManager, Class<T> providerType) {
        providers = Collections.synchronizedList(new ArrayList<ProviderHolder<T>>());
        List<T> foundProviders = serviceManager.getServicesByType(providerType);
        // filter out the NotProviders first
        for (Iterator<T> iterator = foundProviders.iterator(); iterator.hasNext();) {
            T t = iterator.next();
            if (t instanceof NotProvider) {
                iterator.remove();
            }
        }
        Collections.sort(foundProviders, new OrderedServiceComparator());
        for (T t : foundProviders) {
            providers.add( new ProviderHolder<T>(t) );
        }
    }

    /**
     * Construct a provider holder with a given set of providers.
     * This will maintain the current order as long as it does not
     * violate the rules for {@link OrderedService}s.
     *
     * @param currentProviders the current set of providers to register in this stack
     */
    public ProviderStack(T[] currentProviders) {
        providers = Collections.synchronizedList(new ArrayList<ProviderHolder<T>>());
        ArrayList<T> tList = new ArrayList<T>();
        // first add in the ordered ones
        for (int i = 0; i < currentProviders.length; i++) {
            T t = currentProviders[i];
            if (t instanceof NotProvider) {
                continue; // skip all the NotProviders
            }
            if (t instanceof OrderedService) {
                tList.add( t );
            }
        }
        // sort the ordered ones
        Collections.sort(tList, new OrderedServiceComparator());
        // now add in the rest in the order given
        for (int i = 0; i < currentProviders.length; i++) {
            T t = currentProviders[i];
            if (! (t instanceof OrderedService)) {
                if (t instanceof NotProvider) {
                    continue; // skip all the NotProviders
                }
                tList.add( t );
            }
        }
        // now put these into holders
        for (T t : tList) {
            providers.add( new ProviderHolder<T>(t) );
        }
        tList.clear();
    }

    /**
     * Add a provider to the stack of providers.
     * This will be placed at the bottom if the order is less than or
     * equal to 0 or this provider is not ordered.
     * 
     * @param provider the provider to add to the stack
     * @return the position in the stack that this provider was added
     */
    public int addProvider(T provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider to add cannot be null");
        }
        int position = 0;
        refresh();
        int providerOrder = 0;
        if (provider instanceof NotProvider) {
            throw new IllegalArgumentException("Cannot place anything that implements NotProvider into the provider stack, failure for: " + provider);
        }
        if (provider instanceof OrderedService) {
            providerOrder = ((OrderedService)provider).getOrder();
        }
        // place at the bottom of the stack
        providers.add( new ProviderHolder<T>(provider) );
        if (providerOrder > 0) {
            // re-sort the providers
            Collections.sort(this.providers, new ProviderStackComparator());
        }
        return position;
    }

    /**
     * Remove a provider based on the position in the stack (starting at 
     * 0 and ending at size-1).
     * @param position the position to remove the provider from
     * @return true if the provider position was found and removed OR false if not found
     */
    public boolean removeProvider(final int position) {
        boolean removed = false;
        try {
            this.providers.remove(position);
            removed = true;
        } catch (IndexOutOfBoundsException e) {
            removed = false;
        }
        refresh();
        return removed;
    }

    /**
     * Remove a provider by the object equality.
     * @param provider the provider to remove from the stack
     * @return true if the provider was found and removed OR false if not found
     */
    public boolean removeProvider(T provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider to remove cannot be null");
        }
        boolean removed = false;
        refresh();
        for (Iterator<ProviderHolder<T>> iterator = providers.iterator(); iterator.hasNext();) {
            ProviderHolder<T> holder = iterator.next();
            T p = holder.getProvider();
            if (p == null) {
                iterator.remove();
            } else {
                if (p.equals(provider)) {
                    iterator.remove();
                    removed = true;
                }
            }
        }
        return removed;
    }

    /**
     * Get the list of all providers currently in the stack.
     * <p>
     * WARNING: this should not be held onto; it should be only used for
     * iteration and then discarded.
     * @return a read-only list of all providers in the stack in the correct order, highest priority first
     */
    public List<T> getProviders() {
        List<T> l = refresh();
        return Collections.unmodifiableList(l);
    }

    /**
     * This allows access to the provider holders.
     * @return a read only list of the provider holders in the correct order
     */
    public List<ProviderHolder<T>> getProviderHolders() {
        return Collections.unmodifiableList(providers);
    }

    /**
     * Get the iterator of all providers currently in the stack.
     * This will load up lazy so it can be held onto safely but should not be
     * used more than once (i.e. only iterate over this completely once:
     * stopping and continuing later is likely to produce failures).
     * <p>
     * NOTE: This will attempt to iterate over all the valid providers 
     * in the stack, but if the provider has been garbage collected
     * during the iteration then a {@link NoSuchElementException} will 
     * be thrown if the provider was the last one in the stack and 
     * next() is called.  You should probably handle the exception by
     * assuming this indicates all items were iterated over.
     * @return an iterator over all the providers in the stack in order of priority
     */
    public Iterator<T> getIterator() {
        return new Iterator<T>() {
            protected ListIterator<ProviderHolder<T>> it = null;
            public synchronized boolean hasNext() {
                if (it == null) {
                    it = providers.listIterator();
                }
                return it.hasNext();
            }
            public synchronized T next() {
                if (it == null) {
                    it = providers.listIterator();
                }
                /* get the next provider if it is not null, otherwise keep going until we find a non-null one 
                 * or just return null if there are none left
                 */
                T t = null;
                while (it.hasNext()) {
                    ProviderHolder<T> holder = it.next();
                    t = holder.getProvider();
                    if (t != null) {
                        break;
                    }
                }
                if (t == null) {
                    throw new NoSuchElementException("No more providers remain with valid weak references");
                }
                return t;
            }
            public void remove() {
                it.remove();
            }
        };
    }

    /**
     * Get a provider based on the position in the stack (starting at 0 
     * and ending at size-1).
     *
     * @param position the position to check for the provider
     * @return the provider from the position OR null if there is no provider at that position
     */
    public T getProvider(final int position) {
        T provider = null;
        ProviderHolder<T> holder = getProviderHolder(position);
        if (holder != null) {
            provider = holder.getProvider();
        }
        return provider;
    }

    /**
     * Get the provider holder from the position in the stack if there 
     * is one.
     *
     * @param position the position to check for the provider
     * @return the holder from the position OR null if there is no provider at that position
     */
    public ProviderHolder<T> getProviderHolder(final int position) {
        refresh();
        ProviderHolder<T> holder;
        try {
            holder = providers.get(position);
        } catch (IndexOutOfBoundsException e) {
            // no provider at that position
            holder = null;
        }
        return holder;
    }

    /**
     * Check the number of current providers which are available in this 
     * stack.
     *
     * @return the total number of viable providers
     */
    public int size() {
        refresh();
        return providers.size();
    }

    /**
     * Delete all providers from the current provider stack.
     */
    public void clear() {
        providers.clear();
    }

    /**
     * Check to make sure all providers are refreshed and any that are 
     * no longer valid are flushed out of the list.
     * @return list of valid providers
     */
    protected List<T> refresh() {
        ArrayList<T> l = new ArrayList<T>();
        for (Iterator<ProviderHolder<T>> iterator = providers.iterator(); iterator.hasNext();) {
            ProviderHolder<T> holder = iterator.next();
            T provider = holder.getProvider();
            if (provider == null) {
                iterator.remove();
            } else {
                l.add(provider);
            }
        }
        return l;
    }

}
