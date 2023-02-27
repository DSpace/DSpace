import { Component, Input, OnInit } from '@angular/core';

import { Observable, of } from 'rxjs';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';
import { SubscriptionModalComponent } from '../../subscriptions/subscription-modal/subscription-modal.component';
import { FeatureID } from '../../../core/data/feature-authorization/feature-id';

@Component({
  selector: 'ds-dso-page-subscription-button',
  templateUrl: './dso-page-subscription-button.component.html',
  styleUrls: ['./dso-page-subscription-button.component.scss']
})
/**
 * Display a button that opens the modal to manage subscriptions
 */
export class DsoPageSubscriptionButtonComponent implements OnInit {

  /**
   * Whether the current user is authorized to edit the DSpaceObject
   */
  isAuthorized$: Observable<boolean> = of(false);

  /**
   * Reference to NgbModal
   */
  public modalRef: NgbModalRef;

  /**
   * DSpaceObject that is being viewed
   */
  @Input() dso: DSpaceObject;

  constructor(
    protected authorizationService: AuthorizationDataService,
    private modalService: NgbModal,
  ) {
  }

  /**
   * check if the current DSpaceObject can be subscribed by the user
   */
  ngOnInit(): void {
    this.isAuthorized$ = this.authorizationService.isAuthorized(FeatureID.CanSubscribe, this.dso.self);
  }

  /**
   * Open the modal to subscribe to the related DSpaceObject
   */
  public openSubscriptionModal() {
    this.modalRef = this.modalService.open(SubscriptionModalComponent);
    this.modalRef.componentInstance.dso = this.dso;
  }

}
