import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.rmi.Remote;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
/*!
	@files	Client.java, DFS.Java
	@author	Bryson Sherman, Hung Mach, Jimmy Lanh
	@date	4/3/2018
	@version 1.0
	Creators: Bryson Sherman
			  Hung Mach
			  Jimmy Lanh

	Due Date: 4/3/2018

 */
 
/**
 * Map reduce file with reduce function, and md5 encryption
 */
public class Mapper implements MapReduceInterface, Serializable {
	public void map(Long key, String value, ChordMessageInterface context) throws IOException{
		String[] words = value.split(" ");
		for (int i = 0 ; i < words.length; i++){
			long guidGet = md5(words[i]);
			context.emitMap(md5(words[i]), words[i] + ":"+1);
//			System.out.println("mapper: " + words[i] + ": " + guidGet);
		}
	}
	/**
	 * the method to reduce values by a key
	 * @param key 	the key of the map reduce
	 * @param values 	the list of values being reduced
	 * @param context 	the context of the message
	 */
	public void reduce(Long key, List<String> values, ChordMessageInterface context) throws IOException{
		context.emitReduce(key, values.get(0) +":"+values.size());		
//		System.out.println(values.get(0) + ":" + values.size());
	}

	
	/**
	 * returns the md5 encryption of the object
	 * @param objectName the object to be encrypted
	 * @return 	the md5 encryption long of the object
	 */
	private long md5(String objectName)
	{
		try
		{
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(objectName.getBytes());
			BigInteger bigInt = new BigInteger(1,m.digest());
			return Math.abs(bigInt.longValue());
		}
		catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();

		}
		return 0;
	}
}
