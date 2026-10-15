import {TestBed} from '@angular/core/testing';
import {DIALOG_DATA, DialogRef} from '@angular/cdk/dialog';
import {SecretEditor, SecretEditorData} from './secret-editor';

describe('SecretEditor', () => {
  const close = vi.fn();

  function render(data: SecretEditorData) {
    close.mockReset();
    TestBed.configureTestingModule({providers: [{provide: DIALOG_DATA, useValue: data}, {provide: DialogRef, useValue: {close}}]});
    const fixture = TestBed.createComponent(SecretEditor);
    fixture.detectChanges();
    return {fixture, el: fixture.nativeElement as HTMLElement};
  }

  const existing = {key: 'db', value: null, environmentVariable: 'DB_PASSWORD', deployments: ['a']};

  function type(el: HTMLElement, name: string, value: string) {
    const input = el.querySelector(`[name=${name}]`) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('uses a password field and explains that blank keeps the stored value', async () => {
    const {fixture, el} = render({secret: existing, deployments: ['a', 'b']});
    await fixture.whenStable();
    expect((el.querySelector('[name=value]') as HTMLInputElement).type).toBe('password');
    expect((el.querySelector('[name=value]') as HTMLInputElement).value).toBe('');
    expect(el.querySelector('[data-testid=value-hint]')?.textContent).toContain('keep the stored value');
  });

  it('saves a blank value as null so the stored value is retained', async () => {
    const {fixture, el} = render({secret: existing, deployments: ['a', 'b']});
    await fixture.whenStable();
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    expect(close).toHaveBeenCalledWith({
      action: 'save', restart: false,
      secret: {key: 'db', environmentVariable: 'DB_PASSWORD', deployments: ['a'], value: null}
    });
  });

  it('requires a value for a new secret', async () => {
    const {fixture, el} = render({secret: null, deployments: []});
    await fixture.whenStable();
    type(el, 'key', 'k');
    type(el, 'env', 'K');
    fixture.detectChanges();
    expect((el.querySelector('button[type=submit]') as HTMLButtonElement).disabled).toBe(true);
    type(el, 'value', 'secret');
    fixture.detectChanges();
    expect((el.querySelector('button[type=submit]') as HTMLButtonElement).disabled).toBe(false);
  });

  it('adds deployments via filtered autocomplete and removes them via chips', async () => {
    const {fixture, el} = render({secret: existing, deployments: ['a', 'beta', 'bravo']});
    await fixture.whenStable();
    type(el, 'dep-search', 'br');
    fixture.detectChanges();
    const items = [...el.querySelectorAll('[data-testid=suggestions] button')] as HTMLButtonElement[];
    expect(items.map(i => i.textContent?.trim())).toEqual(['bravo']);
    items[0].click();
    fixture.detectChanges();
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    expect(close.mock.calls[0][0].secret.deployments.sort()).toEqual(['a', 'bravo']);
  });

  it('removes a deployment with its chip button', async () => {
    const {fixture, el} = render({secret: existing, deployments: ['a']});
    await fixture.whenStable();
    (el.querySelector('[data-testid=chips] button') as HTMLButtonElement).click();
    fixture.detectChanges();
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    expect(close.mock.calls[0][0].secret.deployments).toEqual([]);
  });

  it('requires a second click to delete', async () => {
    const {fixture, el} = render({secret: existing, deployments: ['a']});
    await fixture.whenStable();
    const del = el.querySelector('[data-testid=delete]') as HTMLButtonElement;
    del.click();
    fixture.detectChanges();
    expect(close).not.toHaveBeenCalled();
    expect(del.textContent).toContain('again');
    del.click();
    expect(close).toHaveBeenCalledWith({action: 'delete', restart: false, secret: existing});
  });
});
