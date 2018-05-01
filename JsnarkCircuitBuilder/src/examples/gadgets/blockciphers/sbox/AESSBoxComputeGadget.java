/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.blockciphers.sbox;

import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.Wire;

/**
 * This gadget does not apply any lookups in the circuit. Instead, it verifies
 * the solution using the AES S-Box properties.
 * (Might need to be revisited in
 * the future to include other ways that have better circuit representation).
 *
 */

public class AESSBoxComputeGadget extends Gadget {

	private final Wire input;
	private Wire inverse;
	private Wire output;

	public AESSBoxComputeGadget(Wire input, String... desc) {
		super(desc);
		this.input = input;
		buildCircuit();
	}

	protected void buildCircuit() {
		inverse = generator.createProverWitnessWire();

		generator.addToEvaluationQueue(new Instruction() {

			@Override
			public void evaluate(CircuitEvaluator evaluator) {
				int p = evaluator.getWireValue(input).intValue(); 
				int q = findInv(p);
				evaluator.setWireValue(inverse, q);

			}
		});

		inverse.restrictBitLength(8);
		Wire v = gmul(input, inverse);
		generator.addAssertion(v.sub(generator.getOneWire()),
				input.add(inverse), generator.getZeroWire());
		Wire constant = generator.createConstantWire(0x63L);
		output = constant.xorBitwise(inverse, 8);
		output = output.xorBitwise(inverse.rotateLeft(8, 1), 8);
		output = output.xorBitwise(inverse.rotateLeft(8, 2), 8);
		output = output.xorBitwise(inverse.rotateLeft(8, 3), 8);
		output = output.xorBitwise(inverse.rotateLeft(8, 4), 8);
	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { output };
	}

	private Wire gmul(Wire a, Wire b) {
		Wire p = generator.getZeroWire();
		int counter;
		for (counter = 0; counter < 8; counter++) {
			Wire tmp = p.xorBitwise(a, 8);
			Wire bit = b.getBitWires(8).get(0);
			p = p.add(bit.mul(tmp.sub(p)));

			Wire bit2 = a.getBitWires(8).get(7);
			a = a.shiftLeft(8, 1);

			Wire tmp2 = a.xorBitwise(generator.createConstantWire(0x1bL), 8);
			a = a.add(bit2.mul(tmp2.sub(a)));
			b = b.shiftRight(8, 1);
		}
		return p;
	}

	private int gmul(int a, int b) {
		int p = 0;
		int j;
		for (j = 0; j < 8; j++) {
			if ((b & 1) != 0)
				p ^= a;
			a <<= 1;
			if ((a & 0x100) != 0)
				a ^= 0x11b;
			b >>= 1;
		}
		return p;
	}

	private int findInv(int a) {
		if (a == 0)
			return 0;
		for (int i = 0; i < 256; i++) {
			if (gmul(i, a) == 1) {
				return i;
			}
		}
		return -1;
	}
}
