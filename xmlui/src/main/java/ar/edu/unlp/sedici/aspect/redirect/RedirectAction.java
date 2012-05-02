/**
 * Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.aspect.redirect;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.environment.http.HttpResponse;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Attempt to authenticate the user based upon their presented credentials. 
 * This action uses the http parameters of login_email, login_password, and 
 * login_realm as credentials.
 * 
 * If the authentication attempt is successfull then an HTTP redirect will be
 * sent to the browser redirecting them to their original location in the 
 * system before authenticated or if none is supplied back to the DSpace 
 * homepage. The action will also return true, thus contents of the action will
 * be excuted.
 * 
 * If the authentication attempt fails, the action returns false.
 * 
 * Example use:
 * 
 * <map:act name="Authenticate">
 *   <map:serialize type="xml"/>
 * </map:act>
 * <map:transform type="try-to-login-again-transformer">
 *
 * @author Scott Phillips
 */

public class RedirectAction extends AbstractAction
{

	private static String PropertiesFilename="sedici.redirect";
	private static String MAP_PREFIX="map.url";
	
	private static String OLD_URL="old_url";
	private static String NEW_URL="new_url";
	private static String URL_PART="url_part";
	private static String TYPE="type";
	
	
	
	
	private static Map<String, Map<String, Object>> mapeosPorPrefijo=new HashMap<String, Map<String,Object>>();
	private static Map<String, List<String>> urls=null;


	private static void generarMapeo(String prefijo) {
		// Genera el hashMap para las properties
		HashMap<String, Object> retorno=new HashMap<String, Object>();
		String clave;
		String valor;
		Boolean valorBooleano;
		String type;
		//cargo la key de la nueva url y el valor
		clave=prefijo+"."+RedirectAction.NEW_URL;
		valor=ConfigurationManager.getProperty(RedirectAction.PropertiesFilename, clave);
		retorno.put(clave,  valor);
		//cargo los parametros
		int inicial=1;
		clave=prefijo+".params."+inicial+".old_name";
		valor=ConfigurationManager.getProperty(RedirectAction.PropertiesFilename, clave);
		while (valor!=null){
			//agrego el nombre del parametro viejo al map
			retorno.put(clave,  valor);
			
			//agrego si el aprametro va a formar parte de la nueva url
			clave=prefijo+".params."+inicial+"."+RedirectAction.URL_PART;			
			valorBooleano=ConfigurationManager.getBooleanProperty(RedirectAction.PropertiesFilename, clave);
			retorno.put(clave, valorBooleano);
			
			//agrego si el dato requiere la transformacion de algun tipo
			clave=prefijo+".params."+inicial+"."+RedirectAction.TYPE;
			type=ConfigurationManager.getProperty(RedirectAction.PropertiesFilename, clave);
			retorno.put(clave, type);
			
			if (!valorBooleano){
				//si no es parte de la url el parametro, tengo que guardar el new name
				clave=prefijo+".params."+inicial+".new_name";
				valor=ConfigurationManager.getProperty(RedirectAction.PropertiesFilename, clave);
				retorno.put(clave,  valor);
			}
			
			//recupero el proximo parametro
			inicial+=1;
			clave=prefijo+".params."+inicial+".old_name";
			valor=ConfigurationManager.getProperty(RedirectAction.PropertiesFilename, clave);
		}
       mapeosPorPrefijo.put(prefijo,  retorno);
		
	}

	public static List<String> getPrefixes(String old_url) {
    	if (urls==null)
    		initUrls();
    	List<String> prefixes = urls.get(old_url); 
    	if (prefixes == null)
    		prefixes = new LinkedList<String>();
    	return prefixes; 
    	
	}
	

	private static void initUrls() {
		urls =new HashMap<String, List<String>>();
		int i=1;
		String old_url_i=ConfigurationManager.getProperty(RedirectAction.PropertiesFilename, "map.url." + i);

		while (old_url_i!=null){
			if (!urls.containsKey(old_url_i))
				urls.put(old_url_i, new LinkedList<String>());
			urls.get(old_url_i).add("map.url." + i);
			i++;
			old_url_i=ConfigurationManager.getProperty(RedirectAction.PropertiesFilename, "map.url." + i);
		}
	}

	public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
		
        Request request = ObjectModelHelper.getRequest(objectModel);
        String old_url = request.getParameter(RedirectAction.OLD_URL);
        
        //recupero la clave para la vieja url
        List<String> prefijos=this.getPrefixes(old_url);
        
        for (String prefijo : prefijos) {
		    Map<String, Object> propertiesPrefijo=this.getMapeos(prefijo);
	        
	        //recupero los valores
	        String new_url=(String)propertiesPrefijo.get(prefijo+"."+RedirectAction.NEW_URL);
	        
	        //recupero la cantidad de parámetros que van a ser pasados al redirect   
	    	Hashtable<String, String> new_url_params=new Hashtable<String, String>();
	    	boolean missingParams = false;
	        int inicial=1;
	        String param_old_name="";
	        String param_new_name="";
	    	Boolean url_part=false;
	    	String type;
	    	String param_value="";
	    	String prefijo_parametro=prefijo+".params."+inicial;
	
	        while (propertiesPrefijo.containsKey(prefijo_parametro+".old_name")){
	            param_old_name=(String)propertiesPrefijo.get(prefijo_parametro+".old_name");
	        	
	    		//debo verificar si el parametro es parte de la url o se pasa como parámetro.
	    		//IMPORTANTE: aca podriamos extender el config en el caso de que varios parametros puedan formar parte de la url, para
	    		//            agregarle el orden y escribir bien la URL nueva.
	    		//            En este caso, solo UN parámetro es parte de la url, los demás se pasan como parámetros.
	    		url_part=(Boolean)propertiesPrefijo.get(prefijo_parametro+"."+RedirectAction.URL_PART);
	    		
	    		//recupero del request el valor del parametro con nomre old_param
	    		param_value=request.getParameter(param_old_name);
	
	    		//Si no existe el parametro con ese nombre salteo la regla
	    		if (param_value==null){
	    			missingParams = true;
	    			break;
//	            	throw new ResourceNotFoundException("No se puede encontrar la pagina "+old_url);
	    		}
	    		
	    		//Transformo en caso de ser necesario el valor del parametro
	    		type=(String)propertiesPrefijo.get(prefijo_parametro+"."+RedirectAction.TYPE); 
	    		if (type!=null){
	    			param_value=this.transformParamValue(request, type, param_value);
	    		}
	    		
	    		//Si el parametro es parte de la url lo agrego separado por /, sino lo agrego como parametro del request
				if (url_part){
					new_url=new_url+"/"+param_value;
				}else{
					//si no es parte de la url se agrega como parametro a la url
	        		param_new_name=(String)propertiesPrefijo.get(prefijo_parametro+".new_name");
	        		new_url_params.put(param_new_name, param_value);				
				}
				
				inicial+=1;
		    	prefijo_parametro=prefijo+".params."+inicial;
	
			};

			if (missingParams)
				continue;
			//recorro el hashtable creado con los parametros y lo agrego a la url
			Enumeration<String> claves_params=new_url_params.keys();
			Boolean first_element=true;
			String clave_param;
			//agrego los parametros a la nueva url
			while (claves_params.hasMoreElements()) {
				if (first_element){
					new_url+="?";
					first_element=false;
				} else {
					new_url+="&";
				};
				clave_param=claves_params.nextElement();
				new_url+=clave_param+"="+new_url_params.get(clave_param);
			};
	
	        
    
	        String redirectURL = request.getContextPath()+ new_url;
	        
	        //System.out.println(redirectURL);
	        
	        final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
	        httpResponse.setStatus(HttpResponse.SC_MOVED_PERMANENTLY);
	        httpResponse.setHeader("Location", redirectURL);
	        httpResponse.flushBuffer();
	        //httpResponse.setHeader("Connection", "close");
	        //httpResponse.sendRedirect(redirectURL);
	        //httpResponse.setStatus(301);
	        //((ForwardRedirector)redirector).permanentRedirect(true, redirectURL);
	        
	        return null;
	        
        }
        
    	throw new ResourceNotFoundException("No se puede encontrar la pagina "+old_url);

    }
    
	/*
	 * Transforma el valor del parametro al valor correspondiente el nueva base de datos.
	 */
    private String transformParamValue(Request request, String type, String param_value) {
	   	if (type.equals("item")){
    		//Obtengo el contexto para hacer la consulta a la BD
    		try {
				Context contexto=ContextUtil.obtainContext(request);
				ItemIterator items=Item.findByMetadataField(contexto, "sedici2003", "identifier", null, param_value);
				if (items.hasNext()){
					param_value=items.next().getHandle();					
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (AuthorizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return param_value;
	}

	private Map<String, Object> getMapeos(String prefijo) {
		if (!mapeosPorPrefijo.containsKey(prefijo)){
			generarMapeo(prefijo);
		};
		return mapeosPorPrefijo.get(prefijo);
	}

}

