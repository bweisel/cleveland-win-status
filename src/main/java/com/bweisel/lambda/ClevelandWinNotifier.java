package com.bweisel.lambda;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.bweisel.lambda.model.TeamStatus;
import com.bweisel.lambda.model.TeamStatusEntity;
import com.bweisel.lambda.model.TeamStatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Calls the ESPN bottomline 'API' to see if any Cleveland sports team just won
 * a game!
 * 
 * @author bweisel
 */
public class ClevelandWinNotifier
{
	// Regex patterns
	private static final Pattern teamPattern = Pattern.compile("\\b[a-zA-Z]+ \\b\\d+");
	private static final Pattern clevelandPattern = Pattern.compile("\\bCleveland \\b\\d+");
	private static final Pattern scorePattern = Pattern.compile("\\d+");
	private static final Pattern inProgressPattern = Pattern.compile("(\\d+:\\d+ IN \\d)");
	private static final Pattern upcomingPattern = Pattern.compile("(\\d+:\\d+ AM|PM)");

	// ESPN Bottomline 'APIs'
	private static final String NFL_URL = "http://sports.espn.go.com/nfl/bottomline/scores";
	private static final String NBA_URL = "http://sports.espn.go.com/nba/bottomline/scores";
	private static final String MLB_URL = "http://sports.espn.go.com/mlb/bottomline/scores";

	// Member variables
	private LambdaLogger logger = null;
	private ObjectMapper mapper = null;
	private AmazonDynamoDBClient dynamoClient = null;
	private DynamoDBMapper db = null;
	
	/**
	 * Checks if any Cleveland team won a game within the last 5 minutes
	 * 
	 * @param input
	 * @param context
	 * @return the JSON result of which Cleveland teams won
	 */
	public TeamStatusResponse didAnyoneWin(Object input, Context context) {
		// Initialize logger and DB connection
		init(context.getLogger());
		
		TeamStatusEntity brownsDBStatus = null;
		TeamStatusEntity cavsDBStatus = null;
		TeamStatusEntity indiansDBStatus = null;
		try {
			brownsDBStatus = db.load(TeamStatusEntity.class, "browns");
			cavsDBStatus = db.load(TeamStatusEntity.class, "cavs");
			indiansDBStatus = db.load(TeamStatusEntity.class, "indians");
		} catch (Exception e) {
			logger.log("Error calling DynamoDB! " + e.getMessage());
		}
		
		CloseableHttpClient httpClient = null;
		try {
			// Execute call to ESPN bottomline service to get real-time game updates
			// Update the status record in Dynamo accordingly
			httpClient = HttpClients.createDefault();
			TeamStatus brownsStatus = updateClevelandTeamStatus(getGameInfo(httpClient, NFL_URL), brownsDBStatus);
			TeamStatus cavsStatus = updateClevelandTeamStatus(getGameInfo(httpClient, NBA_URL), cavsDBStatus);
			TeamStatus tribeStatus = updateClevelandTeamStatus(getGameInfo(httpClient, MLB_URL), indiansDBStatus);

			// Return the results
			TeamStatusResponse response = new TeamStatusResponse(brownsStatus, cavsStatus, tribeStatus);
			logger.log(mapper.writeValueAsString(response));
			return response;
		} catch (Exception e) {
			logger.log("Error parsing score data: " + e.getMessage());
		} finally {
			try {
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
				logger.log("Error closing HTTP client in finally block: " + e.getMessage());
			}
		}
		return null;
	}
	
	/*
	 * Initialization method. Broken out for testing purposes.
	 */
	protected void init(LambdaLogger logger) {
		this.logger = logger;
		this.mapper = new ObjectMapper();
		
		//dynamoClient = new AmazonDynamoDBClient(new BasicAWSCredentials("", ""));
		dynamoClient = new AmazonDynamoDBClient(new ProfileCredentialsProvider());
		dynamoClient.setRegion(Region.getRegion(Regions.fromName("us-west-2")));
		
		db = new DynamoDBMapper(dynamoClient);
	}

	/*
	 * Parses the score line to determine the game status and team scores.
	 * 
	 * Ex: 	Milwaukee 105 Cleveland 94 (3:10 IN 4TH) 
	 * 		Memphis 91 Cleveland 81 (FINAL)
	 * 		Cleveland 33 Baltimore 30 (FINAL - OT)
	 * 		Cleveland 33 Baltimore 30 (00:00 IN OT)
	 */
	protected TeamStatus updateClevelandTeamStatus(Optional<String> scoreLine, TeamStatusEntity teamStatusEntity) {
		final String teamName = teamStatusEntity.getTeamName().toUpperCase();
		
		// Check if there was no Cleveland team in the bottomline result
		if (!scoreLine.isPresent()) {
			logger.log("No game status result found for " + teamName);
			return TeamStatus.NO_OP;
		}
		
		// If the last win date is within the last 10 minutes return WIN!
		if (LocalDateTime.now()
						 .minusMinutes(10)
						 .isBefore(teamStatusEntity.getLastVictory())) {
			logger.log(teamName + " won in the last 10 minutes!");
			return TeamStatus.WIN;
		}

		// Begin to parse the result from the bottomline call
		String fullScoreLine = scoreLine.get();
		logger.log(fullScoreLine);
		
		int split = fullScoreLine.indexOf("(");
		String teamScores = fullScoreLine.substring(0, split);
		String timeLeft = fullScoreLine.substring(split);

		// Check if the game is in progress
		// If so, update the in game timestamp
		Matcher progressMatcher = inProgressPattern.matcher(timeLeft);
		if(progressMatcher.find()) {
			logger.log(teamName + " game in progress.");
			teamStatusEntity.setInGame(true);
			teamStatusEntity.setInGameLastUpdated(LocalDateTime.now());
			db.save(teamStatusEntity);
			return TeamStatus.IN_PROGRESS;
		}
		
		// Check if the game is upcoming
		// If so, just return NO OP
		Matcher upcomingMatcher = upcomingPattern.matcher(timeLeft);
		if (upcomingMatcher.find()) {
			logger.log(teamName + " game upcoming.");
			return TeamStatus.NO_OP;
		}

		// Game must be over, let's check the score!
		int cleScore = -1;
		int oppScore = -1;

		// This regex will essentially divide the teamScores line into 2 pieces:
		// Opponent city & score and Cleveland & score
		Matcher teamMatcher = teamPattern.matcher(teamScores);
		while (teamMatcher.find()) {
			// Parse the score int from the String
			String teamScore = teamMatcher.group();
			Matcher scoreMatcher = scorePattern.matcher(teamScore);
			scoreMatcher.find();
			int parsedScore = Integer.valueOf(scoreMatcher.group());

			// Determine if it's the Cleveland score or opponent score
			if (clevelandPattern.matcher(teamScore).matches()) {
				cleScore = parsedScore;
			} else {
				oppScore = parsedScore;
			}
		}
		
		// Make sure neither score is still -1 (indicating some error parsing)
		logger.log("CLE: " + cleScore + " OPP: " + oppScore);
		if (cleScore < 0 || oppScore < 0) {
			logger.log("Error parsing score values to determine winner! One value is below zero");
			return TeamStatus.NO_OP;
		}
		
		// If Cleveland won, and the in game timestamp is after the last victory, update the DB!
		// Otherwise, just update DB to set in game status to false.
		if (cleScore > oppScore && 
			teamStatusEntity.getInGameLastUpdated()
							.isAfter(teamStatusEntity.getLastVictory())) {
			
			teamStatusEntity.setLastVictory(LocalDateTime.now());
			teamStatusEntity.setInGame(false);
			db.save(teamStatusEntity);
			logger.log(teamName + " JUST won!!!");
			return TeamStatus.WIN;
		} else {
			teamStatusEntity.setInGame(false);
			db.save(teamStatusEntity);
			logger.log(teamName + " game over. Loss, or win is past the alert time.");
		}
		return TeamStatus.NO_OP;
	}

	/*
	 * Executes the HTTP call to get the game data. Not responsible for closing
	 * the passed in HttpClient.
	 */
	private Optional<String> getGameInfo(CloseableHttpClient client, String url) throws Exception {
		CloseableHttpResponse response = null;
		try {
			// Execute call to ESPN bottomline service to get real-time game
			// updates
			HttpGet get = new HttpGet(url);
			response = client.execute(get);

			// Parse the response
			// The response is formatted as a URL query string, need to prepend
			// on a fake host though
			String responseData = EntityUtils.toString(response.getEntity());
			responseData = "http://test.com?p=1" + responseData; // hacky
			responseData = responseData.replaceAll("\\^", "");

			// Loop over results and find the Cleveland line
			List<NameValuePair> results = URLEncodedUtils.parse(new URI(responseData), Charsets.UTF_8.name());
			for (NameValuePair item : results) {
				if (item.getValue() != null && item.getValue().contains("Cleveland")) {
					return Optional.of(item.getValue());
				}
			}
			return Optional.ofNullable(null);
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}
}
