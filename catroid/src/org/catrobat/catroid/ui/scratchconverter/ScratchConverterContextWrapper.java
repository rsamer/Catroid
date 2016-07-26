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

package org.catrobat.catroid.ui.scratchconverter;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.images.WebImage;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.ClientCallback;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.utils.DownloadUtil;
import org.catrobat.catroid.utils.ToastUtil;

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

	public void convertProgram(final long jobID, final String programTitle, final WebImage programImage,
			final boolean verbose, final ClientCallback.SimpleClientCallback callback) {

		// TODO: make sure NOT running on UI-thread!!
		final Job job = new Job(jobID, programTitle, programImage);

		//lock.lock();
		converterClient.convertJob(job, verbose, new ClientCallback() {
			@Override
			public void onConversionReady() {
				//lock.unlock();
				Log.i(TAG, "Conversion ready!");
				if (callback != null) {
					callback.onConversionReady();
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
				if (callback != null) {
					callback.onConversionStart();
				}
			}

			@Override
			public void onConversionFinished() {
				Log.i(TAG, "Conversion finished!");
				if (callback != null) {
					callback.onConversionFinished();
				}
			}

			@Override
			public void onClientIDChanged(final long newClientID) {
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
				SharedPreferences.Editor editor = settings.edit();
				editor.putLong(Constants.SCRATCH_CONVERTER_CLIENT_ID_SHARED_PREFERENCE_NAME, newClientID);
				editor.commit();
			}

			@Override
			public void onDownloadReady(final String downloadURL) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
							Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
							r.play();
						} catch (Exception e) {
							e.printStackTrace();
						}
						//DownloadUtil.getInstance().prepareDownloadAndStartIfPossible(activity, downloadURL);
					}
				});
			}

			@Override
			public void onConnectionFailure(final ClientException ex) {
				//lock.unlock();
				Log.e(TAG, "Connection failed: " + ex.getLocalizedMessage());
				if (callback != null) {
					callback.onConnectionFailure(ex);
				}
			}

			@Override
			public void onAuthenticationFailure(final ClientException ex) {
				//lock.unlock();
				Log.e(TAG, "Authentication failed: " + ex.getLocalizedMessage());
				if (callback != null) {
					callback.onAuthenticationFailure(ex);
				}
			}

			@Override
			public void onConversionFailure(final ClientException ex) {
				//lock.unlock();
				Log.e(TAG, "Conversion failed: " + ex.getLocalizedMessage());
				if (callback != null) {
					callback.onConversionFailure(ex);
				}
			}
		});
	}

}
