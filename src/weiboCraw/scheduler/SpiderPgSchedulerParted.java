/**
 * 
 */
package weiboCraw.scheduler;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import weiboCraw.util.Log;

/**
 * @author weibornhigh
 *
 */
public class SpiderPgSchedulerParted implements IScheduler{
    
    Connection pgconnect;
    String dbName; // The table name stored all the jobs.
    String jobidName; // The id name marking a unique job. Maybe "poiid" or "uid" in Sina Weibo. 
    int partedCount;
    int partedID;
    
    PreparedStatement assignST;
    PreparedStatement getStatusST;
    PreparedStatement setStatusST;
    PreparedStatement uncompleteST;
    PreparedStatement suspendST;

    public SpiderPgSchedulerParted(Connection pgconnect, String dbName, String jobidName, int partedCount, int partedID) throws SQLException{
	this.pgconnect = pgconnect;
	this.dbName = dbName;
	this.jobidName = jobidName;
	this.assignST = pgconnect.prepareStatement("SELECT "+jobidName+" from "+dbName+" WHERE jobid%?=? AND status='waiting' LIMIT 1;");
	this.setStatusST = pgconnect.prepareStatement("UPDATE "+dbName+" SET status=? WHERE "+jobidName+"=?;");
	this.getStatusST = pgconnect.prepareStatement("SELECT status FROM "+dbName+" WHERE "+jobidName+"=?;");
	this.uncompleteST = pgconnect.prepareStatement("UPDATE "+dbName+" SET status='uncompleted',missedpage=? WHERE "+jobidName+"=?;");
	this.suspendST = pgconnect.prepareStatement("UPDATE "+dbName+" SET status='waiting' "
						  + "WHERE status='running' OR status='error' OR status='unknown';");
	
	this.assignST.setInt(1, partedCount);
	this.assignST.setInt(2, partedID);
    }
    
    @Override
    synchronized public String assignJob() {
	// TODO Auto-generated method stub
	ResultSet rs = null;
	try{
	    rs = assignST.executeQuery();
	    if(!rs.next()) return null;
	    String jobId = rs.getString(jobidName);
	    setJobStatus(jobId,RUNNING);
	    return jobId;
	}
	catch ( SQLException e ) {
	    Log.logException("SQL Error: ",e );
	}
	finally {
	    try {
		if (rs!=null )
		    rs.close();
		}
	    catch ( Exception e ) {
		Log.logException("UNKNOW Error: ",e );
		}
	}
	return null;
    }

    @Override
    synchronized public String getJobStatus(String id) {
	// TODO Auto-generated method stub
	ResultSet rs = null;
	try{
	    getStatusST.setString(1, id);
	    rs = getStatusST.executeQuery();
	    if(!rs.next()) return UNKNOWN;
	    String status = rs.getString("status");
	    return status;
	}
	catch( SQLException e ){
	    Log.logException("SQL Error: ",e );
	}
	finally {
	    try {
		if (rs!=null )
		    rs.close();
		}
	    catch ( Exception e ) {
		Log.logException("UNKNOW Error: ",e );
		}
	}
	
	return UNKNOWN;
    }
    
    @Override
    public boolean setJobStatus(String id, String status) {
	// TODO Auto-generated method stub
	try{
	    setStatusST.setString(1, status);
	    setStatusST.setString(2, id);
	    return setStatusST.executeUpdate()==1 ? true:false;
	}
	catch( SQLException e ){
	    Log.logException("SQL Error: ",e );
	    return false;
	}
    }

    @Override
    public boolean uncomplete(String id, List<Integer> missedPage) {
	// TODO Auto-generated method stub
	try{
	    Object[] missedPageObj = missedPage.toArray();
	    Array missedPageArr = this.pgconnect.createArrayOf("int", missedPageObj);
	    uncompleteST.setArray(1, missedPageArr);
	    uncompleteST.setString(2, id);
	    return uncompleteST.executeUpdate()>=1 ? true:false;
	}
	catch( SQLException e ){
	    Log.logException("SQL Error: ",e );
	    return false;
	}
    }
    
    @Override
    public boolean incomplete(String id) {
	// TODO Auto-generated method stub
	return false;
    }
    
    @Override
    synchronized public boolean suspend() {
	// TODO Auto-generated method stub
	try{
	    return suspendST.executeUpdate()>0 ? true:false;
	}
	catch( SQLException e ){
	    Log.logException("SQL Error: ",e );
	    return false;
	}
    }
    
    public static void main(String[] agrs) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
	String pgurl = "jdbc:postgresql://127.0.0.1:5432/sz_weibo";
	String user = "postgres";
	String pwd = "123456";
	
	try{
	    Class.forName("org.postgresql.Driver").newInstance();
	    Connection pgconnect = DriverManager.getConnection(pgurl, user, pwd);
	    SpiderPgSchedulerParted spider = new SpiderPgSchedulerParted(pgconnect,"pois","poiid",5,1);
	    Log.setConsole(true);
	    Log.setLevel(1);
	    Log.log(3, spider.assignJob());
	    Log.log(3, spider.getJobStatus("1"));
	    Log.log(3, Boolean.toString(spider.setJobStatus("1", spider.RUNNING)));
	    ArrayList<Integer> missedPage = new ArrayList<Integer>();
	    missedPage.add(1);
	    missedPage.add(2);
	    missedPage.add(4);
	    Log.log(3, Boolean.toString(spider.uncomplete("1", missedPage)));
	    Log.log(3, Boolean.toString(spider.incomplete("1")));
	    Log.log(3, Boolean.toString(spider.suspend()));
	}
	catch(SQLException e){
		System.out.println(e.getMessage());
		e.getStackTrace();
	}
	
    }

}
