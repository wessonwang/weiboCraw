package weiboCraw.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

public class PoiidInserter {

    public static void main(String[] args) throws IOException {
	// TODO Auto-generated method stub
	String poiPath = "E:\\MyResearch\\weiboCraw\\guangdong.csv";
	File file = new File(poiPath);
	Reader poiIn = new InputStreamReader(new BOMInputStream(new FileInputStream(file)), "UTF-8");
	CSVFormat boxListcsv = CSVFormat.DEFAULT.withDelimiter(',');
	Iterable<CSVRecord> poiRecords = boxListcsv.parse(poiIn);
	
	String pgurl = "jdbc:postgresql://127.0.0.1:5432/shenzhen_mainroad";
	String user = "postgres";
	String pwd = "123456";
	
	try{
	    Class.forName("org.postgresql.Driver").newInstance();
	    Connection pgconnect = DriverManager.getConnection(pgurl, user, pwd);
	    PreparedStatement st = pgconnect.prepareStatement("INSERT INTO sz_pois_thinkgis(poiid,geom) VALUES(?,ST_SETSRID(ST_MAKEPOINT(?,?),4326));");
	    
	    int count = 0;
	    for(CSVRecord r: poiRecords){
		try{
		    if(r.size()==9 && r.get(5).equals("0755")){
			String poiid = r.get(0);
			String lon = r.get(3);
			String lat = r.get(4);
			st.setString(1, poiid);
			st.setDouble(2, Double.parseDouble(lon));
			st.setDouble(3, Double.parseDouble(lat));
			st.executeUpdate();
			count++;
			
			if(count%1000==0) System.out.println(count);
		    }
		}
		catch(Exception e){
		    continue;
		}
	    }
	}
	catch (InstantiationException | IllegalAccessException
		    | ClassNotFoundException e) {
	    e.printStackTrace();
	}
	catch (SQLException e) {
	    e.printStackTrace();
	}
	
    }

}
