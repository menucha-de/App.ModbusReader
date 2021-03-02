package havis.custom.harting.modbus.reader.api;

import havis.custom.harting.modbus.reader.core.FieldProperties;

public interface Module {
	void open(int timeout) throws ModuleException;;

	void close(int timeout) throws ModuleException;

	void expandFields();

	FieldProperties getFieldProperties(FieldProperties startFieldProps, Integer selectAddress, Field selectField);

	FieldValue getFieldValue(Field field, int fieldGroupIndex) throws ModuleException;

	void setFieldValue(Field field, int fieldGroupIndex, FieldValue value) throws ModuleException;
}
