import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute, ActivatedRouteSnapshot } from '@angular/router';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { RemoteData } from '../../../core/data/remote-data';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DSpaceObjectType } from '../../../core/shared/dspace-object-type.model';
import { hasValue, isNotEmpty } from '../../empty.util';

export enum SelectorActionType {
  CREATE = 'create',
  EDIT = 'edit',
  EXPORT_METADATA = 'export-metadata',
  IMPORT_BATCH = 'import-batch',
  SET_SCOPE = 'set-scope',
  EXPORT_BATCH = 'export-batch'
}

/**
 * Abstract base class that represents a wrapper for modal content used to select a DSpace Object
 */
@Component({
  selector: 'ds-dso-selector-modal',
  template: ''
})
export abstract class DSOSelectorModalWrapperComponent implements OnInit {
  /**
   * The current page's DSO
   */
  @Input() dsoRD: RemoteData<DSpaceObject>;

  /**
   * Optional header to display above the selection list
   * Supports i18n keys
   */
  @Input() header: string;

  /**
   * The type of the DSO that's being edited, created or exported
   */
  objectType: DSpaceObjectType;

  /**
   * The types of DSO that can be selected from this list
   */
  selectorTypes: DSpaceObjectType[];

  /**
   * The type of action to perform
   */
  action: SelectorActionType;

  constructor(protected activeModal: NgbActiveModal, protected route: ActivatedRoute) {
  }

  /**
   * Get de current page's DSO based on the selectorType
   */
  ngOnInit(): void {
    const matchingRoute = this.findRouteData(
      (route: ActivatedRouteSnapshot) => hasValue(route.data.dso),
      this.route.root.snapshot
    );
    if (hasValue(matchingRoute)) {
      this.dsoRD = matchingRoute.data.dso;
    }
  }

  findRouteData(predicate: (value: ActivatedRouteSnapshot, index?: number, obj?: ActivatedRouteSnapshot[]) => unknown, ...routes: ActivatedRouteSnapshot[]) {
    const result = routes.find(predicate);
    if (hasValue(result)) {
      return result;
    } else {
      const nextLevelRoutes = routes
        .map((route: ActivatedRouteSnapshot) => route.children)
        .reduce((combined: ActivatedRouteSnapshot[], current: ActivatedRouteSnapshot[]) => [...combined, ...current]);
      if (isNotEmpty(nextLevelRoutes)) {
        return this.findRouteData(predicate, ...nextLevelRoutes);
      } else {
        return undefined;
      }
    }
  }

  /**
   * Method called when an object has been selected
   * @param dso The selected DSpaceObject
   */
  selectObject(dso: DSpaceObject) {
    this.close();
    this.navigate(dso);
  }

  /**
   * Navigate to a page based on the DSpaceObject provided
   * @param dso The DSpaceObject which can be used to calculate the page to navigate to
   */
  abstract navigate(dso: DSpaceObject);

  /**
   * Close the modal
   */
  close() {
    this.activeModal.close();
  }
}
