import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { ChangeDetectionStrategy, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateLoaderMock } from '../../../shared/mocks/translate-loader.mock';
import { By } from '@angular/platform-browser';
import { MetadataUriValuesComponent } from './metadata-uri-values.component';
import { isNotEmpty } from '../../../shared/empty.util';
import { MetadataValue } from '../../../core/shared/metadata.models';
import { APP_CONFIG } from '../../../../config/app-config.interface';
import { environment } from '../../../../environments/environment';

let comp: MetadataUriValuesComponent;
let fixture: ComponentFixture<MetadataUriValuesComponent>;

const mockMetadata = [
  {
    language: 'en_US',
    value: 'http://fakelink.org'
  },
  {
    language: 'en_US',
    value: 'http://another.fakelink.org'
  }
] as MetadataValue[];
const mockSeperator = '<br/>';
const mockLabel = 'fake.message';
const mockLinkText = 'fake link text';

describe('MetadataUriValuesComponent', () => {
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
      ],
      declarations: [MetadataUriValuesComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(MetadataUriValuesComponent, {
      set: {changeDetection: ChangeDetectionStrategy.Default}
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(MetadataUriValuesComponent);
    comp = fixture.componentInstance;
    comp.mdValues = mockMetadata;
    comp.separator = mockSeperator;
    comp.label = mockLabel;
    fixture.detectChanges();
  }));

  it('should display all metadata values', () => {
    const innerHTML = fixture.nativeElement.innerHTML;
    for (const metadatum of mockMetadata) {
      expect(innerHTML).toContain(metadatum.value);
    }
  });

  it('should contain the correct hrefs', () => {
    const links = fixture.debugElement.queryAll(By.css('a'));
    for (const metadatum of mockMetadata) {
      expect(containsHref(links, metadatum.value)).toBeTruthy();
    }
  });

  it('should contain separators equal to the amount of metadata values minus one', () => {
    const separators = fixture.debugElement.queryAll(By.css('a span'));
    expect(separators.length).toBe(mockMetadata.length - 1);
  });

  describe('when linktext is defined', () => {

    beforeEach(() => {
      comp.linktext = mockLinkText;
      fixture.detectChanges();
    });

    it('should replace the metadata value with the linktext', () => {
      const link = fixture.debugElement.query(By.css('a'));
      expect(link.nativeElement.textContent).toContain(mockLinkText);
    });

  });

});

function containsHref(links: DebugElement[], href: string): boolean {
  for (const link of links) {
    const hrefAtt = link.properties.href;
    if (isNotEmpty(hrefAtt)) {
      if (hrefAtt === href) {
        return true;
      }
    }
  }
  return false;
}
