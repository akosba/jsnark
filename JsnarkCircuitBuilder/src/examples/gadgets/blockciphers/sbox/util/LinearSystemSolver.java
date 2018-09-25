package examples.gadgets.blockciphers.sbox.util;

import java.math.BigInteger;

import circuit.config.Config;

/**
 * Solves a linear system of equations over a finite field.
 * 
 * Used for efficient representation of AES S-box gadget
 */

public class LinearSystemSolver {

	public static BigInteger prime = Config.FIELD_PRIME;

	private BigInteger[][] mat;
	private int numRows, numCols;

	public LinearSystemSolver(BigInteger[][] mat) {
		this.mat = mat;
		numRows = mat.length;
		numCols = mat[0].length;
	}

	public void solveInPlace() {

		// https://www.csun.edu/~panferov/math262/262_rref.pdf
		// https://www.math.purdue.edu/~shao92/documents/Algorithm%20REF.pdf
		guassJordan();
		rref();
	}

	private void guassJordan() {
		for (int colIdx = 0, rowIdx = 0; colIdx < numCols; colIdx++, rowIdx++) {
			int pivotRowIdx = rowIdx;
			while (pivotRowIdx < numRows
					&& mat[pivotRowIdx][colIdx].equals(BigInteger.ZERO)) {
				pivotRowIdx++;
			}
			if (pivotRowIdx == numRows)
				continue;

			// swap
			BigInteger[] tmp = mat[pivotRowIdx];
			mat[pivotRowIdx] = mat[rowIdx];
			mat[rowIdx] = tmp;

			pivotRowIdx = rowIdx;

			// dividing by pivot
			BigInteger invF = inverse(mat[pivotRowIdx][colIdx]);
			for (int j = 0; j < numCols; j++) {
				mat[pivotRowIdx][j] = mat[pivotRowIdx][j].multiply(invF).mod(
						prime);
			}

			for (int k = pivotRowIdx + 1; k < numRows; k++) {
				BigInteger f = negate(mat[k][colIdx]);
				for (int j = 0; j < numCols; j++) {
					mat[k][j] = mat[k][j].add(mat[pivotRowIdx][j].multiply(f));
					mat[k][j] = mat[k][j].mod(prime);
				}
			}

		}
	}

	private void rref() {
		for (int rowIdx = numRows - 1; rowIdx >= 0; rowIdx--) {
			int pivotColIdx = 0;
			while (pivotColIdx < numCols
					&& mat[rowIdx][pivotColIdx].equals(BigInteger.ZERO)) {
				pivotColIdx++;
			}
			if (pivotColIdx == numCols)
				continue;

			for (int k = rowIdx - 1; k >= 0; k--) {
				BigInteger f = mat[k][pivotColIdx];
				for (int j = 0; j < numCols; j++) {
					mat[k][j] = mat[k][j]
							.add(negate(mat[rowIdx][j].multiply(f)));
					mat[k][j] = mat[k][j].mod(prime);
				}
			}
		}
	}

	private static BigInteger negate(BigInteger x) {
		return (prime.subtract(x.mod(prime))).mod(prime);
	}

	private static BigInteger inverse(BigInteger x) {
		return (x.mod(prime)).modInverse(prime);
	}

}
