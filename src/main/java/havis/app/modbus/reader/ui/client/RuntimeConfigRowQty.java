package havis.app.modbus.reader.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.ValueAwareEditor;
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

import havis.app.modbus.reader.rest.data.RuntimeConfiguration;

public class RuntimeConfigRowQty extends Composite implements ValueAwareEditor<RuntimeConfiguration>, HasValueChangeHandlers<Boolean> {

	private static RuntimeConfigRowQtyUiBinder uiBinder = GWT
			.create(RuntimeConfigRowQtyUiBinder.class);

	interface RuntimeConfigRowQtyUiBinder extends
			UiBinder<Widget, RuntimeConfigRowQty> {
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
	
	@UiField(provided = true)
	@Ignore
	ValueListBox<Integer> qty;
	
	private RuntimeConfiguration config;
	
	@UiConstructor
	public RuntimeConfigRowQty(String name) {
		length = new ValueListBox<Integer>();
		qty = new ValueListBox<Integer>();
		initWidget(uiBinder.createAndBindUi(this));
		this.name.setText(name);
		fillListBox();
	}
	
	private void fillListBox() {
		for(int i = 1; i < 256; i++)  {
			length.setValue(i);
		}
		
		for(int i = 1; i < 5; i++) {
			qty.setValue(i);
		}
	}

	@UiHandler("switcher")
	void onToggleSwitcher(ValueChangeEvent<Boolean> event) {
		length.setEnabled(event.getValue());
		qty.setEnabled(event.getValue());
		ValueChangeEvent.<Boolean>fire(this, event.getValue());
	}
	
	@UiHandler("length")
	void onChangeLength(ValueChangeEvent<Integer> event) {
		ValueChangeEvent.<Boolean>fire(this, switcher.getValue());
	}
	
	@UiHandler("qty")
	void onChangeQty(ValueChangeEvent<Integer> event) {
		ValueChangeEvent.<Boolean>fire(this, switcher.getValue());
	}
	
	@Override
	public void setValue(RuntimeConfiguration value) {
		config = value;
		switcher.setValue(value.getSelectionMaskCount() > 0);
		length.setEnabled(value.getSelectionMaskCount() > 0);
		length.setValue(value.getSelectionMaskMaxLength() > 0 ? value.getSelectionMaskMaxLength() : 1);
		qty.setEnabled(value.getSelectionMaskCount() > 0);
		qty.setValue(value.getSelectionMaskCount() > 0 ? value.getSelectionMaskCount() : 1);
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

//	@Override
//	public RuntimeConfiguration getValue() {
//		config.setSelectionMaskCount(switcher.getValue() ? qty.getValue() : 0);
//		config.setSelectionMaskMaxLength(switcher.getValue() ? length.getValue() : 0);
//		return config;
//	}
//
	@Override
	public void setDelegate(EditorDelegate<RuntimeConfiguration> delegate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush() {
		config.setSelectionMaskCount(switcher.getValue() ? qty.getValue() : 0);
		config.setSelectionMaskMaxLength(switcher.getValue() ? length.getValue() : 0);
	}

	@Override
	public void onPropertyChange(String... paths) {
		// TODO Auto-generated method stub
		
	}

}
