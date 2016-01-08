/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations;

import circuit.config.Config;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.structure.Wire;

public class WireLabelInstruction implements Instruction {

	public enum LabelType {
		input, output, nizkinput, debug
	}

	private LabelType type;
	private Wire w;
	private String desc;

	public WireLabelInstruction(LabelType type, Wire w, String... desc) {
		this.type = type;
		this.w = w;
		if (desc.length > 0) {
			this.desc = desc[0];
		} else {
			this.desc = "";
		}
	}

	public Wire getWire() {
		return w;
	}

	public String toString() {
		return type + " " + w + (desc.length() == 0 ? "" : "\t\t\t # " + desc);
	}

	public void evaluate(CircuitEvaluator evaluator) {
		// nothing to do.
	}

	@Override
	public void emit(CircuitEvaluator evaluator) {
		if (type == LabelType.output && Config.outputVerbose || type == LabelType.debug && Config.debugVerbose) {
			System.out.println("\t[" + type + "] Value of Wire # " + w + (desc.length() > 0 ? " (" + desc + ")" : "") + " :: "
					+ evaluator.getWireValue(w).toString(Config.hexOutputEnabled ? 16 : 10));
		}
	}

	public LabelType getType() {
		return type;
	}

	public boolean doneWithinCircuit() {
		return type != LabelType.debug;
	}

}
