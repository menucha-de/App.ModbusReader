package havis.app.modbus.reader.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.Widget;

public class RuntimeConfigRow extends Composite implements LeafValueEditor<Integer>, HasValueChangeHandlers<Boolean> {

	private static RuntimeConfigRowUiBinder uiBinder = GWT
			.create(RuntimeConfigRowUiBinder.class);

	interface RuntimeConfigRowUiBinder extends
			UiBinder<Widget, RuntimeConfigRow> {
	}
	
	@UiField
	@Ignore
	Label name;
	
	@UiField
	@Ignore
	ToggleButton switcher;
	
	@UiField(provided = true)
	@Ignore
	ValueListBox<Integer> length;
	
	public RuntimeConfigRow() {
	}

	@UiConstructor
	public RuntimeConfigRow(String name) {
		length = new ValueListBox<Integer>();
		initWidget(uiBinder.createAndBindUi(this));
		this.name.setText(name);
		fillListBox();
	}
	
	protected void fillListBox() {
		for (int i = 1; i < 256; i++) {
			length.setValue(i);
		}
	}

	@UiHandler("switcher")
	void onToggleSwitcher(ValueChangeEvent<Boolean> event) {
		length.setEnabled(event.getValue());
		ValueChangeEvent.<Boolean>fire(this, event.getValue());
	}
	
	@UiHandler("length")
	void onChangeValue(ValueChangeEvent<Integer> event) {
		ValueChangeEvent.<Boolean>fire(this, switcher.getValue());
	}
	
	@Override
	public void setValue(Integer value) {
		switcher.setValue(value > 0);
		length.setEnabled(value > 0);
		length.setValue(value > 0 ? value : 1);
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	@Override
	public Integer getValue() {
		return switcher.getValue() ? length.getValue() : 0;
	}
}
