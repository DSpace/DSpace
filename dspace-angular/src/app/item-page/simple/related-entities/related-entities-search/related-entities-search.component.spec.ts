import { RelatedEntitiesSearchComponent } from './related-entities-search.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Item } from '../../../../core/shared/item.model';

describe('RelatedEntitiesSearchComponent', () => {
  let comp: RelatedEntitiesSearchComponent;
  let fixture: ComponentFixture<RelatedEntitiesSearchComponent>;

  const mockItem = Object.assign(new Item(), {
    id: 'id1'
  });
  const mockRelationType = 'publicationsOfAuthor';
  const mockConfiguration = 'publication';
  const mockFilter = `f.${mockRelationType}=${mockItem.id},equals`;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), NoopAnimationsModule, FormsModule],
      declarations: [RelatedEntitiesSearchComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RelatedEntitiesSearchComponent);
    comp = fixture.componentInstance;
    comp.relationType = mockRelationType;
    comp.item = mockItem;
    comp.configuration = mockConfiguration;
    fixture.detectChanges();
  });

  it('should create a fixedFilter', () => {
    expect(comp.fixedFilter).toEqual(mockFilter);
  });

});
