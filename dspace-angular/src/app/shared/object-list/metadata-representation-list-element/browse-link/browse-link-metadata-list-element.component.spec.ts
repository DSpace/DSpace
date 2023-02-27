import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { BrowseLinkMetadataListElementComponent } from './browse-link-metadata-list-element.component';
import { MetadatumRepresentation } from '../../../../core/shared/metadata-representation/metadatum/metadatum-representation.model';

const mockMetadataRepresentation = Object.assign(new MetadatumRepresentation('type'), {
  key: 'dc.contributor.author',
  value: 'Test Author'
});

const mockMetadataRepresentationWithUrl = Object.assign(new MetadatumRepresentation('type'), {
  key: 'dc.subject',
  value: 'http://purl.org/test/subject'
});

describe('BrowseLinkMetadataListElementComponent', () => {
  let comp: BrowseLinkMetadataListElementComponent;
  let fixture: ComponentFixture<BrowseLinkMetadataListElementComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [],
      declarations: [BrowseLinkMetadataListElementComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(BrowseLinkMetadataListElementComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(BrowseLinkMetadataListElementComponent);
    comp = fixture.componentInstance;
    comp.metadataRepresentation = mockMetadataRepresentation;
    fixture.detectChanges();
  }));

  waitForAsync(() => {
    it('should contain the value as a browse link', () => {
      expect(fixture.debugElement.nativeElement.textContent).toContain(mockMetadataRepresentation.value);
    });
    it('should NOT match isLink', () => {
      expect(comp.isLink).toBe(false);
    });
  });

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(BrowseLinkMetadataListElementComponent);
    comp = fixture.componentInstance;
    comp.metadataRepresentation = mockMetadataRepresentationWithUrl;
    fixture.detectChanges();
  }));

  waitForAsync(() => {
    it('should contain the value expected', () => {
      expect(fixture.debugElement.nativeElement.textContent).toContain(mockMetadataRepresentationWithUrl.value);
    });
    it('should match isLink', () => {
      expect(comp.isLink).toBe(true);
    });
  });

});
