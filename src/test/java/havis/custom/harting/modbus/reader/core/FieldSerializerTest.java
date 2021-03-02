package havis.custom.harting.modbus.reader.core;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import havis.custom.harting.modbus.reader.api.Field;
import havis.custom.harting.modbus.reader.api.FieldValue;
import havis.custom.harting.modbus.reader.common.PathHandler;

public class FieldSerializerTest {
	private static Path basePath;

	@BeforeClass
	public static void init() {
		String classPath = FieldSerializerTest.class.getName().replace(".", System.getProperty("file.separator"))
				+ ".class";
		basePath = new PathHandler().toAbsolutePath(classPath).getParent();
	}

	@Test
	public void readWrite() throws Exception {
		FieldSerializer fs = new FieldSerializer();
		Map<Field, List<FieldValue>> fieldValues = new HashMap<>();
		fieldValues.put(new Field(0), Arrays.asList(new FieldValue(new boolean[] { true })));
		fieldValues.put(new Field(1), Arrays.asList(new FieldValue(new byte[] { 3, 4 })));
		fieldValues.put(new Field(2), Arrays.asList(new FieldValue(new float[] { (float) 5.1 })));
		fieldValues.put(new Field(3), Arrays.asList(new FieldValue(new short[] { (short) 6 })));
		fieldValues.put(new Field(4), Arrays.asList(new FieldValue(new String[] { "aä" })));
		fieldValues.put(new Field(5), Arrays.asList(new FieldValue(new int[] { 7 }), new FieldValue(new int[] { 8, 9 })));
		Path file = basePath.resolve("fields.properties");
		fs.write(fieldValues, file);

		fieldValues = fs.read(file);
		Assert.assertTrue(fieldValues.get(new Field(0)).get(0).getBooleanValue()[0]);
		Assert.assertEquals(fieldValues.get(new Field(1)).get(0).getByteValue()[0], 3);
		Assert.assertEquals(fieldValues.get(new Field(1)).get(0).getByteValue()[1], 4);
		Assert.assertEquals(fieldValues.get(new Field(2)).get(0).getFloatValue()[0], (float) 5.1, 0.0);
		Assert.assertEquals(fieldValues.get(new Field(3)).get(0).getShortValue()[0], 6);
		Assert.assertEquals(fieldValues.get(new Field(4)).get(0).getStringValue()[0], "aä");
		Assert.assertEquals(fieldValues.get(new Field(5)).get(0).getUShortValue()[0], 7);
		Assert.assertEquals(fieldValues.get(new Field(5)).get(1).getUShortValue()[0], 8);
		Assert.assertEquals(fieldValues.get(new Field(5)).get(1).getUShortValue()[1], 9);
	}

	@Test
	public void serializeDeserialize() {
		FieldSerializer fs = new FieldSerializer();

		FieldValue fv1 = new FieldValue();
		String v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "");
		Assert.assertNull(fs.deserialize(v).get(0));

		fv1 = new FieldValue(new boolean[] {});
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "BOOLEAN ");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getBooleanValue(), fv1.getBooleanValue());

		fv1 = new FieldValue(new boolean[] { true });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "BOOLEAN true");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getBooleanValue(), fv1.getBooleanValue());

		fv1 = new FieldValue(new boolean[] { true, false });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "BOOLEAN true,false");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getBooleanValue(), fv1.getBooleanValue());

		fv1 = new FieldValue(new byte[] {});
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "BYTE ");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getByteValue(), fv1.getByteValue());

		fv1 = new FieldValue(new byte[] { 1 });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "BYTE 01");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getByteValue(), fv1.getByteValue());

		fv1 = new FieldValue(new byte[] { 1, -1 });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "BYTE 01FF");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getByteValue(), fv1.getByteValue());

		fv1 = new FieldValue(new float[] {});
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "FLOAT ");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getFloatValue(), fv1.getFloatValue(), 0.0f);

		fv1 = new FieldValue(new float[] { (float) 1.2 });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "FLOAT 1.2");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getFloatValue(), fv1.getFloatValue(), 0.0f);

		fv1 = new FieldValue(new float[] { (float) 1.2, (float) -2.3 });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "FLOAT 1.2,-2.3");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getFloatValue(), fv1.getFloatValue(), 0.0f);

		fv1 = new FieldValue(new short[] {});
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "SHORT ");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getShortValue(), fv1.getShortValue());

		fv1 = new FieldValue(new short[] { (short) 1 });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "SHORT 1");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getShortValue(), fv1.getShortValue());

		fv1 = new FieldValue(new short[] { (short) 1, (short) -1 });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "SHORT 1,-1");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getShortValue(), fv1.getShortValue());

		fv1 = new FieldValue(new String[] {});
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "STRING ");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getStringValue(), fv1.getStringValue());

		fv1 = new FieldValue(new String[] { null });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "STRING null");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getStringValue(), fv1.getStringValue());

		fv1 = new FieldValue(new String[] { "a ä" });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "STRING \"a ä\"");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getStringValue(), fv1.getStringValue());

		fv1 = new FieldValue(new String[] { "a\"b", "c ", null, "\"", " ", "" });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "STRING \"a\\\"b\",\"c \",null,\"\\\"\",\" \",\"\"");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getStringValue(), fv1.getStringValue());

		fv1 = new FieldValue(new int[] {});
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "USHORT ");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getUShortValue(), fv1.getUShortValue());

		fv1 = new FieldValue(new int[] { (short) 1 });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "USHORT 1");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getUShortValue(), fv1.getUShortValue());

		fv1 = new FieldValue(new int[] { (short) 1, (short) 2 });
		v = fs.serialize(Arrays.asList(fv1));
		Assert.assertEquals(v, "USHORT 1,2");
		Assert.assertArrayEquals(fs.deserialize(v).get(0).getUShortValue(), fv1.getUShortValue());
	}
}
