package ch.psi.pshell.xscan.core;

public class TestConfiguration {
	private static final TestConfiguration instance = new TestConfiguration();
	
	private final String otfPrefix = "MTEST-HW3-OTFX";
	private final String crlogicPrefix = "MTEST-HW3-CRL";
	private final String prefixScaler = "MTEST-HW3:JS";
	private final String server = "MTEST-VME-HW3";
	
	private final String motor1 = "MTEST-HW3:MOT1";
	private final String analogIn1 = "MTEST-HW3-AI1:AI_01";
	
	private final String ioc = "MTEST-VME-HW3.psi.ch";
	
	private TestConfiguration(){
	}
	
	public static TestConfiguration getInstance(){
		return instance;
	}

	/**
	 * @return the prefix
	 */
	public String getCrlogicPrefix() {
		return crlogicPrefix;
	}

	/**
	 * @return the prefixScaler
	 */
	public String getPrefixScaler() {
		return prefixScaler;
	}

	/**
	 * @return the server
	 */
	public String getServer() {
		return server;
	}

	/**
	 * @return the motor1
	 */
	public String getMotor1() {
		return motor1;
	}

	/**
	 * @return the analogIn1
	 */
	public String getAnalogIn1() {
		return analogIn1;
	}

	/**
	 * @return the otfPrefix
	 */
	public String getOtfPrefix() {
		return otfPrefix;
	}

	public String getIoc() {
		return ioc;
	}
	
}
