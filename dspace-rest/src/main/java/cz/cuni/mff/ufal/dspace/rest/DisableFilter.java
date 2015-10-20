package cz.cuni.mff.ufal.dspace.rest;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Created by ondra on 20.10.15.
 */
@Disable
@Provider
public class DisableFilter implements ContainerRequestFilter {

    private static Logger log = Logger.getLogger(MyHandleResource.class);
    private static final boolean disabled = !ConfigurationManager.getBooleanProperty("lr", "shortener.enabled", false);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if(disabled){
            log.info("Disabled resource reached");
            requestContext.abortWith(Response
                    .status(Response.Status.FORBIDDEN)
                    .entity("User cannot access the resource. The resource was disabled.")
                    .build());
        }
    }

}
