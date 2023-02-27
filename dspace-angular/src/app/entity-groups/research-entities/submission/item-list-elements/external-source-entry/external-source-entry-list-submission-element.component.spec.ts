import { ExternalSourceEntryListSubmissionElementComponent } from './external-source-entry-list-submission-element.component';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { ExternalSourceEntry } from '../../../../../core/shared/external-source-entry.model';
import { TranslateModule } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('ExternalSourceEntryListSubmissionElementComponent', () => {
  let component: ExternalSourceEntryListSubmissionElementComponent;
  let fixture: ComponentFixture<ExternalSourceEntryListSubmissionElementComponent>;

  const uri = 'https://orcid.org/0001-0001-0001-0001';
  const entry = Object.assign(new ExternalSourceEntry(), {
    id: '0001-0001-0001-0001',
    display: 'John Doe',
    value: 'John, Doe',
    metadata: {
      'dc.identifier.uri': [
        {
          value: uri
        }
      ]
    }
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ExternalSourceEntryListSubmissionElementComponent],
      imports: [TranslateModule.forRoot()],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ExternalSourceEntryListSubmissionElementComponent);
    component = fixture.componentInstance;
    component.object = entry;
    fixture.detectChanges();
  });

  it('should display the entry\'s display value', () => {
    expect(fixture.nativeElement.textContent).toContain(entry.display);
  });

  it('should display the entry\'s uri', () => {
    expect(fixture.nativeElement.textContent).toContain(uri);
  });
});
