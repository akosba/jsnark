/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations.primitive;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.structure.Wire;

public class AddBasicOp extends BasicOp {

	public AddBasicOp(Wire[] ws, Wire output, String...desc) {
		super(ws, new Wire[] { output }, desc);
	}

	public String getOpcode(){
		return "add";
	}
	
	@Override
	public void compute(BigInteger[] assignment) {
		BigInteger s = BigInteger.ZERO;
		for (Wire w : inputs) {
			s = s.add(assignment[w.getWireId()]);
		}
		assignment[outputs[0].getWireId()] = s.mod(Config.FIELD_PRIME);
	}
	
	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;
		if (!(obj instanceof AddBasicOp)) {
			return false;
		}
		AddBasicOp op = (AddBasicOp) obj;
		if(op.inputs.length!=inputs.length ){
			return false;
		}
		
		if(inputs.length == 2){
			boolean check1 = inputs[0].equals(op.inputs[0])
					&& inputs[1].equals(op.inputs[1]);
			boolean check2 = inputs[1].equals(op.inputs[0])
					&& inputs[0].equals(op.inputs[1]);
			return check1 || check2;
		} else {
			boolean check = true;
			for(int i = 0; i < inputs.length; i++){
				check = check && inputs[i].equals(op.inputs[i]);
			}
			return check;
		}
	}

	@Override
	public int getNumMulGates() {
		return 0;
	}

}