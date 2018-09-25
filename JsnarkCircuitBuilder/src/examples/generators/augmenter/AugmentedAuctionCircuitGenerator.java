/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.generators.augmenter;

import java.util.Arrays;

import util.Util;
import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.augmenter.PinocchioGadget;
import examples.gadgets.hash.SHA256Gadget;

/**
 * This circuit generator augments a second-price auction circuit (produced by Pinocchio's compiler) 
 * with SHA-256 gadgets on each input and output value. 
 *
 */

public class AugmentedAuctionCircuitGenerator extends CircuitGenerator {

	// each value is assumed to be a 64-bit value
	private Wire[] secretInputValues;
	private Wire[] secretOutputValues; 

	// randomness vectors for each participant (each random vector is 7 64-bit words)
	private Wire[][] secretInputRandomness;
	private Wire[][] secretOutputRandomness; 
	
	private String pathToCompiledCircuit;
	private int numParties; // includes the auction manager + the participants
	
	public AugmentedAuctionCircuitGenerator(String circuitName, String pathToCompiledCircuit, int numParticipants) {
		super(circuitName);
		this.pathToCompiledCircuit  = pathToCompiledCircuit;
		this.numParties = numParticipants + 1;
	}

	@Override
	protected void buildCircuit() {

		secretInputValues = createProverWitnessWireArray(numParties - 1); // the manager has a zero input (no need to commit to it)
		secretInputRandomness = new Wire[numParties - 1][];
		secretOutputRandomness = new Wire[numParties][];
		for(int i = 0; i < numParties - 1; i++){
			secretInputRandomness[i] =   createProverWitnessWireArray(7);
			secretOutputRandomness[i] =   createProverWitnessWireArray(7);
		}
		secretOutputRandomness[numParties-1] =   createProverWitnessWireArray(7);

		// instantiate a Pinocchio gadget for the auction circuit
		PinocchioGadget auctionGagdet = new PinocchioGadget(Util.concat(zeroWire, secretInputValues), pathToCompiledCircuit);
		Wire[] outputs = auctionGagdet.getOutputWires();
		
		// ignore the last output for this circuit which carries the index of the winner (not needed for this example)
		secretOutputValues = Arrays.copyOfRange(outputs, 0, outputs.length - 1);
		
		// augment the input side
		for(int i = 0; i < numParties - 1; i++){
			SHA256Gadget g = new SHA256Gadget(Util.concat(secretInputValues[i], secretInputRandomness[i]), 64, 64, false, false);
			makeOutputArray(g.getOutputWires(), "Commitment for party # " + i + "'s input balance.");
		}
		
		// augment the output side
		for(int i = 0; i < numParties; i++){
			// adapt the output values to 64-bit values (adaptation is needed due to the way Pinocchio's compiler handles subtractions) 
			secretOutputValues[i] = secretOutputValues[i].getBitWires(64*2).packAsBits(64);
			SHA256Gadget g = new SHA256Gadget(Util.concat(secretOutputValues[i], secretOutputRandomness[i]), 64, 64, false, false);
			makeOutputArray(g.getOutputWires(), "Commitment for party # " + i + "'s output balance.");
		}
	}

	@Override
	public void generateSampleInput(CircuitEvaluator evaluator) {
		
		for(int i = 0; i < numParties - 1; i++){
			evaluator.setWireValue(secretInputValues[i], Util.nextRandomBigInteger(63));
		}		
		
		for(int i = 0; i < numParties - 1; i++){
			for(Wire w:secretInputRandomness[i]){
				evaluator.setWireValue(w, Util.nextRandomBigInteger(64));
			}
		}
		for(int i = 0; i < numParties; i++){
			for(Wire w:secretOutputRandomness[i]){
				evaluator.setWireValue(w, Util.nextRandomBigInteger(64));
			}
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		AugmentedAuctionCircuitGenerator generator = new AugmentedAuctionCircuitGenerator("augmented_auction_10", "auction_10.arith", 10);
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();	
	}

}
