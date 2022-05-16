package ch.psi.pshell.xscan.core;

import ch.psi.jcae.Channel;
import ch.psi.jcae.annotation.CaChannel;
import java.util.List;

public class TemplateVSC16Scaler {

	/**
	 * Command
	 */
	public enum Command {Done, Count};
	@CaChannel(type=Integer.class, name ="${PREFIX}.CNT")
	private Channel<Integer> command;
	
	
	public enum Mode {OneShot, AutoCount};
	/**
	 * Count mode
	 */
	@CaChannel(type=Integer.class, name ="${PREFIX}.CONT")
	private Channel<Integer> mode;
	
	/**
	 * Channel description
	 */
	@CaChannel(type=Boolean.class, name={"${PREFIX}.NM1", "${PREFIX}.NM2", "${PREFIX}.NM3", "${PREFIX}.NM4", "${PREFIX}.NM5", "${PREFIX}.NM6", "${PREFIX}.NM7", "${PREFIX}.NM8", "${PREFIX}.NM9", "${PREFIX}.NM10", "${PREFIX}.NM11", "${PREFIX}.NM12", "${PREFIX}.NM13", "${PREFIX}.NM14", "${PREFIX}.NM15", "${PREFIX}.NM16"})
	private List<Channel<Boolean>> channelDescription;
	
	/**
	 * Channel gate
	 */
	@CaChannel(type=Boolean.class, name={"${PREFIX}.G1", "${PREFIX}.G2", "${PREFIX}.G3", "${PREFIX}.G4", "${PREFIX}.G5", "${PREFIX}.G6", "${PREFIX}.G7", "${PREFIX}.G8", "${PREFIX}.G9", "${PREFIX}.G10", "${PREFIX}.G11", "${PREFIX}.G12", "${PREFIX}.G13", "${PREFIX}.G14", "${PREFIX}.G15", "${PREFIX}.G16"})
	private List<Channel<Boolean>> channelGate;
	
	/**
	 * Channel preset count
	 * If gate is on scaler will only count until this value
	 */
	@CaChannel(type=Integer.class, name={"${PREFIX}.PR1", "${PREFIX}.PR2", "${PREFIX}.PR3", "${PREFIX}.PR4", "${PREFIX}.PR5", "${PREFIX}.PR6", "${PREFIX}.PR7", "${PREFIX}.PR8", "${PREFIX}.PR9", "${PREFIX}.PR10", "${PREFIX}.PR11", "${PREFIX}.PR12", "${PREFIX}.PR13", "${PREFIX}.PR14", "${PREFIX}.PR15", "${PREFIX}.PR16"})
	private List<Channel<Integer>> channelPresetCount;

	
	
	public Channel<Integer> getCommand() {
		return command;
	}
	public Channel<Integer> getMode() {
		return mode;
	}
	public List<Channel<Boolean>> getChannelDescription() {
		return channelDescription;
	}
	public List<Channel<Boolean>> getChannelGate() {
		return channelGate;
	}
	public List<Channel<Integer>> getChannelPresetCount() {
		return channelPresetCount;
	}
}
