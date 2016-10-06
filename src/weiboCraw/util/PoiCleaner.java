/**
 * 
 */
package weiboCraw.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

/**
 * @author weibornhigh
 *
 */
public class PoiCleaner {

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
	// TODO Auto-generated method stub
	String poiPath = "E:\\MyResearch\\weiboCraw\\guangdong.csv";
	File file = new File(poiPath);
	Reader poiIn = new InputStreamReader(new BOMInputStream(new FileInputStream(file)), "UTF-8");
	CSVFormat boxListcsv = CSVFormat.DEFAULT.withDelimiter(',');
	Iterable<CSVRecord> poiRecords = boxListcsv.parse(poiIn);
	
	String normalPoiPath = "E:\\MyResearch\\weiboCraw\\guangdong_normal.csv";
	String errorPoiPath = "E:\\MyResearch\\weiboCraw\\guangdong_error.csv";
	String NEW_LINE_SEPARATOR = "\n";
	FileWriter normalWriter = new FileWriter(normalPoiPath,true);
	CSVFormat normalFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR).withDelimiter(',');
	CSVPrinter normalPrinter = new CSVPrinter(normalWriter,normalFormat);
	FileWriter errorWriter = new FileWriter(errorPoiPath,true);
	CSVFormat errorFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR).withDelimiter(',');
	CSVPrinter errorPrinter = new CSVPrinter(errorWriter,errorFormat);
	
	int szCount = 0;
	for(CSVRecord r: poiRecords){
	    try{
		if(r.get(5).equals("0755")) szCount++;
	    }
	    catch(Exception e){
		continue;
	    }
	    
	    ArrayList<String> array = PoiCleaner.csvToArray(r);
	    int size = r.size();
	    if(size==9){
		normalPrinter.printRecord(array);
	    }
	    else{
		errorPrinter.printRecord(array);
	    }
	}
	
	System.out.println("sz poi count is: "+szCount);
	
	normalWriter.flush();
	normalWriter.close();
	normalPrinter.close();
	errorWriter.flush();
	errorWriter.close();
	errorPrinter.close();
    }
    
    public static ArrayList<String> csvToArray(CSVRecord r){
	int size = r.size();
	ArrayList<String> array = new ArrayList<String>();
	for(int i=0; i<size; i++){
	    array.add(r.get(i));
	}
	
	return array;
    }

}
