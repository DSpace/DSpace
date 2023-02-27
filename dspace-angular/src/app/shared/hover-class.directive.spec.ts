import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HoverClassDirective } from './hover-class.directive';
import { By } from '@angular/platform-browser';

@Component({
  template: `<div dsHoverClass="ds-hover"></div>`
})
class TestComponent {
}

describe('HoverClassDirective', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let el: DebugElement;

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      declarations: [TestComponent, HoverClassDirective]
    }).createComponent(TestComponent);

    fixture.detectChanges();
    component = fixture.componentInstance;
    el = fixture.debugElement.query(By.css('div'));
  });

  it('should add the class on mouseenter and remove on mouseleave', () => {
    el.triggerEventHandler('mouseenter', null);
    fixture.detectChanges();
    expect(el.nativeElement.classList).toContain('ds-hover');

    el.triggerEventHandler('mouseleave', null);
    fixture.detectChanges();
    expect(el.nativeElement.classList).not.toContain('ds-hover');
  });
});
