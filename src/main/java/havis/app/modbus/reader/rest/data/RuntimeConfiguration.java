package havis.app.modbus.reader.rest.data;

public class RuntimeConfiguration {

	private static int KILL_PWD = 0b1;
	private static int ACCESS_PWD = 0b10;
	private static int CRC = 0b100;
	private static int PC = 0b1000;
	private static int XPC = 0b10000;

	private int tagsInField;
	private short memorySelector;
	private int epcLength;
	private int tidLength;
	private int userLength;
	private int selectionMaskCount;
	private int selectionMaskMaxLength;
	private int customOperationMaxLength;

	public int getTagsInField() {
		return tagsInField;
	}

	public void setTagsInField(int tagsInField) {
		this.tagsInField = tagsInField;
	}

	public short getMemorySelector() {
		return memorySelector;
	}

	public void setMemorySelector(short memorySelector) {
		this.memorySelector = memorySelector;
	}

	private void setMemValue(boolean include, int value) {
		if (include) {
			this.memorySelector |= value;
		} else {
			this.memorySelector &= ~value;
		}
	}

	private boolean getMemValue(int value) {
		return (this.memorySelector & value) > 0;
	}

	public boolean isIncludeXPC() {
		return getMemValue(XPC);
	}

	public void setIncludeXPC(boolean includeXPC) {
		setMemValue(includeXPC, XPC);
	}

	public boolean isIncludePC() {
		return getMemValue(PC);
	}

	public void setIncludePC(boolean includePC) {
		setMemValue(includePC, PC);
	}

	public boolean isIncludeCRC() {
		return getMemValue(CRC);
	}

	public void setIncludeCRC(boolean includeCRC) {
		setMemValue(includeCRC, CRC);
	}

	public boolean isIncludeAccessPwd() {
		return getMemValue(ACCESS_PWD);
	}

	public void setIncludeAccessPwd(boolean includeAccessPwd) {
		setMemValue(includeAccessPwd, ACCESS_PWD);
	}

	public boolean isIncludeKillPwd() {
		return getMemValue(KILL_PWD);
	}

	public void setIncludeKillPwd(boolean includeKillPwd) {
		setMemValue(includeKillPwd, KILL_PWD);
	}

	public int getEpcLength() {
		return epcLength;
	}

	public void setEpcLength(int epcLength) {
		this.epcLength = epcLength;
	}

	public int getTidLength() {
		return tidLength;
	}

	public void setTidLength(int tidLength) {
		this.tidLength = tidLength;
	}

	public int getUserLength() {
		return userLength;
	}

	public void setUserLength(int userLength) {
		this.userLength = userLength;
	}

	public int getSelectionMaskCount() {
		return selectionMaskCount;
	}

	public void setSelectionMaskCount(int selectionMaskCount) {
		this.selectionMaskCount = selectionMaskCount;
	}

	public int getSelectionMaskMaxLength() {
		return selectionMaskMaxLength;
	}

	public void setSelectionMaskMaxLength(int selectionMaskMaxLength) {
		this.selectionMaskMaxLength = selectionMaskMaxLength;
	}

	public int getCustomOperationMaxLength() {
		return customOperationMaxLength;
	}

	public void setCustomOperationMaxLength(int customOperationMaxLength) {
		this.customOperationMaxLength = customOperationMaxLength;
	}

}
