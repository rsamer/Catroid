/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2016 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.test.scratchconverter;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.google.android.gms.common.images.WebImage;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.Client.ConvertCallback;
import org.catrobat.catroid.scratchconverter.Client.DownloadFinishedCallback;
import org.catrobat.catroid.scratchconverter.ClientException;
import org.catrobat.catroid.scratchconverter.WebSocketClient;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.scratchconverter.protocol.MessageListener;
import org.catrobat.catroid.scratchconverter.protocol.WebSocketMessageListener;
import org.catrobat.catroid.scratchconverter.protocol.command.AuthenticateCommand;
import org.catrobat.catroid.scratchconverter.protocol.command.CancelDownloadCommand;
import org.catrobat.catroid.scratchconverter.protocol.command.RetrieveInfoCommand;
import org.catrobat.catroid.scratchconverter.protocol.command.ScheduleJobCommand;
import org.catrobat.catroid.scratchconverter.protocol.message.base.ClientIDMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.ErrorMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.InfoMessage;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WebSocketClientTest extends AndroidTestCase {

	private final int VALID_CLIENT_ID = 1;
	private MessageListener messageListenerMock;
	private AsyncHttpClient asyncHttpClientMock;
	private WebSocketClient webSocketClient;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
		messageListenerMock = Mockito.mock(WebSocketMessageListener.class);
		asyncHttpClientMock = Mockito.mock(AsyncHttpClient.class);
		webSocketClient = new WebSocketClient(VALID_CLIENT_ID, messageListenerMock);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Connect, Disconnect & Authenticate tests
	//------------------------------------------------------------------------------------------------------------------
	public void testAsyncConnectAndAuthenticateWhetherWebSocketObjectSettersAreCalledCorrectly() {
		final WebSocket webSocketMock = Mockito.mock(WebSocket.class);
		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument must not be null", invocation.getArguments()[0]);
				assertEquals("First argument must be equal to MessageListener instance",
						messageListenerMock, invocation.getArguments()[0]);
				return null;
			}
		}).when(webSocketMock).setStringCallback(any(WebSocket.StringCallback.class));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument must not be null", invocation.getArguments()[0]);
				assertEquals("First argument must be equal to WebSocketClient instance",
						webSocketClient, invocation.getArguments()[0]);
				return null;
			}
		}).when(webSocketMock).setClosedCallback(any(CompletedCallback.class));

		doAnswer(new Answer<Future<WebSocket>>() {
			@Override
			public Future<WebSocket> answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of websocket() call must not be null", invocation.getArguments()[0]);
				assertNull("Expecting second argument of websocket() call to be null", invocation.getArguments()[1]);
				assertNotNull("Third argument of websocket() call must not be null", invocation.getArguments()[2]);
				assertEquals("Wrong WebSocket-URL given", Constants.SCRATCH_CONVERTER_WEB_SOCKET,
						invocation.getArguments()[0]);

				// call connectCallback.onCompleted(...)
				((WebSocketConnectCallback)invocation.getArguments()[2]).onCompleted(null, webSocketMock);
				verify(webSocketMock, times(1)).setStringCallback(any(WebSocket.StringCallback.class));
				verify(webSocketMock, times(1)).setClosedCallback(any(CompletedCallback.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionFailure(any(ClientException.class));
				return null;
			}
		}).when(asyncHttpClientMock).websocket(any(String.class), any(String.class), any(WebSocketConnectCallback.class));

		// run the test
		webSocketClient.setAsyncHttpClient(asyncHttpClientMock);
		webSocketClient.connectAndAuthenticate(connectAuthCallbackMock);
		verify(asyncHttpClientMock, times(1)).websocket(any(String.class), any(String.class), any(WebSocketConnectCallback.class));
	}

	public void testAsyncConnectAndAuthenticateSuccessfullyConnectedAndAuthenticatedWithValidClientID() {
		final long expectedClientID = VALID_CLIENT_ID;
		webSocketClient.setClientID(expectedClientID);
		final AuthenticateCommand expectedAuthenticateCommand = new AuthenticateCommand(expectedClientID);

		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				final long newClientID = (long)invocation.getArguments()[0];
				assertFalse("Successfully connected and authenticated but Client state is wrong",
						webSocketClient.isClosed());
				assertTrue("Successfully connected and authenticated but Client state is wrong",
						webSocketClient.isConnected());
				assertTrue("Successfully connected and authenticated but Client state is wrong",
						webSocketClient.isAuthenticated());
				assertEquals("Already valid client ID changed unexpectedly by the server!",
						expectedClientID, newClientID);
				return null;
			}
		}).when(connectAuthCallbackMock).onSuccess(any(Long.class));

		final WebSocket webSocketMock = Mockito.mock(WebSocket.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of send() call must not be null", invocation.getArguments()[0]);
				final String jsonCommand = (String) invocation.getArguments()[0];
				assertEquals("Unexpected or invalid command! Expected AuthenticateCommand",
						expectedAuthenticateCommand.toJson().toString(), jsonCommand);

				// simulate authentication response message from server:
				assertTrue("Client seems to be not connected! Cannot send authentication response message",
						webSocketClient.isConnected());
				webSocketClient.onBaseMessage(new ClientIDMessage(expectedClientID));
				verify(connectAuthCallbackMock, times(1)).onSuccess(any(Long.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionClosed(any(ClientException.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionFailure(any(ClientException.class));
				verify(connectAuthCallbackMock, times(0)).onAuthenticationFailure(any(ClientException.class));
				return null;
			}
		}).when(webSocketMock).send(any(String.class));

		doAnswer(new Answer<Future<WebSocket>>() {
			@Override
			public Future<WebSocket> answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of websocket() call must not be null", invocation.getArguments()[0]);
				assertNull("Expecting second argument of websocket() call to be null", invocation.getArguments()[1]);
				assertNotNull("Third argument of websocket() call must not be null", invocation.getArguments()[2]);
				assertEquals("Wrong WebSocket-URL given", Constants.SCRATCH_CONVERTER_WEB_SOCKET,
						invocation.getArguments()[0]);

				// call connectCallback.onCompleted(...)
				WebSocketConnectCallback connectCallback = (WebSocketConnectCallback) invocation.getArguments()[2];
				connectCallback.onCompleted(null, webSocketMock);
				verify(webSocketMock, times(1)).setStringCallback(any(WebSocket.StringCallback.class));
				verify(webSocketMock, times(1)).setClosedCallback(any(CompletedCallback.class));
				verify(webSocketMock, times(1)).send(any(String.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionFailure(any(ClientException.class));
				return null;
			}
		}).when(asyncHttpClientMock).websocket(any(String.class), any(String.class), any(WebSocketConnectCallback.class));

		// run the test
		webSocketClient.setAsyncHttpClient(asyncHttpClientMock);
		webSocketClient.connectAndAuthenticate(connectAuthCallbackMock);
	}

	public void testAsyncConnectAndAuthenticateSuccessfullyConnectedAndAuthenticatedWithInvalidClientID() {
		final long unexpectedInvalidClientID = Client.INVALID_CLIENT_ID;
		webSocketClient.setClientID(unexpectedInvalidClientID);
		final AuthenticateCommand expectedAuthenticateCommand = new AuthenticateCommand(unexpectedInvalidClientID);

		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				final long newClientID = (long)invocation.getArguments()[0];
				assertFalse("Successfully connected and authenticated but Client state is wrong",
						webSocketClient.isClosed());
				assertTrue("Successfully connected and authenticated but Client state is wrong",
						webSocketClient.isConnected());
				assertTrue("Successfully connected and authenticated but Client state is wrong",
						webSocketClient.isAuthenticated());
				assertTrue("Server accepted and sent INVALID client ID back! This should not happen!",
						newClientID != unexpectedInvalidClientID);
				return null;
			}
		}).when(connectAuthCallbackMock).onSuccess(any(Long.class));

		final WebSocket webSocketMock = Mockito.mock(WebSocket.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of send() call must not be null", invocation.getArguments()[0]);

				final String jsonCommand = (String) invocation.getArguments()[0];
				assertEquals("Unexpected or invalid command! Expected AuthenticateCommand",
						expectedAuthenticateCommand.toJson().toString(), jsonCommand);

				// simulate authentication response message from server:
				assertTrue("Client seems to be not connected! Cannot send authentication response message",
						webSocketClient.isConnected());
				webSocketClient.onBaseMessage(new ClientIDMessage(VALID_CLIENT_ID));
				verify(connectAuthCallbackMock, times(1)).onSuccess(any(Long.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionClosed(any(ClientException.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionFailure(any(ClientException.class));
				verify(connectAuthCallbackMock, times(0)).onAuthenticationFailure(any(ClientException.class));
				return null;
			}
		}).when(webSocketMock).send(any(String.class));

		doAnswer(new Answer<Future<WebSocket>>() {
			@Override
			public Future<WebSocket> answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of websocket() call must not be null", invocation.getArguments()[0]);
				assertNull("Expecting second argument of websocket() call to be null", invocation.getArguments()[1]);
				assertNotNull("Third argument of websocket() call must not be null", invocation.getArguments()[2]);
				assertEquals("Wrong WebSocket-URL given", Constants.SCRATCH_CONVERTER_WEB_SOCKET,
						invocation.getArguments()[0]);

				// call connectCallback.onCompleted(...)
				WebSocketConnectCallback connectCallback = (WebSocketConnectCallback) invocation.getArguments()[2];
				connectCallback.onCompleted(null, webSocketMock);
				verify(webSocketMock, times(1)).setStringCallback(any(WebSocket.StringCallback.class));
				verify(webSocketMock, times(1)).setClosedCallback(any(CompletedCallback.class));
				verify(webSocketMock, times(1)).send(any(String.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionFailure(any(ClientException.class));
				return null;
			}
		}).when(asyncHttpClientMock).websocket(any(String.class), any(String.class), any(WebSocketConnectCallback.class));

		// run the test
		webSocketClient.setAsyncHttpClient(asyncHttpClientMock);
		webSocketClient.connectAndAuthenticate(connectAuthCallbackMock);
	}

	public void testAsyncConnectAndAuthenticateConnectionFailed() {
		final String expectedCancelExceptionMessage = "Successfully canceled the connection by the server!";

		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				final ClientException ex = (ClientException) invocation.getArguments()[0];
				assertEquals("Unexpected closedExceptionMessage message",
						"java.lang.Exception: " + expectedCancelExceptionMessage, ex.getMessage());
				assertTrue("Client not did not close connection correctly", webSocketClient.isClosed());
				return null;
			}
		}).when(connectAuthCallbackMock).onConnectionFailure(any(ClientException.class));

		// mocking asyncHttpClient.websocket(...)
		doAnswer(new Answer<Future<WebSocket>>() {
			@Override
			public Future<WebSocket> answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of websocket() call must not be null", invocation.getArguments()[0]);
				assertNull("Expecting second argument of websocket() call to be null", invocation.getArguments()[1]);
				assertNotNull("Third argument of websocket() call must not be null", invocation.getArguments()[2]);
				assertEquals("Wrong WebSocket-URL given",
						Constants.SCRATCH_CONVERTER_WEB_SOCKET, invocation.getArguments()[0]);

				// call connectCallback.onCompleted(...)
				WebSocketConnectCallback connectCallback = (WebSocketConnectCallback) invocation.getArguments()[2];

				// simulate that the connection failed!
				final WebSocket webSocketMock = Mockito.mock(WebSocket.class);
				connectCallback.onCompleted(new Exception(expectedCancelExceptionMessage), webSocketMock);
				verify(webSocketMock, times(0)).setStringCallback(any(WebSocket.StringCallback.class));
				verify(webSocketMock, times(0)).setClosedCallback(any(CompletedCallback.class));
				verify(webSocketMock, times(0)).send(any(String.class));
				verify(connectAuthCallbackMock, times(1)).onConnectionFailure(any(ClientException.class));
				return null;
			}
		}).when(asyncHttpClientMock).websocket(any(String.class), any(String.class), any(WebSocketConnectCallback.class));

		// run the test
		webSocketClient.setAsyncHttpClient(asyncHttpClientMock);
		webSocketClient.connectAndAuthenticate(connectAuthCallbackMock);
	}

	public void testAsyncConnectAndAuthenticateAuthenticationFailed() {
		final long clientID = VALID_CLIENT_ID;
		webSocketClient.setClientID(clientID);
		final String expectedErrorMessage = "Authentication failed!";

		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				final ClientException ex = (ClientException) invocation.getArguments()[0];
				assertEquals("Wrong error message received", expectedErrorMessage, ex.getMessage());
				assertTrue("Wrong client state", webSocketClient.isConnected());
				assertFalse("Wrong client state", webSocketClient.isAuthenticated());
				return null;
			}
		}).when(connectAuthCallbackMock).onAuthenticationFailure(any(ClientException.class));

		final WebSocket webSocketMock = Mockito.mock(WebSocket.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				// simulate authentication failed response message from server:
				webSocketClient.onBaseMessage(new ErrorMessage(expectedErrorMessage));
				verify(connectAuthCallbackMock, times(0)).onSuccess(any(Long.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionClosed(any(ClientException.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionFailure(any(ClientException.class));
				verify(connectAuthCallbackMock, times(1)).onAuthenticationFailure(any(ClientException.class));
				return null;
			}
		}).when(webSocketMock).send(any(String.class));

		doAnswer(new Answer<Future<WebSocket>>() {
			@Override
			public Future<WebSocket> answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of websocket() call must not be null", invocation.getArguments()[0]);
				assertNull("Expecting second argument of websocket() call to be null", invocation.getArguments()[1]);
				assertNotNull("Third argument of websocket() call must not be null", invocation.getArguments()[2]);
				assertEquals("Wrong WebSocket-URL given",
						Constants.SCRATCH_CONVERTER_WEB_SOCKET, invocation.getArguments()[0]);

				// call connectCallback.onCompleted(...)
				WebSocketConnectCallback connectCallback = (WebSocketConnectCallback) invocation.getArguments()[2];
				connectCallback.onCompleted(null, webSocketMock);
				verify(webSocketMock, times(1)).setStringCallback(any(WebSocket.StringCallback.class));
				verify(webSocketMock, times(1)).setClosedCallback(any(CompletedCallback.class));
				verify(webSocketMock, times(1)).send(any(String.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionFailure(any(ClientException.class));
				return null;
			}
		}).when(asyncHttpClientMock).websocket(any(String.class), any(String.class), any(WebSocketConnectCallback.class));

		// run the test
		webSocketClient.setAsyncHttpClient(asyncHttpClientMock);
		webSocketClient.connectAndAuthenticate(connectAuthCallbackMock);
	}

	public void testServerClosedConnectionAfterConnectionIsEstablished() {
		final String expectedClosedExceptionMessage = "Successfully closed the connection!";

		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				final ClientException ex = (ClientException)invocation.getArguments()[0];
				assertTrue("Successfully closed connection but Client state is wrong", webSocketClient.isClosed());
				assertEquals("Unexpected exception message",
						"java.lang.Exception: " + expectedClosedExceptionMessage, ex.getMessage());
				return null;
			}
		}).when(connectAuthCallbackMock).onConnectionClosed(any(ClientException.class));

		webSocketClient.setAsyncHttpClient(asyncHttpClientMock);
		webSocketClient.setState(Client.State.CONNECTED_AUTHENTICATED);
		webSocketClient.setConnectAuthCallback(connectAuthCallbackMock);

		// now simulate that server closes the connection:
		webSocketClient.onCompleted(new Exception(expectedClosedExceptionMessage));
		verify(connectAuthCallbackMock, times(0)).onSuccess(any(Long.class));
		verify(connectAuthCallbackMock, times(1)).onConnectionClosed(any(ClientException.class));
		verify(connectAuthCallbackMock, times(0)).onConnectionFailure(any(ClientException.class));
		verify(connectAuthCallbackMock, times(0)).onAuthenticationFailure(any(ClientException.class));
	}

	public void testClientClosesConnection() throws InterruptedException {
		final String expectedExceptionMessage = "Client closed connection successfully!";

		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);
		final WebSocket webSocketMock = Mockito.mock(WebSocket.class);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				webSocketClient.onCompleted(new Exception(expectedExceptionMessage));
				verify(connectAuthCallbackMock, times(0)).onSuccess(any(Long.class));
				verify(connectAuthCallbackMock, times(1)).onConnectionClosed(any(ClientException.class));
				verify(connectAuthCallbackMock, times(0)).onConnectionFailure(any(ClientException.class));
				verify(connectAuthCallbackMock, times(0)).onAuthenticationFailure(any(ClientException.class));
				return null;
			}
		}).when(webSocketMock).close();

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				final ClientException ex = (ClientException)invocation.getArguments()[0];
				assertTrue("Successfully closed connection but Client state is wrong", webSocketClient.isClosed());
				assertEquals("Unexpected exception message",
						"java.lang.Exception: " + expectedExceptionMessage, ex.getMessage());
				return null;
			}
		}).when(connectAuthCallbackMock).onConnectionClosed(any(ClientException.class));

		webSocketClient.setAsyncHttpClient(asyncHttpClientMock);
		webSocketClient.setState(Client.State.CONNECTED_AUTHENTICATED);
		webSocketClient.setWebSocket(webSocketMock);
		webSocketClient.setConnectAuthCallback(connectAuthCallbackMock);
		webSocketClient.close();
		verify(webSocketMock, times(1)).close();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Command tests
	//------------------------------------------------------------------------------------------------------------------
	public void testSendRetrieveInfoCommand() {
		final RetrieveInfoCommand expectedRetrieveInfoCommand = new RetrieveInfoCommand();

		final WebSocket webSocketMock = Mockito.mock(WebSocket.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of send() call must not be null", invocation.getArguments()[0]);

				final String jsonCommand = (String) invocation.getArguments()[0];
				assertEquals("Unexpected or invalid command! Expected CancelDownloadCommand",
						expectedRetrieveInfoCommand.toJson().toString(), jsonCommand);
				return null;
			}
		}).when(webSocketMock).send(any(String.class));

		webSocketClient.setState(Client.State.CONNECTED_AUTHENTICATED);
		webSocketClient.setWebSocket(webSocketMock);
		webSocketClient.retrieveInfo();
		verify(webSocketMock, times(1)).send(any(String.class));
	}

	public void testSendScheduleJobCommand() {
		final long expectedJobID = 1;
		final boolean expectedForceValue = false;
		final boolean expectedVerboseValue = false;
		final String expectedProgramTitle = "Test program";
		final WebImage expectedProgramImage = new WebImage(Uri.parse("http://www.catrobat.org/images/logo.png"));

		final ScheduleJobCommand expectedScheduleJobCommand = new ScheduleJobCommand(expectedJobID,
				expectedForceValue, expectedVerboseValue);

		final ConvertCallback convertCallbackMock = Mockito.mock(ConvertCallback.class);

		doAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of scheduleJob() call must not be null", invocation.getArguments()[0]);
				assertNotNull("Third argument of scheduleJob() call must not be null", invocation.getArguments()[2]);
				final Job job = (Job)invocation.getArguments()[0];
				assertEquals("Unexpected jobID given", expectedJobID, job.getJobID());
				assertEquals("Unexpected program title given", expectedProgramTitle, job.getTitle());
				assertEquals("Unexpected program image given", expectedProgramImage, job.getImage());
				assertEquals("Force value changed unexpectedly", expectedForceValue, invocation.getArguments()[1]);
				assertEquals("ConvertCallback expected", convertCallbackMock, invocation.getArguments()[2]);
				return true;
			}
		}).when(messageListenerMock).scheduleJob(any(Job.class), any(Boolean.class), any(ConvertCallback.class));

		final WebSocket webSocketMock = Mockito.mock(WebSocket.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of send() call must not be null", invocation.getArguments()[0]);
				final String jsonCommand = (String) invocation.getArguments()[0];
				assertEquals("Unexpected or invalid command! Expected CancelDownloadCommand",
						expectedScheduleJobCommand.toJson().toString(), jsonCommand);
				return null;
			}
		}).when(webSocketMock).send(any(String.class));

		webSocketClient.setState(Client.State.CONNECTED_AUTHENTICATED);
		webSocketClient.setWebSocket(webSocketMock);
		webSocketClient.setConvertCallback(convertCallbackMock);
		webSocketClient.convertProgram(expectedJobID, expectedProgramTitle, expectedProgramImage,
				expectedVerboseValue, expectedForceValue);

		verify(convertCallbackMock, times(0)).onInfo(any(Float.class), any(Job[].class));
		verify(convertCallbackMock, times(0)).onJobScheduled(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionReady(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionStart(any(Job.class));
		verify(convertCallbackMock, times(0)).onJobProgress(any(Job.class), any(Short.class));
		verify(convertCallbackMock, times(0)).onJobOutput(any(Job.class), any(String[].class));
		verify(convertCallbackMock, times(0)).onConversionFinished(any(Job.class),
				any(Client.DownloadFinishedCallback.class), any(String.class), any(Date.class));
		verify(convertCallbackMock, times(0)).onConversionFailure(any(Job.class), any(ClientException.class));
		verify(convertCallbackMock, times(0)).onError(any(String.class));
		verify(webSocketMock, times(1)).send(any(String.class));
	}

	public void testSendCancelDownloadCommand() {
		final long expectedJobID = 1;
		final CancelDownloadCommand expectedCancelDownloadCommand = new CancelDownloadCommand(expectedJobID);

		final WebSocket webSocketMock = Mockito.mock(WebSocket.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of send() call must not be null", invocation.getArguments()[0]);

				final String jsonCommand = (String) invocation.getArguments()[0];
				assertEquals("Unexpected or invalid command! Expected CancelDownloadCommand",
						expectedCancelDownloadCommand.toJson().toString(), jsonCommand);
				return null;
			}
		}).when(webSocketMock).send(any(String.class));

		webSocketClient.setState(Client.State.CONNECTED_AUTHENTICATED);
		webSocketClient.setWebSocket(webSocketMock);
		webSocketClient.cancelDownload(expectedJobID);
		verify(webSocketMock, times(1)).send(any(String.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Event tests
	//------------------------------------------------------------------------------------------------------------------
	public void testReceivedOnBaseMessageEventWithInfoMessage() {
		final Job expectedUnscheduledJob = new Job(1, "Test program",
				new WebImage(Uri.parse("http://www.catrobat.org/images/logo.png")));
		expectedUnscheduledJob.setState(Job.State.UNSCHEDULED);

		final Job expectedFinishedJob = new Job(2, "Test program2", null);
		expectedFinishedJob.setState(Job.State.FINISHED);
		final String expectedDownloadURL = Constants.SCRATCH_CONVERTER_BASE_URL
				+ "/download?job_id=2&client_id=" + VALID_CLIENT_ID + "&fname=Test%20program2";
		expectedFinishedJob.setDownloadURL(expectedDownloadURL);

		final float expectedCatrobatLanguageVersion = Constants.CURRENT_CATROBAT_LANGUAGE_VERSION;

		final Job[] expectedJobs = new Job[] { expectedUnscheduledJob, expectedFinishedJob };
		final InfoMessage infoMessage = new InfoMessage(expectedCatrobatLanguageVersion, expectedJobs);

		final DownloadFinishedCallback downloadCallbackMock = Mockito.mock(DownloadFinishedCallback.class);
		final ConvertCallback convertCallbackMock = Mockito.mock(ConvertCallback.class);

		doAnswer(new Answer<DownloadFinishedCallback>() {
			int counter = 0;
			@Override
			public DownloadFinishedCallback answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of restoreJobIfRunning() call must not be null",
						invocation.getArguments()[0]);
				assertNotNull("Second argument of restoreJobIfRunning() call must not be null",
						invocation.getArguments()[1]);

				final Job job = (Job) invocation.getArguments()[0];
				final ConvertCallback convertCallback = (ConvertCallback) invocation.getArguments()[1];

				assertEquals(job.getTitle(), expectedJobs[counter].getTitle());
				assertEquals("ConvertCallback is not equal to the corresponding mock object",
						expectedJobs[counter], job);
				assertEquals("ConvertCallback is not equal to the corresponding mock object",
						convertCallbackMock, convertCallback);

				counter++;
				if (job.getState() == Job.State.FINISHED) {
					return downloadCallbackMock;
				}
				return null;
			}
		}).when(messageListenerMock).restoreJobIfRunning(any(Job.class), any(ConvertCallback.class));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of onInfo() call must not be null", invocation.getArguments()[0]);
				assertNotNull("Second argument of onInfo() call must not be null", invocation.getArguments()[1]);

				final float catrobatLanguageVersion = (float)invocation.getArguments()[0];
				final Job[] jobs = (Job[])invocation.getArguments()[1];

				assertEquals("Unexpected catrobat language version",
						expectedCatrobatLanguageVersion, catrobatLanguageVersion);
				assertEquals("Unexpected number of jobs given", expectedJobs.length, jobs.length);

				for (int i = 0; i < jobs.length; i++) {
					final Job job = jobs[i];
					final Job expectedJob = expectedJobs[i];
					assertEquals("Unexpected job given", expectedJob, job);
				}
				return null;
			}
		}).when(convertCallbackMock).onInfo(any(Float.class), any(Job[].class));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of onInfo() call must not be null", invocation.getArguments()[0]);
				assertNotNull("Second argument of onInfo() call must not be null", invocation.getArguments()[1]);

				final Job finishedJob = (Job)invocation.getArguments()[0];
				final Client.DownloadFinishedCallback downloadCallback = (Client.DownloadFinishedCallback)
						invocation.getArguments()[1];
				final String downloadURL = (String)invocation.getArguments()[2];
				final Date cachedDate = (Date)invocation.getArguments()[3];

				assertEquals("DownloadCallback must be", downloadCallback, downloadCallbackMock);
				assertEquals("Invalid download URL given", expectedDownloadURL, downloadURL);
				assertEquals("Unexpected job given", expectedFinishedJob, finishedJob);
				assertNull("Expecting cachedDate to be null here, otherwise the conversion cannot be restored "
						+ "correctly!", cachedDate);
				return null;
			}
		}).when(convertCallbackMock).onConversionFinished(any(Job.class), any(Client.DownloadFinishedCallback.class),
				any(String.class), any(Date.class));

		webSocketClient.setState(Client.State.CONNECTED_AUTHENTICATED);
		webSocketClient.setConvertCallback(convertCallbackMock);
		webSocketClient.onBaseMessage(infoMessage);

		verify(convertCallbackMock, times(1)).onInfo(any(Float.class), any(Job[].class));
		verify(convertCallbackMock, times(0)).onJobScheduled(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionReady(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionStart(any(Job.class));
		verify(convertCallbackMock, times(0)).onJobProgress(any(Job.class), any(Short.class));
		verify(convertCallbackMock, times(0)).onJobOutput(any(Job.class), any(String[].class));
		verify(convertCallbackMock, times(1)).onConversionFinished(any(Job.class),
				any(Client.DownloadFinishedCallback.class), any(String.class), any(Date.class));
		verify(convertCallbackMock, times(0)).onConversionFailure(any(Job.class), any(ClientException.class));
		verify(convertCallbackMock, times(0)).onError(any(String.class));
		verify(messageListenerMock, times(2)).restoreJobIfRunning(any(Job.class), any(ConvertCallback.class));
	}

	public void testReceivedOnBaseMessageEventWithErrorMessageWhenClientIsNotConnected() {
		final String expectedErrorMessageString = "Error message successfully received";
		final ErrorMessage expectedErrorMessage = new ErrorMessage(expectedErrorMessageString);

		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);
		final ConvertCallback convertCallbackMock = Mockito.mock(ConvertCallback.class);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of onError() call must not be null", invocation.getArguments()[0]);
				final String errorMessageString = (String)invocation.getArguments()[0];
				assertEquals("Wrong error message given!", expectedErrorMessageString, errorMessageString);
				return null;
			}
		}).when(convertCallbackMock).onError(any(String.class));

		webSocketClient.setState(Client.State.NOT_CONNECTED);
		webSocketClient.setConnectAuthCallback(connectAuthCallbackMock);
		webSocketClient.setConvertCallback(convertCallbackMock);
		webSocketClient.onBaseMessage(expectedErrorMessage);

		verify(connectAuthCallbackMock, times(0)).onAuthenticationFailure(any(ClientException.class));
		verify(convertCallbackMock, times(0)).onInfo(any(Float.class), any(Job[].class));
		verify(convertCallbackMock, times(0)).onJobScheduled(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionReady(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionStart(any(Job.class));
		verify(convertCallbackMock, times(0)).onJobProgress(any(Job.class), any(Short.class));
		verify(convertCallbackMock, times(0)).onJobOutput(any(Job.class), any(String[].class));
		verify(convertCallbackMock, times(0)).onConversionFinished(any(Job.class),
				any(Client.DownloadFinishedCallback.class), any(String.class), any(Date.class));
		verify(convertCallbackMock, times(0)).onConversionFailure(any(Job.class), any(ClientException.class));
		verify(convertCallbackMock, times(1)).onError(any(String.class));
	}

	public void testReceivedOnBaseMessageEventWithErrorMessageWhenClientIsConnectedButNotAuthenticated() {
		final String expectedErrorMessageString = "Error message successfully received";
		final ErrorMessage expectedErrorMessage = new ErrorMessage(expectedErrorMessageString);

		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				final ClientException ex = (ClientException) invocation.getArguments()[0];
				assertEquals("Wrong error message received", expectedErrorMessageString, ex.getMessage());
				assertTrue("Wrong client state", webSocketClient.isConnected());
				assertFalse("Wrong client state", webSocketClient.isAuthenticated());
				return null;
			}
		}).when(connectAuthCallbackMock).onAuthenticationFailure(any(ClientException.class));

		final ConvertCallback convertCallbackMock = Mockito.mock(ConvertCallback.class);

		webSocketClient.setState(Client.State.CONNECTED);
		webSocketClient.setConnectAuthCallback(connectAuthCallbackMock);
		webSocketClient.setConvertCallback(convertCallbackMock);
		webSocketClient.onBaseMessage(expectedErrorMessage);

		verify(connectAuthCallbackMock, times(1)).onAuthenticationFailure(any(ClientException.class));
		verify(convertCallbackMock, times(0)).onInfo(any(Float.class), any(Job[].class));
		verify(convertCallbackMock, times(0)).onJobScheduled(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionReady(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionStart(any(Job.class));
		verify(convertCallbackMock, times(0)).onJobProgress(any(Job.class), any(Short.class));
		verify(convertCallbackMock, times(0)).onJobOutput(any(Job.class), any(String[].class));
		verify(convertCallbackMock, times(0)).onConversionFinished(any(Job.class),
				any(Client.DownloadFinishedCallback.class), any(String.class), any(Date.class));
		verify(convertCallbackMock, times(0)).onConversionFailure(any(Job.class), any(ClientException.class));
		verify(convertCallbackMock, times(0)).onError(any(String.class));
	}

	public void testReceivedOnBaseMessageEventWithErrorMessageWhenClientIsConnectedAndAuthenticated() {
		final String expectedErrorMessageString = "Error message successfully received";
		final ErrorMessage expectedErrorMessage = new ErrorMessage(expectedErrorMessageString);

		final Client.ConnectAuthCallback connectAuthCallbackMock = Mockito.mock(Client.ConnectAuthCallback.class);
		final ConvertCallback convertCallbackMock = Mockito.mock(ConvertCallback.class);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNull("Expecting job to be null here", invocation.getArguments()[0]);
				assertNotNull("ClientException must not be null", invocation.getArguments()[1]);
				final ClientException ex = (ClientException) invocation.getArguments()[1];
				assertEquals("Wrong error message received", expectedErrorMessageString, ex.getMessage());
				assertTrue("Wrong client state", webSocketClient.isConnected());
				assertTrue("Wrong client state", webSocketClient.isAuthenticated());
				return null;
			}
		}).when(convertCallbackMock).onConversionFailure(any(Job.class), any(ClientException.class));

		webSocketClient.setState(Client.State.CONNECTED_AUTHENTICATED);
		webSocketClient.setConnectAuthCallback(connectAuthCallbackMock);
		webSocketClient.setConvertCallback(convertCallbackMock);
		webSocketClient.onBaseMessage(expectedErrorMessage);

		verify(connectAuthCallbackMock, times(0)).onAuthenticationFailure(any(ClientException.class));
		verify(convertCallbackMock, times(0)).onInfo(any(Float.class), any(Job[].class));
		verify(convertCallbackMock, times(0)).onJobScheduled(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionReady(any(Job.class));
		verify(convertCallbackMock, times(0)).onConversionStart(any(Job.class));
		verify(convertCallbackMock, times(0)).onJobProgress(any(Job.class), any(Short.class));
		verify(convertCallbackMock, times(0)).onJobOutput(any(Job.class), any(String[].class));
		verify(convertCallbackMock, times(0)).onConversionFinished(any(Job.class),
				any(Client.DownloadFinishedCallback.class), any(String.class), any(Date.class));
		verify(convertCallbackMock, times(1)).onConversionFailure(any(Job.class), any(ClientException.class));
		verify(convertCallbackMock, times(0)).onError(any(String.class));
	}

	public void testReceivedUserCanceledConversionEvent() {
		final long expectedJobID = 1;

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of send() call must not be null", invocation.getArguments()[0]);

				final Long jobID = (Long) invocation.getArguments()[0];
				assertEquals("Unexpected jobID given", expectedJobID, jobID.longValue());
				return null;
			}
		}).when(messageListenerMock).onUserCanceledConversion(any(Long.class));

		webSocketClient.setState(Client.State.CONNECTED_AUTHENTICATED);
		webSocketClient.onUserCanceledConversion(expectedJobID);
		verify(messageListenerMock, times(1)).onUserCanceledConversion(any(Long.class));
	}

	public void testIsJobInProgressWrapperCalled() {
		final long expectedJobID1 = 1;
		final long expectedJobID2 = 2;

		doAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				assertNotNull("First argument of send() call must not be null", invocation.getArguments()[0]);

				final Long jobID = (Long) invocation.getArguments()[0];
				if (jobID.longValue() == expectedJobID1) {
					return true;
				} else if (jobID.longValue() == expectedJobID2) {
					return false;
				} else {
					fail("Unexpected jobID given: " + jobID);
					return false;
				}
			}
		}).when(messageListenerMock).isJobInProgress(any(Long.class));

		assertTrue("Expecting job to be in progress", webSocketClient.isJobInProgress(expectedJobID1));
		assertFalse("Expecting job to be NOT in progress", webSocketClient.isJobInProgress(expectedJobID2));
		verify(messageListenerMock, times(2)).isJobInProgress(any(Long.class));
	}

	public void testGetNumberOfJobsInProgressWrapperCalled() {
		final int firstNumberOfJobsResult = 1;
		final int secondNumberOfJobsResult = 2;

		doAnswer(new Answer<Integer>() {
			int counter = 0;

			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				if (counter++ == 0) {
					return firstNumberOfJobsResult;
				} else {
					return secondNumberOfJobsResult;
				}
			}
		}).when(messageListenerMock).getNumberOfJobsInProgress();

		assertEquals(webSocketClient.getNumberOfJobsInProgress(), firstNumberOfJobsResult);
		assertEquals(webSocketClient.getNumberOfJobsInProgress(), secondNumberOfJobsResult);
		verify(messageListenerMock, times(2)).getNumberOfJobsInProgress();
	}
}
