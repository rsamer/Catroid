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

package org.catrobat.catroid.uitest.ui.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;

import com.google.android.gms.common.images.WebImage;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ScratchProgramData;
import org.catrobat.catroid.common.ScratchSearchResult;
import org.catrobat.catroid.scratchconverter.Client;
import org.catrobat.catroid.scratchconverter.ConversionManager;
import org.catrobat.catroid.scratchconverter.ScratchConversionManager;
import org.catrobat.catroid.scratchconverter.protocol.Job;
import org.catrobat.catroid.ui.ScratchConverterActivity;
import org.catrobat.catroid.ui.fragment.ScratchConverterSlidingUpPanelFragment;
import org.catrobat.catroid.ui.fragment.SearchScratchSearchProjectsListFragment;
import org.catrobat.catroid.uitest.util.BaseActivityInstrumentationTestCase;
import org.catrobat.catroid.web.ScratchDataFetcher;
import org.catrobat.catroid.web.WebconnectionException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ScratchConverterActivityTest extends BaseActivityInstrumentationTestCase<ScratchConverterActivity> {

	private ScratchDataFetcher fetcherMock;
	private ScratchSearchResult defaultProgramsSearchResult;
	private List<ScratchProgramData> expectedDefaultProgramsList;
	private ConversionManager conversionManager;
	private Client clientMock;

	public ScratchConverterActivityTest() {
		super(ScratchConverterActivity.class);
	}

	private ScratchConverterSlidingUpPanelFragment getSlidingUpPanelFragment() {
		ScratchConverterActivity activity = (ScratchConverterActivity) solo.getCurrentActivity();
		return activity.getConverterSlidingUpPanelFragment();
	}

	private SearchScratchSearchProjectsListFragment getSearchProjectsListFragment() {
		ScratchConverterActivity activity = (ScratchConverterActivity) solo.getCurrentActivity();
		return activity.getSearchProjectsListFragment();
	}

	@Override
	public void setUp() throws Exception {
		// prepare mocks
		final Uri firstImageURL = Uri.parse("https://cdn2.scratch.mit.edu/get_image/project/10205819_480x360.png");
		expectedDefaultProgramsList = new ArrayList<ScratchProgramData>() {{
			add(new ScratchProgramData(10205819, "Program 1", "Owner 1", new WebImage(firstImageURL, 150, 150)));
			add(new ScratchProgramData(10205820, "Program 2", "Owner 2", null));
			add(new ScratchProgramData(10205821, "Program 3", "Owner 3", null));
		}};
		defaultProgramsSearchResult = new ScratchSearchResult(expectedDefaultProgramsList, "", 0);

		fetcherMock = Mockito.mock(ScratchDataFetcher.class);
		when(fetcherMock.fetchDefaultScratchPrograms()).thenReturn(defaultProgramsSearchResult);

		clientMock = Mockito.mock(Client.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				conversionManager = (ConversionManager) invocation.getArguments()[0];
				return null;
			}
		}).when(clientMock).connectAndAuthenticate(any(ScratchConversionManager.class));

		ScratchConverterActivity.setDataFetcher(fetcherMock);
		ScratchConverterActivity.setClient(clientMock);
		super.setUp();
		verify(clientMock, times(1)).setConvertCallback(any(Client.ConvertCallback.class));
		verify(clientMock, times(1)).connectAndAuthenticate(any(ScratchConversionManager.class));
		verifyNoMoreInteractions(clientMock);
	}

	public void testSlidingUpPanelLayoutShouldBeHiddenAtTheBeginning() {
		SlidingUpPanelLayout slidingLayout = (SlidingUpPanelLayout)getActivity().findViewById(R.id.sliding_layout);
		assertEquals(SlidingUpPanelLayout.PanelState.HIDDEN, slidingLayout.getPanelState());
		assertFalse("Fragment should not contain jobs at the beginning", getSlidingUpPanelFragment().hasVisibleJobs());
	}

	public void testShouldSaveClientIDToSharedPreferencesAfterSuccessfullyConnectedAndAuthenticated() {
		// store invalid client ID in shared preferences
		final long oldClientID = Client.INVALID_CLIENT_ID;
		final SharedPreferences settings;
		settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(Constants.SCRATCH_CONVERTER_CLIENT_ID_SHARED_PREFERENCE_NAME, oldClientID);
		editor.commit();

		final long expectedNewClientID = 123;

		when(clientMock.isAuthenticated()).thenReturn(true);
		conversionManager.onSuccess(expectedNewClientID);
		verify(clientMock, times(1)).isAuthenticated();

		// now check if clientID has been updated in shared preferences
		final long storedClientID = settings.getLong(Constants.SCRATCH_CONVERTER_CLIENT_ID_SHARED_PREFERENCE_NAME,
				Client.INVALID_CLIENT_ID);
		assertEquals("ClientID has not been saved to SharedPreferences", expectedNewClientID, storedClientID);
	}

	public void testShouldCallRetrieveInfoAfterSuccessfullyConnectedAndAuthenticated() {
		final long expectedNewClientID = 123;
		when(clientMock.isAuthenticated()).thenReturn(true);
		conversionManager.onSuccess(expectedNewClientID);
		verify(clientMock, times(1)).isAuthenticated();
		verify(clientMock, times(1)).retrieveInfo();
		verifyNoMoreInteractions(clientMock);
	}

	public void testShouldDisplayUpdatePocketCodeDialogIfServersCatrobatLanguageVersionIsNewer() {
		final float serverCatrobatLanguageVersion = Constants.CURRENT_CATROBAT_LANGUAGE_VERSION + 0.01f;
		when(clientMock.isAuthenticated()).thenReturn(true);
		conversionManager.onInfo(serverCatrobatLanguageVersion, new Job[] {});

		solo.sleep(200);

		assertTrue("Server's Catrobat Language version is newer than the app's version, but no warning dialog shown!",
				solo.searchText(getActivity().getString(R.string.error_scratch_converter_outdated_pocketcode_version)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// SlidePanel tests (SlidingUpPanelFragment)
	//------------------------------------------------------------------------------------------------------------------
	public void testShouldShowSlidePanelBarAfterJobsInfoEventReceivedWithJobs() {
		final Uri firstImageURL = Uri.parse("https://cdn2.scratch.mit.edu/get_image/project/10205819_480x360.png");
		final Uri secondImageURL = Uri.parse("https://cdn2.scratch.mit.edu/get_image/project/10205821_480x360.png");

		final Job firstJob = new Job(10205819, "Program 1", new WebImage(firstImageURL, 480, 360));
		firstJob.setState(Job.State.FINISHED);

		final Job secondJob = new Job(10205821, "Program 2", new WebImage(secondImageURL, 480, 360));
		secondJob.setState(Job.State.RUNNING);

		final Job[] expectedJobs = new Job[] { firstJob, secondJob };

		SlidingUpPanelLayout slidingLayout = (SlidingUpPanelLayout)getActivity().findViewById(R.id.sliding_layout);
		assertEquals("SlidingUpPanelBar should be hidden by default!",
				SlidingUpPanelLayout.PanelState.HIDDEN, slidingLayout.getPanelState());

		when(clientMock.isAuthenticated()).thenReturn(true);
		conversionManager.onInfo(Constants.CURRENT_CATROBAT_LANGUAGE_VERSION, expectedJobs);

		solo.sleep(1_000);

		assertEquals("Must show SlidingUpPanelBar when there are jobs!",
				SlidingUpPanelLayout.PanelState.COLLAPSED, slidingLayout.getPanelState());
	}

	public void testSlidingUpPanelLayoutShouldBeShownAfterReceivedInfo() {
		SlidingUpPanelLayout slidingLayout = (SlidingUpPanelLayout)getActivity().findViewById(R.id.sliding_layout);
		assertEquals(SlidingUpPanelLayout.PanelState.HIDDEN, slidingLayout.getPanelState());
		assertFalse("Fragment should not contain jobs at the beginning", getSlidingUpPanelFragment().hasVisibleJobs());
	}

	public void testShouldNotShowSlidePanelBarAfterJobsInfoEventReceivedWithNoJobs() {
		final Job[] expectedJobs = new Job[] {};

		SlidingUpPanelLayout slidingLayout = (SlidingUpPanelLayout)getActivity().findViewById(R.id.sliding_layout);
		assertEquals("SlidingUpPanelBar should be hidden by default!",
				SlidingUpPanelLayout.PanelState.HIDDEN, slidingLayout.getPanelState());

		when(clientMock.isAuthenticated()).thenReturn(true);
		conversionManager.onInfo(Constants.CURRENT_CATROBAT_LANGUAGE_VERSION, expectedJobs);

		solo.sleep(400);

		assertEquals("SlidingUpPanelBar should remain hidden when there are no jobs!",
				SlidingUpPanelLayout.PanelState.HIDDEN, slidingLayout.getPanelState());
	}

	public void testShouldNotShowSlidePanelBarAfterJobsInfoEventReceivedWithOnlyInvisibleUnscheduledJobs() {
		final Job job = new Job(10205819, "Program 1", null);
		job.setState(Job.State.UNSCHEDULED);

		final Job[] expectedJobs = new Job[] {job};

		when(clientMock.isAuthenticated()).thenReturn(true);
		conversionManager.onInfo(Constants.CURRENT_CATROBAT_LANGUAGE_VERSION, expectedJobs);

		solo.sleep(400);

		SlidingUpPanelLayout slidingLayout = (SlidingUpPanelLayout)getActivity().findViewById(R.id.sliding_layout);
		assertEquals("Should not show SlidingUpPanelBar when there are no jobs!",
				SlidingUpPanelLayout.PanelState.HIDDEN, slidingLayout.getPanelState());
	}

	public void testShouldDisplayAllJobsAfterJobsInfoEventReceived() {
		final Uri firstImageURL = Uri.parse("https://cdn2.scratch.mit.edu/get_image/project/10205819_480x360.png");
		final Uri secondImageURL = Uri.parse("https://cdn2.scratch.mit.edu/get_image/project/10205821_480x360.png");

		final Job firstJob = new Job(10205819, "Program 1", new WebImage(firstImageURL, 480, 360));
		firstJob.setState(Job.State.FINISHED);

		final Job secondJob = new Job(10205821, "Program 2", new WebImage(secondImageURL, 480, 360));
		secondJob.setState(Job.State.RUNNING);

		final Job[] expectedJobs = new Job[] { firstJob, secondJob };

		when(clientMock.isAuthenticated()).thenReturn(true);
		conversionManager.onInfo(Constants.CURRENT_CATROBAT_LANGUAGE_VERSION, expectedJobs);

		solo.sleep(400);

		final ScratchConverterActivity activity = (ScratchConverterActivity) solo.getCurrentActivity();

		final RelativeLayout runningJobsList = (RelativeLayout) activity.findViewById(R.id.scratch_conversion_list);
		final ListView runningJobsListView = (ListView) activity.findViewById(R.id.scratch_conversion_list_view);

		final RelativeLayout finishedFailedJobsList = (RelativeLayout) activity.findViewById(
				R.id.scratch_converted_programs_list);
		final ListView finishedFailedJobsListView = (ListView) activity.findViewById(
				R.id.scratch_converted_programs_list_view);

		assertTrue("Fragment does not contain any jobs", getSlidingUpPanelFragment().hasVisibleJobs());
		assertEquals("RunningJobsList must be visible", View.VISIBLE, runningJobsList.getVisibility());
		assertEquals("FinishedFailedJobsList must be visible", View.VISIBLE, finishedFailedJobsList.getVisibility());
		assertEquals("Number of list items of running jobs not as expected",
				1, runningJobsListView.getAdapter().getCount());
		assertEquals("Number of list items of finished or failed jobs not as expected",
				1, finishedFailedJobsListView.getAdapter().getCount());
	}

	public void testSlidePanelBarShouldBeExpandableAfterJobsInfoEventReceivedWithJobs() {
		final Uri firstImageURL = Uri.parse("https://cdn2.scratch.mit.edu/get_image/project/10205819_480x360.png");
		final Job job = new Job(10205819, "Program 1", new WebImage(firstImageURL, 480, 360));
		job.setState(Job.State.FINISHED);

		final Job[] expectedJobs = new Job[] { job };

		when(clientMock.isAuthenticated()).thenReturn(true);
		conversionManager.onInfo(Constants.CURRENT_CATROBAT_LANGUAGE_VERSION, expectedJobs);

		solo.sleep(1_000);

		final SlidingUpPanelLayout slidingLayout = (SlidingUpPanelLayout)getActivity().findViewById(R.id.sliding_layout);
		final RelativeLayout dragView = (RelativeLayout) getActivity().findViewById(R.id.scratch_convert_sliding_up_panel_bar);

		assertEquals("Must show SlidingUpPanelBar when there are jobs!",
				SlidingUpPanelLayout.PanelState.COLLAPSED, slidingLayout.getPanelState());

		// now expand the panel bar by simply tapping on it
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
			}
		});

		solo.sleep(2_000);

		assertEquals("Must show SlidingUpPanelBar when there are jobs!",
				SlidingUpPanelLayout.PanelState.EXPANDED, slidingLayout.getPanelState());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Search tests (SearchScratchSearchProjectsListFragment)
	//------------------------------------------------------------------------------------------------------------------
	public void testCheckIfAllDefaultProgramsArePresentAtTheBeginning() {
		SearchView searchView = (SearchView) solo.getCurrentActivity().findViewById(R.id.search_view_scratch);
		ListView searchListView = (ListView) solo.getCurrentActivity().findViewById(R.id.list_view_search_scratch);

		solo.sleep(200);

		assertEquals("Search view text must be empty at the beginning", 0, searchView.getQuery().length());
		assertEquals("Number of viewed default programs differs",
				expectedDefaultProgramsList.size(), searchListView.getAdapter().getCount());
		for (int index = 0; index < searchListView.getAdapter().getCount(); index++) {
			assertEquals("Default programs at index " + index + " differs from expected default program",
					expectedDefaultProgramsList.get(index), searchListView.getAdapter().getItem(index));
		}
	}

	public void testSearch() throws InterruptedIOException, WebconnectionException, InterruptedException {
		final SearchView searchView = (SearchView) solo.getCurrentActivity().findViewById(R.id.search_view_scratch);
		ListView searchListView = (ListView) solo.getCurrentActivity().findViewById(R.id.list_view_search_scratch);

		final String searchQuery = "test";

		ScratchSearchResult searchResult = new ScratchSearchResult(new ArrayList<ScratchProgramData>() {{
			add(new ScratchProgramData(1, "Program 1", "Owner 1", null));
			final Uri secondImageURL = Uri.parse("https://cdn2.scratch.mit.edu/get_image/project/2_480x360.png");
			add(new ScratchProgramData(2, "Program 2", "Owner 2", new WebImage(secondImageURL, 480, 360)));
		}}, searchQuery, 0);

		when(fetcherMock.scratchSearch(any(String.class), any(Integer.class), any(Integer.class)))
				.thenReturn(searchResult);

		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				searchView.setQuery(searchQuery, true);
				synchronized (this) {
					notify();
				}
			}
		};
		synchronized (runnable) {
			getActivity().runOnUiThread(runnable);
			runnable.wait();
		}

		solo.sleep(200);

		assertEquals("Search view text must be empty at the beginning", searchQuery, searchView.getQuery().toString());
		assertEquals("Number of viewed default programs differs",
				searchResult.getProgramDataList().size(), searchListView.getAdapter().getCount());
		for (int index = 0; index < searchListView.getAdapter().getCount(); index++) {
			assertEquals("Default programs at index " + index + " differs from expected default program",
					searchResult.getProgramDataList().get(index), searchListView.getAdapter().getItem(index));
		}
	}


}

		/*
		// TODO: use this for ScratchConverterActivityTest
		final Object[] convertMethodParams = { null, null };
		WebSocketClient clientMock = Mockito.mock(WebSocketClient.class);
		doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) {
				assertEquals(invocation.getArguments().length, 2);
				convertMethodParams[0] = invocation.getArguments()[0];
				convertMethodParams[1] = invocation.getArguments()[1];
				return null;
			}
		}).when(clientMock).convertProject(any(Long.class), any(String.class));
		ScratchProgramDetailsActivity.setConverterClient(clientMock);
				assertTrue(convertMethodParams[0] instanceof Long);
				assertTrue(convertMethodParams[1] instanceof String);
				assertEquals(convertMethodParams[0], programData.getId());
				assertEquals(convertMethodParams[1], programData.getTitle());
		*/
