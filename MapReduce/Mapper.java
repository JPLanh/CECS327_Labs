import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.rmi.Remote;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Mapper implements MapReduceInterface, Serializable {
    
    /**
     * Distribute the words throughout the file system, putting the word according to it's guid in between two processes.
     * @param key the guid of the page
     * @param value the string value to be mapped
     * @param the chord that is currently doing the distribution of the word
     */
    public void map(Long key, String value, ChordMessageInterface context) throws IOException{
		String[] words = value.split(" ");
		for (int i = 0 ; i < words.length; i++){
			context.emitMap(md5(words[i]), words[i] + ":"+1);
		}
		context.completePeer(key, 1);
	}
	
    /**
     * Take the list of string of map and reduce it down to a reduced tree map instead of having a list, will now just have the count.
     * @param key the guid of the page
     * @param value the string value to be mapped
     * @param the chord that is currently doing the distribution of the word
     */
	public void reduce(Long key, List<String> values, ChordMessageInterface context) throws IOException{
		context.emitReduce(key, values.get(0).substring(0,values.get(0).indexOf(':')) +":"+values.size());		
	}

	/**
	 * Convert an string into a guid
	 * @param objectName The string to be converted into it's guid
	 * @return the guid of the string
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
