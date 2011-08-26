package org.swordapp.server;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;

public class OREStatement extends Statement
{
    private String remUri;
    private String aggUri;

    public OREStatement(String remUri, String aggUri)
    {
        this.remUri = remUri;
        this.aggUri = aggUri;
        this.contentType = "application/rdf+xml";
    }

    @Override
    public void writeTo(Writer out)
            throws IOException
    {
        // create the default model (in memory) to start with
        Model model = ModelFactory.createDefaultModel();

        // create the resource map in the model
        Resource rem = model.createResource(this.remUri);
        rem.addProperty(RDF.type, model.createResource(UriRegistry.ORE_NAMESPACE + "ResourceMap"));

        // create the aggregation
        Resource agg = model.createResource(this.aggUri);
        agg.addProperty(RDF.type, model.createResource(UriRegistry.ORE_NAMESPACE + "Aggregation"));

        // add the aggregation to the resource (and vice versa)
        rem.addProperty(model.createProperty(UriRegistry.ORE_NAMESPACE + "describes"), agg);
        agg.addProperty(model.createProperty(UriRegistry.ORE_NAMESPACE + "isDescribedBy"), rem);

        // now go through and add all the ResourceParts as aggregated resources
        for (ResourcePart rp : this.resources)
        {
            Resource part = model.createResource(rp.getUri());
            part.addProperty(RDF.type, model.createResource(UriRegistry.ORE_NAMESPACE + "AggregatedResource"));
            agg.addProperty(model.createProperty(UriRegistry.ORE_NAMESPACE + "aggregates"), part);
        }

        // now go through all the original deposits and add them as both aggregated
        // resources and as originalDeposits (with all the trimmings)
        for (OriginalDeposit od : this.originalDeposits)
        {
            Resource deposit = model.createResource(od.getUri());
            deposit.addProperty(RDF.type, model.createResource(UriRegistry.ORE_NAMESPACE + "AggregatedResource"));
            if (od.getDepositedBy() != null)
            {
                deposit.addLiteral(model.createProperty(UriRegistry.SWORD_DEPOSITED_BY), od.getDepositedBy());
            }

            if (od.getDepositedOnBehalfOf() != null)
            {
                deposit.addLiteral(model.createProperty(UriRegistry.SWORD_DEPOSITED_ON_BEHALF_OF), od.getDepositedOnBehalfOf());
            }

            if (od.getDepositedOn() != null)
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                deposit.addLiteral(model.createProperty(UriRegistry.SWORD_DEPOSITED_ON), sdf.format(od.getDepositedOn()));
            }

            for (String packaging : od.getPackaging())
            {
                deposit.addLiteral(model.createProperty(UriRegistry.SWORD_PACKAGING.toString()), packaging);
            }

            agg.addProperty(model.createProperty(UriRegistry.ORE_NAMESPACE + "aggregates"), deposit);
            agg.addProperty(model.createProperty(UriRegistry.SWORD_ORIGINAL_DEPOSIT), deposit);
        }

        // now add the state information
        for (String state : this.states.keySet())
        {
            Resource s = model.createResource(state);
            if (this.states.get(state) != null)
            {
                s.addProperty(model.createProperty(UriRegistry.SWORD_STATE_DESCRIPTION), this.states.get(state));
            }
            agg.addProperty(model.createProperty(UriRegistry.SWORD_STATE), s);
        }
        
        // write the model directly to the output
        model.write(out, "RDF/XML");
    }
}
