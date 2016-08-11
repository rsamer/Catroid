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
package org.catrobat.catroid.common;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.common.images.WebImage;

import java.io.Serializable;

public class ScratchProgramPreviewData implements Parcelable, Serializable {

	private static final long serialVersionUID = 1L;

	private long id;
	private String title;
	private String content;
	private WebImage programImage;

	public ScratchProgramPreviewData(long id, String title, String content) {
		this.id = id;
		this.title = title;
		this.content = content;
		this.programImage = null;
	}

	private ScratchProgramPreviewData(Parcel in) {
		this.id = in.readLong();
		this.title = in.readString();
		this.content = in.readString();
		this.programImage = in.readParcelable(WebImage.class.getClassLoader());
	}

	public long getId() { return id; }

	public String getTitle() { return title; }

	public String getContent() { return content; }

	public WebImage getProgramImage() { return programImage; }

	public void setProgramImage(WebImage programImage) { this.programImage = programImage; }

	@Override
	public int describeContents() { return 0; }

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(title);
		dest.writeString(content);
		dest.writeParcelable(programImage, flags);
	}

	public static final Parcelable.Creator<ScratchProgramPreviewData> CREATOR = new Parcelable.Creator<ScratchProgramPreviewData>() {
		@Override
		public ScratchProgramPreviewData createFromParcel(Parcel source) {
			return new ScratchProgramPreviewData(source);
		}

		@Override
		public ScratchProgramPreviewData[] newArray(int size) {
			return new ScratchProgramPreviewData[size];
		}
	};
}
