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

import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.images.WebImage;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.ScratchProgramData;
import org.catrobat.catroid.common.ScratchProgramData.ScratchRemixProjectData;
import org.catrobat.catroid.common.ScratchProgramPreviewData;
import org.catrobat.catroid.scratchconverter.protocol.MessageListener;
import org.catrobat.catroid.transfers.FetchScratchProgramDetailsTask.ScratchDataFetcher;
import org.catrobat.catroid.ui.ScratchProgramDetailsActivity;
import org.catrobat.catroid.ui.adapter.ScratchRemixedProjectAdapter;
import org.catrobat.catroid.uitest.util.BaseActivityInstrumentationTestCase;
import org.catrobat.catroid.utils.Utils;
import org.catrobat.catroid.web.WebScratchProgramException;
import org.catrobat.catroid.web.WebconnectionException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import uk.co.deanwild.flowtextview.FlowTextView;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class ScratchProjectDetailsActivityTest extends BaseActivityInstrumentationTestCase<ScratchProgramDetailsActivity> {
	private static final String TAG = ScratchProjectDetailsActivityTest.class.getSimpleName();

	private ScratchProgramPreviewData projectPreviewData;
	private ScratchProgramData projectData;
	private ScratchRemixProjectData remixedProjectData;

	private ScratchDataFetcher fetcherMock;

	public ScratchProjectDetailsActivityTest()
			throws InterruptedIOException, WebconnectionException, WebScratchProgramException
	{
		super(ScratchProgramDetailsActivity.class);

		List<String> tags = new ArrayList<String>() {{
				add("animations");
				add("castle");
		}};
		long projectID = 10205819;
		projectData = new ScratchProgramData(projectID, "Dancin' in the Castle", "jschombs",
				"Click the flag to run the stack. Click the space bar to change it up!", "First project on Scratch! "
				+ "This was great.", 1_723_123, 37_239, 11, new Date(), new Date(), tags,
				ScratchProgramData.VisibilityState.PUBLIC);

		Uri uri = Uri.parse("https://cdn2.scratch.mit.edu/get_image/project/10211023_144x108.png?v=1368486334.0");
		remixedProjectData = new ScratchRemixProjectData(10211023, "Dancin' in the Castle remake",
				"Amanda69", new WebImage(uri, 150, 150));
		projectData.addRemixProject(remixedProjectData);
		projectPreviewData = new ScratchProgramPreviewData(projectID, projectData.getTitle(), "May 13, 2013 ... Click "
				+ "the flag to run the stack.");

		// mocks
		fetcherMock = Mockito.mock(ScratchDataFetcher.class);
		when(fetcherMock.fetchScratchProgramDetails(any(Long.class))).thenReturn(projectData);

		final MessageListener messageListenerMock = Mockito.mock(MessageListener.class);
		doAnswer(new Answer<Void>() {
			public Void answer(InvocationOnMock invocation) {
				assertNotNull("No arguments for addJobConsoleViewListener call given", invocation.getArguments());
				assertEquals(invocation.getArguments().length, 2);
				assertEquals(invocation.getArguments()[0], projectPreviewData.getId());
				assertTrue(invocation.getArguments()[1] instanceof ScratchProgramDetailsActivity);
				return null;
			}
		}).when(messageListenerMock).addJobConsoleViewListener(any(Long.class), any(ScratchProgramDetailsActivity.class));

		// dependency injection
		ScratchProgramDetailsActivity.setDataFetcher(fetcherMock);
		ScratchProgramDetailsActivity.setMessageListener(messageListenerMock);
		ScratchProgramDetailsActivity.setExecutorService(Executors.newFixedThreadPool(Constants.WEBIMAGE_DOWNLOADER_POOL_SIZE));

		/*
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
				assertEquals(convertMethodParams[0], projectData.getId());
				assertEquals(convertMethodParams[1], projectData.getTitle());
		*/
	}

	@Override
	public ScratchProgramDetailsActivity getActivity() {
		Intent intent = new Intent();
		intent.putExtra(Constants.INTENT_SCRATCH_PROJECT_DATA, (Parcelable) projectPreviewData);
		setActivityIntent(intent);
		return super.getActivity();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testIsTitleViewPresentAndHasCorrectContent() {
		final View titleView = solo.getView(R.id.scratch_project_title);
		assertEquals(titleView.getVisibility(), View.VISIBLE);
		assertTrue(titleView instanceof TextView);
		assertEquals(projectPreviewData.getTitle(), ((TextView) titleView).getText());
	}

	public void testIsOwnerViewPresentAndHasCorrectContent() {
		final View ownerView = solo.getView(R.id.scratch_project_owner);
		assertEquals(ownerView.getVisibility(), View.VISIBLE);
		assertTrue(ownerView instanceof TextView);
		assertEquals(getActivity().getString(R.string.by) + " " + projectData.getOwner(), ((TextView) ownerView).getText());
	}

	public void testIsInstructionsViewPresentAndHasCorrectContent() {
		final View instructionsLabelView = solo.getView(R.id.scratch_project_instructions_label);
		assertEquals(instructionsLabelView.getVisibility(), View.VISIBLE);
		assertTrue(instructionsLabelView instanceof TextView);
		assertEquals(getActivity().getString(R.string.instructions), ((TextView) instructionsLabelView).getText());

		final View instructionsView = solo.getView(R.id.scratch_project_instructions_flow_text);
		assertEquals(instructionsView.getVisibility(), View.VISIBLE);
		assertTrue(instructionsView instanceof FlowTextView);
		assertEquals(projectData.getInstructions(), ((FlowTextView) instructionsView).getText());
	}

	public void testIsNotesAndCreditsViewPresentAndHasCorrectContent() {
		final View notesAndCreditsLabelView = solo.getView(R.id.scratch_project_notes_and_credits_label);
		assertEquals(notesAndCreditsLabelView.getVisibility(), View.VISIBLE);
		assertTrue(notesAndCreditsLabelView instanceof TextView);
		assertEquals(getActivity().getString(R.string.notes_and_credits), ((TextView) notesAndCreditsLabelView).getText());

		final View notesAndCreditsView = solo.getView(R.id.scratch_project_notes_and_credits_text);
		assertEquals(notesAndCreditsView.getVisibility(), View.VISIBLE);
		assertTrue(notesAndCreditsView instanceof TextView);
		assertEquals(projectData.getNotesAndCredits(), ((TextView) notesAndCreditsView).getText());
	}

	public void testIsSharingViewPresentAndHasCorrectContent() {
		final View favoritesLabelView = solo.getView(R.id.scratch_project_favorites_text);
		final String expectedHumanReadableFavoritesNumber = "37k";
		assertEquals(favoritesLabelView.getVisibility(), View.VISIBLE);
		assertTrue(favoritesLabelView instanceof TextView);
		assertEquals(expectedHumanReadableFavoritesNumber, ((TextView) favoritesLabelView).getText());

		final View lovesLabelView = solo.getView(R.id.scratch_project_loves_text);
		assertEquals(lovesLabelView.getVisibility(), View.VISIBLE);
		assertTrue(lovesLabelView instanceof TextView);
		assertEquals(projectData.getLoves(), Integer.parseInt(((TextView) lovesLabelView).getText().toString()));

		final View viewsLabelView = solo.getView(R.id.scratch_project_views_text);
		final String expectedHumanReadableViewsNumber = "1M";
		assertEquals(viewsLabelView.getVisibility(), View.VISIBLE);
		assertTrue(viewsLabelView instanceof TextView);
		assertEquals(expectedHumanReadableViewsNumber, ((TextView) viewsLabelView).getText());
	}

	public void testIsTagViewPresentAndHasCorrectContent() {
		final View tagsLabelView = solo.getView(R.id.scratch_project_tags_text);
		assertEquals(tagsLabelView.getVisibility(), View.VISIBLE);
		assertTrue(tagsLabelView instanceof TextView);
		final StringBuilder tagList = new StringBuilder();
		int index = 0;
		for (String tag : projectData.getTags()) {
			tagList.append((index++ > 0 ? ", " : "") + tag);
		}
		assertEquals(tagList.toString(), ((TextView) tagsLabelView).getText());
	}

	public void testIsSharedDateViewPresentAndHasCorrectContent() {
		final String sharedDateString = Utils.formatDate(projectData.getSharedDate(), Locale.getDefault());
		final View sharedDateView = solo.getView(R.id.scratch_project_shared_text);
		final String sharedDateText = ((TextView) sharedDateView).getText().toString();
		assertEquals(sharedDateView.getVisibility(), View.VISIBLE);
		assertTrue(sharedDateView instanceof TextView);
		assertNotNull(((TextView) sharedDateView).getText());
		assertEquals(getActivity().getString(R.string.shared), sharedDateText.split(":")[0]);
		assertEquals(sharedDateString, sharedDateText.split(":")[1].trim());
	}

	public void testIsModifiedDateViewPresentAndHasCorrectContent() {
		final String modifiedDateString = Utils.formatDate(projectData.getModifiedDate(), Locale.getDefault());
		final View modifiedDateView = solo.getView(R.id.scratch_project_modified_text);
		final String modifiedDateText = ((TextView) modifiedDateView).getText().toString();
		assertEquals(modifiedDateView.getVisibility(), View.VISIBLE);
		assertTrue(modifiedDateView instanceof TextView);
		assertNotNull(((TextView) modifiedDateView).getText());
		assertEquals(getActivity().getString(R.string.modified), modifiedDateText.split(":")[0]);
		assertEquals("Modified date text!", modifiedDateString, modifiedDateText.split(":")[1].trim());
	}

	public void testIsRemixViewPresentAndHasCorrectContent() {
		final View remixesLabelView = solo.getView(R.id.scratch_project_remixes_label);
		assertEquals("Remix-text-view is invisible!", remixesLabelView.getVisibility(), View.VISIBLE);
		assertTrue("Remix-text-view should be instance of TextView-class!", remixesLabelView instanceof TextView);
		assertEquals("Remix-text-view is not labeled correctly!", getActivity().getString(R.string.remixes),
				((TextView) remixesLabelView).getText());
	}

	public void testIsConvertButtonViewPresentAndHasCorrectContent() {
		// convert button
		final View convertButtonView = solo.getView(R.id.scratch_project_convert_button);
		assertEquals("Convert-button is invisible!", convertButtonView.getVisibility(), View.VISIBLE);
		assertTrue("Convert-button should be instance of Button-class!", convertButtonView instanceof Button);
		assertEquals("Wrong label name assigned to convert-button!", getActivity().getString(R.string.convert),
				((TextView) convertButtonView).getText());
	}

	public void testRemixListViewPopulatedWithRemixProjectData() {
		// remixed list view
		View remixesListView = solo.getView(R.id.scratch_project_remixes_list_view);
		assertEquals("ListView is not visible!", remixesListView.getVisibility(), View.VISIBLE);
		assertTrue("View is no list view!", remixesListView instanceof ListView);
		ListAdapter listAdapter = ((ListView) remixesListView).getAdapter();
		assertNotNull("ListView has no adapter!", listAdapter);
		assertTrue("Wrong number of remixes!", listAdapter.getCount() == 1);
		ScratchRemixedProjectAdapter remixedProjectAdapter = (ScratchRemixedProjectAdapter) listAdapter;

		// remixed project
		ScratchRemixProjectData expectedRemixedProjectData = remixedProjectData;
		ScratchRemixProjectData remixedProjectData = remixedProjectAdapter.getItem(0);
		assertTrue("Title not set!", solo.searchText(expectedRemixedProjectData.getTitle()));
		assertEquals("No or wrong project ID set!", expectedRemixedProjectData.getId(), remixedProjectData.getId());
		assertEquals("No or wrong project title set!", expectedRemixedProjectData.getTitle(),
				remixedProjectData.getTitle());
		assertEquals("No or wrong project owner set!", expectedRemixedProjectData.getOwner(),
				remixedProjectData.getOwner());
		assertNotNull("No project image set!", remixedProjectData.getProjectImage());
		assertEquals("Wrong project image set!", expectedRemixedProjectData.getProjectImage().getUrl().toString(),
				remixedProjectData.getProjectImage().getUrl().toString());
	}

	public void testClickOnConvertButtonShouldDisableButton() {
		final ScratchProgramDetailsActivity activity = getActivity();
		final Button convertButton = (Button) activity.findViewById(R.id.scratch_project_convert_button);
		assertTrue("Convert button not clickable!", solo.getButton(solo.getString(R.string.convert)).isClickable());
		assertTrue("Convert button not enabled!", convertButton.isEnabled());
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				convertButton.performClick();
				assertFalse("Convert button not disabled!", convertButton.isEnabled());
			}
		});
	}

}
