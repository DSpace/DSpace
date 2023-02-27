import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { NavbarSectionComponent } from './navbar-section.component';
import { HostWindowService } from '../../shared/host-window.service';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MenuService } from '../../shared/menu/menu.service';
import { HostWindowServiceStub } from '../../shared/testing/host-window-service.stub';
import { Component } from '@angular/core';
import { MenuServiceStub } from '../../shared/testing/menu-service.stub';
import { of as observableOf } from 'rxjs';

describe('NavbarSectionComponent', () => {
  let component: NavbarSectionComponent;
  let fixture: ComponentFixture<NavbarSectionComponent>;
  const menuService = new MenuServiceStub();

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule],
      declarations: [NavbarSectionComponent, TestComponent],
      providers: [
        { provide: 'sectionDataProvider', useValue: {} },
        { provide: MenuService, useValue: menuService },
        { provide: HostWindowService, useValue: new HostWindowServiceStub(800) }
      ]
    }).overrideComponent(NavbarSectionComponent, {
      set: {
        entryComponents: [TestComponent]
      }
    })
      .compileComponents();
  }));

  beforeEach(() => {
    spyOn(menuService, 'getSubSectionsByParentID').and.returnValue(observableOf([]));

    fixture = TestBed.createComponent(NavbarSectionComponent);
    component = fixture.componentInstance;
    spyOn(component as any, 'getMenuItemComponent').and.returnValue(TestComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {
}
