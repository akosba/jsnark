/*******************************************************************************
s * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.generators.hash;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.hash.SHA256Gadget;

public class SHA2CircuitGenerator extends CircuitGenerator {

	private Wire[] inputWires;
	private SHA256Gadget sha2Gadget;

	public SHA2CircuitGenerator(String circuitName) {
		super(circuitName);
	}

	@Override
	protected void buildCircuit() {
		
		// assuming the circuit input will be 64 bytes
		inputWires = createInputWireArray(64);
		// this gadget is not applying any padding.
		sha2Gadget = new SHA256Gadget(inputWires, 8, 64, false, false);
		Wire[] digest = sha2Gadget.getOutputWires();
		makeOutputArray(digest, "digest");
		
		// ======================================================================
		// To see how padding can be done, and see how the gadget library will save constraints automatically, 
		// try the snippet below instead.
		/*
			inputWires = createInputWireArray(3); 	// 3-byte input
			sha2Gadget = new SHA256Gadget(inputWires, 8, 3, false, true);
			Wire[] digest = sha2Gadget.getOutputWires();
			makeOutputArray(digest, "digest");
		*/
		
	}

	@Override
	public void generateSampleInput(CircuitEvaluator evaluator) {
		String inputStr = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijkl";
		for (int i = 0; i < inputWires.length; i++) {
			evaluator.setWireValue(inputWires[i], inputStr.charAt(i));
		}
	}

	public static void main(String[] args) throws Exception {
		SHA2CircuitGenerator generator = new SHA2CircuitGenerator("sha_256");
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
