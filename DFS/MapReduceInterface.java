import java.io.IOException;

public interface MapReduceInterface {
	public void map(Long key, String value, Context context) throws IOException;
	public void reduce(Long key, String[] value, Context context) throws IOException;
}
