package weibo4j.examples.place;

import weibo4j.Place;
import weibo4j.examples.oauth2.Log;
import weibo4j.model.Places;
import weibo4j.model.WeiboException;

public class GetPoisShow {

	public static void main(String[] args) {
		String access_token = "2.006yqUoBYTFH2Cc4ba0fe85aiQkiWB";
		String poiid = "B2094655DA65A1FD439A";
		Place p = new Place(access_token);
		try {
			Places pl = p.poisShow(poiid);
			Log.logInfo(pl.toString());
		} catch (WeiboException e) {
			e.printStackTrace();
		}

	}

}
