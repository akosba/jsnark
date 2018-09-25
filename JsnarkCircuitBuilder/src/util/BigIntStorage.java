package util;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * shares big integer constants
 *
 */
public class BigIntStorage {
	
	private ConcurrentMap<BigInteger, BigInteger> bigIntegerSet;
	private static BigIntStorage instance;
	
	private BigIntStorage(){
		bigIntegerSet = new ConcurrentHashMap<BigInteger, BigInteger>();
	}
	
	public static BigIntStorage getInstance(){
		if(instance == null){
			instance = new BigIntStorage();
		}
		return instance;
	}
	
	public BigInteger getBigInteger(BigInteger x){
		bigIntegerSet.putIfAbsent(x, x);
	    return bigIntegerSet.get(x);
	}
}
