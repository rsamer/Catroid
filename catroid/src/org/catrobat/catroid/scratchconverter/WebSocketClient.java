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

import com.google.common.base.Preconditions;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.scratchconverter.protocol.BaseMessageHandler;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.scratchconverter.protocol.JobHandler;
import org.catrobat.catroid.scratchconverter.protocol.MessageListener;
import org.catrobat.catroid.scratchconverter.protocol.WebSocketMessageListener;
import org.catrobat.catroid.scratchconverter.protocol.command.Command;
import org.catrobat.catroid.scratchconverter.protocol.command.ScheduleJobCommand;
import org.catrobat.catroid.scratchconverter.protocol.command.SetClientIDCommand;
import org.catrobat.catroid.scratchconverter.protocol.message.Message;
import org.catrobat.catroid.scratchconverter.protocol.message.base.BaseMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.ClientIDMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.ErrorMessage;
import org.catrobat.catroid.scratchconverter.protocol.message.base.JobsInfoMessage;
import org.catrobat.catroid.ui.scratchconverter.BaseInfoViewListener;

final public class WebSocketClient implements Client, BaseMessageHandler {

	private interface ConnectCallback {
		void onSuccess();
	}
	private interface AuthCallback {
		void onSuccess();
	}

	private static final String TAG = WebSocketClient.class.getSimpleName();
	public static final long INVALID_CLIENT_ID = -1;

	private Client.State state;
	private long clientID;
	private WebSocketMessageListener messageListener;
	private AsyncHttpClient asyncHttpClient = AsyncHttpClient.getDefaultInstance();
	private WebSocket webSocket;
	private AuthCallback authCallback;
	private CompletionCallback completionCallback;

	public WebSocketClient(final long clientID, final WebSocketMessageListener messageListener) {
		this.clientID = clientID;
		this.state = State.NOT_CONNECTED;
		messageListener.setBaseMessageHandler(this);
		this.messageListener = messageListener;
		this.webSocket = null;
		this.authCallback = null;
		this.completionCallback = null;
	}

	public MessageListener getMessageListener() {
		return messageListener;
	}

	public void setAsyncHttpClient(AsyncHttpClient asyncHttpClient) {
		this.asyncHttpClient = asyncHttpClient;
	}

	public boolean isJobInProgress(final long jobID) {
		JobHandler jobHandler = messageListener.getJobHandler(jobID);
		return jobHandler != null && jobHandler.getState().isInProgress();
	}

	private void connect(final ConnectCallback connectCallback, final CompletionCallback completionCallback) {
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
							state = State.FAILED;
							completionCallback.onAuthenticationFailure(new ClientException(ex));
							return;
						}

						state = State.CONNECTED;
						webSocket = newWebSocket;

						webSocket.setStringCallback(messageListener);
						connectCallback.onSuccess();
					}
				});
	}

	private void authenticate(final AuthCallback authCallback) {
		if (state == State.AUTHENTICATED) {
			authCallback.onSuccess();
			return;
		}
		Preconditions.checkState(state == State.CONNECTED);
		Preconditions.checkState(webSocket != null);
		sendCommand(new SetClientIDCommand(clientID));
	}

	@Override
	public void convertJob(final Job job, final CompletionCallback completionCallback) {
		this.completionCallback = completionCallback;
		this.authCallback = new AuthCallback() {
			@Override
			public void onSuccess() {
				Log.i(TAG, "Authentication successful!");
				convert(job, completionCallback);
			}
		};

		if (state == State.NOT_CONNECTED) {
			connect(new ConnectCallback() {
				@Override
				public void onSuccess() {
					Log.i(TAG, "Successfully connected to WebSocket server");
					authenticate(authCallback);
				}
			}, completionCallback);
		} else if (state == State.CONNECTED) {
			Log.i(TAG, "Already connected to WebSocket server!");
			authenticate(authCallback);
		} else if (state == State.AUTHENTICATED) {
			Log.i(TAG, "Already authenticated!");
			convert(job, completionCallback);
		} else {
			Log.e(TAG, "Unhandled state: " + state);
		}
	}

	private void convert(final Job job, final CompletionCallback completionCallback) {
		Preconditions.checkState(state == State.AUTHENTICATED);
		Preconditions.checkState(webSocket != null);
		Preconditions.checkState(clientID != INVALID_CLIENT_ID);

		JobHandler jobHandler = messageListener.getJobHandler(job.getJobID());
		boolean force = false;
		if (jobHandler != null) {
			if (jobHandler.getState().isInProgress()) {
				completionCallback.onConversionFailure(new ClientException("Job in progress!"));
				return;
			}
			force = true;
		} else {
			jobHandler = new JobHandler(job, completionCallback);
			messageListener.addJobHandler(jobHandler);
		}
		sendCommand(new ScheduleJobCommand(job.getJobID(), clientID, force));
	}

	private void sendCommand(final Command command) {
		Preconditions.checkArgument(command != null);
		Preconditions.checkState(state == State.CONNECTED || state == State.AUTHENTICATED);
		Preconditions.checkState(webSocket != null);

		final String dataToSend = command.toJson().toString();
		Log.d(TAG, "Sending: " + dataToSend);
		webSocket.send(dataToSend);
	}

	@Override
	public void onBaseMessage(BaseMessage baseMessage, BaseInfoViewListener[] viewListeners) {
		// case: JobsInfoMessage
		if (baseMessage == null) { // FIXME: remove after JobsInfoMessage implemented!!!
			return;
		}
		if (baseMessage instanceof JobsInfoMessage) {
			// TODO: implement
			return;
		}


		// case: ErrorMessage
		if (baseMessage instanceof ErrorMessage) {
			final String errorMessage = (String)baseMessage.getArgument(Message.ArgumentType.MSG);
			Log.e(TAG, errorMessage);

			if (state == State.CONNECTED) {
				completionCallback.onAuthenticationFailure(new ClientException(errorMessage));
			} else if (state == State.AUTHENTICATED) {
				completionCallback.onConversionFailure(new ClientException(errorMessage));
			}
			state = State.FAILED;
			return;
		}

		// case: ClientIDMessage & RenewClientIDMessage
		if (baseMessage instanceof ClientIDMessage) {
			Preconditions.checkState(state == State.CONNECTED);
			long oldClientID = clientID;
			clientID = (long)baseMessage.getArgument(Message.ArgumentType.CLIENT_ID);
			// TODO: shared preferences!
			state = State.AUTHENTICATED;

			if (clientID != oldClientID) {
				Log.d(TAG, "New Client ID: " + clientID);
			}
			authCallback.onSuccess();
			return;
		}

		// case: Unhandled base message!
		Log.e(TAG, "No handler implemented for base message: " + baseMessage);
	}
}
