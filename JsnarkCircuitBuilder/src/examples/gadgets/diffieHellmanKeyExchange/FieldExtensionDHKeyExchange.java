/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.diffieHellmanKeyExchange;

import java.math.BigInteger;
import java.util.Arrays;

import util.Util;
import circuit.operations.Gadget;
import circuit.structure.Wire;

/**
 * Performs Key Exchange using a field extension F_p[x]/(x^\mu - \omega), where
 * the polynomial (x^\mu - \omega) is irreducible. The inputs to this gadget:
 * the base g, the other party's input h = g^a, the bits of the secret exponent
 * secExpBits and omega. The outputs of this gadget: the derived key h^s to be
 * used for symmetric key derivation, and g^s which is sent to the other party.
 *
 * A sample parameterization that gives low security (~80 bits of security) can
 * be found in the Junit tests. A sample usage is in:
 * examples/generators/EncryptionCircuitGenerator.java
 * 
 * 
 */
public class FieldExtensionDHKeyExchange extends Gadget {

	private Wire[] g; // base
	private Wire[] h; // other party's public input (supposedly, h = g^(the
						// other party's secret))

	private Wire[] secretExponentBits; // the bits of the secret exponent of the
										// party
	// executing this gadget
	private long omega;
	private int mu;

	// gadget outputs
	private Wire[] outputPublicValue; // g^s (to be sent to the other party)
	private Wire[] sharedSecret; // the derived secret key h^s
	private Wire[][] gPowersTable;
	private Wire[][] hPowersTable;

	/**
	 * Note: In the default mode, the gadget only validates the secret input
	 * provided by the prover, but it does not validate that the base and public
	 * input of the other's party are proper elements. Since these values are
	 * public, they could be checked outside the circuit.
	 * 
	 * If the validation is needed inside, the method "validateInputs()" should
	 * be called explicitly. Example is provided in
	 * FieldExtensionDHKeyExchange_Test
	 * 
	 */
	public FieldExtensionDHKeyExchange(Wire[] g, Wire[] h,
			Wire[] secretExponentBits, long omega, String desc) {
		super(desc);
		this.g = g;
		this.h = h;
		this.secretExponentBits = secretExponentBits;
		this.omega = omega;
		mu = g.length;
		if (h.length != g.length) {
			throw new IllegalArgumentException(
					"g and h must have the same dimension");
		}

		// since this is typically a private input by the prover,
		// the check is also done here for safety. No need to remove this if
		// done also outside the gadget. The back end takes care of caching
		for (Wire w : secretExponentBits) {
			generator.addBinaryAssertion(w);
		}

		buildCircuit();
	}

	protected void buildCircuit() {
		gPowersTable = preparePowersTable(g);
		hPowersTable = preparePowersTable(h);
		outputPublicValue = exp(g, secretExponentBits, gPowersTable);
		sharedSecret = exp(h, secretExponentBits, hPowersTable);
	}

	private Wire[] mul(Wire[] a, Wire[] b) {
		Wire[] c = new Wire[mu];
		int i, j;
		for (i = 0; i < mu; i += 1) {
			c[i] = generator.getZeroWire();
		}
		for (i = 0; i < mu; i += 1) {
			for (j = 0; j < mu; j += 1) {
				int k = i + j;
				if (k < mu) {
					c[k] = c[k].add(a[i].mul(b[j]));
				}
				k = i + j - mu;
				if (k >= 0) {
					c[k] = c[k].add(a[i].mul(b[j]).mul(omega));
				}
			}
		}
		return c;
	}

	private Wire[][] preparePowersTable(Wire[] base) {
		Wire[][] powersTable = new Wire[secretExponentBits.length + 1][mu];
		powersTable[0] = Arrays.copyOf(base, mu);
		for (int j = 1; j < secretExponentBits.length + 1; j += 1) {
			powersTable[j] = mul(powersTable[j - 1], powersTable[j - 1]);
		}
		return powersTable;
	}

	private Wire[] exp(Wire[] base, Wire[] expBits, Wire[][] powersTable) {

		Wire[] c = new Wire[mu];
		Arrays.fill(c, generator.getZeroWire());
		c[0] = generator.getOneWire();
		for (int j = 0; j < expBits.length; j += 1) {
			Wire[] tmp = mul(c, powersTable[j]);
			for (int i = 0; i < mu; i++) {
				c[i] = c[i].add(expBits[j].mul(tmp[i].sub(c[i])));
			}
		}
		return c;
	}

	// TODO: Test more scenarios
	public void validateInputs(BigInteger subGroupOrder) {

		// g and h are not zero and not one

		// checking the first chunk
		Wire zeroOrOne1 = g[0].mul(g[0].sub(1));
		Wire zeroOrOne2 = h[0].mul(h[0].sub(1));

		// checking the rest
		Wire allZero1 = generator.getOneWire();
		Wire allZero2 = generator.getOneWire();

		for (int i = 1; i < mu; i++) {
			allZero1 = allZero1.mul(g[i].checkNonZero().invAsBit());
			allZero2 = allZero2.mul(h[i].checkNonZero().invAsBit());
		}

		// assertion
		generator.addZeroAssertion(zeroOrOne1.mul(allZero1));
		generator.addZeroAssertion(zeroOrOne2.mul(allZero2));

		// verify order of points

		int bitLength = subGroupOrder.bitLength();
		Wire[] bits = new Wire[bitLength];
		for (int i = 0; i < bitLength; i++) {
			if (subGroupOrder.testBit(i))
				bits[i] = generator.getOneWire();
			else
				bits[i] = generator.getZeroWire();
		}

		Wire[] result1 = exp(g, bits, gPowersTable);
		Wire[] result2 = exp(h, bits, hPowersTable);

		// both should be one

		generator.addOneAssertion(result1[0]);
		generator.addOneAssertion(result2[0]);
		for (int i = 1; i < mu; i++) {
			generator.addZeroAssertion(result1[i]);
			generator.addZeroAssertion(result1[i]);
		}
	}

	@Override
	public Wire[] getOutputWires() {
		return Util.concat(outputPublicValue, sharedSecret);
	}

	public Wire[] getOutputPublicValue() {
		return outputPublicValue;
	}

	public Wire[] getSharedSecret() {
		return sharedSecret;
	}

}
