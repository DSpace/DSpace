/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.util.Enumeration;
import java.util.ResourceBundle;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;

public class Logger {
    
    
    static public org.apache.log4j.Logger getLogger(Class<?> clazz) {
        return _tweak_logger(LogManager.getLogger(clazz.getName()));
    }
    
    static private 
    org.apache.log4j.Logger
    _tweak_logger( org.apache.log4j.Logger logger) {
        return new own_logger(logger);
    }
    
    


//
//
//
public static class own_logger extends org.apache.log4j.Logger
{
    // variables
    org.apache.log4j.Logger impl_;
    static String[] ignores = new String[0];


    // ctor
    own_logger(org.apache.log4j.Logger impl_) {
        super(impl_.getName());
        this.impl_ = impl_;
    }

    //
    boolean ignore_exception(String msg) {
        if ( null != get_ignores() ) {
            for ( String i : get_ignores() ) {
                if ( msg.trim().startsWith( i.trim() ) )
                    return true;
            }
        }
        return false;
    }
    
    //
   public void send_error(Throwable t) {
        send_error(ExceptionUtils.getStackTrace(t));
    }

    void send_error(String msg) {

        // shall we ignore this
        if ( ignore_exception( msg ) ) {
            return;
        }

        // send email
        try {
            final String host = ConfigurationManager.getProperty("dspace.baseUrl");
            final String short_name = ConfigurationManager.getProperty("lr.dspace.name.short");
            
            Email email = new Email();
            String errors_to = ConfigurationManager.getProperty("lr", "lr.errors.email");
            if ( errors_to != null) 
            {
                email.setSubject( String.format("Error in %s repository [%s]", short_name, host) );
                email.setContent(msg);
                email.setCharset( "UTF-8" );
                email.addRecipient(errors_to);
                email.send();
                impl_.info( "Error email sent..." );
            }
        }catch(Exception e) {
            impl_.error("Error sending error report :(", e);
        }
    }


    //
    //

    // implemented like here because of local debugging
    String[] get_ignores() {
        if ( null != ignores && ignores.length == 0 ) {
            ignores = null;
            String toignore = ConfigurationManager.getProperty("lr", "lr.errors.ignore");
            if ( toignore != null ) {
                ignores = toignore.split(",");
            }
        }
        return ignores;
    }

    //
    //
    public
    void error(Object message) {
        impl_.error(message);
      send_error(message.toString());
    }
    public
    void error(Object message, Throwable t) {
        impl_.error(message, t);
      send_error(t);
    }

    // better be reflection / mocks
    //
    public void trace(Object message) {
        impl_.trace(message);
    }
    public void trace(Object message, Throwable t) {
        impl_.trace(message, t);
    }
    public boolean isTraceEnabled() {
        return impl_.isTraceEnabled();
    }
    synchronized
    public
    void addAppender(Appender newAppender) {
        impl_.addAppender(newAppender);
    }
    public
    void assertLog(boolean assertion, String msg) {
        impl_.assertLog(assertion, msg);
    }
    public
    void callAppenders(LoggingEvent event) {
        impl_.callAppenders(event);
    }
    public
    void debug(Object message) {
        impl_.debug(message);
    }
    public
    void debug(Object message, Throwable t) {
        impl_.debug(message, t);
    }
    public
    void fatal(Object message) {
        impl_.fatal(message);
    }
    public
    void fatal(Object message, Throwable t) {
        impl_.fatal(message, t);
    }
    public
    boolean getAdditivity() {
        return impl_.getAdditivity();
    }
    synchronized
    public
    Enumeration<?> getAllAppenders() {
        return impl_.getAllAppenders();
    }
    synchronized
    public
    Appender getAppender(String name) {
        return impl_.getAppender(name);
    }
    public
    Level getEffectiveLevel() {
        return impl_.getEffectiveLevel();
    }
    @SuppressWarnings("deprecation")
    public
    Priority getChainedPriority() {
        return impl_.getChainedPriority();
    }
    @SuppressWarnings("deprecation")
    public
    LoggerRepository  getHierarchy() {
        return impl_.getHierarchy();
    }
    public
    LoggerRepository  getLoggerRepository() {
        return impl_.getLoggerRepository();
    }
    public
    void info(Object message) {
        impl_.info(message);
    }
    public
    void info(Object message, Throwable t) {
        impl_.info(message, t);
    }
    public
    boolean isDebugEnabled() {
        return impl_.isDebugEnabled();
    }
    public
    boolean isEnabledFor(Priority level) {
        return impl_.isEnabledFor(level);
    }
    public
    boolean isInfoEnabled() {
        return impl_.isInfoEnabled();
    }
    public
    void log(Priority priority, Object message, Throwable t) {
        impl_.log(priority, message, t);
    }
    public
    void log(Priority priority, Object message) {
        impl_.log(priority, message);
    }
    public
    void log(String callerFQCN, Priority level, Object message, Throwable t) {
        impl_.log(callerFQCN, level, message, t);
    }
    synchronized
    public
    void removeAllAppenders() {
        impl_.removeAllAppenders();
    }
    synchronized
    public
    void removeAppender(Appender appender) {
        impl_.removeAppender(appender);
    }
    synchronized
    public
    void removeAppender(String name) {
      impl_.removeAppender(name);
    }
    public
    void setAdditivity(boolean additive) {
        impl_.setAdditivity(additive);
    }
    public
    void setLevel(Level level) {
        impl_.setLevel(level);
    }
    @SuppressWarnings("deprecation")
    public
    void setPriority(Priority priority) {
        impl_.setPriority(priority);
    }
    public
    void setResourceBundle(ResourceBundle bundle) {
        impl_.setResourceBundle(bundle);
    }
    public
    void warn(Object message) {
        impl_.warn(message);
    }
    public
    void warn(Object message, Throwable t) {
        impl_.warn(message, t);
    }
    
} // class 
} // class
