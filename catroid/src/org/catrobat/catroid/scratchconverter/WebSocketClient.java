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

package org.catrobat.catroid.scratchconverter;

import android.util.Log;

import com.google.android.gms.common.images.WebImage;
import com.google.common.base.Preconditions;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.scratchconverter.protocol.BaseMessageHandler;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.scratchconverter.protocol.JobHandler;
import org.catrobat.catroid.scratchconverter.protocol.MessageListener;
import org.catrobat.catroid.scratchconverter.protocol.WebSocketMessageListener;
import org.catrobat.catroid.scratchconverter.protocol.command.CancelDownloadCommand;
import org.catrobat.catroid.scratchconverter.protocol.command.Command;
import org.catrobat.catroid.scratchconverter.protocol.command.RetrieveInfoCommand;
import org.catrobat.catroid.scratchconverter.protocol.command.ScheduleJobCommand;
import org.catrobat.catroid.scratchconverter.protocol.command.AuthenticateCommand;
import org.catrobat.catroid.scratchconverter.protocol.message.base.BaseMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.ClientIDMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.ErrorMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.InfoMessage;

final public class WebSocketClient implements Client, BaseMessageHandler {

	private interface ConnectCallback {
		void onSuccess();
		void onFailure(ClientException ex);
	}

	private static final String TAG = WebSocketClient.class.getSimpleName();

	private Client.State state;
	private long clientID;
	// TODO: use only MessageListener interface and extend MessageListener interface...
	private final WebSocketMessageListener messageListener;
	private AsyncHttpClient asyncHttpClient = AsyncHttpClient.getDefaultInstance();
	private WebSocket webSocket;
	private ConnectAuthCallback connectAuthCallback;
	private ConvertCallback convertCallback;

	public WebSocketClient(final long clientID, final WebSocketMessageListener messageListener) {
		this.clientID = clientID;
		this.state = State.NOT_CONNECTED;
		messageListener.setBaseMessageHandler(this);
		this.messageListener = messageListener;
		this.webSocket = null;
		this.connectAuthCallback = null;
		this.convertCallback = null;
	}

	@Override
	public boolean isClosed() {
		return state == State.NOT_CONNECTED;
	}

	@Override
	public boolean isAuthenticated() {
		return state == State.CONNECTED_AUTHENTICATED;
	}

	public void setAsyncHttpClient(final AsyncHttpClient asyncHttpClient) {
		this.asyncHttpClient = asyncHttpClient;
	}

	public void setConvertCallback(final ConvertCallback callback) {
		convertCallback = callback;
	}

	private void connect(final ConnectCallback connectCallback) {
		if (state == State.CONNECTED) {
			connectCallback.onSuccess();
			return;
		}
		Preconditions.checkState(webSocket == null);
		Preconditions.checkState(asyncHttpClient != null);

		asyncHttpClient.websocket(Constants.SCRATCH_CONVERTER_WEB_SOCKET, null, new
				AsyncHttpClient.WebSocketConnectCallback() {
					@Override
					public void onCompleted(Exception ex, final WebSocket newWebSocket) {
						Preconditions.checkState(state != State.CONNECTED && webSocket == null);
						if (ex != null) {
							connectCallback.onFailure(new ClientException(ex));
							return;
						}

						state = State.CONNECTED;
						webSocket = newWebSocket;

						// onMessage callback
						webSocket.setStringCallback(messageListener);

						// onClose callback
						webSocket.setClosedCallback(new CompletedCallback() {
							@Override
							public void onCompleted(Exception ex) {
								state = State.NOT_CONNECTED;
								connectAuthCallback.onConnectionClosed(new ClientException(ex));
							}
						});
						connectCallback.onSuccess();
					}
				});
	}

	public void close() {
		Preconditions.checkState(state != State.NOT_CONNECTED);
		Preconditions.checkState(webSocket != null);
		Preconditions.checkState(connectAuthCallback != null);
		webSocket.close();
	}

	private void authenticate() {
		Preconditions.checkState(state == State.CONNECTED);
		Preconditions.checkState(webSocket != null);
		sendCommand(new AuthenticateCommand(clientID));
	}

	public void connectAndAuthenticate(final ConnectAuthCallback connectAuthCallback) {
		this.connectAuthCallback = connectAuthCallback;

		switch (state) {
			case NOT_CONNECTED:
				connect(new ConnectCallback() {
					@Override
					public void onSuccess() {
						Log.i(TAG, "Successfully connected to WebSocket server");
						authenticate();
					}

					@Override
					public void onFailure(ClientException ex) {
						connectAuthCallback.onConnectionFailure(ex);
					}
				});
				break;

			case CONNECTED:
				Log.i(TAG, "Already connected to WebSocket server!");
				authenticate();
				break;

			case CONNECTED_AUTHENTICATED:
				Log.i(TAG, "Already authenticated!");
				connectAuthCallback.onSuccess(clientID);
				break;

		}
	}

	@Override
	public void retrieveInfo() {
		Preconditions.checkState(state == State.CONNECTED_AUTHENTICATED);
		Preconditions.checkState(webSocket != null);
		sendCommand(new RetrieveInfoCommand());
	}

	@Override
	public boolean isJobInProgress(long jobID) {
		return messageListener.isJobInProgress(jobID);
	}

	@Override
	public void convertProgram(final long jobID, final String title, final WebImage image,
			final boolean verbose, final boolean force) {
		Preconditions.checkState(state == State.CONNECTED_AUTHENTICATED);
		Preconditions.checkState(webSocket != null);
		Preconditions.checkState(clientID != INVALID_CLIENT_ID);

		Log.i(TAG, "Scheduling new job with ID: " + jobID);

		// TODO: consider Law of Demeter... implement wrappers!
		JobHandler jobHandler = messageListener.getJobHandler(jobID);
		final Job job = new Job(jobID, title, image);
		if (jobHandler != null) {
			Log.d(TAG, "JobHandler for jobID " + jobID + " already exists!");
			if (!force && jobHandler.isInProgress()) {
				convertCallback.onConversionFailure(job, new ClientException("Job in progress!"));
				return;
			}
			jobHandler.setCallback(convertCallback);
		} else {
			Log.d(TAG, "Creating new JobHandler for jobID " + jobID);
			jobHandler = new JobHandler(job, convertCallback);
			messageListener.setJobHandlerForJobID(jobHandler);
		}

		jobHandler.onJobScheduled();
		sendCommand(new ScheduleJobCommand(jobID, force, verbose));
	}

	@Override
	public void cancelDownload(final long jobID) {
		sendCommand(new CancelDownloadCommand(jobID));
	}

	@Override
	public void onUserCanceledConversion(long jobID) {
		messageListener.onUserCanceledConversion(jobID);
	}

	@Override
	public void onBaseMessage(BaseMessage baseMessage) {
		// case: InfoMessage
		if (baseMessage instanceof InfoMessage) {
			final InfoMessage infoMessage = (InfoMessage) baseMessage;
			convertCallback.onInfo(infoMessage.getCatrobatLanguageVersion(), infoMessage.getJobList());

			final Job[] jobs = infoMessage.getJobList();
			for (Job job : jobs) {
				JobHandler jobHandler = messageListener.getJobHandler(job.getJobID());
				if (jobHandler == null) {
					jobHandler = new JobHandler(job, convertCallback);
				}
				messageListener.setJobHandlerForJobID(jobHandler);

				if (job.getState() == Job.State.FINISHED && !job.isAlreadyDownloaded() && job.getDownloadURL() != null) {
					Log.i(TAG, "Downloading missed converted project...");
					convertCallback.onConversionFinished(job, jobHandler, job.getDownloadURL(), null);
				}
			}
			return;
		}

		// case: ErrorMessage
		if (baseMessage instanceof ErrorMessage) {
			final ErrorMessage errorMessage = (ErrorMessage) baseMessage;
			Log.e(TAG, errorMessage.getMessage());

			if (state == State.CONNECTED) {
				Preconditions.checkState(connectAuthCallback != null);
				connectAuthCallback.onAuthenticationFailure(new ClientException(errorMessage.getMessage()));
				convertCallback.onError(errorMessage.getMessage());
			} else if (state == State.CONNECTED_AUTHENTICATED) {
				convertCallback.onConversionFailure(null, new ClientException(errorMessage.getMessage()));
			} else {
				convertCallback.onError(errorMessage.getMessage());
			}
			return;
		}

		// case: ClientIDMessage & RenewClientIDMessage
		if (baseMessage instanceof ClientIDMessage) {
			Preconditions.checkState(state == State.CONNECTED);

			final ClientIDMessage clientIDMessage = (ClientIDMessage) baseMessage;
			long oldClientID = clientID;
			clientID = clientIDMessage.getClientID();

			if (clientID != oldClientID) {
				Log.d(TAG, "New Client ID: " + clientID);
			}

			state = State.CONNECTED_AUTHENTICATED;
			connectAuthCallback.onSuccess(clientID);
			return;
		}

		// case: Unhandled base message!
		Log.e(TAG, "No handler implemented for base message: " + baseMessage);
	}

	private void sendCommand(final Command command) {
		Preconditions.checkArgument(command != null);
		Preconditions.checkState(state == State.CONNECTED || state == State.CONNECTED_AUTHENTICATED);
		Preconditions.checkState(webSocket != null);

		final String dataToSend = command.toJson().toString();
		Log.d(TAG, "Sending: " + dataToSend);
		webSocket.send(dataToSend);
	}

}
