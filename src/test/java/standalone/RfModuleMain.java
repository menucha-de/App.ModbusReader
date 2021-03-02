package standalone;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import havis.custom.harting.modbus.reader.common.ReflectionServiceFactory;
import havis.custom.harting.modbus.reader.common.ServiceFactory;
import havis.custom.harting.modbus.reader.core.ModbusSlave;
import havis.custom.harting.modbus.reader.rf.RfModule;
import havis.device.rf.RFDevice;

public class RfModuleMain {

	public static void main(String[] args) throws Exception {
		String stateBaseDir = args.length > 0 ? args[0] : null;

		ServiceFactory<RFDevice> sf = new ReflectionServiceFactory<RFDevice>(RFDeviceStub.class.getCanonicalName(),
				null /* addressSetterMethodName */);
		RfModule module = new RfModule(sf);
		final ModbusSlave modbusSlave = new ModbusSlave(null /* configBaseDir */, stateBaseDir,
				false /* createConfigCopy */, module, new String[] { "modbusjni" } /* nativeLibraryNames */);
		ExecutorService threadPool = Executors.newFixedThreadPool(1);
		Future<Object> modbusSlaveFuture = threadPool.submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				modbusSlave.open();
				modbusSlave.run();
				return null;
			}
		});
		try {
			// Thread.sleep(5000);
			// modbusSlave.close();
			modbusSlaveFuture.get();
		} finally {
			threadPool.shutdown();
		}
	}
}
