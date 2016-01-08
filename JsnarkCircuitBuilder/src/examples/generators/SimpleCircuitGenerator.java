/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.generators;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;

public class SimpleCircuitGenerator extends CircuitGenerator {

	private Wire[] inputs;

	public SimpleCircuitGenerator(String circuitName) {
		super(circuitName);
	}

	@Override
	protected void buildCircuit() {

		// declare input array of length 4.
		inputs = createInputWireArray(4);

		// r1 = in0 * in1
		Wire r1 = inputs[0].mul(inputs[1]);

		// r2 = in2 + in3
		Wire r2 = inputs[2].add(inputs[3]);

		// result = (r1+5)*(6*r2)
		Wire result = r1.add(5).mul(r2.mul(6));

		// mark the wire as output
		makeOutput(result);

	}

	@Override
	public void generateSampleInput(CircuitEvaluator circuitEvaluator) {
		for (int i = 0; i < 4; i++) {
			circuitEvaluator.setWireValue(inputs[i], i + 1);
		}
	}

	public static void main(String[] args) throws Exception {

		SimpleCircuitGenerator generator = new SimpleCircuitGenerator("simple_example");
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
