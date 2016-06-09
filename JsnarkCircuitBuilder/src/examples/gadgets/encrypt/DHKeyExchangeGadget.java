/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.encrypt;

import java.util.Arrays;

import util.Util;
import circuit.operations.Gadget;
import circuit.structure.Wire;

/**
 * Performs Key Exchange using a field extension F_p[x]/(x^\mu - \omega), where the polynomial (x^\mu - \omega) is irreducible.
 * The inputs to this gadget: the base g, the other party's input h = g^a, the bits of the secret exponent secExpBits and omega.
 * The outputs of this gadget: the derived key h^s to be used for symmetric key derivation, and g^s which is sent to the other party.
 *
 * A sample parameter choice can be found in the test
 * A sample usage is in: examples/generators/EncryptionCircuitGenerator.java
 */
public class DHKeyExchangeGadget extends Gadget {

	private Wire[] g; // base
	private Wire[] h; // other party's input
	
	private Wire[] secExpBits; // the bits of the secret exponent
	private long omega;
	private int mu;
	
	private Wire[] g_to_s; // exchange material  g^s (to be sent to the other party)
	private Wire[] h_to_s; // the derived key h^s
	
	public DHKeyExchangeGadget(Wire[] g, Wire[] h, Wire[] expBits, long omega, 
			String desc) {
		super(desc);
		this.g = g;
		this.h = h;
		this.secExpBits = expBits;
		this.omega = omega;
		mu = g.length;
		if(h.length != g.length){
			throw new IllegalArgumentException("g and h must have the same dimension");
		}
		buildCircuit();
	}
	
	protected void buildCircuit() {
		g_to_s = exp(g, secExpBits);
		h_to_s = exp(h, secExpBits);
	}
	
	
	private  Wire[] mul(Wire[] a, Wire[] b){
		Wire[] c = new Wire[mu];
		int i,j;
		for( i = 0; i < mu; i+=1){
			c[i] = generator.getZeroWire();
		}	
		for( i = 0; i < mu; i+=1){
			for( j = 0; j < mu; j+=1){
				int k = i + j;
				if(k < mu){
					c[k] = c[k].add(a[i].mul(b[j]));		
				}
				k = i+j-mu;
				if(k >= 0){
					c[k] = c[k].add(a[i].mul(b[j]).mul(omega));		
				}
			}	
		}
		return c;
	}

	private Wire[] exp(Wire[] base, Wire[] expBits){

		Wire[][] powersTable =new Wire[expBits.length][mu];
		powersTable[0] = base;
		for(int j = 1; j < expBits.length; j+=1){
			powersTable[j] = mul(powersTable[j-1],powersTable[j-1]);
		}
	
		Wire[] c = new Wire[mu];
		Arrays.fill(c, generator.getZeroWire());
		c[0] = generator.getOneWire();
		for( int j = 0; j <  expBits.length; j+=1){
			Wire[] tmp = mul(c, powersTable[j]);
			for(int i = 0; i < mu; i++){
				c[i] = c[i].add(expBits[j].mul(tmp[i].sub(c[i])));
			}
		}
		return c;
	}

	@Override
	public Wire[] getOutputWires() {
		return Util.concat(g_to_s, h_to_s);
	}

	public Wire[] getG_to_s() {
		return g_to_s;
	}

	public Wire[] getH_to_s() {
		return h_to_s;
	}

}
