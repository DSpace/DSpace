import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DSpaceObjectType } from '../../../../core/shared/dspace-object-type.model';
import { DSpaceObject } from '../../../../core/shared/dspace-object.model';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import {
  DSOSelectorModalWrapperComponent,
  SelectorActionType
} from '../dso-selector-modal-wrapper.component';
import { getCollectionEditRoute } from '../../../../collection-page/collection-page-routing-paths';

/**
 * Component to wrap a list of existing collections inside a modal
 * Used to choose a collection from to edit
 */

@Component({
  selector: 'ds-edit-collection-selector',
  templateUrl: '../dso-selector-modal-wrapper.component.html',
})
export class EditCollectionSelectorComponent extends DSOSelectorModalWrapperComponent implements OnInit {
  objectType = DSpaceObjectType.COLLECTION;
  selectorTypes = [DSpaceObjectType.COLLECTION];
  action = SelectorActionType.EDIT;

  constructor(protected activeModal: NgbActiveModal, protected route: ActivatedRoute, private router: Router) {
    super(activeModal, route);
  }

  /**
   * Navigate to the collection edit page
   */
  navigate(dso: DSpaceObject) {
    this.router.navigate([getCollectionEditRoute(dso.uuid)]);
  }
}
