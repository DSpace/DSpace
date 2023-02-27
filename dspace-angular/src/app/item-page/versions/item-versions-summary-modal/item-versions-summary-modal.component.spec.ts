import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ItemVersionsSummaryModalComponent } from './item-versions-summary-modal.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

describe('ItemVersionsSummaryModalComponent', () => {
  let component: ItemVersionsSummaryModalComponent;
  let fixture: ComponentFixture<ItemVersionsSummaryModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ItemVersionsSummaryModalComponent ],
      imports: [ TranslateModule.forRoot(), RouterTestingModule.withRoutes([]) ],
      providers: [
        { provide: NgbActiveModal },
      ]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemVersionsSummaryModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
