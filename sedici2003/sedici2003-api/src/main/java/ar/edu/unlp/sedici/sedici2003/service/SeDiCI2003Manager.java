package ar.edu.unlp.sedici.sedici2003.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SeDiCI2003Manager {

	private static Logger log = LoggerFactory.getLogger(SeDiCI2003Manager.class);
	private static ApplicationContext applicationContext;
	
	private static final String APPLICATION_CTX_PATH = "classpath*:/spring/sedici2003.xml";
	
	
	private SeDiCI2003Manager(){}
	
	public static void prepare(){
		if (applicationContext ==null){
			applicationContext = new ClassPathXmlApplicationContext(APPLICATION_CTX_PATH);
			log.info("Se levanta el application context  "+APPLICATION_CTX_PATH + ", el cual tiene " +applicationContext.getBeanDefinitionCount()+" beans definidos");
		}
		
	}
}
