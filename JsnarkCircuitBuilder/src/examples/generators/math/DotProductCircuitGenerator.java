/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.generators.math;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.math.DotProductGadget;

public class DotProductCircuitGenerator extends CircuitGenerator {

	private Wire[] a;
	private Wire[] b;
	private int dimension;

	public DotProductCircuitGenerator(String circuitName, int dimension) {
		super(circuitName);
		this.dimension = dimension;
	}

	@Override
	protected void buildCircuit() {

		a = createInputWireArray(dimension, "Input a");
		b = createInputWireArray(dimension, "Input b");

		DotProductGadget dotProductGadget = new DotProductGadget(a, b);
		Wire[] result = dotProductGadget.getOutputWires();
		makeOutput(result[0], "output of dot product a, b");
	}

	@Override
	public void generateSampleInput(CircuitEvaluator circuitEvaluator) {

		for (int i = 0; i < dimension; i++) {
			circuitEvaluator.setWireValue(a[i], 10 + i);
			circuitEvaluator.setWireValue(b[i], 20 + i);
		}
	}

	public static void main(String[] args) throws Exception {

		DotProductCircuitGenerator generator = new DotProductCircuitGenerator("dot_product", 3);
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();	
	}

}
