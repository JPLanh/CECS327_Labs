import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.rmi.Remote;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Mapper implements MapReduceInterface, Serializable {
	public void map(Long key, String value, ChordMessageInterface context) throws IOException{
		String[] words = value.split(" ");
		for (int i = 0 ; i < words.length; i++){
			long guidGet = md5(words[i]);
			System.out.println("mapper: " + words[i] + ": " + guidGet);
			context.emitMap(md5(words[i]), words[i] + ":"+1);
		}
	}
	
	public void reduce(Long key, List<String> values, ChordMessageInterface context) throws IOException{
		context.emitReduce(key, values.get(0) +":"+values.size());		
		System.out.println(values.get(0) + ":" + values.size());
		/**
		 * word = values[0].split(":")[0]
		 * emit(key, word +":"+ len(values));
		 */
	}

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
