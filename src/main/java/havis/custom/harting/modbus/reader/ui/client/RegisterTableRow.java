package havis.custom.harting.modbus.reader.ui.client;

import havis.custom.harting.modbus.reader.rest.data.RuntimeRegisterItem;
import havis.custom.harting.modbus.reader.rest.data.Type;
import havis.custom.harting.modbus.reader.ui.res.cons.AppConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class RegisterTableRow extends Composite {

	@UiField
	Label regAddress;

	@UiField
	Label length;

	@UiField
	Label regType;

	@UiField
	Label access;

	@UiField
	Label fieldName;

	@UiField
	Label description;

	@UiField
	FlowPanel descriptionContainer;

	AppConstants con = AppConstants.INSTANCE;

	private static RegisterTableRowUiBinder uiBinder = GWT.create(RegisterTableRowUiBinder.class);

	interface RegisterTableRowUiBinder extends UiBinder<Widget, RegisterTableRow> {
	}

	public RegisterTableRow(RuntimeRegisterItem item) {
		initWidget(uiBinder.createAndBindUi(this));
		regAddress.setText(item.getAddressHex());
		length.setText(item.getLength() + "");

		if (item.getType() == Type.HOLDING) {
			regType.setText(con.regTypeHolding());
			access.setText(con.accessReadWrite());
		}

		if (item.getType() == Type.INPUT) {
			regType.setText(con.regTypeInput());
			access.setText(con.accessRead());
		}

		int dotIndex = item.getDescription().indexOf(".");
		fieldName.setText(item.getDescription().substring(0, dotIndex));
		description.setText(item.getDescription().substring(dotIndex + 1));
	}

	// Used to create the table header
	public RegisterTableRow() {
		initWidget(uiBinder.createAndBindUi(this));
		this.regAddress.setText(con.regAddress());
		this.length.setText(con.lengthW());
		this.regType.setText(con.regType());
		this.access.setText(con.access());
		this.description.setText(con.description());
	}

	public Widget[] getWidgets() {
		Widget[] result = new Widget[5];
		result[0] = regAddress;
		result[1] = length;
		result[2] = regType;
		result[3] = access;
		result[4] = descriptionContainer;
		return result;
	}

}
