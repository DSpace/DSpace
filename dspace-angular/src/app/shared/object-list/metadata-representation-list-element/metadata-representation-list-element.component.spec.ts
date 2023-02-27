import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { MetadatumRepresentation } from '../../../core/shared/metadata-representation/metadatum/metadatum-representation.model';
import { mockData } from '../../testing/browse-definition-data-service.stub';
import { MetadataRepresentationListElementComponent } from './metadata-representation-list-element.component';

// Mock metadata representation values
const mockMetadataRepresentation = Object.assign(new MetadatumRepresentation('type', mockData[1]), {
  key: 'dc.contributor.author',
  value: 'Test Author'
});
const mockMetadataRepresentationUrl = Object.assign(new MetadatumRepresentation('type', mockData[1]), {
  key: 'dc.subject',
  value: 'https://www.google.com'
});

describe('MetadataRepresentationListElementComponent', () => {
  let comp: MetadataRepresentationListElementComponent;
  let fixture: ComponentFixture<MetadataRepresentationListElementComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [],
      declarations: [MetadataRepresentationListElementComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(MetadataRepresentationListElementComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(MetadataRepresentationListElementComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  }));

  describe('when the value is not a URL', () => {
    beforeEach(() => {
      comp.metadataRepresentation = mockMetadataRepresentation;
    });
    it('isLink correctly detects a non-URL string as false', () => {
      waitForAsync(() => {
        expect(comp.isLink()).toBe(false);
      });
    });
  });

  describe('when the value is a URL', () => {
    beforeEach(() => {
      comp.metadataRepresentation = mockMetadataRepresentationUrl;
    });
    it('isLink correctly detects a URL string as true', () => {
      waitForAsync(() => {
        expect(comp.isLink()).toBe(true);
      });
    });
  });

});
