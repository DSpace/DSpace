import { animate, state, style, transition, trigger } from '@angular/animations';

export const fromRightInState = state('fromRightIn', style({opacity: 1, transform: 'translateX(0)'}));
export const fromRightEnter = transition('* => fromRightIn', [
  style({opacity: 0, transform: 'translateX(5%)'}),
  animate('400ms ease-in-out')
]);

export const fromRightOutState = state('fromRightOut', style({opacity: 0, transform: 'translateX(-5%)'}));
export const fromRightLeave = transition('fromRightIn => fromRightOut', [
  style({opacity: 1, transform: 'translateX(0)'}),
  animate('300ms ease-in-out')
]);

export const fromRightIn = trigger('fromRightIn', [
  fromRightEnter
]);

export const fromRightOut = trigger('fromRightOut', [
  fromRightLeave
]);

export const fromRightInOut = trigger('fromRightInOut', [
  fromRightEnter,
  fromRightLeave
]);
