package com.bweisel.lambda.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshaller;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * DynamoDB model for tracking the game status of a team.
 * 
 * It is keyed off of the team name ("browns") and stores
 * if a game is in progress, a timestamp of that, and the
 * timestamp of the last victory.
 * 
 * @author bweisel
 */
@DynamoDBTable(tableName = "cleveland-game-status")
public class TeamStatusEntity
{
	@DynamoDBHashKey(attributeName = "team_name")
	private String teamName;
	
	@DynamoDBAttribute(attributeName = "in_game")
	private Boolean inGame;
	
	@DynamoDBAttribute(attributeName = "in_game_last_updated")
	@DynamoDBMarshalling(marshallerClass = DynamoDateMarshaller.class) 
	private LocalDateTime inGameLastUpdated;
	
	@DynamoDBAttribute(attributeName = "last_victory")
	@DynamoDBMarshalling(marshallerClass = DynamoDateMarshaller.class)
	private LocalDateTime lastVictory;

	/**
	 * @return the teamName
	 */
	public String getTeamName() {
		return teamName;
	}

	/**
	 * @param teamName the teamName to set
	 */
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	/**
	 * @return the inGame
	 */
	public Boolean getInGame() {
		return inGame;
	}

	/**
	 * @param inGame the inGame to set
	 */
	public void setInGame(Boolean inGame) {
		this.inGame = inGame;
	}

	/**
	 * @return the inGameLastUpdated
	 */
	public LocalDateTime getInGameLastUpdated() {
		return inGameLastUpdated;
	}

	/**
	 * @param inGameLastUpdated the inGameLastUpdated to set
	 */
	public void setInGameLastUpdated(LocalDateTime inGameLastUpdated) {
		this.inGameLastUpdated = inGameLastUpdated;
	}

	/**
	 * @return the lastVictory
	 */
	public LocalDateTime getLastVictory() {
		return lastVictory;
	}

	/**
	 * @param lastVictory the lastVictory to set
	 */
	public void setLastVictory(LocalDateTime lastVictory) {
		this.lastVictory = lastVictory;
	}
	
	/**
	 * Marshaller for reading/writing LocalDateTimes to Dynamo
	 * 
	 * @author bweisel
	 */
	static public class DynamoDateMarshaller implements DynamoDBMarshaller<LocalDateTime> {

		@Override
		public String marshall(LocalDateTime date) {
			return date.format(DateTimeFormatter.ISO_DATE_TIME);
		}

		@Override
		public LocalDateTime unmarshall(Class<LocalDateTime> date, String value) {
			return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
		}
	}
}
