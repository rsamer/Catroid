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

package org.catrobat.catroid.transfers;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;

import org.catrobat.catroid.web.ServerCalls;

import java.util.ArrayList;

public class ScratchSearchTask extends AsyncTask<String, Void, String> {

    String textSearch;
    ProgressDialog pd;
    ArrayList<ServerCalls.ScratchProject> projectList;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        projectList = new ArrayList<>();
        /*
        pd = new ProgressDialog(getActivity());
        pd.setCancelable(false);
        pd.setMessage("Searching...");
        pd.getWindow().setGravity(Gravity.CENTER);
        pd.show();
        */
    }

    @Override
    protected String doInBackground(String... sText) {
        textSearch = sText[0];
        String returnResult = getProjectList(textSearch);
        return returnResult;
    }

    public String getProjectList(String query) {
        try {
            projectList.clear();
            ServerCalls.ScratchSearchSortType sortType = ServerCalls.ScratchSearchSortType.RELEVANCE;
            ServerCalls.ScratchSearchResult searchResult;
            searchResult = ServerCalls.getInstance().scratchSearch(query, sortType, 20, 0);
            projectList = searchResult.getProjectList();
            return ("OK");
        } catch (Exception e) {
            e.printStackTrace();
            return ("Exception Caught");
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result.equalsIgnoreCase("Exception Caught")) {
            //Toast.makeText(getActivity(), "Unable to connect to server,please try later", Toast.LENGTH_LONG).show();
            //pd.dismiss();
        } else {
            for (ServerCalls.ScratchProject project : projectList) {
                Log.i("S2CC", project.getTitle());
            }
            //searchResults.setAdapter(new SearchResultsAdapter(getActivity(),filteredProductResults));
            //pd.dismiss();
        }
    }

}
