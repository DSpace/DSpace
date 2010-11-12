/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;

class IndexingTask
{
    enum Action { ADD, UPDATE, DELETE };

    private Action   action;
    private Term     term;
    private Document doc;

    IndexingTask(Action pAction, Term pTerm, Document pDoc)
    {
        action = pAction;
        term = pTerm;
        doc = pDoc;
    }

    boolean isAdd()
    {
        return action == Action.ADD;
    }

    boolean isDelete()
    {
        return action == Action.DELETE;
    }

    boolean isUpdate()
    {
        return action == Action.UPDATE;
    }

    Term getTerm()
    {
        return term;
    }

    Document getDocument()
    {
        return doc;
    }
}
