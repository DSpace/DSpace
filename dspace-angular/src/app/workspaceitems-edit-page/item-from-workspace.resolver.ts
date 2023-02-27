import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { RemoteData } from '../core/data/remote-data';
import { Item } from '../core/shared/item.model';
import { Store } from '@ngrx/store';
import { SubmissionObjectResolver } from '../core/submission/resolver/submission-object.resolver';
import { WorkspaceitemDataService } from '../core/submission/workspaceitem-data.service';

/**
 * This class represents a resolver that requests a specific item before the route is activated
 */
@Injectable()
export class ItemFromWorkspaceResolver extends SubmissionObjectResolver<Item> implements Resolve<RemoteData<Item>>  {
    constructor(
        private workspaceItemService: WorkspaceitemDataService,
        protected store: Store<any>
    ) {
        super(workspaceItemService, store);
    }

}
