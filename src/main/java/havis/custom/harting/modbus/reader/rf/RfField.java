package havis.custom.harting.modbus.reader.rf;

import havis.custom.harting.modbus.reader.api.Field;
import havis.custom.harting.modbus.reader.rf.RfConstants.RfFieldType;

public class RfField extends Field {

	private int offset = 0;
	private int length = 0;
	private RfFieldType type;

	public RfField(Field field) {
		super(field.getId());
		this.type = RfFieldType.get(field);
	}

	public RfField(Field field, int offset, int length) {
		this(field);
		this.offset = offset;
		this.length = length;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public RfFieldType getType() {
		return type;
	}
}
