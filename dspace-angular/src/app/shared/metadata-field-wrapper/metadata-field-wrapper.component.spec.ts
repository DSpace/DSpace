/* eslint-disable max-classes-per-file */
import { Component, Input } from '@angular/core';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { MetadataFieldWrapperComponent } from './metadata-field-wrapper.component';

@Component({
    selector: 'ds-component-without-content',
    template: '<ds-metadata-field-wrapper [hideIfNoTextContent]="hideIfNoTextContent" [label]="\'test label\'">\n' +
      '</ds-metadata-field-wrapper>'
})
class NoContentComponent {
  public hideIfNoTextContent = true;
}

@Component({
    selector: 'ds-component-with-empty-spans',
    template: '<ds-metadata-field-wrapper [hideIfNoTextContent]="hideIfNoTextContent" [label]="\'test label\'">\n' +
      '    <span></span>\n' +
      '    <span></span>\n' +
      '</ds-metadata-field-wrapper>'
})
class SpanContentComponent {
  @Input() hideIfNoTextContent = true;
}

@Component({
    selector: 'ds-component-with-text',
    template: '<ds-metadata-field-wrapper [hideIfNoTextContent]="hideIfNoTextContent" [label]="\'test label\'">\n' +
      '    <span>The quick brown fox jumps over the lazy dog</span>\n' +
      '</ds-metadata-field-wrapper>'
})
class TextContentComponent {
  @Input() hideIfNoTextContent = true;
}


describe('MetadataFieldWrapperComponent', () => {
  let component: MetadataFieldWrapperComponent;
  let fixture: ComponentFixture<MetadataFieldWrapperComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [MetadataFieldWrapperComponent, NoContentComponent, SpanContentComponent, TextContentComponent]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MetadataFieldWrapperComponent);
    component = fixture.componentInstance;
  });

  const wrapperSelector = '.simple-view-element';

  it('should create', () => {
    expect(component).toBeDefined();
  });

  describe('with hideIfNoTextContent=true', () => {
    it('should not show the component when there is no content', () => {
      const parentFixture = TestBed.createComponent(NoContentComponent);
      parentFixture.detectChanges();
      const parentNative = parentFixture.nativeElement;
      const nativeWrapper = parentNative.querySelector(wrapperSelector);
      expect(nativeWrapper.classList.contains('d-none')).toBe(true);
    });

    it('should not show the component when there is no text content', () => {
      const parentFixture = TestBed.createComponent(SpanContentComponent);
      parentFixture.detectChanges();
      const parentNative = parentFixture.nativeElement;
      const nativeWrapper = parentNative.querySelector(wrapperSelector);
      expect(nativeWrapper.classList.contains('d-none')).toBe(true);
    });

    it('should show the component when there is text content', () => {
      const parentFixture = TestBed.createComponent(TextContentComponent);
      parentFixture.detectChanges();
      const parentNative = parentFixture.nativeElement;
      const nativeWrapper = parentNative.querySelector(wrapperSelector);
      parentFixture.detectChanges();
      expect(nativeWrapper.classList.contains('d-none')).toBe(false);
    });
  });

  describe('with hideIfNoTextContent=false', () => {
    it('should show the component when there is no content', () => {
      const parentFixture = TestBed.createComponent(NoContentComponent);
      parentFixture.componentInstance.hideIfNoTextContent = false;
      parentFixture.detectChanges();
      const parentNative = parentFixture.nativeElement;
      const nativeWrapper = parentNative.querySelector(wrapperSelector);
      expect(nativeWrapper.classList.contains('d-none')).toBe(false);
    });

    it('should show the component when there is no text content', () => {
      const parentFixture = TestBed.createComponent(SpanContentComponent);
      parentFixture.componentInstance.hideIfNoTextContent = false;
      parentFixture.detectChanges();
      const parentNative = parentFixture.nativeElement;
      const nativeWrapper = parentNative.querySelector(wrapperSelector);
      expect(nativeWrapper.classList.contains('d-none')).toBe(false);
    });

    it('should show the component when there is text content', () => {
      const parentFixture = TestBed.createComponent(TextContentComponent);
      parentFixture.componentInstance.hideIfNoTextContent = false;
      parentFixture.detectChanges();
      const parentNative = parentFixture.nativeElement;
      const nativeWrapper = parentNative.querySelector(wrapperSelector);
      parentFixture.detectChanges();
      expect(nativeWrapper.classList.contains('d-none')).toBe(false);
    });
  });
});
