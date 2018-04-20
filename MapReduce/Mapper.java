import java.io.IOException;
import java.math.BigInteger;
import java.rmi.Remote;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Mapper implements MapReduceInterface {
	public void map(long key, String value, Context context) throws IOException{
		String[] words = value.split(" ");
		for (int i = 0 ; i < words.length; i++){
			long guidGet = md5(words[i]);
			context.chord.emitMap(md5(words[i]), words[i] + ":"+1);
		}
	}

	

	public void reduce(long key, String[] values, Context context) throws IOException{

		String word = values[0].split(":")[0];
		context.chord.emitReduce(key, word +":"+ values.length);
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
