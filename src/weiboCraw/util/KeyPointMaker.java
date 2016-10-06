package weiboCraw.util;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class KeyPointMaker {

    public static void main(String[] args) throws IOException {
	// TODO Auto-generated method stub
	String boxListPath = "E:\\MyResearch\\weiboCraw\\box_1000list.csv";
	String pgurl = "jdbc:postgresql://127.0.0.1:5432/shenzhen_mainroad";
	String user = "postgres";
	String pwd = "123456";
	
	Reader boxListin = new FileReader(boxListPath);
	CSVFormat boxListcsv = CSVFormat.DEFAULT.withHeader("id","lon_leftdown","lat_leftdown","lon_rightup","lat_rightup");
	Iterable<CSVRecord> boxListrecords = boxListcsv.parse(boxListin);
	
	Map<Integer,double[]> list = new HashMap<Integer,double[]>();
	for(CSVRecord r: boxListrecords){
	    double[] loc = {Double.parseDouble(r.get("lon_rightup")),Double.parseDouble(r.get("lat_rightup"))};
	    list.put(Integer.parseInt(r.get("id")), loc);
	}
	
	try{
	    Class.forName("org.postgresql.Driver").newInstance();
	    Connection pgconnect = DriverManager.getConnection(pgurl, user, pwd);
	    String insert = "INSERT INTO kp_1000(id,geom) VALUES(?,ST_SETSRID(ST_MAKEPOINT(?,?),4326))";
	    PreparedStatement insertST = pgconnect.prepareStatement(insert);
	    
	    int id = 1059;
	    for(int row=2; row<47; row=row+2){
		for(int column=2; column<93; column=column+2){
		    double[] rowloc = list.get(row);
		    double[] columnloc = list.get(column);
		    insertST.setInt(1, id);
		    insertST.setDouble(2, columnloc[0]);
		    insertST.setDouble(3, rowloc[1]);
		    insertST.executeUpdate();
		    id++;
		}
	    }
	    insertST.close();
	    pgconnect.close();
	}
	catch(Exception e){
		System.out.println(e.getMessage());
		e.getStackTrace();
	}
    }
}
