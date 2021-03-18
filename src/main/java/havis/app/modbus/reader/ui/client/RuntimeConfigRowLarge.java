package havis.app.modbus.reader.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.Widget;

public class RuntimeConfigRowLarge extends RuntimeConfigRow {

	private static RuntimeConfigRowLargeUiBinder uiBinder = GWT.create(RuntimeConfigRowLargeUiBinder.class);

	interface RuntimeConfigRowLargeUiBinder extends UiBinder<Widget, RuntimeConfigRowLarge> {
	}

	@UiConstructor
	public RuntimeConfigRowLarge(String name) {
		length = new ValueListBox<Integer>();
		initWidget(uiBinder.createAndBindUi(this));
		this.name.setText(name);
		fillListBox();
	}

	@Override
	protected void fillListBox() {
		for (int i = 1; i < 256 + 5; i++) {
			length.setValue(i < 256 ? i : (int) Math.pow(2, i - 256 + 8));
		}
	}
}
