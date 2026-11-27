import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'topics',
    loadComponent: () => import('./topic/topic.component').then((m) => m.TopicComponent),
  },
];
