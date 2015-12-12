package circuit.operations.primitive;

import java.math.BigInteger;

import util.Util;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.structure.Wire;

public abstract class BasicOp implements Instruction {

	protected String opcode;
	protected Wire[] inputs;
	protected Wire[] outputs;
	protected String desc;
	protected int numMulGates;

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

	public int getNumMulGates() {
		return numMulGates;
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

	public String toString() {
		return opcode + " in " + inputs.length + " <" + Util.arrayToString(inputs, " ") + "> out " + outputs.length
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
		// For now, we care about binary operations with an actual cost, so this will be overriden in mul, xor and or.
		// TODO: revisit to complete all optimizations
		if(inputs.length == 2){
			return opcode.hashCode() + inputs[0].hashCode() +  inputs[1].hashCode();
		}
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(this == obj)
			return true;
		if(!(obj instanceof BasicOp)){
			return false;
		}
		BasicOp op = (BasicOp) obj;
		if(inputs.length != 2 || opcode.equals("pack") || opcode.equals("add")){
			return false; // Assume each instruction achieving the above as unique for now, i.e. caching ignored for them.
		} else{
			return op.opcode.equals(opcode) && ((inputs[0].equals(op.inputs[0])) && (inputs[1].equals(op.inputs[1])) || (
					(inputs[1].equals(op.inputs[0])) && (inputs[0].equals(op.inputs[1]))));
		}
	}

}
