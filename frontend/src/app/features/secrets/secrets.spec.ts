import {TestBed} from '@angular/core/testing';
import {Dialog} from '@angular/cdk/dialog';
import {Observable, of, throwError} from 'rxjs';
import {DeploymentsApi} from '../../core/api/deployments-api';
import {Secret} from '../../core/api/models';
import {SecretsApi} from '../../core/api/secrets-api';
import {SecretEditorResult} from './secret-editor';
import {Secrets} from './secrets';

describe('Secrets', () => {
  const list = vi.fn<() => Observable<Secret[]>>();
  const put = vi.fn<(s: Secret, r: boolean) => Observable<void>>();
  const remove = vi.fn<(s: Secret, r: boolean) => Observable<void>>();
  const open = vi.fn();

  const secret: Secret = {key: 'db', value: null, environmentVariable: 'DB_PASSWORD', deployments: ['a', 'b']};

  function render(items: Secret[], dialogResult?: SecretEditorResult) {
    list.mockReturnValue(of(items));
    open.mockReturnValue({closed: of(dialogResult)});
    TestBed.configureTestingModule({
      providers: [
        {provide: SecretsApi, useValue: {list, put, remove}},
        {provide: DeploymentsApi, useValue: {list: () => of([{name: 'a'}, {name: 'b'}])}},
        {provide: Dialog, useValue: {open}}
      ]
    });
    const fixture = TestBed.createComponent(Secrets);
    fixture.detectChanges();
    return {fixture, el: fixture.nativeElement as HTMLElement};
  }

  beforeEach(() => {
    put.mockReset().mockReturnValue(of(undefined));
    remove.mockReset().mockReturnValue(of(undefined));
    open.mockReset();
  });

  it('lists secrets by key, variable and deployments without values', () => {
    const {el} = render([secret]);
    const text = el.querySelector('[data-testid=secret]')?.textContent ?? '';
    expect(text).toContain('db');
    expect(text).toContain('DB_PASSWORD');
    expect(text).toContain('a, b');
  });

  it('shows an empty state', () => {
    expect(render([]).el.querySelector('[data-testid=empty]')).not.toBeNull();
  });

  it('opens the editor with the deployment names', () => {
    const {el} = render([secret]);
    (el.querySelector('[data-testid=secret] button') as HTMLButtonElement).click();
    expect(open.mock.calls[0][1].data).toEqual({secret, deployments: ['a', 'b']});
  });

  it('saves with the restart choice and reloads the list', () => {
    const edited = {...secret, value: 'new'};
    const {el} = render([secret], {action: 'save', secret: edited, restart: true});
    (el.querySelector('[data-testid=new]') as HTMLButtonElement).click();
    expect(put).toHaveBeenCalledWith(edited, true);
    expect(list).toHaveBeenCalledTimes(2);
  });

  it('deletes a secret', () => {
    const {el} = render([secret], {action: 'delete', secret, restart: false});
    (el.querySelector('[data-testid=new]') as HTMLButtonElement).click();
    expect(remove).toHaveBeenCalledWith(secret, false);
  });

  it('does nothing when the editor is dismissed', () => {
    const {el} = render([secret]);
    (el.querySelector('[data-testid=new]') as HTMLButtonElement).click();
    expect(put).not.toHaveBeenCalled();
    expect(remove).not.toHaveBeenCalled();
  });

  it('reports a failed save', () => {
    put.mockReturnValue(throwError(() => new Error('x')));
    const {fixture, el} = render([secret], {action: 'save', secret, restart: false});
    (el.querySelector('[data-testid=new]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.querySelector('[role=alert]')?.textContent).toContain('could not be saved');
  });
});
