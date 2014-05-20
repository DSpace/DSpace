package ar.edu.unlp.sedici.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

public class FlashMessagesUtil {

    private static final String MessagesName="MessagesUtil.mensajes";
    
    public static void setMessage(HttpSession sesion, String mensaje, List<String> parametros, FlashMessage.TYPE tipo){
    	FlashMessage mensajeAGuardar= new FlashMessage(mensaje, parametros, tipo);
    	List<FlashMessage> mensajesGuardados=(List<FlashMessage>)(sesion.getAttribute(MessagesName));
    	if (mensajesGuardados==null){
    		mensajesGuardados=new ArrayList<FlashMessage>();
    	}
		mensajesGuardados.add(mensajeAGuardar);
		sesion.setAttribute(MessagesName, mensajesGuardados);   	
    }
    
    public static void setAlertMessage(HttpSession sesion, String mensaje, List<String> parametros){
    	setMessage(sesion, mensaje, parametros, FlashMessage.TYPE.ALERT);   	
    }
    
    public static void setAlertMessage(HttpSession sesion, String mensaje){
    	setMessage(sesion, mensaje, new ArrayList<String>(), FlashMessage.TYPE.ALERT);   	
    }
    
    public static void setNoticeMessage(HttpSession sesion, String mensaje, List<String> parametros){
    	setMessage(sesion, mensaje, parametros, FlashMessage.TYPE.NOTICE);   	
    }
    
    public static void setNoticeMessage(HttpSession sesion, String mensaje){
    	setMessage(sesion, mensaje, new ArrayList<String>(), FlashMessage.TYPE.NOTICE);   	
    }
    
    public static void setErrorMessage(HttpSession sesion, String mensaje, List<String> parametros){
    	setMessage(sesion, mensaje, parametros, FlashMessage.TYPE.ERROR);   	
    }
    
    public static void setErrorMessage(HttpSession sesion, String mensaje){
    	setMessage(sesion, mensaje, new ArrayList<String>(), FlashMessage.TYPE.ERROR);   	
    }    
    
    public static List<FlashMessage> consume(HttpSession sesion){
    	List<FlashMessage> mensajesGuardados=(List<FlashMessage>)(sesion.getAttribute(MessagesName));
    	if (mensajesGuardados==null){
    		mensajesGuardados=new ArrayList<FlashMessage>();
    	};
    	//remuevo los mensajes de la sesion
    	sesion.setAttribute(MessagesName, new ArrayList<FlashMessage>());
    	//retorno los mensajes previos
    	return mensajesGuardados;
    }
	
}
