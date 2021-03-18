package havis.app.modbus.reader.rest.data;

public class DeviceInfo {
	private String vendorName;
	private String productCode;
	private String majorMinorRevision;
	private String serialNumber;
	private String hardwareRevision;
	private String baseFirmware;
	
	public String getVendorName() {
		return vendorName;
	}
	public void setVendorName(String vendorName) {
		this.vendorName = vendorName;
	}
	public String getProductCode() {
		return productCode;
	}
	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}
	public String getMajorMinorRevision() {
		return majorMinorRevision;
	}
	public void setMajorMinorRevision(String majorMinorRevision) {
		this.majorMinorRevision = majorMinorRevision;
	}
	public String getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	public String getHardwareRevision() {
		return hardwareRevision;
	}
	public void setHardwareRevision(String hardwareRevision) {
		this.hardwareRevision = hardwareRevision;
	}
	public String getBaseFirmware() {
		return baseFirmware;
	}
	public void setBaseFirmware(String baseFirmware) {
		this.baseFirmware = baseFirmware;
	}
}
