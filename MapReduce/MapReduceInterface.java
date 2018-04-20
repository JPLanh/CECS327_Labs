import java.io.IOException;

public interface MapReduceInterface {

	public void map(long key, String value, Context context) throws IOException;
	public void reduce(long key, String[] value, Context context) throws IOException;

}
