
public class ResultPair implements Comparable<ResultPair> {
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
