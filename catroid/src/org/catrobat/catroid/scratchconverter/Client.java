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

public interface Client {
	class ClientException extends Exception {
		public ClientException(String detailMessage) {
			super(detailMessage);
		}

		public ClientException(Throwable throwable) {
			super(throwable);
		}
	}

	interface CompletionCallback {
		void onConversionReady();
		void onConversionStart();
		void onConversionFinished();
		void onConnectionFailure(ClientException ex);
		void onAuthenticationFailure(ClientException ex);
		void onConversionFailure(ClientException ex);
	}

	enum State { CONNECTED, NOT_CONNECTED, AUTHENTICATED, FAILED }

	MessageListener getMessageListener();
	boolean isJobInProgress(final long jobID);
	void convertJob(final Job job, final CompletionCallback callback);
}
