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

package org.catrobat.catroid.ui;

import android.app.Activity;
import android.content.ContextWrapper;
import android.util.Log;

import org.catrobat.catroid.R;
import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.utils.ToastUtil;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ScratchConverterContextWrapper extends ContextWrapper {

	private static final String TAG = ScratchConverterContextWrapper.class.getSimpleName();
	//private static final Lock lock = new ReentrantLock();

	private final Activity activity;
	private final Client converterClient;

	public ScratchConverterContextWrapper(Activity activity, Client converterClient) {
		super(activity);
		this.activity = activity;
		this.converterClient = converterClient;
	}

	public void convertProgram(final long jobID, final String projectTitle, final Client.CompletionCallback completionCallback) {

		// TODO: make sure NOT running on UI-thread!!
		final Job job = new Job(jobID, projectTitle);

		//lock.lock();
		converterClient.convertJob(job, new Client.CompletionCallback() {
			@Override
			public void onConversionReady() {
				//lock.unlock();
				Log.i(TAG, "Conversion ready!");
				if (completionCallback != null) {
					completionCallback.onConversionReady();
				}
			}

			@Override
			public void onConversionStart() {
				Log.i(TAG, "Conversion started!");
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						ToastUtil.showSuccess(activity, activity.getString(R.string.scratch_conversion_started));
					}
				});
				if (completionCallback != null) {
					completionCallback.onConversionStart();
				}
			}

			@Override
			public void onConversionFinished() {
				Log.i(TAG, "Conversion finished!");
				if (completionCallback != null) {
					completionCallback.onConversionFinished();
				}
			}

			@Override
			public void onConnectionFailure(Client.ClientException ex) {
				//lock.unlock();
				Log.e(TAG, "Connection failed: " + ex.getLocalizedMessage());
				if (completionCallback != null) {
					completionCallback.onConnectionFailure(ex);
				}
			}

			@Override
			public void onAuthenticationFailure(Client.ClientException ex) {
				//lock.unlock();
				Log.e(TAG, "Authentication failed: " + ex.getLocalizedMessage());
				if (completionCallback != null) {
					completionCallback.onAuthenticationFailure(ex);
				}
			}

			@Override
			public void onConversionFailure(Client.ClientException ex) {
				//lock.unlock();
				Log.e(TAG, "Conversion failed: " + ex.getLocalizedMessage());
				if (completionCallback != null) {
					completionCallback.onConversionFailure(ex);
				}
			}
		});
	}

}
