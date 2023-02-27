import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { FlatTreeControl } from '@angular/cdk/tree';

import { BehaviorSubject, merge, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { VocabularyTreeFlattener } from './vocabulary-tree-flattener';

/**
 * Data source for flat tree.
 * The data source need to handle expansion/collapsion of the tree node and change the data feed
 * to `MatTree`.
 * The nested tree nodes of type `T` are flattened through `MatTreeFlattener`, and converted
 * to type `F` for `MatTree` to consume.
 */
export class VocabularyTreeFlatDataSource<T, F> extends DataSource<F> {
  _flattenedData = new BehaviorSubject<F[]>([]);

  _expandedData = new BehaviorSubject<F[]>([]);

  _data: BehaviorSubject<T[]>;
  get data() { return this._data.value; }
  set data(value: T[]) {
    this._data.next(value);
    this._flattenedData.next(this._treeFlattener.flattenNodes(this.data));
    this._treeControl.dataNodes = this._flattenedData.value;
  }

  constructor(private _treeControl: FlatTreeControl<F>,
              private _treeFlattener: VocabularyTreeFlattener<T, F>,
              initialData: T[] = []) {
    super();
    this._data = new BehaviorSubject<T[]>(initialData);
  }

  connect(collectionViewer: CollectionViewer): Observable<F[]> {
    const changes = [
      collectionViewer.viewChange,
      this._treeControl.expansionModel.changed,
      this._flattenedData
    ];
    return merge<any>(...changes).pipe(map((): F[] => {
      this._expandedData.next(
        this._treeFlattener.expandFlattenedNodes(this._flattenedData.value, this._treeControl));
      return this._expandedData.value;
    }));
  }

  disconnect() {
    // no op
  }
}
