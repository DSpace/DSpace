import { Item } from '../../../core/shared/item.model';
import { of as observableOf } from 'rxjs';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { TruncatePipe } from '../../utils/truncate.pipe';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { TypeBadgeComponent } from './type-badge.component';

let comp: TypeBadgeComponent;
let fixture: ComponentFixture<TypeBadgeComponent>;

const type = 'authorOfPublication';

const mockItemWithEntityType = Object.assign(new Item(), {
  bundles: observableOf({}),
  metadata: {
    'dspace.entity.type': [
      {
        language: 'en_US',
        value: type
      }
    ]
  }
});

const mockItemWithoutEntityType = Object.assign(new Item(), {
  bundles: observableOf({}),
  metadata: {
    'dc.title': [
      {
        language: 'en_US',
        value: 'This is just another title'
      }
    ]
  }
});

describe('ItemTypeBadgeComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [TypeBadgeComponent, TruncatePipe],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(TypeBadgeComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(TypeBadgeComponent);
    comp = fixture.componentInstance;
  }));

  describe('When the item has an entity type', () => {
    beforeEach(() => {
      comp.object = mockItemWithEntityType;
      fixture.detectChanges();
    });

    it('should show the entity type badge', () => {
      const badge = fixture.debugElement.query(By.css('span.badge'));
      expect(badge.nativeElement.textContent).toContain(type.toLowerCase());
    });
  });

  describe('When the item has no entity type', () => {
    beforeEach(() => {
      comp.object = mockItemWithoutEntityType;
      fixture.detectChanges();
    });

    it('should show an item badge', () => {
      const badge = fixture.debugElement.query(By.css('span.badge'));
      expect(badge.nativeElement.textContent).toContain('item');
    });
  });
});
