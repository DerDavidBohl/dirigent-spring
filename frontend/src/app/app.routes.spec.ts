import {routes} from './app.routes';
import {Route} from '@angular/router';

function flatten(list: Route[]): Route[] {
  return list.flatMap(route => [route, ...flatten(route.children ?? [])]);
}

describe('routes', () => {
  const all = flatten(routes);

  it('defines the documented route structure', () => {
    expect(all.map(r => r.path)).toEqual(['login', '', '', 'deployments', 'updates', 'secrets', 'settings/tokens', '**']);
  });

  it('loads every feature lazily', () => {
    const features = all.filter(r => r.path !== '**' && !(r.path === '' && r.children));
    expect(features.length).toBe(6);
    for (const route of features) {
      expect(route.loadComponent, `route "${route.path}"`).toBeTypeOf('function');
      expect(route.component).toBeUndefined();
    }
  });

  it('protects feature routes and redirects unknown paths to the dashboard', () => {
    expect(routes.find(r => r.path === '')?.canActivate?.length).toBe(1);
    expect(routes.find(r => r.path === 'login')?.canActivate?.length).toBe(1);
    expect(routes.find(r => r.path === '**')?.redirectTo).toBe('');
  });
});
