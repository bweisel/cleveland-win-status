package com.bweisel.lambda;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
import com.bweisel.lambda.model.TeamStatus;
import com.bweisel.lambda.model.TeamStatusResponse;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class ClevelandWinNotifierTest
{
	private Context createContext() {
		TestContext ctx = new TestContext();
		return ctx;
	}

	@Test
	public void testLambdaFunctionHandler() {
		ClevelandWinNotifier notifier = new ClevelandWinNotifier();
		Context ctx = createContext();

		TeamStatusResponse response = notifier.didAnyoneWin("", ctx);
		Assert.assertNotNull(response);
	}

	// @Test
	public void testParsingLogic() {
		ClevelandWinNotifier notifier = new ClevelandWinNotifier();
		Context ctx = createContext();
		
		notifier.init(ctx.getLogger());
		TeamStatus result = notifier.updateClevelandTeamStatus(Optional.of("Milwaukee 105   Cleveland 94 (3:10 IN 4TH)"), null);
		Assert.assertNotNull(result);
	}
}
