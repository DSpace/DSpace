/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Converts domain objects from the DSpace service layer to rest objects, and from rest objects to resource
 * objects, applying {@link Projection}s where applicable.
 */
@Service
public class ConverterService {

    private static final Logger log = Logger.getLogger(ConverterService.class);

    private final Map<String, Projection> projectionMap = new HashMap<>();

    private final Map<Class, DSpaceConverter> converterMap = new HashMap<>();

    private final Map<Class<? extends RestModel>, Constructor> resourceConstructors = new HashMap<>();

    @Autowired
    private Utils utils;

    @Autowired
    private HalLinkService halLinkService;

    @Autowired
    private List<DSpaceConverter> converters;

    @Autowired
    private List<Projection> projections;

    /**
     * Converts the given model object to a rest object, using the appropriate {@link DSpaceConverter} and
     * the given projection.
     * <p>
     * The projection's {@link Projection#transformModel(Object)} method will be automatically applied
     * before conversion. If the rest object is a {@link RestModel}, the projection's
     * {@link Projection#transformRest(RestModel)} method will be automatically called after conversion.
     * </p>
     *
     * @param modelObject the model object, which may be a JPA entity any other class for which a converter exists.
     * @param projection the projection to use.
     * @param <M> the type of model object. A converter {@link Component} must exist that takes this as input.
     * @param <R> the inferred return type.
     * @return the converted object. If it's a {@link RestAddressableModel}, its
     *         {@link RestAddressableModel#getProjection()} will be set to the given projection.
     * @throws IllegalArgumentException if there is no compatible converter.
     * @throws ClassCastException if the converter's return type is not compatible with the inferred return type.
     */
    public <M, R> R toRest(M modelObject, Projection projection) {
        M transformedModel = projection.transformModel(modelObject);
        DSpaceConverter<M, R> converter = requireConverter(modelObject.getClass());
        R restObject = converter.convert(transformedModel, projection);
        if (restObject instanceof RestModel) {
            return (R) projection.transformRest((RestModel) restObject);
        }
        return restObject;
    }

    /**
     * Converts a list of model objects to a page of rest objects using the given {@link Projection}.
     *
     * @param modelObjects the list of model objects.
     * @param pageable the pageable.
     * @param total the total number of items.
     * @param projection the projection to use.
     * @param <M> the model object class.
     * @param <R> the rest object class.
     * @return the page.
     * @throws IllegalArgumentException if there is no compatible converter.
     * @throws ClassCastException if the converter's return type is not compatible with the inferred return type.
     */
    public <M, R> Page<R> toRestPage(List<M> modelObjects, Pageable pageable, long total, Projection projection) {
        return new PageImpl<>(modelObjects, pageable, total).map((object) -> toRest(object, projection));
    }

    /**
     * Converts a list of model objects to a page of rest objects using the given {@link Projection}.
     *
     * @param modelObjects the page of model objects.
     * @param projection the projection to use.
     * @param <M> the model object class.
     * @param <R> the rest object class.
     * @return the page.
     * @throws IllegalArgumentException if there is no compatible converter.
     * @throws ClassCastException if the converter's return type is not compatible with the inferred return type.
     */
    public <M, R> Page<R> toRestPage(Page<M> modelObjects, Projection projection) {
        return modelObjects.map((object) -> toRest(object, projection));
    }

    /**
     * Gets the converter supporting the given class as input.
     *
     * @param sourceClass the desired converter's input type.
     * @param <M> the converter's input type.
     * @param <R> the converter's output type.
     * @return the converter.
     * @throws IllegalArgumentException if there is no such converter.
     */
    <M, R> DSpaceConverter<M, R> getConverter(Class<M> sourceClass) {
        return (DSpaceConverter<M, R>) requireConverter(sourceClass);
    }

    /**
     * Converts the given rest object to a {@link HALResource} object.
     * <p>
     * If the rest object is a {@link RestAddressableModel}, the projection returned by
     * {@link RestAddressableModel#getProjection()} will be used to determine which optional
     * embeds and links will be added, and {@link Projection#transformResource(HALResource)}
     * will be automatically called before returning the final, fully converted resource.
     * </p><p>
     * In all cases, the {@link HalLinkService} will be used immediately after the resource is constructed,
     * to ensure all {@link HalLinkFactory}s have had a chance to add links as needed.
     * </p>
     *
     * @param restObject the input rest object.
     * @param <T> the return type, a subclass of {@link HALResource}.
     * @return the fully converted resource, with all automatic links and embeds applied.
     * @throws IllegalArgumentException if there is no compatible resource constructor.
     * @throws ClassCastException if the resource type is not compatible with the inferred return type.
     */
    public <T extends HALResource> T toResource(RestModel restObject) {
        T halResource = getResource(restObject);
        if (restObject instanceof RestAddressableModel) {
            utils.embedOrLinkClassLevelRels(halResource);
            halLinkService.addLinks(halResource);
            Projection projection = ((RestAddressableModel) restObject).getProjection();
            return projection.transformResource(halResource);
        } else {
            halLinkService.addLinks(halResource);
        }
        return halResource;
    }

    /**
     * Gets the projection with the given name, or the default (no-op) projection if null is given.
     *
     * @param projectionName the projection name, or {@code null}.
     * @return the projection with the given name, or {@link DefaultProjection} if {@code null} is given.
     * @throws IllegalArgumentException if a name is provided and such a projection cannot be found.
     */
    public Projection getProjection(@Nullable String projectionName) {
        return projectionName == null ? Projection.DEFAULT : requireProjection(projectionName);
    }

    /**
     * Creates and returns an instance of the appropriate {@link HALResource} subclass for the given rest object.
     * <p>
     * <b>Note:</b> Only two forms of constructor are supported for resources that can be created with this method:
     * A one-argument constructor taking the wrapped {@link RestModel}, and a two-argument constructor also taking
     * a {@link Utils} instance. If both are found in a candidate resource's constructor, the two-argument form
     * will be used.
     * </p>
     *
     * @param restObject the rest object to wrap.
     * @param <T> the return type, a subclass of {@link HALResource}.
     * @return a new resource instance of the appropriate type.
     */
    private <T extends HALResource> T getResource(RestModel restObject) {
        Constructor constructor = resourceConstructors.get(restObject.getClass());
        if (constructor == null) {
            throw new IllegalArgumentException("No constructor found to get resource class from " + restObject);
        }
        try {
            if (constructor.getParameterCount() == 2) {
                return (T) constructor.newInstance(restObject, utils);
            } else {
                return (T) constructor.newInstance(restObject);
            }
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            }
            throw new RuntimeException(e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the projection with the given name or throws an {@link IllegalArgumentException}.
     *
     * @param projectionName the projection name.
     * @return the projection.
     * @throws IllegalArgumentException if not found.
     */
    private Projection requireProjection(String projectionName) {
        if (!projectionMap.containsKey(projectionName)) {
            throw new IllegalArgumentException("No such projection: " + projectionName);
        }
        return projectionMap.get(projectionName);
    }

    /**
     * Gets the converter that supports the given source/input class or throws an {@link IllegalArgumentException}.
     *
     * @param sourceClass the source/input class.
     * @return the converter.
     * @throws IllegalArgumentException if not found.
     */
    private DSpaceConverter requireConverter(Class sourceClass) {
        if (converterMap.containsKey(sourceClass)) {
            return converterMap.get(sourceClass);
        }
        for (Class converterSourceClass : converterMap.keySet()) {
            if (converterSourceClass.isAssignableFrom(sourceClass)) {
                return converterMap.get(converterSourceClass);
            }
        }
        throw new IllegalArgumentException("No converter found to get rest class from " + sourceClass);
    }

    /**
     * Populates maps of injected components and constructors to be used by this service's public methods.
     */
    @PostConstruct
    private void initialize() {
        // put all available projections in a map keyed by name
        for (Projection projection : projections) {
            projectionMap.put(projection.getName(), projection);
        }
        projectionMap.put(Projection.DEFAULT.getName(), Projection.DEFAULT);

        // put all available converters in a map keyed by model (input) class
        for (DSpaceConverter converter : converters) {
            converterMap.put(converter.getModelClass(), converter);
        }

        // scan all resource classes and look for compatible rest classes (by naming convention),
        // creating a map of resource constructors keyed by rest class, for later use.
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AssignableTypeFilter(Resource.class));
        Set<BeanDefinition> beanDefinitions = provider.findCandidateComponents(
                HALResource.class.getPackage().getName().replaceAll("\\.", "/"));
        for (BeanDefinition beanDefinition : beanDefinitions) {
            String resourceClassName = beanDefinition.getBeanClassName();
            String resourceClassSimpleName = resourceClassName.substring(resourceClassName.lastIndexOf(".") + 1);
            String restClassSimpleName = resourceClassSimpleName
                    .replaceAll("ResourceWrapper$", "RestWrapper")
                    .replaceAll("Resource$", "Rest");
            String restClassName = RestModel.class.getPackage().getName() + "." + restClassSimpleName;
            try {
                Class<? extends RestModel> restClass =
                        (Class<? extends RestModel>) Class.forName(restClassName);
                Class<HALResource<? extends RestModel>> resourceClass =
                        (Class<HALResource<? extends RestModel>>) Class.forName(resourceClassName);
                Constructor compatibleConstructor = null;
                for (Constructor constructor : resourceClass.getDeclaredConstructors()) {
                    if (constructor.getParameterCount() == 2 && constructor.getParameterTypes()[1] == Utils.class) {
                        compatibleConstructor = constructor;
                        break; // found preferred constructor
                    } else if (constructor.getParameterCount() == 1) {
                        compatibleConstructor = constructor;
                    }
                }
                if (compatibleConstructor != null) {
                    resourceConstructors.put(restClass, compatibleConstructor);
                } else {
                    log.warn("Skipping registration of resource class " + resourceClassName
                                    + "; compatible constructor not found");
                }
            } catch (ClassNotFoundException e) {
                log.warn("Skipping registration of resource class " + resourceClassName
                        + "; rest class not found: " + restClassName);
            }
        }
    }
}
