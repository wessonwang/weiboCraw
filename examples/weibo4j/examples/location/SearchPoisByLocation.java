package weibo4j.examples.location;

import java.util.List;

import weibo4j.Location;
import weibo4j.examples.oauth2.Log;
import weibo4j.model.Poisition;
import weibo4j.model.WeiboException;

public class SearchPoisByLocation {

	public static void main(String[] args) {
		String access_token = "2.006yqUoB0VjzZldfd9e5be03nCNPIC";
		String q = args[1];
		Location l = new Location(access_token);
		try {
			List<Poisition> list = l.searchPoisByLocationByQ(q);
			for (Poisition p : list) {
				Log.logInfo(p.toString());
			}
		} catch (WeiboException e) {
			e.printStackTrace();
		}

	}

}
