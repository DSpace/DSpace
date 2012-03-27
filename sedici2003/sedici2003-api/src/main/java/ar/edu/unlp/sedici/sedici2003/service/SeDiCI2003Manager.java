package ar.edu.unlp.sedici.sedici2003.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SeDiCI2003Manager {

	private static Logger log = LoggerFactory.getLogger(SeDiCI2003Manager.class);
	private static ApplicationContext applicationContext = null;
	private static String APP_CONTEXT_CONFIG_LOCATION = "spring/sedici2003.xml";
	
	private SeDiCI2003Manager(){}
	
	public static void prepare(String propertiesPath){
		if(SeDiCI2003Manager.applicationContext == null) {
			log.info("Iniciando contexto para SeDiCI2003-API: "+propertiesPath);
			SeDiCI2003Manager.initializeApplicationContext(propertiesPath);
		}
	}

	private static void initializeApplicationContext(String propertiesPath) {
		if(propertiesPath != null)
			System.setProperty("sedici2003_config", propertiesPath);
		
		SeDiCI2003Manager.applicationContext = new ClassPathXmlApplicationContext(APP_CONTEXT_CONFIG_LOCATION);
	}
}
