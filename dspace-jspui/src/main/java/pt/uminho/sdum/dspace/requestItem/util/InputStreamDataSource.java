/*
 * InputStreamDataSource.java
 *
 * Created on 31 de Marco de 2006, 19:52
 *
 */

package pt.uminho.sdum.dspace.requestItem.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.DataSource;

/**
 *
 * @author arnaldo
 */
public class InputStreamDataSource implements DataSource {
    private String name;       
    private String contentType;        
    private ByteArrayOutputStream baos;                
    
    InputStreamDataSource(String name, String contentType, InputStream inputStream) throws IOException {            
        this.name = name;            
        this.contentType = contentType;                        
        baos = new ByteArrayOutputStream();                        
        int read;            
        byte[] buff = new byte[256];            
        while((read = inputStream.read(buff)) != -1) {                
            baos.write(buff, 0, read);            
        }        
    }                
    
    public String getContentType() {            
        return contentType;        
    }         
    
    public InputStream getInputStream() throws IOException {            
        return new ByteArrayInputStream(baos.toByteArray());        
    }         
    
    public String getName() {            
        return name;        
    }         
    
    public OutputStream getOutputStream() throws IOException {            
        throw new IOException("Cannot write to this read-only resource");        
    }    
}