import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RuntimeConfigRowComponent } from './runtime-config-row.component';

describe('RuntimeConfigRowComponent', () => {
  let component: RuntimeConfigRowComponent;
  let fixture: ComponentFixture<RuntimeConfigRowComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RuntimeConfigRowComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RuntimeConfigRowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
