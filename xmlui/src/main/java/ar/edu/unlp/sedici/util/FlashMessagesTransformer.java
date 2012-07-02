package ar.edu.unlp.sedici.util;

import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;

public class FlashMessagesTransformer extends AbstractDSpaceTransformer{

	/** Language Strings */
	private static final Message T_head =
		message("xmlui.general.notice.default_head");
	
	/**
	 * Add the notice div to the body.
	 */
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException 
	{
		Request request = ObjectModelHelper.getRequest(objectModel);
		HttpSession sesion=request.getSession();
		
		List<ar.edu.unlp.sedici.util.FlashMessage> mensajes=FlashMessagesUtil.consume(sesion);
		
		for (FlashMessage flashMessage : mensajes) {
			agregarMensaje(flashMessage, body);
		}
	}

	private void agregarMensaje(FlashMessage flashMessage, Body body) throws WingException {
		String rend = "notice";
		switch (flashMessage.getTipo()) {
		case ALERT:
			rend += " netural";
			break;
		case ERROR:
			rend += " failure";
			break;
		case NOTICE:
			rend += " success";
			break;
		default:
			rend += " success";
			break;
		}

		
		Division div = body.addDivision("flash-message",rend);

		if (flashMessage.getMensaje() != null && flashMessage.getMensaje().length() > 0)
        {
            div.addPara(message(flashMessage.getMensaje()).parameterize(flashMessage.getParametros().toArray()));
        }
		
		
	}
	
}
