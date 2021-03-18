package havis.app.modbus.reader.core;

import havis.app.modbus.reader.api.Field;

public class FieldProperties {
    public enum FieldType {
        COILS, //
        DISCRETE_INPUTS, //
        HOLDING_REGISTERS, //
        INPUT_REGISTERS
    }

    public enum FieldDataType {
        BOOLEAN, BYTE, SHORT, USHORT, FLOAT, STRING
    }

	private Field field;
    private int address;
    private Integer addressQuantity;
    private FieldType type;
    private FieldDataType dataType;
    private Integer fieldGroupIndex;
    private Boolean isConfigField;
	private Field[] scannedFields;

	public FieldProperties(int address, Field[] scannedFields) {
        this.address = address;
        this.scannedFields = scannedFields;
    }

	public Field getField() {
        return field;
    }

	public void setField(Field field) {
        this.field = field;
    }

    public int getAddress() {
        return address;
    }

    public Integer getAddressQuantity() {
        return addressQuantity;
    }

    public void setAddressQuantity(Integer addressQuantity) {
        this.addressQuantity = addressQuantity;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public FieldDataType getDataType() {
        return dataType;
    }

    public void setDataType(FieldDataType dataType) {
        this.dataType = dataType;
    }

    public Integer getFieldGroupIndex() {
        return fieldGroupIndex;
    }

    public void setFieldGroupIndex(Integer fieldGroupIndex) {
        this.fieldGroupIndex = fieldGroupIndex;
    }

    public Boolean isConfigField() {
        return isConfigField;
    }

    public void setConfigField(Boolean isConfigField) {
        this.isConfigField = isConfigField;
    }

	public Field[] getScannedFields() {
        return scannedFields;
    }
}
