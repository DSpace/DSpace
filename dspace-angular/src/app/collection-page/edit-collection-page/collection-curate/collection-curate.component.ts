import { Component } from '@angular/core';
import { filter, map, take } from 'rxjs/operators';
import { RemoteData } from '../../../core/data/remote-data';
import { Observable } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { DSONameService } from '../../../core/breadcrumbs/dso-name.service';
import { Collection } from '../../../core/shared/collection.model';
import { hasValue } from '../../../shared/empty.util';

/**
 * Component for managing a collection's curation tasks
 */
@Component({
  selector: 'ds-collection-curate',
  templateUrl: './collection-curate.component.html',
})
export class CollectionCurateComponent {
  dsoRD$: Observable<RemoteData<Collection>>;
  collectionName$: Observable<string>;

  constructor(
    private route: ActivatedRoute,
    private dsoNameService: DSONameService,
  ) {
  }

  ngOnInit(): void {
    this.dsoRD$ = this.route.parent.data.pipe(
      take(1),
      map((data) => data.dso),
    );

    this.collectionName$ = this.dsoRD$.pipe(
      filter((rd: RemoteData<Collection>) => hasValue(rd)),
      map((rd: RemoteData<Collection>) => {
        return this.dsoNameService.getName(rd.payload);
      })
    );
  }
}
