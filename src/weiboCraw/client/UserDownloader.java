/**
 * 
 */
package weiboCraw.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Random;

import weibo4j.model.User;
import weibo4j.model.WeiboException;
import weibo4j.org.json.JSONException;
import weiboCraw.downloader.UserAtPoiLoader;
import weiboCraw.scheduler.SpiderPgScheduler;
import weiboCraw.scheduler.SpiderPgSchedulerParted;
import weiboCraw.util.Log;

/**
 * @author weibornhigh
 *
 */
public class UserDownloader {

    /**
     * @param args
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws SQLException 
     * @throws InterruptedException 
     * @throws WeiboException 
     * @throws JSONException 
     */
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, InterruptedException, JSONException, WeiboException {
	// TODO Auto-generated method stub
	Log.setConsole(true);
	if(args.length != 6){
	    System.out.println("Usage: dbHost(IP) coffeeTime(int,毫秒) "
		    	   + "ip_limit_hourly(int,次数) "
		    	   + "partedCount(int,任务分割数) partedID(int,任务ID)"
		    	   + " accessToken");
	    System.exit(0);
	}
	String accessToken = args[5];
	int coffeeTime = Integer.parseInt(args[1]); 	//随机睡眠时间
	int ip_limit_hourly = Integer.parseInt(args[2]);
	int partedCount = Integer.parseInt(args[3]);
	int partedID = Integer.parseInt(args[4]);
	
	Random random = new Random(System.currentTimeMillis());
	
	String pgurl = "jdbc:postgresql://"+args[0]+":54323/szweibo";
	String user = "postgres";
	String pwd = "Hf864110";
	String poiDb = "pois";
	String jobid_column = "poiid";
	String userDb = "users";
	Connection pgconnect = null;
	SpiderPgSchedulerParted spider = null;
	PreparedStatement userInsert = null;
	
	try{
	    Class.forName("org.postgresql.Driver").newInstance();
	    pgconnect = DriverManager.getConnection(pgurl, user, pwd);
	    spider = new SpiderPgSchedulerParted(pgconnect,poiDb,jobid_column,partedCount,partedID);
	    userInsert = pgconnect.prepareStatement("INSERT INTO "+userDb+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
	    UserAtPoiLoader loader = new UserAtPoiLoader(accessToken,coffeeTime,ip_limit_hourly);
	    Log.setConsole(true);
	    Log.setLevel(1);
	    
	    String poiid = spider.assignJob();
	    int poiFinishedCount = 0;
	    while(null != poiid){
		Thread.sleep(random.nextInt(coffeeTime));
		loader.getAllUserAtPoi(poiid);
		if(!loader.userList.isEmpty()){
		    List<User> userList = loader.userList;
		    for(User u:userList){
			userInsert.setString(1, u.getId());
			userInsert.setString(2, u.getScreenName());
			userInsert.setInt(3, u.getProvince());
			userInsert.setInt(4, u.getCity());
			userInsert.setString(5, u.getLocation());
			userInsert.setString(6, u.getDescription());
			userInsert.setString(7, u.getProfile_url());
			userInsert.setString(8, u.getGender());
			userInsert.setInt(9, u.getFollowersCount());
			userInsert.setInt(10, u.getFriendsCount());
			userInsert.setInt(11, u.getPagefriends_count());
			userInsert.setInt(12, u.getbiFollowersCount());
			userInsert.setInt(13, u.getStatusesCount());
			userInsert.setInt(14, u.getFavouritesCount());
			Date date = u.getCreatedAt();
			userInsert.setDate(15, new java.sql.Date(date.getYear(),date.getMonth(),date.getDay()));
			userInsert.setBoolean(16, u.getGeo_enabled());
			userInsert.setBoolean(17, u.isVerified());
			userInsert.setInt(18, u.getVerifiedType());
			userInsert.setString(19, u.getVerifiedReason());
			userInsert.setString(20, u.getRemark());
			userInsert.setInt(21, u.getCredit_Score());
			userInsert.setInt(22, u.getUser_ability());
			userInsert.setInt(23, u.getUrank());
			
			try{
			    userInsert.executeUpdate();
			}
			catch(SQLException e){
			    userInsert.setString(1, u.getId());
			    userInsert.setString(2, null);
			    userInsert.setInt(3, -1);
			    userInsert.setInt(4, -1);
			    userInsert.setString(5, null);
			    userInsert.setString(6, null);
			    userInsert.setString(7, null);
			    userInsert.setString(8, null);
			    userInsert.setInt(9, -1);
			    userInsert.setInt(10, -1);
			    userInsert.setInt(11, -1);
			    userInsert.setInt(12, -1);
			    userInsert.setInt(13, -1);
			    userInsert.setInt(14, -1);
			    userInsert.setDate(15, null);
			    userInsert.setBoolean(16, false);
			    userInsert.setBoolean(17, false);
			    userInsert.setInt(18, -1);
			    userInsert.setString(19, null);
			    userInsert.setString(20, null);
			    userInsert.setInt(21, -1);
			    userInsert.setInt(22, -1);
			    userInsert.setInt(23, -1);
			    userInsert.executeUpdate();
			}
		    }
		    
		    if(!loader.missedPage.isEmpty()){
			spider.uncomplete(poiid, loader.missedPage);
		    }
		    else{
			spider.setJobStatus(poiid, SpiderPgScheduler.COMPLETED);
		    }
		    
		    poiFinishedCount++;
		    Log.log(3, poiFinishedCount+"th kps finished. "+userList.size()+" users found.");
		    Log.log(3, "Request count reached "+loader.requestNowCount+" now.");
		    loader.clear();
		}
		
		else{
		    spider.setJobStatus(poiid, SpiderPgScheduler.ERROR);
		    poiid = spider.assignJob();
		    continue;
		}
		
		poiid = spider.assignJob();
	    }
	}
	catch(SQLException e){
	    System.out.println(e.getMessage());
	    e.getStackTrace();
	}
	finally{
	    spider.suspend();
	    userInsert.close();
	}
    }
}
