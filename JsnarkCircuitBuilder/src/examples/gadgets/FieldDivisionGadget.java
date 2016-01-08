/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.gadgets;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.Wire;

public class FieldDivisionGadget extends Gadget {

	private Wire a;
	private Wire b;
	private Wire c;

	public FieldDivisionGadget(Wire a, Wire b, String... desc) {
		super(desc);
		this.a = a;
		this.b = b;
		buildCircuit();
	}

	private void buildCircuit() {

		c = generator.createProverWitnessWire(debugStr("division result"));
		generator.specifyProverWitnessComputation(new Instruction() {
			@Override
			public void evaluate(CircuitEvaluator evaluator) {
				BigInteger aValue = evaluator.getWireValue(a);
				BigInteger bValue = evaluator.getWireValue(b);
				BigInteger cValue = aValue.multiply(bValue.modInverse(Config.FIELD_PRIME)).mod(Config.FIELD_PRIME);
				evaluator.setWireValue(c, cValue);
			}

		});
		generator.addAssertion(b, c, a, debugStr("Assertion for division result"));
	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { c };
	}

}
