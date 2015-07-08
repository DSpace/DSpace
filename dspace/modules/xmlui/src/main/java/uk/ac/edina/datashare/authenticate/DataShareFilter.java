package uk.ac.edina.datashare.authenticate;

import javax.servlet.FilterConfig;

import edu.umich.auth.cosign.CosignAuthenticationFilterIII;

/**
 * Extension of the cosign authentication filter.
 */
public class DataShareFilter extends CosignAuthenticationFilterIII
{
    /**
     * Initialisation overridden to set an EASE class (EASEResponse) as the call
     * back handler
     */
    public void init(FilterConfig config)
    {
        // do standard initialisation
        super.init(config);
        
        this.setJAASServletCallbackHandler(EASEresponse.class);
        //this.callbackHandlerClass = EASEresponse.class;
    }
    
    // this temporary hack gets round an exception being raised by cosign
    /*@Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain filterChain) {
        try
        {
            super.doFilter(request, response, filterChain);
        }
        catch(Exception ex)
        {
            
        }
    }*/
}
