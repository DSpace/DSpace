import { Item } from '../../../../../core/shared/item.model';
import { Collection } from '../../../../../core/shared/collection.model';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { createSidebarSearchListElementTests } from '../../../../../shared/object-list/sidebar-search-list-element/sidebar-search-list-element.component.spec';
import { OrgUnitSidebarSearchListElementComponent } from './org-unit-sidebar-search-list-element.component';

const object = Object.assign(new ItemSearchResult(), {
  indexableObject: Object.assign(new Item(), {
    id: 'test-item',
    metadata: {
      'dspace.entity.type': [
        {
          value: 'OrgUnit'
        }
      ],
      'organization.legalName': [
        {
          value: 'title'
        }
      ],
      'dc.description': [
        {
          value: 'description'
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

describe('OrgUnitSidebarSearchListElementComponent',
  createSidebarSearchListElementTests(OrgUnitSidebarSearchListElementComponent, object, parent, 'parent title', 'title', 'description')
);
