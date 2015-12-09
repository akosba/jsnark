package circuit.structure;

public class LinearCombinationBitWire extends BitWire {

	public LinearCombinationBitWire(int wireId) {
		super(wireId);
	}
	
	public WireArray getBitWires() {
		return new WireArray(new Wire[]{this});
	}

}
