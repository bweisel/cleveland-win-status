package com.bweisel.lambda.model;

/**
 * The response returned by the Lambda function.
 * 
 * @author bweisel
 */
public class TeamStatusResponse
{
	private TeamStatus browns;
	private TeamStatus cavs;
	private TeamStatus indians;

	public TeamStatusResponse() {}
	
	/**
	 * Set the response with the win boolean values
	 * 
	 * @param browns did the browns win
	 * @param cavs did the cavs win
	 * @param indians did the indians win
	 */
	public TeamStatusResponse(TeamStatus browns, TeamStatus cavs, TeamStatus indians) {
		this.browns = browns;
		this.cavs = cavs;
		this.indians = indians;
	}

	/**
	 * @return the browns
	 */
	public TeamStatus getBrowns() {
		return browns;
	}

	/**
	 * @param browns the browns to set
	 */
	public void setBrowns(TeamStatus browns) {
		this.browns = browns;
	}

	/**
	 * @return the cavs
	 */
	public TeamStatus getCavs() {
		return cavs;
	}

	/**
	 * @param cavs the cavs to set
	 */
	public void setCavs(TeamStatus cavs) {
		this.cavs = cavs;
	}

	/**
	 * @return the indians
	 */
	public TeamStatus getIndians() {
		return indians;
	}

	/**
	 * @param indians the indians to set
	 */
	public void setIndians(TeamStatus indians) {
		this.indians = indians;
	}
}
