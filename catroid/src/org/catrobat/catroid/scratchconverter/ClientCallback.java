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

public interface ClientCallback {
	class ClientException extends Exception {
		public ClientException(String detailMessage) {
			super(detailMessage);
		}

		public ClientException(Throwable throwable) {
			super(throwable);
		}
	}

	void onConversionReady();
	void onConversionStart();
	void onConversionFinished();
	void onClientIDChanged(long newClientID);
	void onDownloadReady(String downloadURL);
	void onConnectionFailure(ClientException ex);
	void onAuthenticationFailure(ClientException ex);
	void onConversionFailure(ClientException ex);

	abstract class BaseCallback implements ClientCallback {
		@Override
		public void onConversionReady() {}

		@Override
		public void onConversionStart() {}

		@Override
		public void onConversionFinished() {}

		@Override
		public void onClientIDChanged(final long newClientID) {}

		@Override
		public void onDownloadReady(final String downloadURL) {}

		@Override
		public void onConnectionFailure(final ClientException ex) {}

		@Override
		public void onAuthenticationFailure(final ClientException ex) {}

		@Override
		public void onConversionFailure(final ClientException ex) {}
	}
}
