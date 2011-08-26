package org.swordapp.server;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Service;
import org.apache.abdera.model.Workspace;

public class ServiceDocument
{
    private String version = "2.0";
    private int maxUploadSize = -1;

    private Service service;

    public ServiceDocument()
    {
        Abdera abdera = new Abdera();
        this.service = abdera.newService();
    }

    public Service getWrappedService()
    {
        return service;
    }

    public Service getAbderaService()
    {
        // here is where we compress everything from SWORD into Abdera
        // and the output has to be a full clone, not by reference
        Service abderaService = (Service) this.service.clone();
        abderaService.addSimpleExtension(UriRegistry.SWORD_VERSION, this.version);
        if (maxUploadSize > -1)
        {
            abderaService.addSimpleExtension(UriRegistry.SWORD_MAX_UPLOAD_SIZE, Integer.toString(this.maxUploadSize));
        }
        return abderaService;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public void setMaxUploadSize(int maxUploadSize)
    {
        this.maxUploadSize = maxUploadSize;
    }

    public void addWorkspace(SwordWorkspace workspace)
    {
        // FIXME: or do we just keep a reference of these until we get a call to getAbderaService()?
        Workspace abderaWorkspace = workspace.getAbderaWorkspace();
        this.service.addWorkspace(abderaWorkspace);
    }
}
