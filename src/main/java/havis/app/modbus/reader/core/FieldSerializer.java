package havis.app.modbus.reader.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import havis.app.modbus.reader.api.Field;
import havis.app.modbus.reader.api.FieldValue;
import havis.app.modbus.reader.common.FileHandler;
import havis.app.modbus.reader.core.FieldProperties.FieldDataType;

class FieldSerializer {

	Map<Field, List<FieldValue>> read(Path file) throws IOException {
		Properties props = new Properties();
		InputStream in = new FileHandler().newInputStream(file);
		try {
			props.load(in);
		} finally {
			in.close();
		}
		Map<Field, List<FieldValue>> ret = new HashMap<>();
		for (Entry<Object, Object> prop : props.entrySet()) {
			int id = Integer.parseInt((String) prop.getKey());
			List<FieldValue> fieldValues = deserialize((String) prop.getValue());
			ret.put(new Field(id), fieldValues);
		}
		return ret;
	}

	void write(Map<Field, List<FieldValue>> fieldValues, Path file) throws IOException {
		Properties props = new Properties();
		for (Entry<Field, List<FieldValue>> fieldValueEntry : fieldValues.entrySet()) {
			props.put(fieldValueEntry.getKey().toString(), serialize(fieldValueEntry.getValue()));
		}
		OutputStream out = Files.newOutputStream(file);
		try {
			props.store(out, null /* comments */);
		} finally {
			out.close();
		}
	}

	/**
	 * Serializes a list of field values to eg. <code>USHORT 7;USHORT 8,9</code>
	 * .
	 * 
	 * @param values
	 * @return
	 */
	String serialize(List<FieldValue> values) {
		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < values.size(); i++) {
			FieldValue value = values.get(i);
			if (i > 0) {
				ret.append(";");
			}
			if (value == null || value.getDataType() == null) {
				continue;
			}
			ret.append(value.getDataType());
			ret.append(" ");
			switch (value.getDataType()) {
			case BOOLEAN:
				boolean[] booleanValue = value.getBooleanValue();
				for (int j = 0; j < booleanValue.length; j++) {
					if (j > 0) {
						ret.append(",");
					}
					ret.append(booleanValue[j]);
				}
				break;
			case BYTE:
				byte[] byteValue = value.getByteValue();
				ret.append(bytes2hex(byteValue));
				break;
			case FLOAT:
				float[] floatValue = value.getFloatValue();
				for (int j = 0; j < floatValue.length; j++) {
					if (j > 0) {
						ret.append(",");
					}
					ret.append(floatValue[j]);
				}
				break;
			case SHORT:
				short[] shortValue = value.getShortValue();
				for (int j = 0; j < shortValue.length; j++) {
					if (j > 0) {
						ret.append(",");
					}
					ret.append(shortValue[j]);
				}
				break;
			case STRING:
				// characters "," + ";" are not allowed in strings
				String[] stringUtf8Value = value.getStringValue();
				for (int j = 0; j < stringUtf8Value.length; j++) {
					if (j > 0) {
						ret.append(",");
					}
					if (stringUtf8Value[j] == null) {
						ret.append("null");
					} else {
						ret.append('"');
						ret.append(stringUtf8Value[j].replace("\"", "\\\""));
						ret.append('"');
					}
				}
				break;
			case USHORT:
				int[] ushortValue = value.getUShortValue();
				for (int j = 0; j < ushortValue.length; j++) {
					if (j > 0) {
						ret.append(",");
					}
					ret.append(ushortValue[j]);
				}
				break;
			default:
				break;
			}
		}
		return ret.toString();
	}

	List<FieldValue> deserialize(String values) {
		List<FieldValue> ret = new ArrayList<>();
		values = values.trim();
		// -1: keeps empty strings
		for (String value : values.split(";", -1)) {
			if (value.isEmpty()) {
				ret.add(null);
				continue;
			}
			// value: "STRING any value" -> dataType: "STRING", value: "any
			// value"
			int index = value.indexOf(' ');
			FieldDataType dataType = FieldDataType.valueOf(index < 0 ? value : value.substring(0, index));
			value = index < 0 || value.length() == index + 1 ? "" : value.substring(index + 1);
			FieldValue fieldValue = new FieldValue();
			switch (dataType) {
			case BOOLEAN:
				if (value.isEmpty()) {
					fieldValue.setBooleanValue(new boolean[0]);
				} else {
					String[] v = value.split(",");
					boolean[] booleanValue = new boolean[v.length];
					for (int i = 0; i < v.length; i++) {
						booleanValue[i] = Boolean.parseBoolean(v[i]);
					}
					fieldValue.setBooleanValue(booleanValue);
				}
				break;
			case BYTE:
				if (value.isEmpty()) {
					fieldValue.setByteValue(new byte[0]);
				} else {
					fieldValue.setByteValue(hex2bytes(value));
				}
				break;
			case FLOAT:
				if (value.isEmpty()) {
					fieldValue.setFloatValue(new float[0]);
				} else {
					String[] v = value.split(",");
					float[] floatValue = new float[v.length];
					for (int i = 0; i < v.length; i++) {
						floatValue[i] = Float.parseFloat(v[i]);
					}
					fieldValue.setFloatValue(floatValue);
				}
				break;
			case SHORT:
				if (value.isEmpty()) {
					fieldValue.setShortValue(new short[0]);
				} else {
					String[] v = value.split(",");
					short[] shortValue = new short[v.length];
					for (int i = 0; i < v.length; i++) {
						shortValue[i] = Short.parseShort(v[i]);
					}
					fieldValue.setShortValue(shortValue);
				}
				break;
			case STRING:
				// characters "," + ";" are not allowed in strings
				if (value.isEmpty()) {
					fieldValue.setStringValue(new String[0]);
				} else {
					String[] v = value.split(",");
					String[] stringUtf8Value = new String[v.length];
					for (int i = 0; i < v.length; i++) {
						if ("null".equals(v[i])) {
							stringUtf8Value[i] = null;
						} else {
							String str = v[i].substring(1);
							str = str.substring(0, str.length() - 1);
							stringUtf8Value[i] = str.replace("\\\"", "\"");
						}
					}
					fieldValue.setStringValue(stringUtf8Value);
				}
				break;
			case USHORT:
				if (value.isEmpty()) {
					fieldValue.setUShortValue(new int[0]);
				} else {
					String[] v = value.split(",");
					int[] ushortValue = new int[v.length];
					for (int i = 0; i < v.length; i++) {
						ushortValue[i] = Integer.parseInt(v[i]);
					}
					fieldValue.setUShortValue(ushortValue);
				}
				break;
			}
			ret.add(fieldValue);
		}
		return ret;
	}

	byte[] hex2bytes(String s) {
		if (s.length() % 2 == 1) {
			s += "0";
		}
		byte data[] = new byte[s.length() / 2];
		for (int i = 0; i < s.length(); i += 2) {
			data[i / 2] = (Integer.decode("0x" + s.charAt(i) + s.charAt(i + 1))).byteValue();
		}
		return data;
	}

	String bytes2hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}
}
