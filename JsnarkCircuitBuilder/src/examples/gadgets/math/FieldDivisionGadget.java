/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.gadgets.math;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.ConstantWire;
import circuit.structure.Wire;

public class FieldDivisionGadget extends Gadget {

	private final Wire a;
	private final Wire b;
	private Wire c;

	public FieldDivisionGadget(Wire a, Wire b, String... desc) {
		super(desc);
		this.a = a;
		this.b = b;
		// if the input values are constant (i.e. known at compilation time), we
		// can save one constraint
		if (a instanceof ConstantWire && b instanceof ConstantWire) {
			c = generator.createConstantWire(((ConstantWire) a)
					.getConstant()
					.multiply(
							((ConstantWire) b).getConstant().modInverse(
									Config.FIELD_PRIME))
					.mod(Config.FIELD_PRIME));
		} else {
			c = generator.createProverWitnessWire(debugStr("division result"));
			buildCircuit();
		}
	}

	private void buildCircuit() {

		// This is an example of computing a value outside the circuit and
		// verifying constraints about it in the circuit

		generator.specifyProverWitnessComputation(new Instruction() {
			@Override
			public void evaluate(CircuitEvaluator evaluator) {
				BigInteger aValue = evaluator.getWireValue(a);
				BigInteger bValue = evaluator.getWireValue(b);
				BigInteger cValue = aValue.multiply(
						bValue.modInverse(Config.FIELD_PRIME)).mod(
						Config.FIELD_PRIME);
				evaluator.setWireValue(c, cValue);
			}

		});
		generator.addAssertion(b, c, a,
				debugStr("Assertion for division result"));

		/*
		 * Two notes: 1) The order of the above two statements matters (the
		 * specification and the assertion). In the current version, it's not
		 * possible to swap them, as in the evaluation sequence, the assertion
		 * must happen after the value is assigned.
		 * 
		 * 2) The instruction defined above relies on the values of wires (a)
		 * and (b) during runtime. This means that if any point later in the
		 * program, the references a, and b referred to other wires, these wires
		 * are going to be used instead in this instruction. Therefore, it will
		 * be safer to use final references in cases like that to reduce the
		 * probability of errors.
		 */
	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { c };
	}

}
