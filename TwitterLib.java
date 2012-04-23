import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;

public class TwitterLib {

	/**
	 * array of all the words that should be excluded
	 */
	private static ArrayList<String> excluded = null;

	/**
	 * For some query, get related posts on twitter via GET api.
	 * 
	 * @param queryString
	 * @return ArrayList<String> of all the tweets
	 */
	public static ArrayList<String> query(String queryString) {
		
		try {
			queryString = URLEncoder.encode(queryString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.err.println("Unsupported Encoding; This should not happen.");
			System.exit(1);
		}
		String path = "http://api.twitter.com/search.json?q=" + queryString + "&lang=en";

		StringBuffer webContent = new StringBuffer();
		String line, language, urlpath;
		Socket socket = null;
		PrintWriter output = null;
		BufferedReader input = null;

		try {
			//old code to connect to url via socket 
			URL url = new URL(path);
			socket = new Socket(InetAddress.getByName(url.getHost()).getHostAddress(), 80);

			output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			if (!url.getPath().trim().equals("")) {
				urlpath = url.getPath() + "?" + url.getQuery();
			} else {
				urlpath = "/";
			}

			language = " HTTP/1.1\nHost: " + url.getHost() + "\n";

			//write proper get request
			output.println("GET " + urlpath + language);
			output.flush();

			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			//skip headers
			while ((line = input.readLine()) != null && !line.contains("{"));

			//save content
			do {
				webContent.append(line + " ");
			} while ((line = input.readLine()) != null);

		} catch (UnknownHostException e) {
			System.out.println("Unknown Host: " + path);
		} catch (IOException e) {
		} finally {
			output.close();
			try {
				input.close();
			} catch (IOException e) {
			}
		}

		String rawContent = new String(webContent);

		//parse json
		JsonRootNode json = null;
		try {
			json = new JdomParser().parse(rawContent);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return null;
		}

		//get results
		List<JsonNode> results = json.getArrayNode("results");
		ArrayList<String> resultsList = new ArrayList<String>();

		//get text value in results
		for (JsonNode j : results) {
			resultsList.add(j.getStringValue("text"));
		}

		return resultsList;
	}

	/**
	 * Counts the number of occurrences of all the words and returns a hash map of each count
	 * 
	 * @param texts
	 * @param query
	 * @return
	 */
	public static HashMap<String, Integer> countFrequency(ArrayList<String> texts, String query) {
		HashMap<String, Integer> counts = new HashMap<String, Integer>();

		for (String s : texts) {
			//get rid of hash tags
			String text = s.replaceAll("#[\\S]*?(\\s|$)", " ");
			//get rid of usernames
			text = text.replaceAll("@[\\S]*?(\\s|$)", " ");
			//get rid of urls
			text = text.replaceAll("http://[\\S]*?(\\s|$)", " ");
			//get rid of non letters
			text = text.replaceAll("[^\\p{Alpha}\\p{Space}]", "");

			//for each of the words, determine if it should count and count it
			String[] words = text.trim().toLowerCase().split(" ");
			for (String word : words) {
				if (word.length() > 1 && !isExcluded(word, query)) {
					counts.put(word,
							((counts.get(word) != null) ? counts.get(word)
									: 0) + 1);
				}
			}
		}

		return counts;
	}
	
	/**
	 * Post the given message to account associated with token using application associated with application
	 * associated with consumerkey.
	 * 
	 * @param cKey
	 * @param cSecret
	 * @param token
	 * @param tokenSecret
	 * @param message
	 */
	public static int postTweet(String cKey, String cSecret, String token, String tokenSecret, String message) {
		
		OAuthConsumer c = new DefaultOAuthConsumer(cKey, cSecret);
		c.setTokenWithSecret(token, tokenSecret);
		
		final String params = "status=" + message;
		URL url = null;
		try {
			url = new URL("http://twitter.com/statuses/update.xml?" + params);
		} catch (MalformedURLException e1) {
			System.err.println("Malformed URL; This should not happen");
			System.exit(1);
		}
		HttpURLConnection request = null;
		try {
			request = (HttpURLConnection) url.openConnection();
		} catch (IOException e2) {
			System.err.println("Connection Error");
			System.exit(1);
		}

		try {
			request.setRequestMethod("POST");
		} catch (ProtocolException e1) {
			System.err.println("Invalid protocol");
			System.exit(1);
		}
		request.setRequestProperty("Content-Length", "0");
		request.setUseCaches(false);
		try {
			c.sign(request);
		} catch (OAuthMessageSignerException e) {
			System.err.println("Message Signer failed...");
			System.exit(1);
		} catch (OAuthExpectationFailedException e) {
			System.err.println("Expectation failed...");
			System.exit(1);
		} catch (OAuthCommunicationException e) {
			System.err.println("Communication failed...");
			System.exit(1);
		}

		try {
			request.connect();
		} catch (IOException e) {
			System.err.println("Connection failed");
			System.exit(1);
		}
		int statusCode = 0;
        try {
			statusCode = request.getResponseCode();
		} catch (IOException e) {
			System.err.println("Connection failed");
			System.exit(1);
		}
        return statusCode;
	}

	/**
	 *  helper method to determine if the word should be counted
	 */
	private static boolean isExcluded(String word, String query) {
		if (excluded == null) {
			excluded = new ArrayList<String>();

			// http://dev.mysql.com/doc/refman/5.0/en/fulltext-stopwords.html
			String[] exWords = { "as", "able", "about", "above", "according",
					"accordingly", "across", "actually", "after", "afterwards",
					"again", "against", "aint", "all", "allow", "allows",
					"almost", "alone", "along", "already", "also", "although",
					"always", "am", "among", "amongst", "an", "and", "another",
					"any", "anybody", "anyhow", "anyone", "anything", "anyway",
					"anyways", "anywhere", "apart", "appear", "appreciate",
					"appropriate", "are", "arent", "around", "as", "aside",
					"ask", "asking", "associated", "at", "available", "away",
					"awfully", "be", "became", "because", "become", "becomes",
					"becoming", "been", "before", "beforehand", "behind",
					"being", "believe", "below", "beside", "besides", "best",
					"better", "between", "beyond", "both", "brief", "but",
					"by", "cmon", "cs", "came", "can", "cant", "cannot",
					"cant", "cause", "causes", "certain", "certainly",
					"changes", "clearly", "co", "com", "come", "comes",
					"concerning", "consequently", "consider", "considering",
					"contain", "containing", "contains", "corresponding",
					"could", "couldnt", "course", "currently", "definitely",
					"described", "despite", "did", "didnt", "different", "do",
					"does", "doesnt", "doing", "dont", "done", "down",
					"downwards", "during", "each", "edu", "eg", "eight",
					"either", "else", "elsewhere", "enough", "entirely",
					"especially", "et", "etc", "even", "ever", "every",
					"everybody", "everyone", "everything", "everywhere", "ex",
					"exactly", "example", "except", "far", "few", "fifth",
					"first", "five", "followed", "following", "follows", "for",
					"former", "formerly", "forth", "four", "from", "further",
					"furthermore", "get", "gets", "getting", "given", "gives",
					"go", "goes", "going", "gone", "got", "gotten",
					"greetings", "had", "hadnt", "happens", "hardly", "has",
					"hasnt", "have", "havent", "having", "he", "hes",
					"hello", "help", "hence", "her", "here", "heres",
					"hereafter", "hereby", "herein", "hereupon", "hers",
					"herself", "hi", "him", "himself", "his", "hither",
					"hopefully", "how", "howbeit", "however", "id", "ill",
					"im", "ive", "ie", "if", "ignored", "immediate", "in",
					"inasmuch", "inc", "indeed", "indicate", "indicated",
					"indicates", "inner", "insofar", "instead", "into",
					"inward", "is", "isnt", "it", "itd", "itll", "its",
					"its", "itself", "just", "keep", "keeps", "kept", "know",
					"known", "knows", "last", "lately", "later", "latter",
					"latterly", "least", "less", "lest", "let", "lets",
					"like", "liked", "likely", "little", "look", "looking",
					"looks", "ltd", "mainly", "many", "may", "maybe", "me",
					"mean", "meanwhile", "merely", "might", "more", "moreover",
					"most", "mostly", "much", "must", "my", "myself", "name",
					"namely", "nd", "near", "nearly", "necessary", "need",
					"needs", "neither", "never", "nevertheless", "new", "next",
					"nine", "no", "nobody", "non", "none", "noone", "nor",
					"normally", "not", "nothing", "novel", "now", "nowhere",
					"obviously", "of", "off", "often", "oh", "ok", "okay",
					"old", "on", "once", "one", "ones", "only", "onto", "or",
					"other", "others", "otherwise", "ought", "our", "ours",
					"ourselves", "out", "outside", "over", "overall", "own",
					"particular", "particularly", "per", "perhaps", "placed",
					"please", "plus", "possible", "presumably", "probably",
					"provides", "que", "quite", "qv", "rather", "rd", "re",
					"really", "reasonably", "regarding", "regardless",
					"regards", "relatively", "respectively", "right", "said",
					"same", "saw", "say", "saying", "says", "second",
					"secondly", "see", "seeing", "seem", "seemed", "seeming",
					"seems", "seen", "self", "selves", "sensible", "sent",
					"serious", "seriously", "seven", "several", "shall", "she",
					"should", "shouldnt", "since", "six", "so", "some",
					"somebody", "somehow", "someone", "something", "sometime",
					"sometimes", "somewhat", "somewhere", "soon", "sorry",
					"specified", "specify", "specifying", "still", "sub",
					"such", "sup", "sure", "ts", "take", "taken", "tell",
					"tends", "th", "than", "thank", "thanks", "thanx", "that",
					"thats", "thats", "the", "their", "theirs", "them",
					"themselves", "then", "thence", "there", "theres",
					"thereafter", "thereby", "therefore", "therein", "theres",
					"thereupon", "these", "they", "theyd", "theyll",
					"theyre", "theyve", "think", "third", "this", "thorough",
					"thoroughly", "those", "though", "three", "through",
					"throughout", "thru", "thus", "to", "together", "too",
					"took", "toward", "towards", "tried", "tries", "truly",
					"try", "trying", "twice", "two", "un", "under",
					"unfortunately", "unless", "unlikely", "until", "unto",
					"up", "upon", "us", "use", "used", "useful", "uses",
					"using", "usually", "value", "various", "very", "via",
					"viz", "vs", "want", "wants", "was", "wasnt", "way", "we",
					"wed", "well", "were", "weve", "welcome", "well",
					"went", "were", "werent", "what", "whats", "whatever",
					"when", "whence", "whenever", "where", "wheres",
					"whereafter", "whereas", "whereby", "wherein", "whereupon",
					"wherever", "whether", "which", "while", "whither", "who",
					"whos", "whoever", "whole", "whom", "whose", "why",
					"will", "willing", "wish", "with", "within", "without",
					"wont", "wonder", "would", "wouldnt", "yes", "yet",
					"you", "youd", "youll", "youre", "youve", "your",
					"yours", "yourself", "yourselves", "zero", "rt", "quot" };

			for (String w : exWords) {
				excluded.add(w);
			}
		}
		String [] qWords = query.split("\\p{Space}");
		for (String w : qWords) {
			if (w.trim().equals(word)) {
				return true;
			}
		}
		return excluded.contains(word);
	}
}
