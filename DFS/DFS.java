import java.rmi.*;
import java.net.*;
import java.util.*;

import javax.json.*;

import java.io.*;
import java.nio.file.*;
import java.math.BigInteger;
import java.security.*;
// import a json package


/* JSON Format

 {
    "metadata" :
    {
        file :
        {
            name  : "File1"
            numberOfPages : "3"
            pageSize : "1024"
            size : "2291"
            page :
            {
                number : "1"
                guid   : "22412"
                size   : "1024"
            }
            page :
            {
                number : "2"
                guid   : "46312"
                size   : "1024"
            }
            page :
            {
                number : "3"
                guid   : "93719"
                size   : "243"
            }
        }
    }
}


 */


public class DFS
{
	int port;
	Chord  chord;

	/** Function that will help generate the GUID
	 * @objectName not sure yet
	 * @return a GUID
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

	/** Constructor that initialize the port and create the metadata
	 * @Port: the port that the user wish to connect to
	 */
	public DFS(int port) throws Exception
	{

		this.port = port;
		long guid = md5("" + port);
		chord = new Chord(port, guid);
		Files.createDirectories(Paths.get(guid+"/repository"));
	}


	/** Connect to the desired destination through a certain port
	 * @Ip The destination
	 * @port The port which we will connect to
	 */
	public  void join(String Ip, int port) throws Exception
	{
		chord.joinRing(Ip, port);
		chord.Print();
	}


	/** Function that will help generate the GUID
	 * @return JsonReader with the Json information ready to be read
	 */
	public JsonReader readMetaData() throws Exception
	{
		long guid = md5("Metadata");

		ChordMessageInterface peer = chord.locateSuccessor(guid);
		InputStream metadataraw = peer.get(guid);
		return Json.createReader(metadataraw);
	}

	public void writeMetaData(InputStream stream) throws Exception
	{
		long guid = md5("Metadata");
		ChordMessageInterface peer = chord.locateSuccessor(guid);
		peer.put(guid, stream);
	}

	/** ??
	 */   
	public void mv(String oldName, String newName) throws Exception
	{
		// TODO:  Change the name in Metadata
		// Write Metadata
		// Mental Note: Just renames the file.. apparently
		JsonArray metaReader = getMetaData();
		for (int i = 0; i < metaReader.size();i++){
			JsonObject getJson = metaReader.getJsonObject(i).getJsonObject("file");
			if (getJson.getJsonString("name").toString().replaceAll("\"", "").equals(oldName)){
				JsonArray getJsonArray = getJson.getJsonArray("page");
				JsonObjectBuilder newFileMeta = Json.createObjectBuilder()
						.add("file", Json.createObjectBuilder()
								.add("name", newName)
								.add("numberOfPages", getJson.getJsonNumber("numberOfPages").intValue())
								.add("pageSize", getJson.getJsonNumber("pageSize").intValue())
								.add("size", getJson.getJsonNumber("size").intValue())
								.add("page", getJsonArray));						
				delete(oldName);
				JsonArrayBuilder newMetaJsonArray = Json.createArrayBuilder(getMetaData())
						.add(newFileMeta.build());
				JsonArray newMeta = newMetaJsonArray.build();

				JsonObject newMetaData = Json.createObjectBuilder()
						.add("metadata", newMeta).build();

				writeMetaData(newMetaData.toString());
				break;
			}
		}	

	}


	/** Gather all files
	 * @return a string of all files
	 */    
	public String ls() throws Exception
	{
		String listOfFiles = "";
		JsonArray metaReader = getMetaData();

		for (int i = 0; i < metaReader.size(); i++){
			JsonObject getJson = metaReader.getJsonObject(i);
			JsonObject getJsonFile = getJson.getJsonObject("file");
			listOfFiles += getJsonFile.getJsonString("name") + "\n";
		}

		return listOfFiles;
	}

	/** Create the filename by adding a new entry to the metadata
	 *
	 * @param fileName the name of the new file
	 * @throws Exception
	 */
	public void touch(String fileName) throws Exception
	{
		JsonArray metaReader = getMetaData();

		JsonObjectBuilder newFile = Json.createObjectBuilder()
				.add("file", Json.createObjectBuilder()
						.add("name", fileName)
						.add("numberOfPages", 0)
						.add("pageSize", 1024)
						.add("size", 0)
						.add("page", Json.createArrayBuilder()));

		JsonArrayBuilder newMetaJsonArray = Json.createArrayBuilder(metaReader)
				.add(newFile.build());
		JsonArray newMeta = newMetaJsonArray.build();

		JsonObject newMetaData = Json.createObjectBuilder()
				.add("metadata", newMeta).build();

		writeMetaData(newMetaData.toString());
	}

	/**
	 * Delete the pages from the peer who holds the metadata, and then delete the file from it
	 * @param fileName name of the file to be deleted
	 * @throws Exception
	 */
	public void delete(String fileName) throws Exception
	{
		JsonArray metaReader = getMetaData();
		JsonArrayBuilder newMeta = Json.createArrayBuilder();
		JsonArrayBuilder newFileMeta = Json.createArrayBuilder();
		for (int i = 0; i < metaReader.size();i++){
			JsonObject getJson = metaReader.getJsonObject(i).getJsonObject("file");
			if (getJson.getJsonString("name").toString().replaceAll("\"", "").equals(fileName)){
				JsonArray getJsonPages = getJson.getJsonArray("page");
				for (int j = 0; j < getJsonPages.size(); j++){
					JsonObject tempPage = getJsonPages.getJsonObject(j);
					long guidPage = tempPage.getJsonNumber("guid").longValue();

					ChordMessageInterface peer = chord.locateSuccessor(guidPage);
					peer.delete(guidPage);

				}
			} else {							
				newFileMeta.add(Json.createObjectBuilder(metaReader.getJsonObject(i)).build());
			}
		}    	
		JsonObjectBuilder getNewMeta = Json.createObjectBuilder()
				.add("metadata", newFileMeta.build());
		writeMetaData(getNewMeta.build().toString());        
	}

	public byte[] read(String fileName, int pageNumber) throws Exception
	{   
		JsonArray metaReader =  getMetaData();
		for ( int i = 0 ; i < metaReader.size(); i++){
			JsonObject getJson = metaReader.getJsonObject(i).getJsonObject("file");
			if (getJson.getJsonString("name").toString().replaceAll("\"", "").equals(fileName)){
				JsonArray getJsonPage = getJson.getJsonArray("page");
				if (pageNumber != -1){
					for (int j = 0; j < getJsonPage.size(); j++){                    
						if (getJsonPage.get(j).asJsonObject().getJsonNumber("number").intValue() == (pageNumber)){
							int sizeGet = getJsonPage.get(j).asJsonObject().getJsonNumber("size").intValue();
							long  guidGet = getJsonPage.get(j).asJsonObject().getJsonNumber("guid").longValue();
							System.out.println(guidGet + "  |  " + pageNumber);
							ChordMessageInterface peer = chord.locateSuccessor(guidGet);
							InputStream is = peer.get(guidGet);

							byte[] array = new byte[sizeGet];
							ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
							int nRead = is.read();
							while (nRead != 0){
								byteBuffer.write(nRead);
								nRead = is.read();
							}
							byteBuffer.flush();
							is.close();
							return byteBuffer.toByteArray();
						}
					}
				} else {
					int j = getJsonPage.size()-1;
					int sizeGet = getJsonPage.get(j).asJsonObject().getJsonNumber("size").intValue();
					long  guidGet = getJsonPage.get(j).asJsonObject().getJsonNumber("guid").longValue();
					System.out.println(guidGet + "  |  " + pageNumber);
					ChordMessageInterface peer = chord.locateSuccessor(guidGet);
					InputStream is = peer.get(guidGet);

					byte[] array = new byte[sizeGet];
					ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
					int nRead = is.read();
					while (nRead != 0){
						//						System.out.println(nRead);
						byteBuffer.write(nRead);
						nRead = is.read();
					}
					byteBuffer.flush();
					is.close();
					return byteBuffer.toByteArray();					
				}
			}
		}
		return null;
	}


	public byte[] tail(String fileName) throws Exception
	{
		return read(fileName, -1);
	}
	public byte[] head(String fileName) throws Exception
	{
		return read(fileName, 1);
	}
	public void append(String fileName, byte[] data) throws Exception
	{
		// TODO: append data to fileName. If it is needed, add a new page.
		// Let guid be the last page in Metadata.filename
		//ChordMessageInterface peer = chord.locateSuccessor(guid);
		//peer.put(guid, data);
		// Write Metadata
		JsonObject foundObject = null;

		JsonArray metaReader = getMetaData();
		for ( int i = 0 ; i < metaReader.size(); i++){
			JsonObject getJson = metaReader.getJsonObject(i).getJsonObject("file");
			if (getJson.getJsonString("name").toString().replaceAll("\"", "").equals(fileName)){
				foundObject = getJson;
				break;
			}
		}

		if (foundObject == null){
			touch(fileName);
			append(fileName, data);
		} else{
			int count = Integer.parseInt(foundObject.getJsonNumber("numberOfPages").toString().replaceAll("\"", ""));
			int maxSize = Integer.parseInt(foundObject.getJsonNumber("pageSize").toString().replaceAll("\"", ""));
			int size = Integer.parseInt(foundObject.getJsonNumber("size").toString().replaceAll("\"", ""));
			JsonArrayBuilder pageList = Json.createArrayBuilder(foundObject.getJsonArray("page"));

			while(((count+1)*maxSize) < data.length){
				count++;
				int remainingLength = 0;
				byte[] dataChunk = new byte[maxSize];
				if ((data.length-(count*maxSize)) > maxSize) remainingLength = maxSize;
				else remainingLength = data.length-(count*maxSize);
				System.arraycopy(data, (count-1)*maxSize, dataChunk, 0, remainingLength);
				InputStream is = new ByteArrayInputStream(dataChunk);

				long guid = md5(fileName + "" + count);
				ChordMessageInterface peer = chord.locateSuccessor(guid);
				peer.put(guid, is);
				JsonObjectBuilder newPage = Json.createObjectBuilder()
						.add("number", count)
						.add("guid", guid)
						.add("size", remainingLength);
				pageList.add(newPage);
				size += remainingLength;
			}

			delete(fileName);
			metaReader = getMetaData();
			JsonObjectBuilder newFile = Json.createObjectBuilder()
					.add("file", Json.createObjectBuilder()
							.add("name", fileName)
							.add("numberOfPages", count)
							.add("pageSize", maxSize)
							.add("size", size)
							.add("page", pageList.build()));

			JsonArrayBuilder newMetaJsonArray = Json.createArrayBuilder(metaReader)
					.add(newFile.build());
			JsonArray newMeta = newMetaJsonArray.build();

			JsonObject newMetaData = Json.createObjectBuilder()
					.add("metadata", newMeta).build();

			writeMetaData(newMetaData.toString());
		}

	}

	public JsonArray getMetaData() throws Exception{
		JsonReader reader = readMetaData();
		JsonObject readerGet = reader.readObject();
		reader.close();
		JsonArray metaReader = readerGet.getJsonArray("metadata");
		return metaReader;    
	}

	public void writeMetaData(String getString) throws Exception{
		InputStream is = new ByteArrayInputStream(getString.toString().getBytes());
		writeMetaData(is);
	}
}
