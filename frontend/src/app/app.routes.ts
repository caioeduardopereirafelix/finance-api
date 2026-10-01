import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'entrar',
    canActivate: [guestGuard],
    title: 'Entrar · Finance',
    loadComponent: () => import('./features/login/login').then(m => m.LoginPage),
  },
  {
    path: 'criar-conta',
    canActivate: [guestGuard],
    title: 'Criar conta · Finance',
    loadComponent: () => import('./features/register/register').then(m => m.RegisterPage),
  },
  {
    path: 'esqueci-senha',
    canActivate: [guestGuard],
    title: 'Recuperar senha · Finance',
    loadComponent: () => import('./features/forgot-password/forgot-password').then(m => m.ForgotPasswordPage),
  },
  {
    path: 'redefinir-senha',
    title: 'Nova senha · Finance',
    loadComponent: () => import('./features/reset-password/reset-password').then(m => m.ResetPasswordPage),
  },
  {
    path: 'confirmar-email',
    title: 'Confirmar e-mail · Finance',
    loadComponent: () => import('./features/confirm-email/confirm-email').then(m => m.ConfirmEmailPage),
  },
  {
    path: 'painel',
    canActivate: [authGuard],
    title: 'Painel · Finance',
    loadComponent: () => import('./features/dashboard/dashboard').then(m => m.DashboardPage),
  },
  {
    path: 'transacoes',
    canActivate: [authGuard],
    title: 'Transações · Finance',
    loadComponent: () => import('./features/transactions/transactions').then(m => m.TransactionsPage),
  },
  {
    path: 'bancos',
    canActivate: [authGuard],
    title: 'Bancos · Finance',
    loadComponent: () => import('./features/banks/banks').then(m => m.BanksPage),
  },
  { path: '', pathMatch: 'full', redirectTo: 'painel' },
  {
    path: '**',
    title: 'Página não encontrada · Finance',
    loadComponent: () => import('./features/not-found').then(m => m.NotFoundPage),
  },
];
