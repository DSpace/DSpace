import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { ItemMetadataRepresentation } from '../../../../core/shared/metadata-representation/item/item-metadata-representation.model';
import { Item } from '../../../../core/shared/item.model';
import { PersonItemMetadataListElementComponent } from './person-item-metadata-list-element.component';
import { MetadataValue } from '../../../../core/shared/metadata.models';

const jobTitle = 'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.';
const firstName = 'Joe';
const lastName = 'Anonymous';
const mockItem = Object.assign(new Item(), { metadata: { 'person.jobTitle': [{ value: jobTitle }], 'person.givenName': [{ value: firstName }], 'person.familyName': [{ value: lastName }] } });
const virtMD = Object.assign(new MetadataValue(), { value: lastName + ', ' + firstName });

const mockItemMetadataRepresentation = Object.assign(new ItemMetadataRepresentation(virtMD), mockItem);

describe('PersonItemMetadataListElementComponent', () => {
  let comp: PersonItemMetadataListElementComponent;
  let fixture: ComponentFixture<PersonItemMetadataListElementComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports:[
        NgbModule
      ],
      declarations: [PersonItemMetadataListElementComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(PersonItemMetadataListElementComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PersonItemMetadataListElementComponent);
    comp = fixture.componentInstance;
    comp.metadataRepresentation = mockItemMetadataRepresentation;
    fixture.detectChanges();
  });

  it('should show the person\'s name as a link', () => {
    const linkText = fixture.debugElement.query(By.css('a')).nativeElement.textContent;
    expect(linkText).toBe(lastName + ', ' + firstName);
  });

  it('should show the description on hover over the link in a tooltip', () => {
    const link = fixture.debugElement.query(By.css('a'));
    link.triggerEventHandler('mouseenter', null);
    fixture.detectChanges();
    const tooltip = fixture.debugElement.query(By.css('.item-list-job-title')).nativeElement.textContent;
    expect(tooltip).toBe(jobTitle);
  });
});
