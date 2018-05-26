/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import circuit.structure.Wire;

public class Util {

	// seeded by 1 for testing purposes
	static Random rand = new Random(1);

	public static BigInteger[] split(BigInteger x, int numchunks, int chunksize) {
		BigInteger[] chunks = new BigInteger[numchunks];
		BigInteger mask = new BigInteger("2").pow(chunksize).subtract(BigInteger.ONE);
		for (int i = 0; i < numchunks; i++) {
			chunks[i] = x.shiftRight(chunksize * i).and(mask);
		}
		return chunks;
	}

	public static BigInteger combine(BigInteger[] table, Wire[] blocks, int bitwidth) {
		BigInteger sum = BigInteger.ZERO;
		for (int i = 0; i < blocks.length; i++) {
			if (table[blocks[i].getWireId()] == null) {
				continue;
			}
			sum = sum.add(table[blocks[i].getWireId()].multiply(new BigInteger("2").pow(bitwidth * i)));
		}
		return sum;
	}

	public static BigInteger group(BigInteger[] list, int width) {
		BigInteger x = BigInteger.ZERO;
		for (int i = 0; i < list.length; i++) {
			x = x.add(list[i].shiftLeft(width * i));
		}
		return x;
	}

	public static int[] concat(int[] a1, int[] a2) {
		int[] all = new int[a1.length + a2.length];
		for (int i = 0; i < all.length; i++) {
			all[i] = i < a1.length ? a1[i] : a2[i - a1.length];
		}
		return all;
	}

	public static Wire[] concat(Wire[] a1, Wire[] a2) {
		Wire[] all = new Wire[a1.length + a2.length];
		for (int i = 0; i < all.length; i++) {
			all[i] = i < a1.length ? a1[i] : a2[i - a1.length];
		}
		return all;
	}

	public static Wire[] concat(Wire w, Wire[] a) {
		Wire[] all = new Wire[1 + a.length];
		for (int i = 0; i < all.length; i++) {
			all[i] = i < 1 ? w : a[i - 1];
		}
		return all;
	}

	public static int[] concat(int[][] arrays) {
		int sum = 0;
		for (int i = 0; i < arrays.length; i++) {
			sum += arrays[i].length;
		}
		int[] all = new int[sum];
		int idx = 0;
		for (int i = 0; i < arrays.length; i++) {
			for (int j = 0; j < arrays[i].length; j++) {
				all[idx++] = arrays[i][j];
			}
		}
		return all;
	}

	public static BigInteger[] randomBigIntegerArray(int num, BigInteger n) {

		BigInteger[] result = new BigInteger[num];
		for (int i = 0; i < num; i++) {
			result[i] = nextRandomBigInteger(n);
		}
		return result;
	}

	public static BigInteger nextRandomBigInteger(BigInteger n) {

		BigInteger result = new BigInteger(n.bitLength(), rand);
		while (result.compareTo(n) >= 0) {
			result = new BigInteger(n.bitLength(), rand);
		}
		return result;
	}

	public static BigInteger[] randomBigIntegerArray(int num, int numBits) {

		BigInteger[] result = new BigInteger[num];
		for (int i = 0; i < num; i++) {
			result[i] = nextRandomBigInteger(numBits);
		}
		return result;
	}

	public static BigInteger nextRandomBigInteger(int numBits) {

		BigInteger result = new BigInteger(numBits, rand);
		return result;
	}

	public static String getDesc(String... desc) {
		if (desc.length == 0) {
			return "";
		} else {
			return desc[0];
		}

	}

	public static ArrayList<Integer> parseSequenceLists(String s) {

		ArrayList<Integer> list = new ArrayList<Integer>();
		String[] chunks = s.split(",");
		for (String chunk : chunks) {
			if (chunk.equals(""))
				continue;
			int lower = Integer.parseInt(chunk.split(":")[0]);
			int upper = Integer.parseInt(chunk.split(":")[1]);
			for (int i = lower; i <= upper; i++) {
				list.add(i);
			}
		}
		return list;
	}

	public static Wire[] reverseBytes(Wire[] inBitWires) {
		Wire[] outs = Arrays.copyOf(inBitWires, inBitWires.length);
		int numBytes = inBitWires.length / 8;
		for (int i = 0; i < numBytes / 2; i++) {
			int other = numBytes - i - 1;
			for (int j = 0; j < 8; j++) {
				Wire temp = outs[i * 8 + j];
				outs[i * 8 + j] = outs[other * 8 + j];
				outs[other * 8 + j] = temp;
			}
		}
		return outs;
	}

	public static String arrayToString(int[] a, String separator) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < a.length - 1; i++) {
			s.append(a[i] + separator);
		}
		s.append(a[a.length - 1]);
		return s.toString();
	}

	public static String arrayToString(Wire[] a, String separator) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < a.length - 1; i++) {
			s.append(a[i] + separator);
		}
		s.append(a[a.length - 1]);
		return s.toString();
	}

	public static boolean isBinary(BigInteger v) {
		return v.equals(BigInteger.ZERO) || v.equals(BigInteger.ONE);
	}

	public static String padZeros(String s, int l) {
		return String.format("%" + l + "s",s).replace(' ', '0');
	}

	public static BigInteger computeMaxValue(int numBits){
		return BigIntStorage.getInstance().getBigInteger(
				new BigInteger("2").pow(numBits).subtract(
						BigInteger.ONE));
	}
	
	public static BigInteger computeBound(int numBits){
		return BigIntStorage.getInstance().getBigInteger(
				new BigInteger("2").pow(numBits));
	}
	
	public static BigInteger[] split(BigInteger x, int chunksize) {
		int numChunks = (int)Math.ceil(x.bitLength()*1.0/chunksize);
		BigInteger[] chunks = new BigInteger[numChunks];
		BigInteger mask = new BigInteger("2").pow(chunksize).subtract(BigInteger.ONE);
		for (int i = 0; i < numChunks; i++) {
			chunks[i] = x.shiftRight(chunksize * i).and(mask);
		}
		return chunks;
	}
	
	public static Wire[] padWireArray(Wire[] a, int length, Wire p) {
		if (a.length == length) {
			return a;
		} else if (a.length > length) {
			System.err.println("No padding needed!");
			return a;
		} else {
			Wire[] newArray = new Wire[length];
			System.arraycopy(a, 0, newArray, 0, a.length);
			for (int k = a.length; k < length; k++) {
				newArray[k] = p;
			}
			return newArray;
		}
	}
}
