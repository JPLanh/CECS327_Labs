import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.rmi.Remote;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Mapper implements MapReduceInterface, Serializable {
	//Maps the word
	public void map(Long key, String value, ChordMessageInterface context) throws IOException{
		String[] words = value.split(" ");
		for (int i = 0 ; i < words.length; i++){
			long guidGet = md5(words[i]);
			System.out.println("mapper: " + words[i] + ": " + guidGet);
			context.emitMap(md5(words[i]), words[i] + ":"+1);
		}
	}
	
	public void reduce(Long key, List<String> values, ChordMessageInterface context) throws IOException{
		String[] wordPair = values.get(0).split(":");
		context.emitReduce(key, wordPair[0] +":"+values.size());		
		System.out.println(wordPair[0] + ":" + values.size());
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
