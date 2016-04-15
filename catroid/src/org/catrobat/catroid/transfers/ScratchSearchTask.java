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

import android.os.AsyncTask;

import org.catrobat.catroid.common.ScratchSearchResult;
import org.catrobat.catroid.web.ServerCalls;

public class ScratchSearchTask extends AsyncTask<String, Integer, ScratchSearchResult> {

    public interface ScratchSearchTaskDelegate {
        void onPreExecute();
        void onPostExecute(ScratchSearchResult result);
    }

    private ScratchSearchTaskDelegate delegate = null;

    public ScratchSearchTask setDelegate(ScratchSearchTaskDelegate delegate) {
        this.delegate = delegate;
        return this;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (delegate != null) {
            delegate.onPreExecute();
        }
    }

    @Override
    protected ScratchSearchResult doInBackground(String... params) {
        return fetchProjectList(params[0]);
    }

    public ScratchSearchResult fetchProjectList(String query) {
        try {
            ServerCalls.ScratchSearchSortType sortType = ServerCalls.ScratchSearchSortType.RELEVANCE;
            return ServerCalls.getInstance().scratchSearch(query, sortType, 20, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(ScratchSearchResult result) {
        super.onPostExecute(result);
        if (delegate != null) {
            delegate.onPostExecute(result);
        }
    }

}
