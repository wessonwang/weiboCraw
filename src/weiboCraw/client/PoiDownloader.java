/**
 * 
 */
package weiboCraw.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import weibo4j.model.Places;
import weiboCraw.downloader.NearbyPoiLoader;
import weiboCraw.scheduler.SpiderPgScheduler;
import weiboCraw.util.Log;

/**
 * @author weibornhigh
 *
 */
public class PoiDownloader {

    /**
     * @param args
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws SQLException 
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, InterruptedException {
	// TODO Auto-generated method stub
	if(args.length != 4){
	    Log.log(3,"Usage: dbHost(IP) coffeeTime(int,∫¡√Î) sleepTime(int,∫¡√Î) ip_limit_hourly(int,¥Œ ˝)");
	    System.exit(0);
	}
	String accessToken = "2.006yqUoBYTFH2Cc4ba0fe85aiQkiWB";
	int coffeeTime = Integer.parseInt(args[1]); //∫¡√Î
	long sleepTime = Long.parseLong(args[2]);
	int ip_limit_hourly = Integer.parseInt(args[3]);
	
	Random random = new Random(System.currentTimeMillis());
	
	String pgurl = "jdbc:postgresql://"+args[0]+":5432/shenzhen_mainroad";
	String user = "postgres";
	String pwd = "123456";
	String kpDb = "plist_1000";
	String kpIdName = "id";
	String poiDb = "sz_pois";
	Connection pgconnect = null;
	SpiderPgScheduler spider = null;
	PreparedStatement poiInsert = null;
	PreparedStatement kpLocRes = null;
	
	try{
	    Class.forName("org.postgresql.Driver").newInstance();
	    pgconnect = DriverManager.getConnection(pgurl, user, pwd);
	    spider = new SpiderPgScheduler(pgconnect,kpDb,kpIdName);
	    poiInsert = pgconnect.prepareStatement("INSERT INTO "+poiDb+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
	    kpLocRes = pgconnect.prepareStatement("SELECT ST_X(geom) as lon,ST_Y(geom) as lat from "+kpDb+" WHERE "+kpIdName+ "=?;");
	    NearbyPoiLoader loader = new NearbyPoiLoader(accessToken,coffeeTime,sleepTime,ip_limit_hourly);
	    Log.setConsole(true);
	    Log.setLevel(1);
	    
	    String kp_id = spider.assignJob();
	    ResultSet loc = null;
	    String kp_lon = "0";
	    String kp_lat = "0";
	    int KPFinishedCount = 0;
	    while(null != kp_id){
		Thread.sleep(random.nextInt(coffeeTime));
		kpLocRes.setString(1, kp_id);
		loc = kpLocRes.executeQuery();
		if(loc.next()){
		    kp_lon = loc.getString("lon");
		    kp_lat = loc.getString("lat");
		}
		else{
		    kp_id = spider.assignJob();
		    continue;
		}
		
		if(loader.nearbyPois(kp_lon, kp_lat)){
		    List<Places> resultList = loader.poiList;
		    for(Places p: resultList){
			poiInsert.setString(1, p.getPoiid());
			poiInsert.setString(2, p.getTitle());
			poiInsert.setString(3, p.getAddress());
			poiInsert.setDouble(4, p.getLon());
			poiInsert.setDouble(5, p.getLat());
			poiInsert.setString(6, p.getCategory());
			poiInsert.setString(7, p.getCity());
			poiInsert.setString(8, p.getProvince());
			poiInsert.setString(9, p.getCountry());
			poiInsert.setString(10, p.getUrl());
			poiInsert.setString(11, p.getPhone());
			poiInsert.setString(12, p.getPostcode());
			poiInsert.setString(13, p.getCategorys());
			poiInsert.setString(14, p.getCategoryName());
			poiInsert.setInt(15, (int)p.getCheckinNum());
			poiInsert.setInt(16, (int)p.getCheckinUserNum());
			poiInsert.setInt(17, (int)p.getTipNum());
			poiInsert.setInt(18, (int)p.getPhotoNum());
			poiInsert.setInt(19, (int)p.getTodoNum());
			try{
			    poiInsert.executeUpdate();
			}
			catch(SQLException e){
			    poiInsert.clearParameters();
			    poiInsert.setString(1, p.getPoiid());
			    poiInsert.setString(2, null);
			    poiInsert.setString(3, null);
			    poiInsert.setDouble(4, 0);
			    poiInsert.setDouble(5, 0);
			    poiInsert.setString(6, null);
			    poiInsert.setString(7, null);
			    poiInsert.setString(8, null);
			    poiInsert.setString(9, null);
			    poiInsert.setString(10, null);
			    poiInsert.setString(11, null);
			    poiInsert.setString(12, null);
			    poiInsert.setString(13, null);
			    poiInsert.setString(14, null);
			    poiInsert.setInt(15, 0);
			    poiInsert.setInt(16, 0);
			    poiInsert.setInt(17, 0);
			    poiInsert.setInt(18, 0);
			    poiInsert.setInt(19, 0);
			    poiInsert.executeUpdate();
			}
			
		    }
		    
		    if(!loader.missedPage.isEmpty()){
			spider.uncomplete(kp_id, loader.missedPage);
		    }
		    else{
			spider.setJobStatus(kp_id, SpiderPgScheduler.COMPLETED);
		    }
		    
		    if(loader.isReached200()){
			spider.incomplete(kp_id);
		    }
		    
		    KPFinishedCount++;
		    Log.log(3, KPFinishedCount+"th kps finished. "+resultList.size()+" pois found.");
		    Log.log(3, "Request count reached "+loader.requestNowCount+" now.");
		    loader.clear();
		}
		else{
		    spider.setJobStatus(kp_id, SpiderPgScheduler.ERROR);
		    kp_id = spider.assignJob();
		    continue;
		}
		
		kp_id = spider.assignJob();
	    }
	}
	catch(SQLException e){
	    System.out.println(e.getMessage());
	    e.getStackTrace();
	}
	finally{
	    spider.suspend();
	    poiInsert.close();
	    kpLocRes.close();
	}
    }

}
