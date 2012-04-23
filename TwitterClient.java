import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import argo.jdom.JdomParser;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;

public class TwitterClient {

	public static void main(String[] args) {

		// read in config
		BufferedReader scan = null;
		try {
			scan = new BufferedReader(new FileReader("config.json"));
		} catch (FileNotFoundException e) {
			System.err.println("config file empty");
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		StringBuffer i = new StringBuffer();
		String config = null;
		String line = null;
		try {
			while ((line = scan.readLine()) != null) {
				i.append(line);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if ((config = i.toString()).length() <= 0) {
			System.err.println("config file empty");
			System.exit(1);
		}

		JsonRootNode json = null;
		try {
			json = new JdomParser().parse(config);
		} catch (InvalidSyntaxException e) {
			System.err.println("Invalid JSON format");
			System.exit(1);
		}

		String query = json.getStringValue("query");
		System.out.println("query: " + query);
		String consumerkey = json.getStringValue("consumerkey");
		System.out.println("consumerkey: " + consumerkey);
		String consumersecret = json.getStringValue("consumersecret");
		System.out.println("consumersecret: " + consumersecret);
		String token = json.getStringValue("token");
		System.out.println("token: " + token);
		String tokensecret = json.getStringValue("tokensecret");
		System.out.println("tokensecret: " + tokensecret);

		// get the texts of each of the tweets matching the query
		ArrayList<String> texts = TwitterLib.query(query);

		// count how many time each _valid_ word is mentioned
		HashMap<String, Integer> counts = TwitterLib.countFrequency(texts, query);

		// some logic to get the two most popular words
		ArrayList<ResultPair> resultArray = new ArrayList<ResultPair>();
		for (String s : counts.keySet()) {
			resultArray.add(new ResultPair(s, counts.get(s)));
		}
		Collections.sort(resultArray);

		// format and post tweet
		int response = 0;
		try {
			response = TwitterLib.postTweet(
					consumerkey,
					consumersecret,
					token,
					tokensecret,
					URLEncoder.encode(
							"The most discussed keyword associated with '" + query + "' are: "
									+ resultArray.get(0).word + " and " + resultArray.get(1).word, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.err.println("Unsupported Encoding; This shouldn't happen");
			System.exit(1);
		}
		System.out.println("Respose: " + response);
		System.out.println("Note: if not 200, it's possible twitter blocked smilar tweets...");
	}

	private static class ResultPair implements Comparable<ResultPair> {
		public String word;
		public int count;

		public ResultPair(String word, int count) {
			this.word = word;
			this.count = count;
		}

		public int compareTo(ResultPair other) {
			if (count > other.count)
				return -1;
			return 1;
		}

	}

}
