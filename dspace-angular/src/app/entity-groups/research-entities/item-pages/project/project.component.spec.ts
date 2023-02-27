import {
  createRelationshipsObservable,
  getItemPageFieldsTest
} from '../../../../item-page/simple/item-types/shared/item.component.spec';
import { buildPaginatedList } from '../../../../core/data/paginated-list.model';
import { Item } from '../../../../core/shared/item.model';
import { PageInfo } from '../../../../core/shared/page-info.model';
import { createSuccessfulRemoteDataObject$ } from '../../../../shared/remote-data.utils';
import { ProjectComponent } from './project.component';

const mockItem: Item = Object.assign(new Item(), {
  bundles: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
  metadata: {
    // 'project.identifier.status': [
    //   {
    //     language: 'en_US',
    //     value: 'published'
    //   }
    // ],
    'dc.identifier': [
      {
        language: 'en_US',
        value: '1'
      }
    ],
    // 'project.identifier.expectedcompletion': [
    //   {
    //     language: 'en_US',
    //     value: 'exp comp'
    //   }
    // ],
    'dc.description': [
      {
        language: 'en_US',
        value: 'keyword'
      }
    ],
    'dc.subject': [
      {
        language: 'en_US',
        value: 'keyword'
      }
    ]
  },
  relationships: createRelationshipsObservable()
});

describe('ProjectComponent', getItemPageFieldsTest(mockItem, ProjectComponent));
