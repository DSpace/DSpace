package ar.edu.unlp.sedici.util;

import java.util.List;

public class FlashMessage {
	
    public static enum TYPE{
        ERROR, NOTICE, ALERT
    }

	String mensaje;
	List<String> parametros;
	FlashMessage.TYPE tipo;
	
	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	public List<String> getParametros() {
		return parametros;
	}

	public void setParametros(List<String> parametros) {
		this.parametros = parametros;
	}

	public FlashMessage.TYPE getTipo() {
		return tipo;
	}

	public void setTipo(FlashMessage.TYPE tipo) {
		this.tipo = tipo;
	}
	
	public FlashMessage(String mensaje, List<String> parametros, FlashMessage.TYPE tipo){
		super();
		this.mensaje=mensaje;
		this.parametros=parametros;
		this.tipo=tipo;
	}
	
	
    
}
