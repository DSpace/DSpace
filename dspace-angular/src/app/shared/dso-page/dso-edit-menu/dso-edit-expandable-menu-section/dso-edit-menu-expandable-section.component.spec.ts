import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { MenuServiceStub } from '../../../testing/menu-service.stub';
import { TranslateModule } from '@ngx-translate/core';
import { MenuService } from '../../../menu/menu.service';
import { CSSVariableService } from '../../../sass-helper/css-variable.service';
import { CSSVariableServiceStub } from '../../../testing/css-variable-service.stub';
import { Router } from '@angular/router';
import { RouterStub } from '../../../testing/router.stub';
import { of as observableOf } from 'rxjs';
import { Component } from '@angular/core';
import { DsoEditMenuExpandableSectionComponent } from './dso-edit-menu-expandable-section.component';
import { By } from '@angular/platform-browser';
import { MenuItemType } from 'src/app/shared/menu/menu-item-type.model';

describe('DsoEditMenuExpandableSectionComponent', () => {
  let component: DsoEditMenuExpandableSectionComponent;
  let fixture: ComponentFixture<DsoEditMenuExpandableSectionComponent>;
  const menuService = new MenuServiceStub();
  const iconString = 'test';

  const dummySection = {
    id: 'dummy',
    active: false,
    visible: true,
    model: {
      type: MenuItemType.TEXT,
      disabled: false,
      text: 'text'
    },
    icon: iconString
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [DsoEditMenuExpandableSectionComponent, TestComponent],
      providers: [
        {provide: 'sectionDataProvider', useValue: dummySection},
        {provide: MenuService, useValue: menuService},
        {provide: CSSVariableService, useClass: CSSVariableServiceStub},
        {provide: Router, useValue: new RouterStub()},
      ]
    }).overrideComponent(DsoEditMenuExpandableSectionComponent, {
      set: {
        entryComponents: [TestComponent]
      }
    })
      .compileComponents();
  }));

  beforeEach(() => {
    spyOn(menuService, 'getSubSectionsByParentID').and.returnValue(observableOf([]));
    fixture = TestBed.createComponent(DsoEditMenuExpandableSectionComponent);
    component = fixture.componentInstance;
    spyOn(component as any, 'getMenuItemComponent').and.returnValue(TestComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show a button with the icon', () => {
    const button = fixture.debugElement.query(By.css('.btn-dark'));
    expect(button.nativeElement.innerHTML).toContain('fa-' + iconString);
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {
}
