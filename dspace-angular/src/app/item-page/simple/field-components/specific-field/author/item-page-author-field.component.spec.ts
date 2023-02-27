import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateLoaderMock } from '../../../../../shared/testing/translate-loader.mock';
import { MetadataValuesComponent } from '../../../../field-components/metadata-values/metadata-values.component';
import { mockItemWithMetadataFieldsAndValue } from '../item-page-field.component.spec';
import { ItemPageAuthorFieldComponent } from './item-page-author-field.component';
import { APP_CONFIG } from '../../../../../../config/app-config.interface';
import { environment } from '../../../../../../environments/environment';
import { BrowseDefinitionDataService } from '../../../../../core/browse/browse-definition-data.service';
import { BrowseDefinitionDataServiceStub } from '../../../../../shared/testing/browse-definition-data-service.stub';

let comp: ItemPageAuthorFieldComponent;
let fixture: ComponentFixture<ItemPageAuthorFieldComponent>;

const mockFields = ['dc.contributor.author', 'dc.creator', 'dc.contributor'];
const mockValue = 'test value';

describe('ItemPageAuthorFieldComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useClass: TranslateLoaderMock
        }
      })],
      providers: [
        { provide: APP_CONFIG, useValue: environment },
        { provide: BrowseDefinitionDataService, useValue: BrowseDefinitionDataServiceStub }
      ],
      declarations: [ItemPageAuthorFieldComponent, MetadataValuesComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ItemPageAuthorFieldComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  for (const field of mockFields) {
    beforeEach(waitForAsync(() => {
      fixture = TestBed.createComponent(ItemPageAuthorFieldComponent);
      comp = fixture.componentInstance;
      comp.item = mockItemWithMetadataFieldsAndValue([field], mockValue);
      fixture.detectChanges();
    }));

    describe(`when the item contains metadata for ${field}`, () => {
      it('should display display the correct metadata value', () => {
        expect(fixture.nativeElement.innerHTML).toContain(mockValue);
      });
    });
  }
});
