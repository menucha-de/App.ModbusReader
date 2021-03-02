package havis.custom.harting.modbus.reader.rest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import havis.custom.harting.modbus.reader.core.SlaveProcessor;
import havis.custom.harting.modbus.reader.rest.provider.ModbusReaderExceptionMapper;

public class RESTApplication extends Application {

	private final static String PROVIDERS = "javax.ws.rs.ext.Providers";

	private Set<Object> singletons = new HashSet<Object>();
	private Set<Class<?>> empty = new HashSet<Class<?>>();
	private Map<String, Object> properties = new HashMap<>();

	public RESTApplication(SlaveProcessor slaveProcessor) {
		singletons.add(new ModbusReaderService(slaveProcessor));
		properties.put(PROVIDERS, new Class<?>[] { ModbusReaderExceptionMapper.class });
	}

	@Override
	public Set<Class<?>> getClasses() {
		return empty;
	}

	public Set<Object> getSingletons() {
		return singletons;
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}
}