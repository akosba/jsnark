/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.generators.hybridEncryption;

import java.math.BigInteger;
import java.util.Arrays;

import util.Util;
import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import examples.gadgets.blockciphers.SymmetricEncryptionCBCGadget;
import examples.gadgets.diffieHellmanKeyExchange.FieldExtensionDHKeyExchange;
import examples.gadgets.hash.SHA256Gadget;

// This gadget shows a simple example of hybrid encryption for illustration purposes
// It currently uses the field extension key exchange gadget with the speck cipher

public class HybridEncryptionCircuitGenerator extends CircuitGenerator {

	private Wire[] plaintext; // as 64-bit words
	private int plaintextSize; // number of 64-bit words
	private Wire[] ciphertext; // as 64-bit words

	private String ciphername;
	private Wire[] secExpBits; 

	// Will assume the parameterization used in the test files ~ 80-bits
	// security
	public static final int EXPONENT_BITWIDTH = 397; // in bits
	public static final int MU = 4;
	public static final int OMEGA = 7;
	
	public HybridEncryptionCircuitGenerator(String circuitName, int plaintextSize,
			String ciphername) {
		super(circuitName);
		this.ciphername = ciphername;
		this.plaintextSize = plaintextSize;
	}

	@Override
	protected void buildCircuit() {

		plaintext = createInputWireArray(plaintextSize, "plaint text");

		
		// Part I: Exchange a key:
		
		// The secret exponent is a private input by the prover
		secExpBits = createProverWitnessWireArray(EXPONENT_BITWIDTH, "SecretExponent");
		for(int i = 0; i < EXPONENT_BITWIDTH; i++){
			addBinaryAssertion(secExpBits[i]); // verify all bits are binary
		}

		Wire[] g = new Wire[MU];
		Wire[] h = new Wire[MU];

		// Hardcode the base and the other party's key (suitable when keys are not expected to change)
		g[0] = createConstantWire(new BigInteger("16377448892084713529161739182205318095580119111576802375181616547062197291263"));
		g[1] = createConstantWire(new BigInteger("13687683608888423916085091250849188813359145430644908352977567823030408967189"));
		g[2] = createConstantWire(new BigInteger("12629166084120705167185476169390021031074363183264910102253898080559854363106"));
		g[3] = createConstantWire(new BigInteger("19441276922979928804860196077335093208498949640381586557241379549605420212272"));

		h[0] = createConstantWire(new BigInteger("8252578783913909531884765397785803733246236629821369091076513527284845891757"));
		h[1] = createConstantWire(new BigInteger("20829599225781884356477513064431048695774529855095864514701692089787151865093"));
		h[2] = createConstantWire(new BigInteger("1540379511125324102377803754608881114249455137236500477169164628692514244862"));
		h[3] = createConstantWire(new BigInteger("1294177986177175279602421915789749270823809536595962994745244158374705688266"));

		// To make g and h variable inputs to the circuit, simply do the following
		// instead, and supply the above values using the generateSampleInput()
		// method instead.
		/*
		 * Wire[] g = createInputWireArray(mu); 
		 * Wire[] h = createInputWireArray(mu);
		 */

		// Exchange keys
		FieldExtensionDHKeyExchange exchange = new FieldExtensionDHKeyExchange(g, h, secExpBits,
				OMEGA, "");

		// Output g^s
		Wire[] g_to_s = exchange.getOutputPublicValue();
		makeOutputArray(g_to_s, "DH Key Exchange Output");

		// Use h^s to generate a symmetric secret key and an initialization
		// vector. Apply a Hash-based KDF.
		Wire[] h_to_s = exchange.getSharedSecret();
		SHA256Gadget hashGadget = new SHA256Gadget(h_to_s, 256, 128, true,
				false);
		Wire[] secret = hashGadget.getOutputWires();
		Wire[] key = Arrays.copyOfRange(secret, 0, 128);
		Wire[] iv = Arrays.copyOfRange(secret, 128, 256);

	
		// Part II: Apply symmetric Encryption

		Wire[] plaintextBits = new WireArray(plaintext).getBits(64).asArray();
		SymmetricEncryptionCBCGadget symEncGagdet = new SymmetricEncryptionCBCGadget(
				plaintextBits, key, iv, ciphername);
		ciphertext = symEncGagdet.getOutputWires();
		makeOutputArray(ciphertext, "Cipher Text");
	}

	@Override
	public void generateSampleInput(CircuitEvaluator evaluator) {
		// TODO Auto-generated method stub
		for(int i = 0; i < plaintextSize; i++){
			evaluator.setWireValue(plaintext[i], Util.nextRandomBigInteger(64));
		}
		for(int i = 0; i < EXPONENT_BITWIDTH; i++){
			evaluator.setWireValue(secExpBits[i], Util.nextRandomBigInteger(1));
		}
		
	}

	public static void main(String[] args) throws Exception {
		HybridEncryptionCircuitGenerator generator = new HybridEncryptionCircuitGenerator(
				"enc_example", 16, "speck128");
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
