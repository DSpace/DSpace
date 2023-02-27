import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { TranslateModule } from '@ngx-translate/core';

import { createTestComponent } from '../../testing/utils.test';
import { EpersonSearchBoxComponent } from './eperson-search-box.component';
import { SearchEvent } from '../eperson-group-list.component';

describe('EpersonSearchBoxComponent test suite', () => {
  let comp: EpersonSearchBoxComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<EpersonSearchBoxComponent>;
  let de;
  let formBuilder: FormBuilder;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ReactiveFormsModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        EpersonSearchBoxComponent,
        TestComponent
      ],
      providers: [
        FormBuilder,
        EpersonSearchBoxComponent
      ],
      schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      const html = `
        <ds-group-search-box></ds-group-search-box>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create EpersonSearchBoxComponent', inject([EpersonSearchBoxComponent], (app: EpersonSearchBoxComponent) => {

      expect(app).toBeDefined();

    }));
  });

  describe('', () => {
    beforeEach(() => {
      // initTestScheduler();
      fixture = TestBed.createComponent(EpersonSearchBoxComponent);
      formBuilder = TestBed.inject(FormBuilder);
      comp = fixture.componentInstance;
      compAsAny = fixture.componentInstance;
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      de = null;
      fixture.destroy();
    });

    it('should reset the form', () => {
      comp.searchForm = formBuilder.group(({
        query: 'test',
      }));

      comp.reset();

      expect(comp.searchForm.controls.query.value).toBe('');
    });

    it('should emit new search event', () => {
      const data = {
        scope: 'metadata',
        query: 'test'
      };

      const event: SearchEvent = {
        scope: 'metadata',
        query: 'test'
      };
      spyOn(comp.search, 'emit');

      comp.submit(data);

      expect(comp.search.emit).toHaveBeenCalledWith(event);
    });
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

}
