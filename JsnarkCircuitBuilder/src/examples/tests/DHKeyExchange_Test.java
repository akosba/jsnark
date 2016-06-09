/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.tests;

import java.math.BigInteger;
import java.util.ArrayList;

import org.junit.Test;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.encrypt.DHKeyExchangeGadget;
import junit.framework.TestCase;

/**
 * Tests Key Exchange via Field Extension Gadget (DHKeyExchangeGadget.java) 
 * Parameters used here assumes 80-bit security
 */

public class DHKeyExchange_Test extends TestCase {

	
	// This is a very simple example for testing purposes. To see how the gadget should be used, 
	// check the EncryptionCircuitGenerator.
	
	// The sage script to compute the ground truth is commented in the end of the file.
	
	@Test
	public void testCase1() {
		
		CircuitGenerator generator = new CircuitGenerator("FieldExtension_Test") {

			int mu = 4;
			int omega = 7;
			int exponentBitlength = 398;
			
			private Wire[] exponentBits;
			
			@Override
			protected void buildCircuit() {
				
				exponentBits = createInputWireArray(exponentBitlength, "exponent");

				Wire[] g = new Wire[mu];
				Wire[] h = new Wire[mu];

				// Hardcode the base and the other party's key (suitable when keys are not expected to change)
				g[0] = createConstantWire(new BigInteger("16377448892084713529161739182205318095580119111576802375181616547062197291263"));
				g[1] = createConstantWire(new BigInteger("13687683608888423916085091250849188813359145430644908352977567823030408967189"));
				g[2] = createConstantWire(new BigInteger("12629166084120705167185476169390021031074363183264910102253898080559854363106"));
				g[3] = createConstantWire(new BigInteger("19441276922979928804860196077335093208498949640381586557241379549605420212272"));

				h[0] = createConstantWire(new BigInteger("8252578783913909531884765397785803733246236629821369091076513527284845891757"));
				h[1] = createConstantWire(new BigInteger("20829599225781884356477513064431048695774529855095864514701692089787151865093"));
				h[2] = createConstantWire(new BigInteger("1540379511125324102377803754608881114249455137236500477169164628692514244862"));
				h[3] = createConstantWire(new BigInteger("1294177986177175279602421915789749270823809536595962994745244158374705688266"));

				DHKeyExchangeGadget dhKeyExchangeGadget = new DHKeyExchangeGadget(g, h, exponentBits,
						omega, "");

				Wire[] g_to_s = dhKeyExchangeGadget.getG_to_s();
				makeOutputArray(g_to_s, "DH Key Exchange Output");
				Wire[] h_to_s = dhKeyExchangeGadget.getH_to_s();
				makeOutputArray(h_to_s, "Derived Secret Key");
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				BigInteger exponent = new BigInteger("15403795111253241023778037546088811142494551372365004771691646286925142448620047716");
				for(int i = 0; i < exponentBitlength; i++){
					evaluator.setWireValue(exponentBits[i], exponent.testBit(i)?1:0);
				}
			}
		};

		generator.generateCircuit();
		generator.evalCircuit();
		CircuitEvaluator evaluator = generator.getCircuitEvaluator();
		ArrayList<Wire> output = generator.getOutWires();

		assertEquals(evaluator.getWireValue(output.get(0)), new BigInteger("5274624870134387213194184481949787806855248845359140549651696236170840383586"));
		assertEquals(evaluator.getWireValue(output.get(1)), new BigInteger("11408140113684302779148109128829830097222611544566630815277728255742325118113"));
		assertEquals(evaluator.getWireValue(output.get(2)), new BigInteger("18428984967641070417362655758139097563657834695139328130598865460729981248233"));
		assertEquals(evaluator.getWireValue(output.get(3)), new BigInteger("21795632195716898536277950755769517177046635486265098935154714495187346662803"));
		
		assertEquals(evaluator.getWireValue(output.get(4)), new BigInteger("18308269669967206985184468389331894151323011494636311991143094642137020992929"));
		assertEquals(evaluator.getWireValue(output.get(5)), new BigInteger("14722891607526974801993704488759871881652873250470609759275846621731517574370"));
		assertEquals(evaluator.getWireValue(output.get(6)), new BigInteger("13121843281460962144748056879571146169256820065972863227584019539593381558653"));
		assertEquals(evaluator.getWireValue(output.get(7)), new BigInteger("10744654123421919207899402303776582569502180625523222736897964458082852477687"));
	
	}
	
	/* Sage Script generating the above values:
	 *  F.<x> = GF(21888242871839275222246405745257275088548364400416034343698204186575808495617)[]
		K.<a> = GF(21888242871839275222246405745257275088548364400416034343698204186575808495617**4, name='a', modulus=x^4-7)
		
		base = 16377448892084713529161739182205318095580119111576802375181616547062197291263*a^0 + 13687683608888423916085091250849188813359145430644908352977567823030408967189*a^1 + 12629166084120705167185476169390021031074363183264910102253898080559854363106*a^2 + 19441276922979928804860196077335093208498949640381586557241379549605420212272*a^3
		h = 1294177986177175279602421915789749270823809536595962994745244158374705688266*a^3 + 1540379511125324102377803754608881114249455137236500477169164628692514244862*a^2 + 20829599225781884356477513064431048695774529855095864514701692089787151865093*a + 8252578783913909531884765397785803733246236629821369091076513527284845891757
		
		baseOrder = base.multiplicative_order()
		hOrder = h.multiplicative_order()
		print(baseOrder)
		print(hOrder)
		print(is_prime(baseOrder))
		
		secret = 15403795111253241023778037546088811142494551372365004771691646286925142448620047716
		base_to_secret = base^secret
		h_to_secret = h^secret
		print(base_to_secret)
		print(h_to_secret)
	 */
}
