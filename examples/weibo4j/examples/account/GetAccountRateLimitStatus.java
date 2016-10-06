package weibo4j.examples.account;

import weibo4j.Account;
import weibo4j.examples.oauth2.Log;
import weibo4j.model.RateLimitStatus;
import weibo4j.model.WeiboException;

public class GetAccountRateLimitStatus {

	public static void main(String[] args) {
		String access_token = "2.006yqUoBYTFH2Cc4ba0fe85aiQkiWB";
		Account am = new Account(access_token);
		try {
            RateLimitStatus json = am.getAccountRateLimitStatus();
			Log.logInfo(json.toString());
		} catch (WeiboException e) {
			e.printStackTrace();
		}
	}

}
