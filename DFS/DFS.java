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
    
    
    
    public DFS(int port) throws Exception
    {
        
        this.port = port;
        long guid = md5("" + port);
        chord = new Chord(port, guid);
        Files.createDirectories(Paths.get(guid+"/repository"));
        System.out.println(md5("Metadata"));
        File f = new File(guid+"/repository/"+md5("Metadata"));
        if (!f.exists()){
            System.out.println("Creating meta");
            PrintWriter pr = new PrintWriter(f);
            pr.print("{\"metadata\":[]}");
            pr.close();
            f.createNewFile();
        } else {
            System.out.println("metadata already exist");
        }
    }
    
    public  void join(String Ip, int port) throws Exception
    {
        chord.joinRing(Ip, port);
        chord.Print();
    }
    
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
   
    public void mv(String oldName, JsonValue newName) throws Exception
    {
        JsonObject parser = (JsonObject) readMetaData();
        JsonArray fileList = parser.getJsonArray("metadata");
        
        for (int i = 0; i < fileList.size(); i++){
            JsonObject getJson = fileList.get(i).asJsonObject();
            if (getJson.get("name").toString().equals(oldName)){
                getJson.put("name",  newName);
                JsonArray pageList = getJson.get("pages").asJsonArray();
                for (int j = 0; j < pageList.size(); j++){
                    JsonObject pageGet = pageList.get(j).asJsonObject();
                    long newValue = md5(newName.toString()+(j+1));
                    pageGet.put("guid",  Json.createValue(newValue));
                    byte[] pageByte = read(oldName, j+1);

                    File tempFile = File.createTempFile("temp", null);
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    fos.write(pageByte);
                    fos.close();
                    
                    InputStream input = new FileStream("temp.tmp");
                    writeMetaData(input);
                }
            }
        }
    }

    
    public String ls() throws Exception
    {
        String listOfFiles = "";
       JsonReader reader = readMetaData();
       System.out.println("Starting ls");
       
        return listOfFiles;
    }

    
    public void touch(String fileName) throws Exception
    {
         // TODO: Create the file fileName by adding a new entry to the Metadata
        // Write Metadata

        
        
    }
    public void delete(String fileName) throws Exception
    {
        // TODO: remove all the pages in the entry fileName in the Metadata and then the entry
        // for each page in Metadata.filename
        //     peer = chord.locateSuccessor(page.guid);
        //     peer.delete(page.guid)
        // delete Metadata.filename
        // Write Metadata

        
    }
    
    public byte[] read(String fileName, int pageNumber) throws Exception
    {
        // TODO: read pageNumber from fileName
        return null;
    }
    
    
    public byte[] tail(String fileName) throws Exception
    {
        // TODO: return the last page of the fileName
        return null;
    }
    public byte[] head(String fileName) throws Exception
    {
        // TODO: return the first page of the fileName
        return null;
    }
    public void append(String filename, Byte[] data) throws Exception
    {
        // TODO: append data to fileName. If it is needed, add a new page.
        // Let guid be the last page in Metadata.filename
        //ChordMessageInterface peer = chord.locateSuccessor(guid);
        //peer.put(guid, data);
        // Write Metadata

        
    }
    
}
