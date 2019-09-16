package org.dspace.app.rest.link.process;

import org.dspace.app.rest.ProcessRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class ProcessHalLinkFactory<T> extends HalLinkFactory<T, ProcessRestController> {

    public UriComponentsBuilder buildProcessesBaseLink() {
        try {
            UriComponentsBuilder uriBuilder = uriBuilder(getMethodOn().getProcesses(null, null));

            return uriBuilder;
        } catch (Exception ex) {
            //The method throwing the exception is never really executed, so this exception can never occur
            return null;
        }
    }
}