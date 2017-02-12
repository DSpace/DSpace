package org.dspace.app.rest.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableSpringDataWebSupport
@ComponentScan({ "org.dspace.app.rest.converter", "org.dspace.app.rest.repository", "org.dspace.app.rest.utils" })
public class ApplicationConfig {
	@Value("${dspace.dir}")
	private String dspaceHome;

	public String getDspaceHome() {
		return dspaceHome;
	}
}
