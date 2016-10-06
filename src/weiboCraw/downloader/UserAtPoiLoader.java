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
import weiboCraw.util.ErrorCode;
import weiboCraw.util.Log;


/**
 * @author weibornhigh
 */
public class UserAtPoiLoader {
    public static final String count = "50"; // 单页返回的记录条数，设为微博API最大值50，以尽可能减少请求次数。

    public int coffeeTime; // 获取下一页User的最大随机间隔时间，以消除机器程序的嫌疑

    public int ip_limit_hourly; // 每小时限制上限

    public HttpClient client;

    public String access_token;

    public List<Integer> missedPage;

    public List<User> userList;

    public Random random;

    public static int totalNumber = 0;

    public static int requestNowCount = 0; // 本轮请求总数

    public static int requestTotalCount = 0; // 自运行起请求总数

    public static int nowHour = (new Date()).getHours(); // 当前整点

    public UserAtPoiLoader(String access_token, int coffeeTime, int ip_limit_hourly) {
        this.access_token = access_token;
        this.coffeeTime = coffeeTime;
        this.ip_limit_hourly = ip_limit_hourly;
        this.client = new HttpClient();
        this.missedPage = new ArrayList<Integer>();
        this.userList = new ArrayList<User>();
        this.random = new Random(System.currentTimeMillis());
    }

    public void getAllUserAtPoi(String poiid) throws JSONException, WeiboException {
        PostParameter[] parList = new PostParameter[] {new PostParameter("poiid", poiid),
            new PostParameter("count", count), new PostParameter("base_app", "0")};

        requestNowCount++ ;
        requestTotalCount++ ;
        Response res = new Response();
        JSONObject jsonUsers = new JSONObject();
        int errorCode = -2;
        try {
            res = client.get(WeiboConfig.getValue("baseURL") + "place/pois/users.json",
                parList, access_token);
            jsonUsers = res.asJSONObject();
            errorCodeDetect(jsonUsers);
        }
        catch (WeiboException e) {
            errorCode = e.getErrorCode();
            ErrorCode errorCodeEnum = ErrorCode.getErrorCodeByCode(errorCode);
            switch (errorCodeEnum) {
                case MISSING_SOURCE:
                    Log.log(Log.LOG_LEVEL_ERROR, ErrorCode.MISSING_SOURCE.toString());
                    System.exit(-1);// appKey过期，直接中断
                case IP_OUT_LIMIT:
                    sleepTime(SleepType.outLimit);
                    return;
                case USER_OUT_LIMIT:
                    sleepTime(SleepType.outLimit);
                    return;
                case POI_DOES_NOT_EXIST:
                    Log.log(Log.LOG_LEVEL_ERROR, ErrorCode.POI_DOES_NOT_EXIST.toString());
                    return;
                default:
                    Log.log(Log.LOG_LEVEL_ERROR, ErrorCode.UNSOLVE_ERROR.toString());
                    return;
            }
        }
        System.out.println("Now PoiID: " + poiid);
        
        JSONArray user = new JSONArray();
        try {
            user = jsonUsers.getJSONArray("users");
        }
        catch (JSONException je) {
            return;
        }

        int size = user.length();
        for (int i = 0; i < size; i++ ) {
            try {
                this.userList.add(new User(user.getJSONObject(i)));
            }
            catch (JSONException e) {
                continue;// 单条记录存在问题，直接跳过
            }
            catch (WeiboException e) {
                continue;// 单条记录存在问题，直接跳过
            }
        }

        sleepTime(SleepType.underLimit); // 稍作休息

        totalNumber = jsonUsers.getInt("total_number");
        if (totalNumber > Integer.parseInt(count)) {
            restUserAtPoi(poiid);
        }

        return;
    }

    private void restUserAtPoi(String poiid)
        throws JSONException, WeiboException {
        int totalPageNumber = (totalNumber - 1) / Integer.parseInt(count) + 1;

        for (int pageId = 2; pageId <= totalPageNumber; pageId++ ) {
            PostParameter[] parList = new PostParameter[] {new PostParameter("poiid", poiid),
                new PostParameter("count", count), new PostParameter("page", pageId),
                new PostParameter("base_app", "0")};
            Response res = new Response();
            JSONObject jsonUsers = new JSONObject();
            try {
                res = client.get(WeiboConfig.getValue("baseURL") + "place/pois/users.json",
                    parList, access_token);
                jsonUsers = res.asJSONObject();
                errorCodeDetect(jsonUsers);
            }
            catch (WeiboException e) {
                int errorCode = e.getErrorCode();
                ErrorCode errorCodeEnum = ErrorCode.getErrorCodeByCode(errorCode);
                switch (errorCodeEnum) {
                    case MISSING_SOURCE:
                        Log.log(Log.LOG_LEVEL_ERROR, ErrorCode.MISSING_SOURCE.toString());
                        System.exit(-1);// appKey过期，直接中断
                    case IP_OUT_LIMIT:
                        sleepTime(SleepType.outLimit);
                        pageId-- ;// 重新请求
                        continue;
                    case USER_OUT_LIMIT:
                        sleepTime(SleepType.outLimit);
                        pageId-- ;// 重新请求
                        continue;
                    case POI_DOES_NOT_EXIST:
                        Log.log(Log.LOG_LEVEL_ERROR, ErrorCode.POI_DOES_NOT_EXIST.toString());
                        return;
                    default:
                        Log.log(Log.LOG_LEVEL_ERROR, ErrorCode.UNSOLVE_ERROR.toString());
                        return;
                }
            }

            JSONArray user = new JSONArray();
            try {
                user = jsonUsers.getJSONArray("users");
            }
            catch (JSONException je) {
                this.missedPage.add(pageId);
                continue;
            }

            int size = user.length();
            for (int i = 0; i < size; i++ ) {
                try {
                    this.userList.add(new User(user.getJSONObject(i)));
                }
                catch (JSONException e) {
                    continue;// 单条记录存在问题，直接跳过
                }
                catch (WeiboException e) {
                    continue;// 单条记录存在问题，直接跳过
                }
            }

            sleepTime(SleepType.underLimit); // 稍作休息
        }
    }
    
    private void errorCodeDetect(JSONObject userObj) throws WeiboException, JSONException{
        Integer errorCode = null;
        try{
            errorCode = userObj.getInt("error_code");
        }
        catch(JSONException e){
            return;
        }
        
        if(null == errorCode){
            return;
        }else {
            throw new WeiboException(null, userObj, -1);
        }
    }

    public void sleepTime(SleepType type) {
        if (requestNowCount >= ip_limit_hourly * 0.98) type = SleepType.upLimit;
        switch (type) {
            case underLimit:
                try {
                    Thread.sleep(coffeeTime + random.nextInt(500));
                }
                catch (InterruptedException e1) {
                    e1.printStackTrace();
                    return;
                }
                ;
                break;
            case upLimit:
                Log.log(3, "Request count almost reached ip_limit_hourly= " + ip_limit_hourly
                           + " now.");
                Log.log(3, "Request count reached " + requestNowCount + " on this trip.");
                Log.log(3, "Request count reached " + requestTotalCount + " totally.");
                requestNowCount = 0;
                Log.log(3, "Request count in this trip cleared.");
                Log.log(3, "I had to take a rest...");
                sleepHourly();
                break;
            case outLimit:
                Log.log(3, "Got 10023 error, I had to take a rest...");
                Log.log(3, "Request count reached " + requestNowCount + " on this trip.");
                Log.log(3, "Request count reached " + requestTotalCount + " totally.");
                requestNowCount = 0;
                Log.log(3, "Request count in this trip cleared.");
                Log.log(3, "I had to take a rest...");
                sleepHourly();
                sleepHourly(); // 睡眠两小时
                break;
        }
    }

    @SuppressWarnings("deprecation")
    public void sleepHourly() {
        System.out.println();
        Date now = null;
        while (true) {
            now = new Date();
            if (now.getHours() == nowHour) {
                System.out.print(now.getMinutes() + ". ");
                try {
                    Thread.sleep(600000L);
                }
                catch (InterruptedException e1) {
                    e1.printStackTrace();
                    return;
                };
            }
            else {
                nowHour = now.getHours();
                Log.log(3, "New round begins, keep going now...");
                break;
            }
        }
    }

    public void clear() {
        missedPage = new ArrayList<Integer>();
        userList = new ArrayList<User>();
        totalNumber = 0;
        client.httpsStatusCode = 0;
    }

    /**
     * @param args
     * @throws WeiboException 
     * @throws JSONException 
     */
    public static void main(String[] args) throws JSONException, WeiboException {
        // TODO Auto-generated method stub
        String accessToken = "2.006yqUoBbN1Z8D0e11a3ee85xaqpgD";
        String poiid = "B209445CD06CA4F84493";
        UserAtPoiLoader loader = new UserAtPoiLoader(accessToken, 10000, 8);
        Log.setConsole(true);
        Log.setLevel(1);
        loader.getAllUserAtPoi(poiid);
        if (!loader.userList.isEmpty()) {
            List<User> list = loader.userList;
            int i = 1;
            for (User u : list) {
                // if(u.getId().equals("1662303105"))
                Log.log(3, i + "th " + u.toString());
                i++ ;
            }
        }
    }
}
