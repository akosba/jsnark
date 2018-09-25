/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.gadgets.math;

import circuit.operations.Gadget;
import circuit.structure.Wire;

public class DotProductGadget extends Gadget {

	private Wire[] a;
	private Wire[] b;
	private Wire output;

	public DotProductGadget(Wire[] a, Wire[] b, String... desc) {
		super(desc);
		if (a.length != b.length) {
			throw new IllegalArgumentException();
		}
		this.a = a;
		this.b = b;
		buildCircuit();
	}

	private void buildCircuit() {
		output = generator.getZeroWire();
		for (int i = 0; i < a.length; i++) {
			Wire product = a[i].mul(b[i], "Multiply elements # " + i);
			output = output.add(product);
		}
	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { output };
	}
}
