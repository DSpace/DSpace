package cz.cuni.mff.ufal.dspace.app.xmlui.cocoon.generation;

import java.io.IOException;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.generation.ExceptionGenerator;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.Logger;

public class EmailExceptionGenerator extends ExceptionGenerator {
	private static final Logger.own_logger log = (Logger.own_logger) Logger.getLogger(EmailExceptionGenerator.class);

	@Override
    public void generate() throws IOException, SAXException, ProcessingException {
		super.generate();
		if(this.parameters.getParameterAsBoolean("send_email", false)){
			Throwable t = ObjectModelHelper.getThrowable(objectModel);
			log.send_error(t);
		}
    }
	

}
