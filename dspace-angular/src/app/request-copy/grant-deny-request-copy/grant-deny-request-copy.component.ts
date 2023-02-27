import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { map, switchMap } from 'rxjs/operators';
import { ItemRequest } from '../../core/shared/item-request.model';
import { Observable } from 'rxjs';
import {
  getFirstCompletedRemoteData,
  getFirstSucceededRemoteDataPayload
} from '../../core/shared/operators';
import { RemoteData } from '../../core/data/remote-data';
import { AuthService } from '../../core/auth/auth.service';
import { getRequestCopyDenyRoute, getRequestCopyGrantRoute } from '../request-copy-routing-paths';
import { Item } from '../../core/shared/item.model';
import { ItemDataService } from '../../core/data/item-data.service';
import { DSONameService } from '../../core/breadcrumbs/dso-name.service';
import { getItemPageRoute } from '../../item-page/item-page-routing-paths';
import { redirectOn4xx } from '../../core/shared/authorized.operators';

@Component({
  selector: 'ds-grant-deny-request-copy',
  styleUrls: ['./grant-deny-request-copy.component.scss'],
  templateUrl: './grant-deny-request-copy.component.html'
})
/**
 * Component for an author to decide to grant or deny an item request
 */
export class GrantDenyRequestCopyComponent implements OnInit {
  /**
   * The item request to grant or deny
   */
  itemRequestRD$: Observable<RemoteData<ItemRequest>>;

  /**
   * The item the request is requesting access to
   */
  itemRD$: Observable<RemoteData<Item>>;

  /**
   * The name of the item
   */
  itemName$: Observable<string>;

  /**
   * The url of the item
   */
  itemUrl$: Observable<string>;

  /**
   * The route to the page for denying access to the item
   */
  denyRoute$: Observable<string>;

  /**
   * The route to the page for granting access to the item
   */
  grantRoute$: Observable<string>;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    private itemDataService: ItemDataService,
    private nameService: DSONameService,
  ) {

  }

  ngOnInit(): void {
    this.itemRequestRD$ = this.route.data.pipe(
      map((data) => data.request as RemoteData<ItemRequest>),
      getFirstCompletedRemoteData(),
      redirectOn4xx(this.router, this.authService),
    );
    this.itemRD$ = this.itemRequestRD$.pipe(
      getFirstSucceededRemoteDataPayload(),
      switchMap((itemRequest: ItemRequest) => this.itemDataService.findById(itemRequest.itemId)),
    );
    this.itemName$ = this.itemRD$.pipe(
      getFirstSucceededRemoteDataPayload(),
      map((item) => this.nameService.getName(item)),
    );
    this.itemUrl$ = this.itemRD$.pipe(
      getFirstSucceededRemoteDataPayload(),
      map((item) => getItemPageRoute(item)),
    );

    this.denyRoute$ = this.itemRequestRD$.pipe(
      getFirstSucceededRemoteDataPayload(),
      map((itemRequest: ItemRequest) => getRequestCopyDenyRoute(itemRequest.token))
    );
    this.grantRoute$ = this.itemRequestRD$.pipe(
      getFirstSucceededRemoteDataPayload(),
      map((itemRequest: ItemRequest) => getRequestCopyGrantRoute(itemRequest.token))
    );
  }

}
