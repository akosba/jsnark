/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.blockciphers.sbox;

import circuit.operations.Gadget;
import circuit.structure.Wire;
import examples.gadgets.blockciphers.AES128CipherGadget;

public class AESSBoxNaiveLookupGadget extends Gadget {

	private static int SBox[] = AES128CipherGadget.SBox;

	private Wire input;
	private Wire output;

	public AESSBoxNaiveLookupGadget(Wire input, String... desc) {
		super(desc);
		this.input = input;
		buildCircuit();
	}

	protected void buildCircuit() {
		output = generator.getZeroWire();
		for (int i = 0; i < 256; i++) {
			output = output.add(input.isEqualTo(i).mul(SBox[i]));
		}
	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { output };
	}
}
