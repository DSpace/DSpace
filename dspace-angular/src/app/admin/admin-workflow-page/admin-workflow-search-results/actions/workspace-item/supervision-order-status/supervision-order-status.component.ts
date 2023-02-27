import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

import { BehaviorSubject, from, Observable } from 'rxjs';
import { map, mergeMap, reduce } from 'rxjs/operators';

import { SupervisionOrder } from '../../../../../../core/supervision-order/models/supervision-order.model';
import { Group } from '../../../../../../core/eperson/models/group.model';
import { getFirstCompletedRemoteData } from '../../../../../../core/shared/operators';
import { isNotEmpty } from '../../../../../../shared/empty.util';
import { RemoteData } from '../../../../../../core/data/remote-data';

export interface SupervisionOrderListEntry {
  supervisionOrder: SupervisionOrder;
  group: Group
}

@Component({
  selector: 'ds-supervision-order-status',
  templateUrl: './supervision-order-status.component.html',
  styleUrls: ['./supervision-order-status.component.scss']
})
export class SupervisionOrderStatusComponent implements OnChanges {

  /**
   * The list of supervision order object to show
   */
  @Input() supervisionOrderList: SupervisionOrder[] = [];

  /**
   * List of the supervision orders combined with the group
   */
  supervisionOrderEntries$: BehaviorSubject<SupervisionOrderListEntry[]> = new BehaviorSubject([]);

  @Output() delete: EventEmitter<SupervisionOrderListEntry> = new EventEmitter<SupervisionOrderListEntry>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes && changes.supervisionOrderList) {
      this.getSupervisionOrderEntries(changes.supervisionOrderList.currentValue)
        .subscribe((supervisionOrderEntries: SupervisionOrderListEntry[]) => {
          this.supervisionOrderEntries$.next(supervisionOrderEntries);
        });
    }
  }

  /**
   * Create a list of SupervisionOrderListEntry by the given SupervisionOrder list
   *
   * @param supervisionOrderList
   */
  private getSupervisionOrderEntries(supervisionOrderList: SupervisionOrder[]): Observable<SupervisionOrderListEntry[]> {
    return from(supervisionOrderList).pipe(
      mergeMap((so: SupervisionOrder) => so.group.pipe(
        getFirstCompletedRemoteData(),
        map((sogRD: RemoteData<Group>) => {
          if (sogRD.hasSucceeded) {
            const entry: SupervisionOrderListEntry = {
              supervisionOrder: so,
              group: sogRD.payload
            };
            return entry;
          } else {
            return null;
          }
        })
      )),
      reduce((acc: SupervisionOrderListEntry[], value: any) => {
        if (isNotEmpty(value)) {
          return [...acc, value];
        } else {
          return acc;
        }
      }, []),
    );
  }

  /**
   * Emit a delete event with the given SupervisionOrderListEntry.
   */
  deleteSupervisionOrder(supervisionOrder: SupervisionOrderListEntry) {
    this.delete.emit(supervisionOrder);
  }
}
