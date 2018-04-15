import java.io.IOException;

public class Mapper implements MapReduceInterface {
	public void map(Long key, String value) throws IOException{
		/**
		 * for each word in value
		 * emit(md5(word), word + ":"+1);
		 */
	}
	
	public void reduce(Long key, String values[]) throws IOException{
		/**
		 * word = values[0].split(":")[0]
		 * emit(key, word +":"+ len(values));
		 */
	}

}
