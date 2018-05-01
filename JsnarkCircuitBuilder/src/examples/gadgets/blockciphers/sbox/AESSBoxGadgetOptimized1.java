/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.blockciphers.sbox;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;

import circuit.config.Config;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.Wire;
import examples.gadgets.blockciphers.AES128CipherGadget;
import examples.gadgets.blockciphers.sbox.util.LinearSystemSolver;

/**
 * This gadget implements the efficient read-only memory access from xjsnark
 * (the generic way). A  more efficient variant is implemented in
 * AESSBoxGadgetOptimized2.java
 * 
 *
 */

public class AESSBoxGadgetOptimized1 extends Gadget {

	private static int SBox[] = AES128CipherGadget.SBox;
	
	static ArrayList<BigInteger[]> allCoeffSet;

	static {
		// preprocessing
		solveLinearSystems();
	}

	private final Wire input;
	private Wire output;

	public AESSBoxGadgetOptimized1(Wire input, String... desc) {
		super(desc);
		this.input = input;
		buildCircuit();
	}

	public static void solveLinearSystems() {
		allCoeffSet = new ArrayList<BigInteger[]>();
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i <= 255; i++) {
			list.add(256 * i + SBox[i]);
		}
		boolean done = false;
		int trialCounter = 0;
		loop1: while (!done) {
			trialCounter++;
			if (trialCounter == 100) {
				throw new RuntimeException(
						"Was not possible to find an adequate solution to the current setting of the AES gadget sbox");
			}
			System.out
					.println("Attempting to solve linear systems for efficient S-Box Access: Attempt#"
							+ trialCounter);

			for (int i = 0; i <= 15; i++) {
				
				HashSet<Integer> memberValueSet = new HashSet<>();
				BigInteger[][] mat = new BigInteger[16][17];
				for (int k = 0; k < mat.length; k++) {
					int value = list.get(k + i * 16);
					memberValueSet.add(value);
					BigInteger p = BigInteger.valueOf(value);
					mat[k][0] = BigInteger.ONE;
					for (int j = 1; j <= 16; j++) {
						mat[k][j] = p.multiply(mat[k][j - 1]).mod(
								Config.FIELD_PRIME);
					}
				}
				new LinearSystemSolver(mat).solveInPlace();
				if (checkIfProverCanCheat(mat, memberValueSet)) {
					System.out.println("Invalid solution");
					BigInteger[] coeffs = new BigInteger[16];
					for (int ii = 0; ii < 16; ii++) {
						coeffs[ii] = mat[ii][16];
						if (coeffs[ii].equals(BigInteger.ZERO)) {
							System.out
									.println("Possibly invalid due to having zero coefficient(s)");
							break;
						}
					}

					continue loop1;
				}
				BigInteger[] coeffs = new BigInteger[16];
				for (int ii = 0; ii < 16; ii++) {
					coeffs[ii] = mat[ii][16];
					if (coeffs[ii].equals(BigInteger.ZERO)) {
						System.out.println("Invalid due to Case 2");
						continue loop1;
					}
				}
				allCoeffSet.add(coeffs);

			}
			done = true;
		}

	}

	private static boolean checkIfProverCanCheat(BigInteger[][] mat, HashSet<Integer> valueSet) {
		
		BigInteger[] coeffs = new BigInteger[16];
		for (int i = 0; i < 16; i++) {
			coeffs[i] = mat[i][16];
		}

		int validResults = 0;
		int outsidePermissibleSet = 0;

		// loop over the whole permissible domain (recall that input & output
		// are bounded)
		for (int k = 0; k < 256 * 256; k++) {

			BigInteger result = BigInteger.ZERO;
			result = coeffs[0];
			BigInteger value = BigInteger.valueOf(k);
			for (int i = 1; i < 16; i++) {
				result = result.add(value.multiply(coeffs[i]));
				value = value.multiply(BigInteger.valueOf(k)).mod(
						Config.FIELD_PRIME);
			}
			result = result.mod(Config.FIELD_PRIME);
			
			if (result.equals(value)){ 
				validResults++;
				if(!valueSet.contains(k)){
					outsidePermissibleSet++;
				}
			}
			
		}
		if (validResults != 16 || outsidePermissibleSet!=0) {
			System.out.println("Prover can cheat with linear system solution");
			System.out.println("Num of valid values that the prover can use = "
					+ validResults);
			System.out.println("Num of valid values outside permissible set = "
					+ validResults);
			return true;
		} else{
			return false;
		}
	}

	protected void buildCircuit() {

		output = generator.createProverWitnessWire();
		input.restrictBitLength(8);
		generator.specifyProverWitnessComputation(new Instruction() {

			@Override
			public void evaluate(CircuitEvaluator evaluator) {
				// TODO Auto-generated method stub
				BigInteger value = evaluator.getWireValue(input);
				evaluator.setWireValue(output,
						BigInteger.valueOf(SBox[value.intValue()]));
			}
		});

		output.getBitWires(8); // make sure output is bounded
		Wire[] vars = new Wire[16];
		Wire p = input.mul(256).add(output);
		vars[0] = generator.getOneWire();
		for (int i = 1; i < 16; i++) {
			vars[i] = vars[i - 1].mul(p);
		}

		Wire product = generator.getOneWire();
		for (BigInteger[] coeffs : allCoeffSet) {
			Wire accum = generator.getZeroWire();
			for (int j = 0; j < vars.length; j++) {
				accum = accum.add(vars[j].mul(coeffs[j]));
			}
			accum = accum.sub(vars[15].mul(p));
			product = product.mul(accum);
		}
		generator.addZeroAssertion(product);
	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { output };
	}

}
