/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelation;
import org.dspace.app.rest.signposting.model.LinksetRelationType;

/**
 * Class for mapping {@link Linkset} objects.
 */
public class LinksetMapper {

    private LinksetMapper() {
    }

    /**
     * Converts list of linkset nodes into linkset.
     *
     * @param linksetNodes list of linkset nodes
     * @return linkset
     */
    public static Linkset map(List<LinksetNode> linksetNodes) {
        Linkset linkset = new Linkset();
        linkset.setLinkset(getLinksetRelationsByType(linksetNodes, LinksetRelationType.LINKSET));
        linkset.setAuthor(getLinksetRelationsByType(linksetNodes, LinksetRelationType.AUTHOR));
        linkset.setItem(getLinksetRelationsByType(linksetNodes, LinksetRelationType.ITEM));
        linkset.setType(getLinksetRelationsByType(linksetNodes, LinksetRelationType.TYPE));
        linkset.setCollection(getLinksetRelationsByType(linksetNodes, LinksetRelationType.COLLECTION));
        linkset.setLicense(getLinksetRelationsByType(linksetNodes, LinksetRelationType.LICENSE));
        linkset.setCiteAs(getLinksetRelationsByType(linksetNodes, LinksetRelationType.CITE_AS));
        linkset.setDescribes(getLinksetRelationsByType(linksetNodes, LinksetRelationType.DESCRIBES));
        linkset.setDescribedby(getLinksetRelationsByType(linksetNodes, LinksetRelationType.DESCRIBED_BY));
        if (!linksetNodes.isEmpty()) {
            linkset.setAnchor(linksetNodes.get(0).getAnchor());
        }
        return linkset;
    }

    private static List<LinksetRelation> getLinksetRelationsByType(List<LinksetNode> linkset,
                                                                   LinksetRelationType type) {
        return linkset.stream()
                .filter(linksetNode -> type.equals(linksetNode.getRelation()))
                .map(linksetNode -> new LinksetRelation(linksetNode.getLink(), linksetNode.getType()))
                .collect(Collectors.toList());
    }
}
