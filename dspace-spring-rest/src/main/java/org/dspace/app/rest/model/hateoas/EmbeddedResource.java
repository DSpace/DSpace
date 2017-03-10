/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.dspace.app.rest.model.EmbeddedRestModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.Utils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * A base class for Embedded (not directly addressable) Rest HAL Resource. The HAL Resource wraps the REST
 * Resource adding support for the links and embedded resources. Each property
 * of the wrapped REST resource is automatically translated in a link and the
 * available information included as embedded resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class EmbeddedResource<T extends EmbeddedRestModel> extends ResourceSupport {
	@JsonUnwrapped
	@JsonInclude(content=Include.NON_EMPTY)
	private final T data;
	
	@JsonProperty(value = "_embedded")
	@JsonInclude(Include.NON_EMPTY)
	private Map<String, Object> embedded = new HashMap<String, Object>();

	public EmbeddedResource(T data, Utils utils, String baseUrl, String... rels) {
		this.data = data;

		if (data != null) {
			try {
				for (PropertyDescriptor pd : Introspector.getBeanInfo(data.getClass()).getPropertyDescriptors()) {
					Method readMethod = pd.getReadMethod();
					if (readMethod != null && !"class".equals(pd.getName())) {
						Class<?> returnType = readMethod.getReturnType();
						if (RestModel.class.isAssignableFrom(returnType)) {
							RestModel linkedObject = (RestModel) readMethod.invoke(data);
							if (linkedObject != null) {
								embedded.put(pd.getName(),
										utils.getResourceRepository(linkedObject.getType()).wrapResource(linkedObject));
								this.add(utils.linkToSingleResource(linkedObject, pd.getName()));
							}
	
							Method writeMethod = pd.getWriteMethod();
							writeMethod.invoke(data, new Object[] { null });
						}
						else if (EmbeddedRestModel.class.isAssignableFrom(returnType)) {
							Link linkToSubResource = utils.linkToSubResource(baseUrl, pd.getName());
							this.add(linkToSubResource.withRel(pd.getName()));
							EmbeddedRestModel linkedObject = (EmbeddedRestModel) readMethod.invoke(data);
							if (linkedObject != null) {
								embedded.put(pd.getName(),
										new EmbeddedResource<EmbeddedRestModel>(linkedObject, utils, linkToSubResource.getHref(), rels));
							} else {
								embedded.put(pd.getName(), null);
							}
	
							Method writeMethod = pd.getWriteMethod();
							writeMethod.invoke(data, new Object[] { null });
						}
						else if (Map.class.isAssignableFrom(returnType)) {
							Type type = readMethod.getGenericReturnType();
							if (type instanceof ParameterizedType) {
								ParameterizedType ptype = (ParameterizedType) type;
								if (ptype.getActualTypeArguments()[0].equals(String.class)) {
									Map<String, ?> mapObject = (Map<String, ?>) readMethod.invoke(data);
									if (EmbeddedRestModel.class.isAssignableFrom((Class) ptype.getActualTypeArguments()[1])) {
										Set<String> keys = mapObject.keySet();
										for (String key : keys){
											Link linkToSubResource = utils.linkToSubResource(baseUrl, key);
											this.add(linkToSubResource);				
											EmbeddedRestModel linkedObject = (EmbeddedRestModel) mapObject.get(key);
											if (linkedObject != null) {
												embedded.put(key,
														new EmbeddedResource<EmbeddedRestModel>(linkedObject, utils, linkToSubResource.getHref(), rels));
											} else {
												embedded.put(key, null);
											}
										}
										mapObject.clear();
									}
								}
							}
						}
					}
				}
			} catch (IntrospectionException | IllegalArgumentException | IllegalAccessException
					| InvocationTargetException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			this.add(new Link(baseUrl, Link.REL_SELF));
		}
	}

	public Map<String, Object> getEmbedded() {
		return embedded;
	}

	public T getData() {
		return data;
	}
}
