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
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.LinksRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

/**
 * A base class for DSpace Rest HAL Resource. The HAL Resource wraps the REST
 * Resource adding support for the links and embedded resources. Each property
 * of the wrapped REST resource is automatically translated in a link and the
 * available information included as embedded resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public abstract class DSpaceResource<T extends RestModel> extends ResourceSupport {
	@JsonUnwrapped
	private final T data;

	@JsonProperty(value = "_embedded")
	@JsonInclude(Include.NON_EMPTY)
	private Map<String, Object> embedded = new HashMap<String, Object>();

	public DSpaceResource(T data, Utils utils, String... rels) {
		this.data = data;

		if (data != null) {
			try {
				LinksRest links = data.getClass().getDeclaredAnnotation(LinksRest.class);
				if (links != null && rels != null) {
					List<String> relsList = Arrays.asList(rels);
					for (LinkRest linkAnnotation : links.links()) {
						if (!relsList.contains(linkAnnotation.name())) {
							continue;
						}
						String name = linkAnnotation.name();
						Link linkToSubResource = utils.linkToSubResource(data, name);
						String apiCategory = data.getCategory();
						String model = data.getType();
						LinkRestRepository linkRepository = utils.getLinkResourceRepository(apiCategory, model, linkAnnotation.name());

						if (!linkRepository.isEmbeddableRelation(data, linkAnnotation.name())) {
							continue;
						}
						try {
							Method[] methods = linkRepository.getClass().getMethods();
							boolean found = false;
							for (Method m : methods) { 
								if (StringUtils.equals(m.getName(), linkAnnotation.method())) {
										// TODO add support for single linked object other than for collections
										Page<? extends Serializable> pageResult = (Page<? extends RestModel>) m.invoke(linkRepository, null, ((BaseObjectRest) data).getId(), null, null);
										EmbeddedPage ep = new EmbeddedPage(linkToSubResource.getHref(), pageResult, null);
										embedded.put(name, ep);
										found = true;
								}
							}
							// TODO custom exception
							if (!found) {
								throw new RuntimeException("Method for relation " + linkAnnotation.name() + " not found: " + linkAnnotation.method());
							}
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException(e.getMessage(), e);
						}
					}
				}
				
				for (PropertyDescriptor pd : Introspector.getBeanInfo(data.getClass()).getPropertyDescriptors()) {
					Method readMethod = pd.getReadMethod();
					String name = pd.getName();
					if (readMethod != null && !"class".equals(name)) {
						LinkRest linkAnnotation = readMethod.getAnnotation(LinkRest.class);
						
						if (linkAnnotation != null) {
							if (StringUtils.isNotBlank(linkAnnotation.name())) {
								name = linkAnnotation.name();
							}
							Link linkToSubResource = utils.linkToSubResource(data, name);	
							// no method is specified to retrieve the linked object(s) so check if it is already here
							if (StringUtils.isBlank(linkAnnotation.method())) {

								Object linkedObject = readMethod.invoke(data);
								Object wrapObject = linkedObject;
								if (linkedObject instanceof RestModel) {
									RestModel linkedRM = (RestModel) linkedObject; 
									wrapObject = utils.getResourceRepository(linkedRM.getCategory(), linkedRM.getType())
											.wrapResource(linkedRM);
									if(linkAnnotation.linkClass() != null && linkAnnotation.linkClass().isAssignableFrom(linkedRM.getClass())) {
										linkToSubResource = utils.linkToSingleResource(linkedRM, name);
									}
								}
								else {
									if (linkedObject instanceof List) {
										List<RestModel> linkedRMList = (List<RestModel>) linkedObject; 
										if (linkedRMList.size() > 0) {
											
											DSpaceRestRepository<RestModel, ?> resourceRepository = utils.getResourceRepository(linkedRMList.get(0).getCategory(), linkedRMList.get(0).getType());
											// TODO should we force pagination also of embedded resource? 
											// This will force a pagination with size 10 for embedded collections as well
//											int pageSize = 1;
//											PageImpl<RestModel> page = new PageImpl(
//													linkedRMList.subList(0,
//															linkedRMList.size() > pageSize ? pageSize : linkedRMList.size()), new PageRequest(0, pageSize), linkedRMList.size()); 
											PageImpl<RestModel> page = new PageImpl(linkedRMList);
											wrapObject = new EmbeddedPage(linkToSubResource.getHref(), page.map(resourceRepository::wrapResource), linkedRMList);
										}
										else {
											wrapObject = null;
										}
									}
								}
								if (linkedObject != null) {
									embedded.put(name, wrapObject);
									this.add(linkToSubResource);
								} else if(!linkAnnotation.optional()) {
									embedded.put(name, null);
									this.add(linkToSubResource);
								}

								Method writeMethod = pd.getWriteMethod();
								writeMethod.invoke(data, new Object[] { null });
							}
							else {
								// call the link repository
								try {
									String apiCategory = data.getCategory();
									String model = data.getType();
									LinkRestRepository linkRepository = utils.getLinkResourceRepository(apiCategory, model, linkAnnotation.name());
									Method[] methods = linkRepository.getClass().getMethods();
									boolean found = false;
									for (Method m : methods) { 
										if (StringUtils.equals(m.getName(), linkAnnotation.method())) {
												// TODO add support for single linked object other than for collections
												Page<? extends Serializable> pageResult = (Page<? extends RestModel>) m.invoke(linkRepository, null, ((BaseObjectRest) data).getId(), null, null);
												EmbeddedPage ep = new EmbeddedPage(linkToSubResource.getHref(), pageResult, null);
												embedded.put(name, ep);
												found = true;
										}
									}
									// TODO custom exception
									if (!found) {
										throw new RuntimeException("Method for relation " + linkAnnotation.name() + " not found: " + linkAnnotation.method());
									}
								} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
									throw new RuntimeException(e.getMessage(), e);
								}
							}
						}
						else if (RestModel.class.isAssignableFrom(readMethod.getReturnType())) {
							Link linkToSubResource = utils.linkToSubResource(data, name);
							this.add(linkToSubResource);
							RestModel linkedObject = (RestModel) readMethod.invoke(data);
							if (linkedObject != null) {
								embedded.put(name,
										utils.getResourceRepository(linkedObject.getCategory(), linkedObject.getType())
												.wrapResource(linkedObject));
							} else {
								embedded.put(name, null);
							}

							Method writeMethod = pd.getWriteMethod();
							writeMethod.invoke(data, new Object[] { null });
						}
					}
				}
			} catch (IntrospectionException | IllegalArgumentException | IllegalAccessException
					| InvocationTargetException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			this.add(utils.linkToSingleResource(data, Link.REL_SELF));
		}
	}

	@Override
	public void add(Link link) {
		System.out.println("Chiamato "+link.getRel());
		super.add(link);
	}
	public Map<String, Object> getEmbedded() {
		return embedded;
	}

	public T getData() {
		return data;
	}
}
