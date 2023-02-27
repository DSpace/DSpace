import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Item } from '../../../../core/shared/item.model';
import { TranslateLoaderMock } from '../../../../shared/mocks/translate-loader.mock';
import { ItemPageFieldComponent } from './item-page-field.component';
import { MetadataValuesComponent } from '../../../field-components/metadata-values/metadata-values.component';
import { MetadataMap, MetadataValue } from '../../../../core/shared/metadata.models';
import { createSuccessfulRemoteDataObject$ } from '../../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../../shared/testing/utils.test';
import { environment } from '../../../../../environments/environment';
import { MarkdownPipe } from '../../../../shared/utils/markdown.pipe';
import { SharedModule } from '../../../../shared/shared.module';
import { APP_CONFIG } from '../../../../../config/app-config.interface';
import { By } from '@angular/platform-browser';
import { BrowseDefinitionDataService } from '../../../../core/browse/browse-definition-data.service';
import { BrowseDefinitionDataServiceStub } from '../../../../shared/testing/browse-definition-data-service.stub';
import { RouterTestingModule } from '@angular/router/testing';

let comp: ItemPageFieldComponent;
let fixture: ComponentFixture<ItemPageFieldComponent>;
let markdownSpy;

const mockValue = 'test value';
const mockField = 'dc.test';
const mockLabel = 'test label';
const mockAuthorField = 'dc.contributor.author';
const mockDateIssuedField = 'dc.date.issued';
const mockFields = [mockField, mockAuthorField, mockDateIssuedField];

describe('ItemPageFieldComponent', () => {

  let appConfig = Object.assign({}, environment, {
    markdown: {
      enabled: false,
      mathjax: false,
    }
  });

  const buildTestEnvironment = async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes([]),
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
        SharedModule,
      ],
      providers: [
        { provide: APP_CONFIG, useValue: appConfig },
        { provide: BrowseDefinitionDataService, useValue: BrowseDefinitionDataServiceStub }
      ],
      declarations: [ItemPageFieldComponent, MetadataValuesComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ItemPageFieldComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
    markdownSpy = spyOn(MarkdownPipe.prototype, 'transform');
    fixture = TestBed.createComponent(ItemPageFieldComponent);
    comp = fixture.componentInstance;
    comp.item = mockItemWithMetadataFieldsAndValue(mockFields, mockValue);
    comp.fields = mockFields;
    comp.label = mockLabel;
    fixture.detectChanges();
  };

  it('should display display the correct metadata value', waitForAsync(async () => {
    await buildTestEnvironment();
    expect(fixture.nativeElement.innerHTML).toContain(mockValue);
  }));

  describe('when markdown is disabled in the environment config', () => {

    beforeEach(waitForAsync(async () => {
      appConfig.markdown.enabled = false;
      await buildTestEnvironment();
    }));

    describe('and markdown is disabled in this component', () => {

      beforeEach(() => {
        comp.enableMarkdown = false;
        fixture.detectChanges();
      });

      it('should not use the Markdown Pipe', () => {
        expect(markdownSpy).not.toHaveBeenCalled();
      });
    });

    describe('and markdown is enabled in this component', () => {

      beforeEach(() => {
        comp.enableMarkdown = true;
        fixture.detectChanges();
      });

      it('should not use the Markdown Pipe', () => {
        expect(markdownSpy).not.toHaveBeenCalled();
      });
    });
  });

  describe('when markdown is enabled in the environment config', () => {

    beforeEach(waitForAsync(async () => {
      appConfig.markdown.enabled = true;
      await buildTestEnvironment();
    }));

    describe('and markdown is disabled in this component', () => {

      beforeEach(() => {
        comp.enableMarkdown = false;
        fixture.detectChanges();
      });

      it('should not use the Markdown Pipe', () => {
        expect(markdownSpy).not.toHaveBeenCalled();
      });
    });

    describe('and markdown is enabled in this component', () => {

      beforeEach(() => {
        comp.enableMarkdown = true;
        fixture.detectChanges();
      });

      it('should use the Markdown Pipe', () => {
        expect(markdownSpy).toHaveBeenCalled();
      });
    });

  });

  describe('test rendering of configured browse links', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });
    waitForAsync(() => {
      it('should have a browse link', () => {
        expect(fixture.debugElement.query(By.css('a.ds-browse-link')).nativeElement.innerHTML).toContain(mockValue);
      });
    });
  });

  describe('test rendering of configured regex-based links', () => {
    beforeEach(() => {
      comp.urlRegex = '^test';
      fixture.detectChanges();
    });
    waitForAsync(() => {
      it('should have a rendered (non-browse) link since the value matches ^test', () => {
        expect(fixture.debugElement.query(By.css('a.ds-simple-metadata-link')).nativeElement.innerHTML).toContain(mockValue);
      });
    });
  });

  describe('test skipping of configured links that do NOT match regex', () => {
    beforeEach(() => {
      comp.urlRegex = '^nope';
      fixture.detectChanges();
    });
    beforeEach(waitForAsync(() => {
      it('should NOT have a rendered (non-browse) link since the value matches ^test', () => {
        expect(fixture.debugElement.query(By.css('a.ds-simple-metadata-link'))).toBeNull();
      });
    }));
  });


});

export function mockItemWithMetadataFieldsAndValue(fields: string[], value: string): Item {
  const item = Object.assign(new Item(), {
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    metadata: new MetadataMap()
  });
  fields.forEach((field: string) => {
    item.metadata[field] = [{
      language: 'en_US',
      value: value
    }] as MetadataValue[];
  });
  return item;
}
