package havis.custom.harting.modbus.reader.osgi;

import havis.custom.harting.modbus.reader.common.OSGiServiceFactory;
import havis.custom.harting.modbus.reader.core.ModbusSlave;
import havis.custom.harting.modbus.reader.core.SlaveProcessor;
import havis.custom.harting.modbus.reader.rest.RESTApplication;
import havis.custom.harting.modbus.reader.rf.RfModule;
import havis.device.rf.RFDevice;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The OSGi activator starts the modbus service and provides RF controllers. The
 * controllers are searched with OSGi filter
 * <code>(&(objectClass=havis.device.rf.RFDevice))</code>.
 * <p>
 * The required properties are configured in <code>bundle.properties</code>
 * file:
 * <ul>
 * <li><code>havis.custom.harting.modbus.reader.config.base.path</code>: the
 * path to the configuration files of the modbus service</li>
 * <li><code>havis.custom.harting.modbus.reader.state.base.path</code>: the path
 * to the state files of the modbus service</li>
 * </ul>
 * </p>
 * <p>
 * If the file <code>bundle.properties</code> does not exist then the bundle
 * properties provided by the OSGi container are used.
 */
public class Activator implements BundleActivator {

	private final static Logger log = Logger.getLogger(Activator.class.getName());

	private ServiceRegistration<Application> app;

	private static final String BUNDLE_PROP_FILE = "bundle.properties";
	private static final String BUNDLE_PROP_PREFIX = "havis.custom.harting.modbus.reader.";
	private static final String PROP_CONFIG_BASE_PATH = "config.base.path";
	private static final String PROP_CONFIG_COPY = "config.copy";
	private static final String PROP_STATE_BASE_PATH = "state.base.path";

	private ModbusSlave modbusSlave;
	private ExecutorService threadPool;
	private Future<?> modbusSlaveFuture;

	@Override
	public void start(final BundleContext context) throws Exception {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());
		} finally {
			Thread.currentThread().setContextClassLoader(loader);
		}

		// load bundle properties file
		Properties bundleProps = null;
		URL propFileURL = context.getBundle().getResource(BUNDLE_PROP_FILE);
		if (propFileURL != null) {
			bundleProps = new Properties();
			try (InputStream propStream = propFileURL.openStream()) {
				bundleProps.load(propStream);
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Loaded bundle properties from file " + propFileURL);
				}
			}
		}
		// get properties
		String configBaseDir = getBundleProperty(bundleProps, context, PROP_CONFIG_BASE_PATH);
		String configCopyStr = getBundleProperty(bundleProps, context, PROP_CONFIG_COPY);
		boolean createConfigCopy = Boolean.parseBoolean(configCopyStr);
		String stateBaseDir = getBundleProperty(bundleProps, context, PROP_STATE_BASE_PATH);
		// create and start the modbus service
		OSGiServiceFactory<RFDevice> rfcServiceFactory = new OSGiServiceFactory<>(context, RFDevice.class);
		RfModule module = new RfModule(rfcServiceFactory);
		modbusSlave = new ModbusSlave(configBaseDir, stateBaseDir, createConfigCopy, module,
				null /* nativeLibraryNames */);
		threadPool = Executors.newFixedThreadPool(1);
		modbusSlaveFuture = threadPool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					SlaveProcessor slaveProcessor = modbusSlave.open();
					app = context.registerService(Application.class, new RESTApplication(slaveProcessor), null);
					modbusSlave.run();
				} catch (Throwable e) {
					log.log(Level.SEVERE, "Execution of modbus slave failed", e);
				}
			}
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// stop the modbus service
		if (modbusSlaveFuture != null) {
			try {
				modbusSlave.close();
				modbusSlaveFuture.get();
			} catch (Exception e) {
				log.log(Level.SEVERE, "Cannot close modbus slave", e);
			}
			threadPool.shutdown();
		}

		if (app != null) {
			app.unregister();
			app = null;
		}
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Bundle stopped");
		}
	}

	/**
	 * Gets a bundle property from <code>bundle.properties</code> file. If the
	 * file does not contain the property then the property is read via the
	 * bundle context.
	 * 
	 * @param bundleProps
	 * @param bundleContext
	 * @param key
	 * @return The bundle property value
	 * @throws MissingPropertyException
	 */
	private String getBundleProperty(Properties bundleProps, BundleContext bundleContext, String key)
			throws MissingPropertyException {
		String value = null;
		if (bundleProps != null) {
			value = bundleProps.getProperty(BUNDLE_PROP_PREFIX + key);
		}
		if (value == null) {
			value = bundleContext.getProperty(BUNDLE_PROP_PREFIX + key);
		}
		if (value == null) {
			throw new MissingPropertyException("Missing bundle property '" + BUNDLE_PROP_PREFIX + key + "'");
		}
		return value.trim();
	}
}