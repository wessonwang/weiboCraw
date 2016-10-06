/**
 * 
 */
package weiboCraw.downloader;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import weibo4j.http.HttpClient;
import weibo4j.http.Response;
import weibo4j.model.PostParameter;
import weibo4j.model.User;
import weibo4j.model.WeiboException;
import weibo4j.org.json.JSONArray;
import weibo4j.org.json.JSONException;
import weibo4j.org.json.JSONObject;
import weibo4j.util.WeiboConfig;
import weiboCraw.util.Log;

/**
 * @author weibornhigh
 *
 */
public class UserAtPoiLoader {
    public static final String count="50"; //单页返回的记录条数，设为微博API最大值50，以尽可能减少请求次数。
    public int coffeeTime;		   //获取下一页User的最大随机间隔时间，以消除机器程序的嫌疑
    public int ip_limit_hourly;		   //每小时限制上限
    public HttpClient client;
    public String access_token;
    public List<Integer> missedPage;
    public List<User> userList;
    public Random random;
    public static int totalNumber = 0;
    public static int requestNowCount = 0;	//本轮请求总数
    public static int requestTotalCount = 0;	//自运行起请求总数
    public static int nowHour = (new Date()).getHours();	//当前整点
    
    public UserAtPoiLoader(String access_token,int coffeeTime,int ip_limit_hourly){
	this.access_token = access_token;
	this.coffeeTime = coffeeTime;
	this.ip_limit_hourly = ip_limit_hourly;
	this.client = new HttpClient();
	this.missedPage = new ArrayList<Integer>();
	this.userList = new ArrayList<User>();
	this.random = new Random(System.currentTimeMillis());
    }
    
    public boolean userAtPoi(String poiid){
	PostParameter[] parList = new PostParameter[]{new PostParameter("poiid", poiid),
						      new PostParameter("count", count),
						      new PostParameter("base_app","0")};
	
	try{
	    requestNowCount++;
	    requestTotalCount++;
	    Response res = client.get(WeiboConfig.getValue("baseURL") + "place/pois/users.json",
		    		      parList,
		    		      access_token);
	    System.out.println("Now PoiID: "+poiid);
	    
//	    if(res.getStatusCode() == 0){
//		Log.log(3, "No response! Mark this Poi as Error!");
//		return false;
//	    }
	    
	    JSONObject jsonUsers = res.asJSONObject();
	    JSONArray user = jsonUsers.getJSONArray("users");
	    
	    if(null == user){
		return false;
	    }
	    
	    totalNumber = jsonUsers.getInt("total_number");
	    int size = user.length();
	    for (int i = 0; i < size; i++) {
		this.userList.add(new User(user.getJSONObject(i)));
	    }
	    sleepTime(SleepType.underLimit);	//稍作休息
	    
	    if(totalNumber>Integer.parseInt(count)){
		restUserAtPoi(poiid);
	    }
	    
	    return true;
	}
	catch(JSONException jsone){
	    if(client.httpsStatusCode==200) return true;
	    if(client.httpsStatusCode==403)
		sleepTime(SleepType.outLimit);
	    return false;
	}
	catch(WeiboException e){
	    if(e.getMessage().equals("A JSONObject text must begin with '{' at character 1:[]")){
		Log.log(3, "No response! Mark this Poi as Error!");
		return false;
	    }
	    else if(client.httpsStatusCode==200)
		return true;
	    
	    if(client.httpsStatusCode==403)
		sleepTime(SleepType.outLimit);
	    return false;
	}
    }
    
    private void restUserAtPoi(String poiid){
	int totalPageNumber = (totalNumber-1)/Integer.parseInt(count) + 1;
	
	for(int pageId=2; pageId<=totalPageNumber; pageId++){
	    PostParameter[] parList = new PostParameter[]{new PostParameter("poiid", poiid),
		     					  new PostParameter("count", count),
		     					  new PostParameter("page",pageId),
		     					  new PostParameter("base_app","0")};
	    
	    try{
		requestNowCount++;
		requestTotalCount++;
		Response res = client.get(WeiboConfig.getValue("baseURL") + "place/pois/users.json",
					  parList,
					  access_token);
		JSONObject jsonUsers = res.asJSONObject();
		JSONArray user = jsonUsers.getJSONArray("users");
		int size = user.length();
		for (int i = 0; i < size; i++) {
		    this.userList.add(new User(user.getJSONObject(i)));
		}
		
		sleepTime(SleepType.underLimit);	//稍作休息
	    }
	    catch(WeiboException e){
		this.missedPage.add(pageId);
		if(client.httpsStatusCode==403)
		    sleepTime(SleepType.outLimit);
	    }
	    catch(JSONException jsone){
		this.missedPage.add(pageId);
		if(client.httpsStatusCode==403)
		    sleepTime(SleepType.outLimit);
	    }
	}
    }
    
    public void sleepTime(SleepType type){
	if(requestNowCount >= ip_limit_hourly*0.98)
	    type = SleepType.upLimit;
	switch(type){
	case underLimit:
	    try{
		Thread.sleep(coffeeTime+random.nextInt(500));
	    }
	    catch (InterruptedException e1) {
		e1.printStackTrace();
		return;
	    };
	    break;
	case upLimit:
	    Log.log(3, "Request count almost reached ip_limit_hourly= "+ip_limit_hourly+" now.");
	    Log.log(3, "Request count reached "+requestNowCount+" on this trip.");
	    Log.log(3, "Request count reached "+requestTotalCount+" totally.");
	    requestNowCount = 0;
	    Log.log(3, "Request count in this trip cleared.");
	    Log.log(3, "I had to take a rest...");
	    sleepHourly();
	    break;
	case outLimit:
	    Log.log(3, "Got 10023 error, I had to take a rest...");
	    Log.log(3, "Request count reached "+requestNowCount+" on this trip.");
	    Log.log(3, "Request count reached "+requestTotalCount+" totally.");
	    requestNowCount = 0;
	    Log.log(3, "Request count in this trip cleared.");
	    Log.log(3, "I had to take a rest...");
	    sleepHourly();
	    sleepHourly();	//睡眠两小时
	    break;
	}
    }
    
    public void sleepHourly(){
	System.out.println();
	Date now = null;
	while(true){
	    now = new Date();
	    if(now.getHours() == nowHour){
		System.out.print(now.getMinutes()+". ");
		try{
		    Thread.sleep(600000L);
		}
		catch (InterruptedException e1) {
		    e1.printStackTrace();
		    return;
		};
	    }
	    else{
		nowHour = now.getHours();
		Log.log(3, "New round begins, keep going now...");
		break;
	    }
	}
    }
    
    public void clear(){
	missedPage = new ArrayList<Integer>();
	userList = new ArrayList<User>();
	totalNumber = 0;
	client.httpsStatusCode = 0;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
	// TODO Auto-generated method stub
	String accessToken = "2.006yqUoBbN1Z8D0e11a3ee85xaqpgD";
	String poiid = "B209445CD06CA4F84493";
	UserAtPoiLoader loader = new UserAtPoiLoader(accessToken,10000,8);
	Log.setConsole(true);
	Log.setLevel(1);
	if(loader.userAtPoi(poiid)){
	    List<User> list = loader.userList;
	    int i = 1;
	    for(User u:list){
//		if(u.getId().equals("1662303105"))
		Log.log(3, i+"th "+u.toString());
		i++;
	    }
	}
    }
}
