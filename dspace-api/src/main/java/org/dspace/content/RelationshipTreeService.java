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
    public static final String SCOPE_RECURSIVE_SUFFIX = ":r";

    public Node getTree(Context context, Item item, String scopeString) {
        Node node = null;
        getNode(context, item, buildScopeMap(scopeString), new HashSet<>(), -1, node);
        return node;
    }

    public Set<UUID> getItemsInTree(Context context, Item item, String scopeString, boolean includeRoot) {
        Set<UUID> itemsInTree = new HashSet<>();
        Node node = null;
        getNode(context, item, buildScopeMap(scopeString), itemsInTree, -1, node);
        if (!includeRoot) {
            itemsInTree.remove(item.getID());
        }
        return itemsInTree;
    }

    public static Map<String, Boolean> buildScopeMap(String scopeString) {
        Map<String, Boolean> scope = new HashMap<>();
        if (isScopeNone(scopeString)) {
            return scope; // empty map = no traversal
        }
        if (isScopeAll(scopeString)) {
            scope.put(SCOPE_ALL, true); // recursive by definition
            return scope;
        }
        for (String part : scopeString.split(",")) {
            boolean recursive = part.endsWith(SCOPE_RECURSIVE_SUFFIX);
            String relName = recursive ? part.substring(0, part.length() - SCOPE_RECURSIVE_SUFFIX.length()) : part;
            scope.put(relName, recursive);
        }
        return scope;
    }

    private void getNode(Context context, Item item, Map<String, Boolean> scope,
                         Set<UUID> itemsInTree, int place, Node node) {
        try {
            if (item.getHandle() == null) {
                //Not archived don't add
            } else {
                itemsInTree.add(item.getID());
                node = new Node(item.getID(), getFirstMetadataValue(item, "relationship", "type"),
                        getFirstMetadataValue(item, "dc", "title"),
                        place, getRels(context, item, scope, itemsInTree));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, SortedSet<Node>> getRels(Context context, Item item, Map<String, Boolean> scope,
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
            if (scope.containsKey(relName) || scope.containsKey(SCOPE_ALL)) {
                // we care about this relationship
                SortedSet<Node> relatedItems = rels.get(relName);
                if (relatedItems == null) {
                    relatedItems = new TreeSet<>(Comparator.comparingInt(node -> node.place));
                    rels.put(relName, relatedItems);
                }
                Map<String, Boolean> childScope;
                if (itemsInTree.contains(childItem.getID())) {
                    childScope = Map.of();
                } else {
                    // if the child isn't in the tree yet, include in-scope rels
                    childScope = new HashMap<>(scope);
                    // ..but exclude the current relName if it's non-recursive
                    boolean recursive = false;
                    if (scope.containsKey(SCOPE_ALL)) {
                        recursive = scope.get(SCOPE_ALL);
                        if (!recursive) {
                            childScope.remove(SCOPE_ALL);
                        }
                    }
                    if (scope.containsKey(relName)) { // if exact relName is specified, prefer its recursive setting
                        recursive = scope.get(relName);
                        if (!recursive) {
                            childScope.remove(relName); // don't go deeper for this relName if given as non-recursive
                        }
                    }
                }
                Node node = null;
                getNode(context, childItem, childScope, itemsInTree, childPlace, node);
                if (node != null) {
                    relatedItems.add(node);
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

    public static boolean isRecursive(String scope) {
        if (isScopeNone(scope)) {
            return false;
        }
        if (isScopeAll(scope)) {
            return true;
        }
        return scope.endsWith(SCOPE_RECURSIVE_SUFFIX);
    }

    public static ParsedScope parseScope(String scope) {
        if (isScopeNone(scope) || isScopeAll(scope)) {
            return null;
        }
        boolean recursive = scope.endsWith(SCOPE_RECURSIVE_SUFFIX);
        String typeName = recursive
                ? scope.substring(0, scope.length() - SCOPE_RECURSIVE_SUFFIX.length())
                : scope;
        return new ParsedScope(typeName, recursive);
    }

    public static final class ParsedScope {
        public final String typeName;
        public final boolean recursive;

        public ParsedScope(String typeName, boolean recursive) {
            this.typeName  = typeName;
            this.recursive = recursive;
        }
    }
}

