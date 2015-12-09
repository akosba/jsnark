package examples.generators;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.SHA256Gadget;

public class SHA2CircuitGenerator extends CircuitGenerator {

	private Wire[] inputWires;
	private SHA256Gadget sha2Gadget;

	public SHA2CircuitGenerator(String circuitName) {
		super(circuitName);
	}

	@Override
	protected void buildCircuit() {
		inputWires = createInputWireArray(3, "");
		sha2Gadget = new SHA256Gadget(inputWires, 8, 3, false, true, "");
		Wire[] digest = sha2Gadget.getOutputWires();
		makeOutputArray(digest, "digest");
	}

	@Override
	public void generateSampleInput(CircuitEvaluator evaluator) {
		String inputStr = "abc";
		for (int i = 0; i < inputStr.length(); i++) {
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
