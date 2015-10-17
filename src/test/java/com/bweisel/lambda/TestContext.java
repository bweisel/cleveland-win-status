package com.bweisel.lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * A simple mock implementation of the {@code Context} interface. Default values
 * are stubbed out, and setters are provided so you can customize the context
 * before passing it to your function.
 */
public class TestContext implements Context
{
	private String awsRequestId = "EXAMPLE";
	private ClientContext clientContext;
	private String functionName = "EXAMPLE";
	private String functionVersion = "1.0";
	private String invokedFunctionArn = "ARN";
	private CognitoIdentity identity;
	private String logGroupName = "EXAMPLE";
	private String logStreamName = "EXAMPLE";
	private LambdaLogger logger = new TestLogger();
	private int memoryLimitInMB = 128;
	private int remainingTimeInMillis = 15000;

	public String getAwsRequestId() {
		return awsRequestId;
	}

	public String getLogGroupName() {
		return logGroupName;
	}

	public String getLogStreamName() {
		return logStreamName;
	}

	public String getFunctionName() {
		return functionName;
	}

	public String getFunctionVersion() {
		return functionVersion;
	}

	public String getInvokedFunctionArn() {
		return invokedFunctionArn;
	}

	public CognitoIdentity getIdentity() {
		return identity;
	}

	public ClientContext getClientContext() {
		return clientContext;
	}

	public int getRemainingTimeInMillis() {
		return remainingTimeInMillis;
	}

	public int getMemoryLimitInMB() {
		return memoryLimitInMB;
	}

	public LambdaLogger getLogger() {
		return logger;
	}

	/**
	 * A simple {@code LambdaLogger} that prints everything to stderr.
	 */
	private static class TestLogger implements LambdaLogger {

		public void log(String message) {
			System.err.println(message);
		}
	}

}
