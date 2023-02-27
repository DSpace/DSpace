import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { AdminCurationTasksComponent } from './admin-curation-tasks.component';
import { TranslateModule } from '@ngx-translate/core';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

describe('AdminCurationTasksComponent', () => {
  let comp: AdminCurationTasksComponent;
  let fixture: ComponentFixture<AdminCurationTasksComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [AdminCurationTasksComponent],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdminCurationTasksComponent);
    comp = fixture.componentInstance;
  });
  describe('init', () => {
    it('should initialise the comp', () => {
      expect(comp).toBeDefined();
      expect(fixture.debugElement.nativeElement.innerHTML).toContain('ds-curation-form');
    });
  });
});
