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
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Component;

/**
 * Service to convert domain objects from the service layer to rest and resource form on the way out of DSpace.
 */
@Component
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

    @PostConstruct
    private void initialize() {
        for (Projection projection : projections) {
            projectionMap.put(projection.getName(), projection);
        }
        projectionMap.put(Projection.DEFAULT.getName(), Projection.DEFAULT);

        for (DSpaceConverter converter : converters) {
            converterMap.put(converter.getModelClass(), converter);
        }

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
                    logSkipping(resourceClassName, "compatible constructor not found");
                }
            } catch (ClassNotFoundException e) {
                logSkipping(resourceClassName, "rest class not found: " + restClassName);
            }
        }
    }

    private void logSkipping(String resourceClassName, String reason) {
        log.warn("Skipping registration of resource class " + resourceClassName + "; " + reason);
    }

    private Projection requireProjection(String projectionName) {
        if (!projectionMap.containsKey(projectionName)) {
            throw new IllegalArgumentException("No such projection: " + projectionName);
        }
        return projectionMap.get(projectionName);
    }

    public <T> T toRest(Object modelObject) {
        return toRest(modelObject, null);
    }

    public <M, R> R toRest(M modelObject, @Nullable String projectionName) {
        Projection projection = projectionName == null ? Projection.DEFAULT : requireProjection(projectionName);
        M transformedModel = projection.transformModel(modelObject);
        DSpaceConverter<M, R> converter = requireConverter(modelObject.getClass());
        R restObject = converter.convert(transformedModel);
        if (restObject instanceof RestAddressableModel) {
            RestAddressableModel ram = projection.transformRest((RestAddressableModel) restObject);
            ram.setProjection(projection);
            return (R) ram;
        }
        return restObject;
    }

    public <M, R> DSpaceConverter<M, R> getConverter(Class<M> sourceClass) {
        return (DSpaceConverter<M, R>) requireConverter(sourceClass);
    }

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

    public <T extends HALResource> T toResource(RestModel restObject, @Nullable String projectionName) {
        if (restObject instanceof RestAddressableModel) {
            ((RestAddressableModel) restObject).setProjection(requireProjection(projectionName));
        }
        return toResource(restObject);
    }

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

    private <T extends HALResource> T getResource(RestModel restObject) {
        Constructor constructor = resourceConstructors.get(restObject.getClass());
        try {
            if (constructor == null) {
                constructor = DSpaceResource.class.getDeclaredConstructor();
            }
            if (constructor.getParameterCount() == 2) {
                return (T) constructor.newInstance(restObject, utils);
            } else {
                return (T) constructor.newInstance(restObject);
            }
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
