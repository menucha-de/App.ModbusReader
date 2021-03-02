import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { IncludeRowComponent } from './include-row.component';

describe('IncludeRowComponent', () => {
  let component: IncludeRowComponent;
  let fixture: ComponentFixture<IncludeRowComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ IncludeRowComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IncludeRowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
