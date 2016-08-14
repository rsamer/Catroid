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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ScratchProgramData;
import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.ConversionManager;
import org.catrobat.catroid.scratchconverter.WebSocketClient;
import org.catrobat.catroid.R;
import org.catrobat.catroid.scratchconverter.protocol.WebSocketMessageListener;
import org.catrobat.catroid.ui.fragment.ScratchConverterSlidingUpPanelFragment;
import org.catrobat.catroid.ui.fragment.SearchScratchSearchProjectsListFragment;
import org.catrobat.catroid.scratchconverter.ScratchConversionManager;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.web.ScratchDataFetcher;
import org.catrobat.catroid.web.ServerCalls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScratchConverterActivity extends BaseActivity {

	private static final String TAG = ScratchConverterActivity.class.getSimpleName();

	// to avoid using singleton in fragment
	private static ScratchDataFetcher dataFetcher = ServerCalls.getInstance();

	private SearchScratchSearchProjectsListFragment searchProjectsListFragment;
	private ScratchConverterSlidingUpPanelFragment converterSlidingUpPanelFragment;
	private SlidingUpPanelLayout slidingLayout;
	private ConversionManager conversionManager;

	// dependency-injection for testing with mock object
	public static void setDataFetcher(final ScratchDataFetcher fetcher) {
		dataFetcher = fetcher;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scratch_converter);
		setUpActionBar();

		final ExecutorService executorService = Executors.newFixedThreadPool(Constants.WEBIMAGE_DOWNLOADER_POOL_SIZE);
		searchProjectsListFragment = (SearchScratchSearchProjectsListFragment)getFragmentManager().findFragmentById(
				R.id.fragment_scratch_search_projects_list);
		searchProjectsListFragment.setDataFetcher(dataFetcher);
		searchProjectsListFragment.setExecutorService(executorService);
		converterSlidingUpPanelFragment = (ScratchConverterSlidingUpPanelFragment)getFragmentManager().findFragmentById(
				R.id.fragment_scratch_converter_sliding_up_panel);
		converterSlidingUpPanelFragment.setExecutorService(executorService);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final long clientID = settings.getLong(Constants.SCRATCH_CONVERTER_CLIENT_ID_SHARED_PREFERENCE_NAME,
				Client.INVALID_CLIENT_ID);

		final WebSocketMessageListener messageListener = new WebSocketMessageListener();
		final Client client = new WebSocketClient(clientID, messageListener);
		conversionManager = new ScratchConversionManager(this, client, false);
		conversionManager.addDownloadFinishedCallback(converterSlidingUpPanelFragment);
		conversionManager.setCurrentActivity(this);
		conversionManager.addBaseInfoViewListener(converterSlidingUpPanelFragment);
		conversionManager.addGlobalJobConsoleViewListener(converterSlidingUpPanelFragment);
		searchProjectsListFragment.setConversionManager(conversionManager);

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

		appendColoredBetaLabelToTitle(Color.RED);
		hideSlideUpPanelBar();

		conversionManager.connectAndAuthenticate();
		Log.i(TAG, "Scratch Converter Activity created");
	}

	@Override
	protected void onStart() {
		super.onStart();
		conversionManager.setCurrentActivity(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Destroyed " + TAG);
		conversionManager.shutdown();
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

	public void convertProjects(List<ScratchProgramData> projectList) {
		for (ScratchProgramData programData : projectList) {
			Log.i(TAG, "Converting program: " + programData.getTitle());
			conversionManager.convertProgram(programData.getId(), programData.getTitle(), programData.getImage(), false);
		}
		ToastUtil.showSuccess(this, R.string.scratch_conversion_started);
	}

	public boolean isSlideUpPanelEmpty() {
		return !converterSlidingUpPanelFragment.hasJobs();
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
			final ScratchProgramData projectData = data.getParcelableExtra(Constants.INTENT_SCRATCH_PROJECT_DATA);
			final List<ScratchProgramData> projectList = new ArrayList<>();
			projectList.add(projectData);
			convertProjects(projectList);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
