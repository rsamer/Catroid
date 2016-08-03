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

import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.scratchconverter.protocol.MessageListener;

import java.util.Date;

public interface Client {

	enum State { NOT_CONNECTED, CONNECTED, CONNECTED_AUTHENTICATED }
	long INVALID_CLIENT_ID = -1;

	MessageListener getMessageListener();
	boolean isClosed();
	boolean isAuthenticated();
	void connectAndAuthenticate(ConnectAuthCallback connectAuthCallback);
	void retrieveJobsInfo();
	void convertJob(Job job, boolean verbose, boolean force);
	void close();

	// callbacks
	interface ConnectAuthCallback {
		void onSuccess(long clientID);
		void onConnectionClosed(ClientException ex);
		void onConnectionFailure(ClientException ex);
		void onAuthenticationFailure(ClientException ex);
	}

	interface ConvertCallback {
		void onConversionReady(Job job);
		void onConversionStart(Job job);
		void onConversionFinished(Job job);
		void onDownloadReady(Job job, DownloadFinishedListener downloadFinishedListener, String downloadURL,
				Date cachedDate);
		void onConversionFailure(Job job, ClientException ex);
	}

	interface DownloadFinishedListener {
		void onDownloadFinished(String catrobatProgramName, String url);
		void onUserCanceledDownload(String url);
	}

	// convenient callback base class
	class SimpleConvertCallback implements ConvertCallback {
		@Override
		public void onConversionReady(Job job) {}

		@Override
		public void onConversionStart(Job job) {}

		@Override
		public void onConversionFinished(Job job) {}

		@Override
		public void onDownloadReady(Job job, DownloadFinishedListener downloadFinishedListener, String downloadURL,
				Date cachedDate) {}

		@Override
		public void onConversionFailure(Job job, ClientException ex) {}
	}

}
