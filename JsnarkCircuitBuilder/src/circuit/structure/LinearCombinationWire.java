/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.structure;

public class LinearCombinationWire extends Wire {

	private WireArray bitWires;

	public LinearCombinationWire(int wireId) {
		super(wireId);
	}
	
	public LinearCombinationWire(WireArray bits) {
		super(bits);
	}
	
	WireArray getBitWires() {
		return bitWires;
	}

	void setBits(WireArray bitWires) {
		this.bitWires = bitWires;
	}

}
