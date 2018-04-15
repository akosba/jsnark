/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.tests.diffieHellmanKeyExchange;

import java.math.BigInteger;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Test;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.diffieHellmanKeyExchange.ECDHKeyExchangeGadget;

/**
 * Tests Key Exchange via Elliptic curve Gadget (ECDHKeyExchangeGadget.java) 

 */

public class ECDHKeyExchange_Test extends TestCase {

	
	// The sage script to compute the sample case is commented in the end of the file.
	// TODO: Add more test cases
	
	@Test
	public void testVariableInputCase() {
		
		CircuitGenerator generator = new CircuitGenerator("ECDH_Test") {

			int exponentBitlength = ECDHKeyExchangeGadget.SECRET_BITWIDTH;			
			private Wire[] secretBits;
			private Wire baseX;
			private Wire hX;
			
			@Override
			protected void buildCircuit() {
				
				secretBits = createInputWireArray(exponentBitlength, "exponent");
				baseX = createInputWire();
				hX = createInputWire();
				

				ECDHKeyExchangeGadget keyExchangeGadget = 
						new ECDHKeyExchangeGadget(baseX, hX, secretBits);

				makeOutput(keyExchangeGadget.getOutputPublicValue());		
				
				// Just for testing. In real scenarios, this should not be made public
				makeOutput(keyExchangeGadget.getSharedSecret());
				
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				
				evaluator.setWireValue(baseX, new BigInteger("4"));
				evaluator.setWireValue(hX, new BigInteger("21766081959050939664800904742925354518084319102596785077490863571049214729748"));
				
				BigInteger exponent = new BigInteger("13867691842196510828352345865165018381161315605899394650350519162543016860992");
				for(int i = 0; i < exponentBitlength; i++){
					evaluator.setWireValue(secretBits[i], exponent.testBit(i)?1:0);
				}
			}
		};

		generator.generateCircuit();
		generator.evalCircuit();
		CircuitEvaluator evaluator = generator.getCircuitEvaluator();
		ArrayList<Wire> output = generator.getOutWires();

		assertEquals(evaluator.getWireValue(output.get(0)), new BigInteger("13458082339735734368462130456283583571822918321676509705348825437102113182254"));
		assertEquals(evaluator.getWireValue(output.get(1)), new BigInteger("4167917227796707610764894996898236918915412447839980711033808347811701875717"));	
	}
	

	@Test
	public void testHardcodedInputCase() {
		
		CircuitGenerator generator = new CircuitGenerator("ECDH_Test2") {


			int exponentBitlength = ECDHKeyExchangeGadget.SECRET_BITWIDTH;			
			private Wire[] secretBits;
			private Wire baseX;
			private Wire hX;
			
			@Override
			protected void buildCircuit() {
				
				secretBits = createInputWireArray(exponentBitlength, "exponent");
				baseX = createConstantWire(new BigInteger("4"));
				hX = createConstantWire(new BigInteger("21766081959050939664800904742925354518084319102596785077490863571049214729748"));

				ECDHKeyExchangeGadget keyExchangeGadget = 
						new ECDHKeyExchangeGadget(baseX, hX, secretBits);

				makeOutput(keyExchangeGadget.getOutputPublicValue());		
				
				// Just for testing. In real scenarios, this should not be made public
				makeOutput(keyExchangeGadget.getSharedSecret());
				
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				

				BigInteger exponent = new BigInteger("13867691842196510828352345865165018381161315605899394650350519162543016860992");
				for(int i = 0; i < exponentBitlength; i++){
					evaluator.setWireValue(secretBits[i], exponent.testBit(i)?1:0);
				}
			}
		};

		generator.generateCircuit();
		generator.evalCircuit();
		CircuitEvaluator evaluator = generator.getCircuitEvaluator();
		ArrayList<Wire> output = generator.getOutWires();

		assertEquals(evaluator.getWireValue(output.get(0)), new BigInteger("13458082339735734368462130456283583571822918321676509705348825437102113182254"));
		assertEquals(evaluator.getWireValue(output.get(1)), new BigInteger("4167917227796707610764894996898236918915412447839980711033808347811701875717"));	
	}

	
	@Test
	public void testInputValidation1() {
		
		CircuitGenerator generator = new CircuitGenerator("ECDH_Test_InputValidation") {


			int exponentBitlength = ECDHKeyExchangeGadget.SECRET_BITWIDTH;			
			private Wire[] secretBits;
			private Wire baseX;
			private Wire hX;
			
			@Override
			protected void buildCircuit() {
				
				secretBits = createInputWireArray(exponentBitlength, "exponent");
				baseX = createInputWire();
				hX = createInputWire();
				

				ECDHKeyExchangeGadget keyExchangeGadget = 
						new ECDHKeyExchangeGadget(baseX, hX, secretBits);

				keyExchangeGadget.validateInputs();
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				
				evaluator.setWireValue(baseX, new BigInteger("4"));
				evaluator.setWireValue(hX, new BigInteger("21766081959050939664800904742925354518084319102596785077490863571049214729748"));
				
				BigInteger exponent = new BigInteger("13867691842196510828352345865165018381161315605899394650350519162543016860992");
				for(int i = 0; i < exponentBitlength; i++){
					evaluator.setWireValue(secretBits[i], exponent.testBit(i)?1:0);
				}
			}
		};

		generator.generateCircuit();
		generator.evalCircuit();

		// if no exception get thrown we are ok
	}
	
	

	public void testInputValidation2() {
		
		
		// try invalid input
		CircuitGenerator generator = new CircuitGenerator("ECDH_Test_InputValidation2") {


			int exponentBitlength = ECDHKeyExchangeGadget.SECRET_BITWIDTH;			
			private Wire[] secretBits;
			private Wire baseX;
			private Wire hX;
			
			@Override
			protected void buildCircuit() {
				
				secretBits = createInputWireArray(exponentBitlength, "exponent");
				baseX = createInputWire();
				hX = createInputWire();


				ECDHKeyExchangeGadget keyExchangeGadget = 
						new ECDHKeyExchangeGadget(baseX, baseX, hX, hX, secretBits);

				keyExchangeGadget.validateInputs();
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				
				// invalid
				evaluator.setWireValue(baseX, new BigInteger("14"));
				evaluator.setWireValue(hX, new BigInteger("21766081959050939664800904742925354518084319102596785077490863571049214729748"));
				
				BigInteger exponent = new BigInteger("13867691842196510828352345865165018381161315605899394650350519162543016860992");
				for(int i = 0; i < exponentBitlength; i++){
					evaluator.setWireValue(secretBits[i], exponent.testBit(i)?1:0);
				}
			}
		};

		generator.generateCircuit();

		// we expect an exception somewhere
		try{
			generator.evalCircuit();
			assertTrue(false);		
		} catch(Exception e){
			System.out.println("Exception Expected!");
			assertTrue(true);
		}
		
		// TODO: test more error conditions
	}
	
	
	
//		Sage Script generating the above values:
//		
//		p = 21888242871839275222246405745257275088548364400416034343698204186575808495617
//		K.<a> = NumberField(x-1)
//		aa = 126932
//		E = EllipticCurve(GF(p),[0,aa,0,1,0])
//		print(E.order())
//		print(n(log(E.order(),2)))
//		print(n(log(2736030358979909402780800718157159386074658810754251464600343418943805806723,2)))
//		
//		secret = 13867691842196510828352345865165018381161315605899394650350519162543016860992
//		
//		base = E(4,  5854969154019084038134685408453962516899849177257040453511959087213437462470)
//		print(base*secret)
//		print(h*secret)
}
