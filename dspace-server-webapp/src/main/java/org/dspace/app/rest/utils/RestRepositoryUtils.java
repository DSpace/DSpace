/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.MissingParameterException;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.QueryMethodParameterConversionException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.core.AnnotationAttribute;
import org.springframework.hateoas.core.MethodParameters;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * Collection of utility methods to work with the Rest Repositories
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class RestRepositoryUtils {
    private static final AnnotationAttribute PARAM_ANNOTATION = new AnnotationAttribute(Parameter.class);
    private static final String NAME_NOT_FOUND = "Unable to detect parameter names for query method %s! Use @Param or" +
        " compile with -parameters on JDK 8.";

    private static final Logger log = Logger.getLogger(RestRepositoryUtils.class);

    @Autowired(required = true)
    @Qualifier(value = "mvcConversionService")
    private ConversionService conversionService;

    /**
     * @param repository
     * @return if the repository have at least one search method
     */
    public boolean haveSearchMethods(DSpaceRestRepository repository) {
        for (Method method : repository.getClass().getMethods()) {
            // We need to use AnnotationUtils because the DSpaceRestRepository is possibly enhanced by a Spring AOP
            // proxy. The regular "method.getAnnotation()" method would then search the proxy instead of the
            // underlying actual class. The proxy does not inherit the annotations.
            SearchRestMethod ann =
                    AnnotationUtils.findAnnotation(method, SearchRestMethod.class);
            if (ann != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param repository
     * @return the names of the search methods if any. Otherwise an empty list
     */
    public List<String> listSearchMethods(DSpaceRestRepository repository) {
        List<String> searchMethods = new LinkedList<String>();
        for (Method method : repository.getClass().getMethods()) {
            // We need to use AnnotationUtils because the DSpaceRestRepository is possibly enhanced by a Spring AOP
            // proxy. The regular "method.getAnnotation()" method would then search the proxy instead of the
            // underlying actual class. The proxy does not inherit the annotations.
            SearchRestMethod ann = AnnotationUtils.findAnnotation(method, SearchRestMethod.class);
            if (ann != null) {
                String name = ann.name();
                if (name.isEmpty()) {
                    name = method.getName();
                }
                searchMethods.add(name);
            }
        }

        return searchMethods;
    }

    /**
     * @param searchMethodName
     * @param repository
     * @return the search method in the repository with the specified name or
     * null if it is not found
     */
    public Method getSearchMethod(String searchMethodName, DSpaceRestRepository repository) {
        return _getSearchMethod(searchMethodName, repository);
    }

    /**
     * @param searchMethodName
     * @param repository
     * @return the search method in the repository with the specified name or
     * null if it is not found
     */
    public Method getSearchMethod(String searchMethodName, LinkRestRepository repository) {
        return _getSearchMethod(searchMethodName, repository);
    }

    /**
     * @param searchMethodName
     * @param repository
     * @return the search method in the repository with the specified name or
     * null if it is not found
     */
    private Method _getSearchMethod(String searchMethodName, Object repository) {
        Method searchMethod = null;
        // DSpaceRestRepository is possibly enhanced with a Spring AOP proxy. Therefor use ClassUtils to determine
        // the underlying implementation class.
        Method[] methods = ClassUtils.getUserClass(repository.getClass()).getMethods();
        for (Method method : methods) {
            // We need to use AnnotationUtils because the DSpaceRestRepository is possibly enhanced by a Spring AOP
            // proxy. The regular "method.getAnnotation()" method would then search the proxy instead of the
            // underlying actual class. The proxy does not inherit the annotations.
            SearchRestMethod ann =
                    AnnotationUtils.findAnnotation(method, SearchRestMethod.class);
            if (ann != null) {
                String name = ann.name();
                if (name.isEmpty()) {
                    name = method.getName();
                }
                if (StringUtils.equals(name, searchMethodName)) {
                    searchMethod = method;
                    break;
                }
            }
        }
        return searchMethod;
    }

    /*
     * Adapted from
     * org.springframework.data.rest.webmvc.RepositorySearchController.
     * executeQueryMethod(RepositoryInvoker, MultiValueMap<String, Object>,
     * Method, DefaultedPageable, Sort, PersistentEntityResourceAssembler)
     */
    public Object executeQueryMethod(DSpaceRestRepository repository, MultiValueMap<String, Object> parameters,
                                     Method method, Pageable pageable, Sort sort, PagedResourcesAssembler assembler) {

        MultiValueMap<String, Object> result = new LinkedMultiValueMap<String, Object>(parameters);
        MethodParameters methodParameters = new MethodParameters(method, PARAM_ANNOTATION);

        for (MethodParameter parameter : methodParameters.getParameters()) {
            final Parameter parameterAnnotation = parameter.getParameterAnnotation(Parameter.class);
            String paramName = getParamName(parameter, parameterAnnotation);
            List<Object> value = parameters.get(paramName);
            if (value == null) {
                if (parameterAnnotation != null && parameterAnnotation.required()) {
                    throw new MissingParameterException(
                            String.format("Required Parameter[%s] Missing",
                                    parameter.getParameterName()));
                }
                continue;
            }

            result.put(paramName, value);
        }

        return invokeQueryMethod(repository, method, result, pageable, sort);
    }

    /*
     * Adapted from
     * org.springframework.data.repository.support.ReflectionRepositoryInvoker.
     * invokeQueryMethod(Method, MultiValueMap<String, ? extends Object>,
     * Pageable, Sort)
     */
    public Object invokeQueryMethod(DSpaceRestRepository repository, Method method,
                                    MultiValueMap<String, ? extends Object> parameters, Pageable pageable, Sort sort) {

        Assert.notNull(method, "Method must not be null!");
        Assert.notNull(parameters, "Parameters must not be null!");

        ReflectionUtils.makeAccessible(method);

        return ReflectionUtils.invokeMethod(method, repository, prepareParameters(method, parameters, pageable, sort));
    }

    /*
     * Taken from
     * org.springframework.data.repository.support.ReflectionRepositoryInvoker.
     * prepareParameters(Method, MultiValueMap<String, ? extends Object>,
     * Pageable, Sort)
     */
    private Object[] prepareParameters(Method method, MultiValueMap<String, ? extends Object> rawParameters,
                                       Pageable pageable, Sort sort) {

        List<MethodParameter> parameters = new MethodParameters(method, PARAM_ANNOTATION).getParameters();

        if (parameters.isEmpty()) {
            return new Object[0];
        }

        Object[] result = new Object[parameters.size()];
        Sort sortToUse = pageable == null ? sort : pageable.getSort();

        for (int i = 0; i < result.length; i++) {

            MethodParameter param = parameters.get(i);
            Class<?> targetType = param.getParameterType();

            if (Pageable.class.isAssignableFrom(targetType)) {
                result[i] = pageable;
            } else if (Sort.class.isAssignableFrom(targetType)) {
                result[i] = sortToUse;
            } else {
                final Parameter parameterAnnotation = param.getParameterAnnotation(Parameter.class);
                String parameterName = getParamName(param, parameterAnnotation);

                if (StringUtils.isBlank(parameterName)) {
                    throw new IllegalArgumentException(
                        String.format(NAME_NOT_FOUND, ClassUtils.getQualifiedMethodName(method)));
                }

                Object value = unwrapSingleElement(rawParameters.get(parameterName));

                result[i] = targetType.isInstance(value) ? value : convert(value, param);
            }
        }

        return result;
    }

    private String getParamName(MethodParameter parameter, final Parameter parameterAnnotation) {
        String paramName = null;

        if (parameterAnnotation != null) {
            paramName = parameterAnnotation.value();
        }
        if (paramName == null) {
            paramName = parameter.getParameterName();
        }
        return paramName;
    }

    /**
     * Unwraps the first item if the given source has exactly one element. Taken
     * from
     * org.springframework.data.repository.support.ReflectionRepositoryInvoker.unwrapSingleElement(List<?
     * extends Object>)
     *
     * @param source can be {@literal null}.
     * @return
     */
    private static Object unwrapSingleElement(List<? extends Object> source) {
        return source == null ? null : source.size() == 1 ? source.get(0) : source;
    }

    /**
     * Taken from
     * org.springframework.data.repository.support.ReflectionRepositoryInvoker.convert(Object,
     * MethodParameter)
     *
     * @param value
     * @param parameter
     * @return
     */
    private Object convert(Object value, MethodParameter parameter) {

        try {
            return conversionService.convert(value, TypeDescriptor.forObject(value), new TypeDescriptor(parameter));
        } catch (ConversionException o_O) {
            throw new QueryMethodParameterConversionException(value, parameter, o_O);
        }
    }

    public Method getLinkMethod(String methodName, LinkRestRepository linkRepository) {
        Method linkMethod = null;
        Method[] methods = linkRepository.getClass().getMethods();
        for (Method m : methods) {
            if (StringUtils.equals(m.getName(), methodName)) {
                linkMethod = m;
                break;
            }
        }
        return linkMethod;
    }

}
