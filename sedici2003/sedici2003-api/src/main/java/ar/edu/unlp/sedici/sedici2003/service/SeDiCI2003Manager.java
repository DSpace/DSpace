package ar.edu.unlp.sedici.sedici2003.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeDiCI2003Manager {

	private static Logger log = LoggerFactory.getLogger(SeDiCI2003Manager.class);
	
	private SeDiCI2003Manager(){}
	
	public static void prepare(){
		log.debug("Iniciando contexto para SeDiCI2003-API");
		// No hace nada, porque se levanta dentro del contexto web
	}
	
}
