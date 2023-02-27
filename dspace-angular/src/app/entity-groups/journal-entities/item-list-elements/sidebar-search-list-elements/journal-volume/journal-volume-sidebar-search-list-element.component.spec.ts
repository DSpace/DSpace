import { Item } from '../../../../../core/shared/item.model';
import { Collection } from '../../../../../core/shared/collection.model';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { createSidebarSearchListElementTests } from '../../../../../shared/object-list/sidebar-search-list-element/sidebar-search-list-element.component.spec';
import { JournalVolumeSidebarSearchListElementComponent } from './journal-volume-sidebar-search-list-element.component';

const object = Object.assign(new ItemSearchResult(), {
  indexableObject: Object.assign(new Item(), {
    id: 'test-item',
    metadata: {
      'dc.title': [
        {
          value: 'title'
        }
      ],
      'journal.title': [
        {
          value: 'journal title'
        }
      ],
      'publicationvolume.volumeNumber': [
        {
          value: '1'
        },
        {
          value: '2'
        }
      ]
    }
  })
});
const parent = Object.assign(new Collection(), {
  id: 'test-collection',
  metadata: {
    'dc.title': [
      {
        value: 'parent title'
      }
    ]
  }
});

describe('JournalVolumeSidebarSearchListElementComponent',
  createSidebarSearchListElementTests(JournalVolumeSidebarSearchListElementComponent, object, parent, 'parent title', 'title', 'journal title (1) (2)')
);
