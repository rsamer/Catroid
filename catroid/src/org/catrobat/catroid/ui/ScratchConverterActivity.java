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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.WebSocketClient;
import org.catrobat.catroid.R;
import org.catrobat.catroid.scratchconverter.protocol.WebSocketMessageListener;
import org.catrobat.catroid.ui.fragment.ScratchConverterSlidingUpPanelFragment;
import org.catrobat.catroid.ui.fragment.ScratchSearchProjectsListFragment;
import org.catrobat.catroid.ui.scratchconverter.JobConsoleViewListener;

import java.util.List;

public class ScratchConverterActivity extends BaseActivity {

	private static final String TAG = ScratchConverterActivity.class.getSimpleName();

	private ScratchSearchProjectsListFragment searchProjectsListFragment;
	private ScratchConverterSlidingUpPanelFragment converterSlidingUpPanelFragment;
	private static final int SPEECH_REQUEST_CODE = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scratch_converter);
		setUpActionBar();

		searchProjectsListFragment = (ScratchSearchProjectsListFragment)getFragmentManager().findFragmentById(
				R.id.fragment_scratch_search_projects_list);
		converterSlidingUpPanelFragment = (ScratchConverterSlidingUpPanelFragment)getFragmentManager().findFragmentById(
				R.id.fragment_scratch_converter_sliding_up_panel);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final long clientID = settings.getLong(Constants.SCRATCH_CONVERTER_CLIENT_ID_SHARED_PREFERENCE_NAME,
				Client.INVALID_CLIENT_ID);

		WebSocketMessageListener messageListener = new WebSocketMessageListener();
		messageListener.addBaseInfoViewListener(converterSlidingUpPanelFragment);
		messageListener.addGlobalJobConsoleViewListener(converterSlidingUpPanelFragment);
		searchProjectsListFragment.setConverterClient(new WebSocketClient(clientID, messageListener));
		Log.i(TAG, "Scratch Converter Activity created");

		appendColoredBetaLabelToTitle(Color.RED);
		//hideSlideUpPanelBar();
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
		// Create an intent that can start the Speech Recognizer activity
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		// Start the activity, the intent will be populated with the speech text
		startActivityForResult(intent, SPEECH_REQUEST_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		// This callback is invoked when the Speech Recognizer returns.
		// This is where you process the intent and extract the speech text from the intent.
		if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
			List<String> results = data.getStringArrayListExtra(
					RecognizerIntent.EXTRA_RESULTS);
			String spokenText = results.get(0);
			searchProjectsListFragment.searchAndUpdateText(spokenText);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
