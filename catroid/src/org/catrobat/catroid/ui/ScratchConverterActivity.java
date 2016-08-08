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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.common.base.Preconditions;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ScratchProjectPreviewData;
import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.ClientException;
import org.catrobat.catroid.scratchconverter.WebSocketClient;
import org.catrobat.catroid.R;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.scratchconverter.protocol.WebSocketMessageListener;
import org.catrobat.catroid.ui.dialogs.ScratchReconvertDialog;
import org.catrobat.catroid.ui.fragment.ScratchConverterSlidingUpPanelFragment;
import org.catrobat.catroid.ui.fragment.ScratchSearchProjectsListFragment;
import org.catrobat.catroid.ui.scratchconverter.ScratchConverterContextWrapper;
import org.catrobat.catroid.utils.DownloadUtil;
import org.catrobat.catroid.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScratchConverterActivity extends BaseActivity implements Client.ConnectAuthCallback {

	private static final String TAG = ScratchConverterActivity.class.getSimpleName();

	private ScratchSearchProjectsListFragment searchProjectsListFragment;
	private ScratchConverterSlidingUpPanelFragment converterSlidingUpPanelFragment;
	private SlidingUpPanelLayout slidingLayout;
	private WebSocketClient converterClient;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scratch_converter);
		setUpActionBar();

		final ExecutorService sharedExecutorService = Executors.newFixedThreadPool(Constants.WEBIMAGE_DOWNLOADER_POOL_SIZE);
		searchProjectsListFragment = (ScratchSearchProjectsListFragment)getFragmentManager().findFragmentById(
				R.id.fragment_scratch_search_projects_list);
		searchProjectsListFragment.setExecutorService(sharedExecutorService);
		converterSlidingUpPanelFragment = (ScratchConverterSlidingUpPanelFragment)getFragmentManager().findFragmentById(
				R.id.fragment_scratch_converter_sliding_up_panel);
		converterSlidingUpPanelFragment.setExecutorService(sharedExecutorService);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final long clientID = settings.getLong(Constants.SCRATCH_CONVERTER_CLIENT_ID_SHARED_PREFERENCE_NAME,
				Client.INVALID_CLIENT_ID);

		WebSocketMessageListener messageListener = new WebSocketMessageListener();
		messageListener.addBaseInfoViewListener(converterSlidingUpPanelFragment);
		messageListener.addGlobalJobConsoleViewListener(converterSlidingUpPanelFragment);

		final ScratchConverterActivity activity = this;
		converterClient = new WebSocketClient(this, clientID, messageListener, new Client.SimpleConvertCallback() {
			@Override
			public void onConversionReady(final Job job) {
				//lock.unlock();
				Log.i(TAG, "Conversion ready!");
			}

			@Override
			public void onConversionStart(final Job job) {
				Log.i(TAG, "Conversion started!");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						ToastUtil.showSuccess(activity, activity.getString(R.string.scratch_conversion_started));
					}
				});
			}

			@Override
			public void onConversionFinished(final Job job) {
				Log.i(TAG, "Conversion finished!");
			}

			@Override
			public void onDownloadReady(final Job job, final Client.DownloadFinishedListener downloadFinishedListener,
					final String downloadURL, final Date cachedUTCDate)
			{
				final Client.DownloadFinishedListener[] callbacks;
				callbacks = new Client.DownloadFinishedListener[] { downloadFinishedListener, converterSlidingUpPanelFragment };

				if (cachedUTCDate != null) {
					final ScratchReconvertDialog reconvertDialog = new ScratchReconvertDialog();
					reconvertDialog.setContext(activity);
					reconvertDialog.setCachedUTCDate(cachedUTCDate);
					reconvertDialog.setReconvertDialogCallback(new ScratchReconvertDialog.ReconvertDialogCallback() {
						@Override
						public void onDownloadExistingProgram() {
							downloadProgram(downloadURL, callbacks);
						}

						@Override
						public void onReconvertProgram() {
							if (Looper.getMainLooper().equals(Looper.myLooper())) {
								throw new AssertionError("You should not run this on the UI thread!");
							}
							converterClient.convertJob(job, false, true);
						}

						@Override
						public void onCancel() {
							converterClient.getMessageListener().onUserCanceledConversion(job.getJobID());
						}
					});
					reconvertDialog.show(activity.getFragmentManager(), ScratchReconvertDialog.DIALOG_FRAGMENT_TAG);
					return;
				}

				downloadProgram(downloadURL, callbacks);
			}

			@Override
			public void onConversionFailure(final Job job, final ClientException ex) {
				//lock.unlock();
				Log.e(TAG, "Conversion failed: " + ex.getMessage());
			}
		});
		searchProjectsListFragment.setMessageListener(messageListener);
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				converterClient.connectAndAuthenticate(activity);
			}
		}, 500);

		slidingLayout = (SlidingUpPanelLayout)findViewById(R.id.sliding_layout);
		slidingLayout.addPanelSlideListener(new SlidingUpPanelLayout.SimplePanelSlideListener() {
			@Override
			public void onPanelSlide(View panel, float slideOffset) {
				converterSlidingUpPanelFragment.rotateImageButton(slideOffset * 180.0f);
			}

			public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
				Log.d(TAG, "SlidingUpPanel state changed: " + newState.toString());
				switch (newState) {
					case EXPANDED:
						converterSlidingUpPanelFragment.rotateImageButton(180);
						break;
					case COLLAPSED:
						converterSlidingUpPanelFragment.rotateImageButton(0);
						break;
				}
			}
		});

		Log.i(TAG, "Scratch Converter Activity created");

		appendColoredBetaLabelToTitle(Color.RED);
		hideSlideUpPanelBar();
	}

	private void downloadProgram(final String downloadURL, final Client.DownloadFinishedListener[] downloadCallbacks) {
		final String baseUrl = Constants.SCRATCH_CONVERTER_BASE_URL;
		final String fullDownloadUrl = baseUrl.substring(0, baseUrl.length() - 1) + downloadURL;
		final ScratchConverterActivity activity = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "Start download: " + fullDownloadUrl);
				DownloadUtil
						.getInstance()
						.prepareDownloadAndStartIfPossible(activity, fullDownloadUrl, downloadCallbacks);
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Destroyed " + ScratchConverterActivity.class.getSimpleName());

		final Client client = converterClient;
		converterClient = null;
		if (client.isClosed() == false) {
			client.close();
		}
	}

	private void appendColoredBetaLabelToTitle(final int color) {
		final String title = getString(R.string.title_activity_scratch_converter);
		final String beta = getString(R.string.beta).toUpperCase();
		final SpannableString spanTitle = new SpannableString(title + " " + beta);
		final int begin = title.length() + 1;
		final int end = begin + beta.length();
		spanTitle.setSpan(new ForegroundColorSpan(color), begin, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		getActionBar().setTitle(spanTitle);
	}

	public void convertProjects(List<ScratchProjectPreviewData> projectList) {
		Log.i(TAG, "Converting projects:");
		final ScratchConverterContextWrapper contextWrapper = new ScratchConverterContextWrapper(this, converterClient);
		for (final ScratchProjectPreviewData projectData : projectList) {
			Log.i(TAG, projectData.getTitle());
			contextWrapper.convertProgram(projectData.getId(), projectData.getTitle(), projectData.getProjectImage(),
					false, false);
		}
		ToastUtil.showSuccess(this, R.string.scratch_conversion_started);
	}

	public boolean isSlideUpPanelEmpty() {
		return converterSlidingUpPanelFragment.hasJobs() == false;
	}

	public void showSlideUpPanelBar(final long delayMillis) {
		if (delayMillis > 0) {
			slidingLayout.postDelayed(new Runnable() {
				public void run() {
					int marginTop = getResources().getDimensionPixelSize(R.dimen.scratch_project_search_list_view_margin_top);
					int marginBottom = getResources().getDimensionPixelSize(R.dimen.scratch_project_search_list_view_margin_bottom);
					slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
					searchProjectsListFragment.setSearchResultsListViewMargin(0, marginTop, 0, marginBottom);
				}
			}, delayMillis);
		} else {
			slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
		}
	}

	public void hideSlideUpPanelBar() {
		int marginTop = getResources().getDimensionPixelSize(R.dimen.scratch_project_search_list_view_margin_top);
		searchProjectsListFragment.setSearchResultsListViewMargin(0, marginTop, 0, 0);
		slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_scratch_projects, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		handleShowDetails(searchProjectsListFragment.getShowDetails(),
				menu.findItem(R.id.menu_scratch_projects_show_details));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_scratch_projects_convert:
				Log.d(TAG, "Selected menu item 'convert'");
				searchProjectsListFragment.startConvertActionMode();
				break;
			case R.id.menu_scratch_projects_show_details:
				Log.d(TAG, "Selected menu item 'Show/Hide details'");
				handleShowDetails(!searchProjectsListFragment.getShowDetails(), item);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setUpActionBar() {
		final ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.title_activity_scratch_converter);
		actionBar.setHomeButtonEnabled(true);
	}

	private void handleShowDetails(boolean showDetails, MenuItem item) {
		searchProjectsListFragment.setShowDetails(showDetails);
		item.setTitle(showDetails ? R.string.hide_details : R.string.show_details);
	}

	public void displaySpeechRecognizer() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		startActivityForResult(intent, Constants.INTENT_REQUEST_CODE_SPEECH);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Constants.INTENT_REQUEST_CODE_SPEECH && resultCode == RESULT_OK) {
			List<String> results = data.getStringArrayListExtra(
					RecognizerIntent.EXTRA_RESULTS);
			String spokenText = results.get(0);
			searchProjectsListFragment.searchAndUpdateText(spokenText);
		} else if (requestCode == Constants.INTENT_REQUEST_CODE_CONVERT && resultCode == RESULT_OK) {
			if (! data.hasExtra(Constants.INTENT_SCRATCH_PROJECT_DATA)) {
				super.onActivityResult(requestCode, resultCode, data);
				return;
			}
			final ScratchProjectPreviewData projectData = data.getParcelableExtra(Constants.INTENT_SCRATCH_PROJECT_DATA);
			final List<ScratchProjectPreviewData> projectList = new ArrayList<>();
			projectList.add(projectData);
			convertProjects(projectList);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onSuccess(long clientID) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(Constants.SCRATCH_CONVERTER_CLIENT_ID_SHARED_PREFERENCE_NAME, clientID);
		editor.commit();
		Log.i(TAG, "Connection established (clientID: " + clientID + ")");
		final Activity activity = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ToastUtil.showSuccess(activity, R.string.connection_established);
			}
		});
		Preconditions.checkState(converterClient.isAuthenticated());
		converterClient.retrieveJobsInfo();
	}

	@Override
	public void onConnectionClosed(ClientException ex) {
		Log.d(TAG, "Connection closed!");
		final Activity activity = this;
		final String exceptionMessage = ex.getMessage();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (exceptionMessage != null) {
					Log.e(TAG, exceptionMessage);
					ToastUtil.showError(activity, R.string.connection_closed);
				} else {
					ToastUtil.showSuccess(activity, R.string.connection_closed);
				}
				finish();
			}
		});
	}

	@Override
	public void onConnectionFailure(ClientException ex) {
		Log.e(TAG, ex.getMessage());
		final Activity activity = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ToastUtil.showError(activity, R.string.connection_failed);
				finish();
			}
		});
	}

	@Override
	public void onAuthenticationFailure(ClientException ex) {
		Log.e(TAG, ex.getMessage());
		final Activity activity = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ToastUtil.showError(activity, R.string.authentication_failed);
				finish();
			}
		});
	}
}
