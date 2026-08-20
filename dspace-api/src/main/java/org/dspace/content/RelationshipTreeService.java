/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipTreeService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelationshipService relationshipService;

    public static final String SCOPE_ALL = "all";

    public Node getTree(Context context, Item item, String scopeString) {
        return getNode(context, item, buildScopeSet(scopeString), new HashSet<>(), -1);
    }

    public Set<UUID> getItemsInTree(Context context, Item item, String scopeString, boolean includeRoot) {
        Set<UUID> itemsInTree = new HashSet<>();

        getNode(context, item, buildScopeSet(scopeString), itemsInTree, -1);

        if (!includeRoot) {
            itemsInTree.remove(item.getID());
        }

        return itemsInTree;
    }

    public static Set<String> buildScopeSet(String scopeString) {
        Set<String> scope = new HashSet<>();

        if (isScopeNone(scopeString)) {
            return scope; // empty set = no traversal
        }

        if (isScopeAll(scopeString)) {
            scope.add(SCOPE_ALL);
            return scope;
        }

        for (String part : scopeString.split(",")) {
            scope.add(part.trim());
        }

        return scope;
    }

    private Node getNode(Context context, Item item, Set<String> scope,
                         Set<UUID> itemsInTree, int place) {
        try {
            if (item.getHandle() == null) {
                //Not archived don't add
                return null;
            } else {
                itemsInTree.add(item.getID());

                return new Node(item.getID(), getFirstMetadataValue(item, "relationship", "type"),
                        getFirstMetadataValue(item, "dc", "title"), place, getRels(context, item, scope, itemsInTree)
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, SortedSet<Node>> getRels(Context context, Item item, Set<String> scope,
                                                 Set<UUID> itemsInTree)
            throws SQLException {
        Map<String, SortedSet<Node>> rels = new HashMap<>();

        for (Relationship rel : relationshipService.findByItem(context, item)) {
            String relName;
            Item childItem;
            int childPlace;

            if (rel.getLeftItem().getID().equals(item.getID())) { // this item is on the left
                relName = rel.getRelationshipType().getRightwardType();
                childItem = rel.getRightItem();
                childPlace = rel.getLeftPlace();
            } else { // this item is on the right
                relName = rel.getRelationshipType().getLeftwardType();
                childItem = rel.getLeftItem();
                childPlace = rel.getRightPlace();
            }

            if (scope.contains(relName) || scope.contains(SCOPE_ALL)) {
                // we care about this relationship

                SortedSet<Node> relatedItems = rels.get(relName);

                if (relatedItems == null) {
                    relatedItems = new TreeSet<>(Comparator.comparingInt(node -> node.place));
                    rels.put(relName, relatedItems);
                }

                if (!itemsInTree.contains(childItem.getID())) {
                    Node node = getNode(context, childItem, Set.of(), itemsInTree, childPlace);

                    if (node != null) {
                        relatedItems.add(node);
                    }
                }
            }
        }

        return rels;
    }

    private String getFirstMetadataValue(Item item, String schema, String element) {
        String value = itemService.getMetadataFirstValue(item, schema, element, null, Item.ANY);
        return value != null ? value : "";
    }

    public class Node {
        UUID id;
        String entityType;
        String title;
        int place;
        Map<String, SortedSet<Node>> rels;

        Node(UUID id, String entityType, String title, int place, Map<String, SortedSet<Node>> rels) {
            this.id = id;
            this.entityType = entityType;
            this.title = title;
            this.place = place;
            this.rels = rels;
        }

        public void print(PrintStream printStream) {
            print(printStream, "");
        }

        private void print(PrintStream printStream, String prefix) {
            String placeString = "";

            if (place >= 0) {
                placeString = "[" + place + "] ";
            }

            printStream.println(prefix + placeString + id + " (" + entityType + ") \"" + title + "\"");

            for (String relName : rels.keySet()) {
                System.out.println(prefix + "  " + relName);

                for (Node child : rels.get(relName)) {
                    child.print(printStream, prefix + "    ");
                }
            }
        }
    }

    public static boolean isScopeAll(String scope) {
        return SCOPE_ALL.equalsIgnoreCase(scope) || "*".equals(scope);
    }

    public static boolean isScopeNone(String scope) {
        return scope == null || scope.isEmpty();
    }
}
