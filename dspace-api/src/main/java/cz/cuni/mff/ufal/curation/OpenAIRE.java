/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.IOException;

public class OpenAIRE extends AbstractCurationTask
{

    @Override
    public
    int perform(DSpaceObject dso) throws IOException
    {
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item)dso;

            // any eu funds?
            int eu_funds = 0;
            Metadatum[] metas = item.getMetadata("local", "sponsor", null, Item.ANY);
            for (Metadatum m : metas) {
                if (m.value.endsWith("euFunds")) {
                    ++eu_funds;
                }
            }

            // check them
            if (0 < eu_funds) {
                metas = item.getMetadata("dc", "relation", null, Item.ANY);
                if ( metas.length != eu_funds ) {
                    String msg = String.format(
                        "Object [%s] metadata are not synced with OpenAIRE requirements - euFunds != size(dc.relation).",
                        item.getHandle()
                    );
                    setResult(msg);
                    report(msg);
                    return Curator.CURATE_ERROR;
                }

                String msg = String.format(
                    "Object [%s] is OpenAIRE compatible.",
                    item.getHandle()
                );
                setResult(msg);
                report(msg);
                return Curator.CURATE_SUCCESS;
            }

            // skip
            setResult("");
            report("");
            return Curator.CURATE_SKIP;

        }

        setResult("");
        report("");
        return Curator.CURATE_SKIP;
    }
}
