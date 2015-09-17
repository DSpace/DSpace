package cz.cuni.mff.ufal.dspace.rest;

import org.glassfish.jersey.server.ResourceConfig;

public class Application extends ResourceConfig{
    public Application(){
        packages("org.dspace.rest","cz.cuni.mff.ufal.dspace.rest");
    }
}
