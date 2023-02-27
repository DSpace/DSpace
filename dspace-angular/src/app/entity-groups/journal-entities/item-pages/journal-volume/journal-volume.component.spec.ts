import { Item } from '../../../../core/shared/item.model';
import { buildPaginatedList } from '../../../../core/data/paginated-list.model';
import { PageInfo } from '../../../../core/shared/page-info.model';
import { JournalVolumeComponent } from './journal-volume.component';
import {
  createRelationshipsObservable,
  getItemPageFieldsTest
} from '../../../../item-page/simple/item-types/shared/item.component.spec';
import { createSuccessfulRemoteDataObject$ } from '../../../../shared/remote-data.utils';

const mockItem: Item = Object.assign(new Item(), {
  bundles: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])),
  metadata: {
    'publicationvolume.volumeNumber': [
      {
        language: 'en_US',
        value: '1234'
      }
    ],
    'creativework.datePublished': [
      {
        language: 'en_US',
        value: '2018'
      }
    ],
    'dc.description': [
      {
        language: 'en_US',
        value: 'desc'
      }
    ]
  },
  relationships: createRelationshipsObservable()
});

describe('JournalVolumeComponent', getItemPageFieldsTest(mockItem, JournalVolumeComponent));
