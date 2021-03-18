package havis.app.modbus.reader.ui.res.cons;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.client.Constants;

public interface AppConstants extends Constants {

	public static final AppConstants INSTANCE = GWT.create(AppConstants.class);

	String header();
	String vendor();
	String product();
	String swRevision();
	String serialNo();
	String hwRevision();
	String firmware();
	String useManagement();
	String setupDevice();
	String tagsInField();
	String pc();
	String crc();
	String aPw();
	String kPw();
	String epc();
	String tid();
	String userMem();
	String selMasks();
	String customOp();
	String length();
	String qty();
	String bitWords();
	String regAddress();
	String lengthW();
	String regType();
	String access();
	String description();
	String sectionDeviceInfo();
	String sectionDeviceConfig();
	String sectionRuntimeConfig();
	String sectionRuntimeRegister();
	String regTypeHolding();
	String regTypeInput();
	String accessRead();
	String accessReadWrite();
	String failRuntimeConfig();
	String failDeviceInfo();
	String failRuntimeRegister();
	String failRuntimeChange();
}