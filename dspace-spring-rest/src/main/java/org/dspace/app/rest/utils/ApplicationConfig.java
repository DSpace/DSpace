package org.dspace.app.rest.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;

@Configuration
@EnableSpringDataWebSupport
@EnableHypermediaSupport(type={HypermediaType.HAL})
@ComponentScan({ "org.dspace.app.rest.converter", "org.dspace.app.rest.repository" })
public class ApplicationConfig {
	@Value("${dspace.dir}")
	private String dspaceHome;

	public String getDspaceHome() {
		return dspaceHome;
	}
}
