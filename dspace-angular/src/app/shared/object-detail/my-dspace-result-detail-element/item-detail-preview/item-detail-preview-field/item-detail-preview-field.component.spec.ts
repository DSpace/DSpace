import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { of as observableOf } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { ItemDetailPreviewFieldComponent } from './item-detail-preview-field.component';
import { Item } from '../../../../../core/shared/item.model';
import { TruncatePipe } from '../../../../utils/truncate.pipe';
import { TranslateLoaderMock } from '../../../../mocks/translate-loader.mock';
import { By } from '@angular/platform-browser';

let component: ItemDetailPreviewFieldComponent;
let fixture: ComponentFixture<ItemDetailPreviewFieldComponent>;

const mockItemWithAuthorAndDate: Item = Object.assign(new Item(), {
  bundles: observableOf({}),
  metadata: {
    'dc.contributor.author': [
      {
        language: 'en_US',
        value: 'Smith, Donald'
      }
    ],
    'dc.date.issued': [
      {
        language: null,
        value: '2015-06-26'
      }
    ],
    'dc.title': [
      {
        language: 'en_US',
        value: 'This is just another title'
      }
    ],
    'dc.type': [
      {
        language: null,
        value: 'Article'
      }
    ]
  }
});

describe('ItemDetailPreviewFieldComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
      ],
      declarations: [ItemDetailPreviewFieldComponent, TruncatePipe],
      providers: [
        { provide: 'objectElementProvider', useValue: { mockItemWithAuthorAndDate } }

      ],

      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ItemDetailPreviewFieldComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(ItemDetailPreviewFieldComponent);
    component = fixture.componentInstance;

  }));

  beforeEach(() => {
    component.object = { hitHighlights: {} } as any;
    component.item = mockItemWithAuthorAndDate;
    component.label = 'test label';
    component.metadata = 'dc.title';
    component.placeholder = 'No title';
    fixture.detectChanges();
  });

  it('should display dc.title value', () => {
    const span = fixture.debugElement.query(By.css('span'));
    expect(span.nativeElement.innerHTML).toContain('This is just another title');
  });

  it('should display placeholder when metadata has no value', () => {
    component.metadata = 'dc.abstract';
    component.placeholder = 'No abstract';
    fixture.detectChanges();
    const span = fixture.debugElement.query(By.css('.text-muted'));
    expect(span.nativeElement.innerHTML).toContain('No abstract');
  });
});
