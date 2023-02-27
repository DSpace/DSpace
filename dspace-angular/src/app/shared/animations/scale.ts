import { animate, state, style, transition, trigger } from '@angular/animations';

export const scaleInState = state('scaleIn', style({opacity: 1, transform: 'scale(1)'}));
export const scaleEnter =  transition('* => scaleIn', [
  style({opacity: 0, transform: 'scale(0)'}),
  animate('400ms ease-in-out')
]);

export const scaleOutState = state('scaleOut', style({opacity: 0, transform: 'scale(0)'}));
export const scaleLeave = transition('scaleIn => scaleOut', [
  style({opacity: 1, transform: 'scale(1)'}),
  animate('400ms ease-in-out')
]);

export const scaleIn = trigger('scaleIn', [
  scaleEnter
]);

export const scaleOut = trigger('scaleOut', [
  scaleLeave
]);

export const scaleInOut = trigger('scaleInOut', [
  scaleEnter,
  scaleLeave
]);
