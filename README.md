# TwitterClient #

This program is a basic Twitter client that will access and update Twitter. The program will query
Twitter for tweets related to a given word or phrase. The program will then find the most frequently
used words in those tweets and tweet the results with the given account.

-----

To run this program:

	$ java -cp argo-2.23.jar:commons-codec-1.6.jar:signpost-core-1.2.1.2.jar:. TwitterClient

This program expects a config file called config.json in proper JSON format.

Example config.json;
>{
> 	"query": "**detroit**",
>	"consumerkey": "CcF1oVnW6G9ukIXqSJ4JNQ",
>	"consumersecret": "RmaPWt2uYGUO8R0pZO8vjt9iZvsAowJhL9BD7IcbvU",
>	"token": "363467149-QM8EOTp00TLf0r9cLBVIKJXb6qBBp3H25DxXtU2i",
>	"tokensecret": "YtcYrYeSUgtLTMHZt1JiRhLIpcAai3v1qTiQVPwU" 	
>}

Libraries used: **argo-2.23.jar**, **commons-codec-1.6.jar**, **signpost-core-1.2.1.2.jar**

-----

## Details ## 

### Design ###

This is a single function program whose purpose is to interact with the Twitter API and practice using
technologies such as JSON and oAuth. The `TwitterClient` class is the driver class that uses several
static methods from `TwitterLib`

### TwitterClient.java ###

The `TwitterClient` class is the driver for the program. It does a few simple tasks:
- parses `config.json` file
- queries the twitter server
- counts the frequencies
- posts the results
The actual implementations of each of these processes are in the `TwitterLib` class

### TwitterLib.java ###

The `TwitterLib` class is a collection of static methods that will do several different tasks

#### query ####

The `query` method will take in a word or phrase and query the twitter servers with it. The JSON results
are parsed and an ArrayList of tweets are returned.

#### counts ####

The `counts` method will take an ArrayList of tweets and count the frequency of non-excluded words
that occure in all the tweets. It will return a HashMap that maps the words to the counts.

#### post ####

The `post` method will post a tweet to the given account using the given oAuth authentication
information.
