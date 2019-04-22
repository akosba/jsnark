/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations.primitive;

import java.math.BigInteger;

import util.Util;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.structure.Wire;

public abstract class BasicOp implements Instruction {

	protected Wire[] inputs;
	protected Wire[] outputs;
	protected String desc;

	public BasicOp(Wire[] inputs, Wire[] outputs, String... desc) {
		this.inputs = inputs;
		this.outputs = outputs;
		if (desc.length > 0) {
			this.desc = desc[0];
		} else {
			this.desc = "";
		}

		for (Wire w : inputs) {
			if (w == null) {
				System.err.println("One of the input wires is null: " + this);
				throw new NullPointerException("A null wire");
			} else if (w.getWireId() == -1) {
				System.err.println("One of the input wires is not packed: " + this);
				throw new IllegalArgumentException("A wire with a negative id");
			}
		}
		for (Wire w : outputs) {
			if (w == null) {
				System.err.println("One of the output wires is null" + this);
				throw new NullPointerException("A null wire");
			}
		}

	}


	public BasicOp(Wire[] inputs, Wire[] outputs) {
		this(inputs, outputs, "");
	}

	public void evaluate(CircuitEvaluator evaluator) {
		BigInteger[] assignment = evaluator.getAssignment();
		checkInputs(assignment);
		checkOutputs(assignment);
		compute(assignment);
	}

	protected void checkInputs(BigInteger[] assignment) {
		for (Wire w : inputs) {
			if (assignment[w.getWireId()] == null) {
				System.err.println("Error - The inWire " + w + " has not been assigned\n" + this);
				throw new RuntimeException("Error During Evaluation");
			}
		}
	}

	protected abstract void compute(BigInteger[] assignment);

	protected void checkOutputs(BigInteger[] assignment) {
		for (Wire w : outputs) {
			if (assignment[w.getWireId()] != null) {
				System.err.println("Error - The outWire " + w + " has already been assigned\n" + this);
				throw new RuntimeException("Error During Evaluation");
			}
		}
	}

	public abstract String getOpcode();
	public abstract int getNumMulGates();
	
	public String toString() {
		return getOpcode() + " in " + inputs.length + " <" + Util.arrayToString(inputs, " ") + "> out " + outputs.length
				+ " <" + Util.arrayToString(outputs, " ") + ">" + (desc.length() > 0 ? (" \t\t# " + desc) : "");
	}

	public Wire[] getInputs() {
		return inputs;
	}

	public Wire[] getOutputs() {
		return outputs;
	}

	public boolean doneWithinCircuit() {
		return true;
	}
	
	@Override
	public int hashCode() {
		// this method should be overriden when a subclass can have more than one opcode, or have other arguments
		int h = getOpcode().hashCode();
		for(Wire in:inputs){
			h+=in.hashCode();
		}
		return h;
	}
	
	
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		else
			return false;

		// logic moved to subclasses
	}

}
