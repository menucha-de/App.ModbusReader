package havis.custom.harting.modbus.reader.rest.data;

public class RuntimeRegisterItem {
	private String addressHex;
	private String addressDez;
	private int length;
	private Type type;
	private String description;

	public RuntimeRegisterItem(String addressHex, String addressDez, int length, Type type, String description) {
		super();
		this.addressHex = addressHex;
		this.addressDez = addressDez;
		this.length = length;
		this.type = type;
		this.description = description;
	}

	public RuntimeRegisterItem() {
	}

	public String getAddressHex() {
		return addressHex;
	}

	public void setAddressHex(String address) {
		this.addressHex = address;
	}

	public String getAddressDez() {
		return addressDez;
	}

	public void setAddressDez(String addressDez) {
		this.addressDez = addressDez;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
