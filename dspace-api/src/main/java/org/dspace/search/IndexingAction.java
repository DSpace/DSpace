package org.dspace.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;

class IndexingAction
{
    enum Action { ADD, UPDATE, DELETE };

    private Action   action;
    private Term     term;
    private Document doc;

    IndexingAction(Action pAction, Term pTerm, Document pDoc)
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
