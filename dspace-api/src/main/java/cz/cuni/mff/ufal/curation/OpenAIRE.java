/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import org.dspace.app.util.DCInput;
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
            Metadatum[] local_sponsor_metas = item.getMetadata("local", "sponsor", null, Item.ANY);
            for (Metadatum m : local_sponsor_metas) {
                if (m.value.contains("euFunds")) {
                    ++eu_funds;
                }
            }

            // check them
            if (0 < eu_funds) {
                Metadatum[] dc_relation_metas = item.getMetadata("dc", "relation", null, Item.ANY);
                if ( dc_relation_metas.length != eu_funds ) {
                    String msg = String.format(
                        "Object [%s] metadata are not synced with OpenAIRE requirements - euFunds != size(dc.relation).",
                        item.getHandle()
                    );
                    setResult(msg);
                    report(msg);
                    return Curator.CURATE_ERROR;
                }

                for (Metadatum lsm : local_sponsor_metas) {
                    String[] splits = lsm.value.split(DCInput.ComplexDefinition.SEPARATOR);
                    // new version with OpenAIRE at the end
                    if (5 == splits.length) {
                        String id = splits[splits.length - 1];
                        boolean found = false;
                        for (Metadatum drm : dc_relation_metas) {
                            if (drm.value.equals(id)) {
                                found = true;
                                break;
                            }
                        }
                        if ( !found ) {
                            String msg = String.format(
                                "Object [%s] metadata are not synced with OpenAIRE requirements - local.sponsor[OpenAIRE] not in dc.relation.",
                                item.getHandle()
                            );
                            setResult(msg);
                            report(msg);
                            return Curator.CURATE_ERROR;
                        }
                    }
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
