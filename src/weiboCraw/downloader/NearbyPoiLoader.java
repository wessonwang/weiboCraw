/**
 * 
 */
package weiboCraw.downloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import weibo4j.http.HttpClient;
import weibo4j.http.Response;
import weibo4j.model.Places;
import weibo4j.model.PostParameter;
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
public class NearbyPoiLoader {
    public static final String count="50"; //��ҳ���صļ�¼��������Ϊ΢��API���ֵ50
    public static final String maxSearchRadius = "1000";
    public Random random;
    public int coffeeTime;		   //��ȡ��һҳUser�����������ʱ�䣬�������������������
    public long sleepTime;
    public int ip_limit_hourly;
    public HttpClient client;
    public String access_token;
    public List<Integer> missedPage;
    public List<Places> poiList;
    public static int totalNumber = 0;
    public static int requestNowCount = 0;
    public static int requestTotalCount = 0;
    
    public NearbyPoiLoader(String access_token,int coffeeTime,long sleepTime,int ip_limit_hourly){
	this.access_token = access_token;
	this.coffeeTime = coffeeTime;
	this.sleepTime = sleepTime;
	this.ip_limit_hourly = ip_limit_hourly;
	this.client = new HttpClient();
	this.missedPage = new ArrayList<Integer>();
	this.poiList = new ArrayList<Places>();
	this.random = new Random(System.currentTimeMillis());
    }
    
    /**
     * ����΢��API ��place/nearby/pois.json�� ���������ĿΪ1000����
     * ��ץ�뾶Ӧ��ǡ����С��Ĭ����Ϊ1000�ף��ɸ������������á�
     * @param lon
     * @param lat
     * @return FALSE->��ȡʧ�ܣ�TRUE->��ȡ�ɹ�
     */
    public boolean nearbyPois(String lon, String lat){
	PostParameter[] parList = new PostParameter[]{new PostParameter("long", lon),
						      new PostParameter("lat", lat),
						      new PostParameter("count", count),
						      new PostParameter("range", maxSearchRadius),
						      new PostParameter("sort", "1")};
	try{
	    requestNowCount++;
	    requestTotalCount++;
	    Response res = client.get(WeiboConfig.getValue("baseURL") + "place/nearby/pois.json", parList, access_token);
	    JSONObject jsonObj = res.asJSONObject();
	    totalNumber = jsonObj.getInt("total_number");
	    JSONArray json = jsonObj.getJSONArray("pois");
	    int size = json.length();
	    for (int i = 0; i < size; i++) {
		poiList.add(new Places(json.getJSONObject(i)));
	    }
	    
	    Log.log(3, "Request count reached "+requestNowCount+" now.");
	    sleepTime();
	    
	    if(totalNumber>Integer.parseInt(count)){
		this.nearbyRestPois(lon,lat);
	    }
	    
	    return true;
	}
	catch(JSONException jsone){
//	    return false;
	    if(client.httpsStatusCode==200) return true;
	    if(client.httpsStatusCode==403)
		try {
		    Log.log(3, "Got 10023 error, I had to take a rest...");
		    Log.log(3, "Request count reached "+requestNowCount+" on this trip.");
		    Log.log(3, "Request count reached "+requestTotalCount+" totally.");
		    Thread.sleep(sleepTime+random.nextInt(coffeeTime));
		} catch (InterruptedException e1) {
		    // TODO Auto-generated catch block
		    e1.printStackTrace();
		}
	    return false;
	}
	catch(WeiboException e){
//	    return false;
	    if(client.httpsStatusCode==200) return true;
	    if(client.httpsStatusCode==403)
		try {
		    Log.log(3, "Got 10023 error, I had to take a rest...");
		    Log.log(3, "Request count reached "+requestNowCount+" on this trip.");
		    Log.log(3, "Request count reached "+requestTotalCount+" totally.");
		    Thread.sleep(sleepTime+random.nextInt(coffeeTime));
		} catch (InterruptedException e2) {
		    // TODO Auto-generated catch block
		    e2.printStackTrace();
		}
	    return false;
	}
    }
    
    private void nearbyRestPois(String lon, String lat){
	int remainder = totalNumber%Integer.parseInt(count);
	int multiNum = totalNumber/Integer.parseInt(count);
	int totalPageNumber = (int) (remainder==0 ? multiNum:multiNum+1);
	Random random = new Random(System.currentTimeMillis());
	//ѭ���ӵڶ�ҳ��ʼ
	for(int pageId=2; pageId<=totalPageNumber; pageId++){
	    PostParameter[] parList = new PostParameter[]{new PostParameter("long", lon),
		    					  new PostParameter("lat", lat),
		    					  new PostParameter("count", count),
		    					  new PostParameter("page", pageId),
		    					  new PostParameter("range", maxSearchRadius),
		    					  new PostParameter("sort", "1")};
	    try{
		Thread.sleep(coffeeTime+random.nextInt(500));
		requestNowCount++;
		requestTotalCount++;
		Response res = client.get(WeiboConfig.getValue("baseURL") + "place/nearby/pois.json", parList, access_token);
		JSONObject jsonObj = res.asJSONObject();
		JSONArray json = jsonObj.getJSONArray("pois");
		int size = json.length();
		for (int i = 0; i < size; i++) {
		    poiList.add(new Places(json.getJSONObject(i)));
		}
		
		Log.log(3, "Request count reached "+requestNowCount+" now.");
		sleepTime();
		
	    }
	    catch(WeiboException e){
		this.missedPage.add(pageId);
		if(client.httpsStatusCode==403)
		    try {
			Log.log(3, "Got 10023 error, I had to take a rest...");
			Log.log(3, "Request count reached "+requestNowCount+" on this trip.");
			Log.log(3, "Request count reached "+requestTotalCount+" totally.");
			Thread.sleep(sleepTime+random.nextInt(coffeeTime));
		    } catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		    }
	    }
	    catch(JSONException jsone){
		this.missedPage.add(pageId);
		if(client.httpsStatusCode==403)
		    try {
			Log.log(3, "Got 10023 error, I had to take a rest...");
			Log.log(3, "Request count reached "+requestNowCount+" on this trip.");
			Log.log(3, "Request count reached "+requestTotalCount+" totally.");
			Thread.sleep(sleepTime+random.nextInt(coffeeTime));
		    } catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		    }
	    }
	    catch(InterruptedException e){
		this.missedPage.add(pageId);
	    }
	}
    }
    
    /**
     * �ڸ��������뾶�£����������Ŀ����200����ζ�Ÿ�������ܻ���������POIδ�����ء�
     * @return
     */
    public boolean isReached200(){
	return totalNumber>=200 ? true:false;
    }
    
    public void clear(){
	missedPage = new ArrayList<Integer>();
	poiList = new ArrayList<Places>();
	totalNumber = 0;
	client.httpsStatusCode = 0;
    }
    
    public void sleepTime(){
	if(requestNowCount>ip_limit_hourly*0.95){
	    Log.log(3, "Request count almost reached ip_limit_hourly= "+ip_limit_hourly+" now.");
	    Log.log(3, "Request count reached "+requestNowCount+" on this trip.");
	    Log.log(3, "Request count reached "+requestTotalCount+" totally.");
	    requestNowCount = 0;
	    Log.log(3, "Request count in this trip cleared.");
	    Log.log(3, "I had to take a rest... Maybe just "+sleepTime/60000+ "min ^_^.");
	    try{
		Thread.sleep(random.nextInt(coffeeTime));
		int minutes = (int) (sleepTime/60000);
		for(int i=1; i<=minutes; i++){
		    Thread.sleep(60000);
		    System.out.print(i+"...");
		}
	    }
	    catch (InterruptedException e1) {
		e1.printStackTrace();
	    }
	}
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
	// TODO Auto-generated method stub
//	String accessToken = "2.006yqUoBYTFH2Cc4ba0fe85aiQkiWB";
	String accessToken = "2.006yqUoB0v77_y2108a15b6b7cva2E";
	String lat = "22.548270235714";
	String lon = "113.924334313249";
	NearbyPoiLoader loader = new NearbyPoiLoader(accessToken,120000,600000,200);
	Log.setConsole(true);
	Log.setLevel(1);
	if(loader.nearbyPois(lon, lat)){
	    List<Places> resultList = loader.poiList;
	    int i=1;
	    for (Places pl : resultList) {
		Log.log(3,i+"th "+pl.toString());
		i++;
	    }
	}
    }

}
