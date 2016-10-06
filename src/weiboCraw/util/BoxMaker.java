/**
 * 
 */
package weiboCraw.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import weiboCraw.scheduler.SpiderPgScheduler;

/**
 * @author weibornhigh
 *
 */
public class BoxMaker {

    /**
     * @param args
     * @throws ClassNotFoundException 
     */
    public static void main(String[] args) throws ClassNotFoundException {
	// TODO Auto-generated method stub
	String pgurl = "jdbc:postgresql://127.0.0.1:5432/shenzhen_mainroad";
	String user = "postgres";
	String pwd = "123456";
	
	try{
	    Class.forName("org.postgresql.Driver").newInstance();
	    Connection pgconnect = DriverManager.getConnection(pgurl, user, pwd);
	    PreparedStatement st = pgconnect.prepareStatement(
		    			"insert into box_1000(id,box) "
		    			+ "values(?,box2d("
		    			+ "geometry("
		    			+ "st_buffer("
		    			+ "geography("
		    			+ "st_setsrid("
		    			+ "st_makepoint(113.6932709,22.4625425),4326)),?))));");
	    int size = 92;
	    for(int i=1; i<=size; i++){
		st.setInt(1, i);
		st.setInt(2, 1000*i);
		st.executeUpdate();
	    }
	    
	    st.close();
	    pgconnect.close();
	}
	catch(IllegalAccessException e){
	    System.out.println(e.getMessage());
	    e.getStackTrace();
	}
	catch(InstantiationException e){
	    System.out.println(e.getMessage());
	    e.getStackTrace();
	}
	catch(SQLException e){
	    System.out.println(e.getMessage());
	    e.getStackTrace();
	}
    }

}
