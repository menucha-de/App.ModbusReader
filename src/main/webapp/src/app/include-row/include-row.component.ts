import { Component, OnInit, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-include-row',
  templateUrl: './include-row.component.html',
  styleUrls: ['./include-row.component.scss']
})
export class IncludeRowComponent implements OnInit {

  @Input() label: string;
  @Input() value: boolean;
  @Output() valueChange = new EventEmitter<boolean>();
  constructor() { }

  ngOnInit() {
  }

  changeValue() {
    this.valueChange.emit(this.value);
  }

}
