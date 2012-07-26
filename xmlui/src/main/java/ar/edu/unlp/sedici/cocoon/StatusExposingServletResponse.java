package ar.edu.unlp.sedici.cocoon;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class StatusExposingServletResponse extends HttpServletResponseWrapper {
	
	private int httpStatus;

    public StatusExposingServletResponse(HttpServletResponse response) {
        super(response);
        httpStatus=-1;
    }

    @Override
    public void sendError(int sc) throws IOException {
        httpStatus = sc;
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        httpStatus = sc;
        super.sendError(sc, msg);
    }
    
    @Override
    public void sendRedirect(String location) throws IOException {
        httpStatus = 302;
        super.sendRedirect(location);
    }

    @Override
    public void setStatus(int sc) {
        httpStatus = sc;
        super.setStatus(sc);
    }

    public int getStatus() {
        return httpStatus;
    }
    
    @Override
    public void reset() {
        super.reset();
        this.httpStatus = SC_OK;
    }
    
    @Override
    public void setStatus(int status, String string) {
        super.setStatus(status, string);
        this.httpStatus = status;
    }

}
