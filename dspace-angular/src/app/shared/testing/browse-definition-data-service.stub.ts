import { EMPTY, Observable, of as observableOf } from 'rxjs';
import { RemoteData } from '../../core/data/remote-data';
import { buildPaginatedList, PaginatedList } from '../../core/data/paginated-list.model';
import { BrowseDefinition } from '../../core/shared/browse-definition.model';
import { BrowseService } from '../../core/browse/browse.service';
import { createSuccessfulRemoteDataObject } from '../remote-data.utils';
import { PageInfo } from '../../core/shared/page-info.model';

// This data is in post-serialized form (metadata -> metadataKeys)
export const mockData: BrowseDefinition[] = [
      Object.assign(new BrowseDefinition, {
      'id' : 'dateissued',
      'metadataBrowse' : false,
      'dataType' : 'date',
      'sortOptions' : EMPTY,
      'order' : 'ASC',
      'type' : 'browse',
      'metadataKeys' : [ 'dc.date.issued' ],
      '_links' : EMPTY
     }),
     Object.assign(new BrowseDefinition, {
      'id' : 'author',
      'metadataBrowse' : true,
      'dataType' : 'text',
      'sortOptions' : EMPTY,
      'order' : 'ASC',
      'type' : 'browse',
      'metadataKeys' : [ 'dc.contributor.*', 'dc.creator' ],
      '_links' : EMPTY
    })
];

export const BrowseDefinitionDataServiceStub: any = {

  /**
   * Get all BrowseDefinitions
   */
  findAll(): Observable<RemoteData<PaginatedList<BrowseDefinition>>> {
    return observableOf(createSuccessfulRemoteDataObject(buildPaginatedList(new PageInfo(), mockData)));
  },

  /**
   * Get all BrowseDefinitions with any link configuration
   */
  findAllLinked(): Observable<RemoteData<PaginatedList<BrowseDefinition>>> {
    return observableOf(createSuccessfulRemoteDataObject(buildPaginatedList(new PageInfo(), mockData)));
  },

  /**
   * Get the browse URL by providing a list of metadata keys
   *
   * @param metadataKeys  a list of fields eg. ['dc.contributor.author', 'dc.creator']
   */
  findByFields(metadataKeys: string[]): Observable<RemoteData<BrowseDefinition>> {
    let searchKeyArray: string[] = [];
    metadataKeys.forEach((metadataKey) => {
      searchKeyArray = searchKeyArray.concat(BrowseService.toSearchKeyArray(metadataKey));
    });
    // Return just the first, as a pretend match
    return observableOf(createSuccessfulRemoteDataObject(mockData[0]));
  }

};
