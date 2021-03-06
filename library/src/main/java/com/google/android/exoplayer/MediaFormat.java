/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer;

import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.Util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Defines the format of an elementary media stream.
 */
public final class MediaFormat {

  public static final int NO_VALUE = -1;

  /**
   * A value for {@link #subsampleOffsetUs} to indicate that subsample timestamps are relative to
   * the timestamps of their parent samples.
   */
  public static final long OFFSET_SAMPLE_RELATIVE = Long.MAX_VALUE;

  /**
   * The identifier for the track represented by the format, or {@link #NO_VALUE} if unknown or not
   * applicable.
   */
  public final int trackId;
  /**
   * The mime type of the format.
   */
  public final String mimeType;
  /**
   * The average bandwidth in bits per second, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int bitrate;
  /**
   * The maximum size of a buffer of data (typically one sample) in the format, or {@link #NO_VALUE}
   * if unknown or not applicable.
   */
  public final int maxInputSize;
  /**
   * The duration in microseconds, or {@link C#UNKNOWN_TIME_US} if the duration is unknown, or
   * {@link C#MATCH_LONGEST_US} if the duration should match the duration of the longest track whose
   * duration is known.
   */
  public final long durationUs;
  /**
   * Initialization data that must be provided to the decoder. Will not be null, but may be empty
   * if initialization data is not required.
   */
  public final List<byte[]> initializationData;
  /**
   * Whether the format represents an adaptive track, meaning that the format of the actual media
   * data may change (e.g. to adapt to network conditions).
   */
  public final boolean adaptive;

  // Video specific.

  /**
   * The width of the video in pixels, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int width;

  /**
   * The height of the video in pixels, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int height;
  /**
   * For formats that belong to an adaptive video track (either describing the track, or describing
   * a specific format within it), this is the maximum width of the video in pixels that will be
   * encountered in the stream. Set to {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int maxWidth;
  /**
   * For formats that belong to an adaptive video track (either describing the track, or describing
   * a specific format within it), this is the maximum height of the video in pixels that will be
   * encountered in the stream. Set to {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int maxHeight;
  /**
   * The clockwise rotation that should be applied to the video for it to be rendered in the correct
   * orientation, or {@link #NO_VALUE} if unknown or not applicable. Only 0, 90, 180 and 270 are
   * supported.
   */
  public final int rotationDegrees;
  /**
   * The width to height ratio of pixels in the video, or {@link #NO_VALUE} if unknown or not
   * applicable.
   */
  public final float pixelWidthHeightRatio;

  // Audio specific.

  /**
   * The number of audio channels, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int channelCount;
  /**
   * The audio sampling rate in Hz, or {@link #NO_VALUE} if unknown or not applicable.
   */
  public final int sampleRate;

  // Text specific.

  /**
   * The language of the track, or null if unknown or not applicable.
   */
  public final String language;

  /**
   * For samples that contain subsamples, this is an offset that should be added to subsample
   * timestamps. A value of {@link #OFFSET_SAMPLE_RELATIVE} indicates that subsample timestamps are
   * relative to the timestamps of their parent samples.
   */
  public final long subsampleOffsetUs;

  // Lazy-initialized hashcode and framework media format.

  private int hashCode;
  private android.media.MediaFormat frameworkMediaFormat;

  public static MediaFormat createVideoFormat(int trackId, String mimeType, int bitrate,
      int maxInputSize, long durationUs, int width, int height, List<byte[]> initializationData) {
    return createVideoFormat(trackId, mimeType, bitrate, maxInputSize, durationUs, width, height,
        initializationData, NO_VALUE, NO_VALUE);
  }

  public static MediaFormat createVideoFormat(int trackId, String mimeType, int bitrate,
      int maxInputSize, long durationUs, int width, int height, List<byte[]> initializationData,
      int rotationDegrees, float pixelWidthHeightRatio) {
    return new MediaFormat(trackId, mimeType, bitrate, maxInputSize, durationUs, width, height,
        rotationDegrees, pixelWidthHeightRatio, NO_VALUE, NO_VALUE, null, OFFSET_SAMPLE_RELATIVE,
        initializationData, false, NO_VALUE, NO_VALUE);
  }

  public static MediaFormat createAudioFormat(int trackId, String mimeType, int bitrate,
      int maxInputSize, long durationUs, int channelCount, int sampleRate,
      List<byte[]> initializationData, String language) {
    return new MediaFormat(trackId, mimeType, bitrate, maxInputSize, durationUs, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, channelCount, sampleRate, language, OFFSET_SAMPLE_RELATIVE,
        initializationData, false, NO_VALUE, NO_VALUE);
  }

  public static MediaFormat createTextFormat(int trackId, String mimeType, int bitrate,
      long durationUs, String language) {
    return createTextFormat(trackId, mimeType, bitrate, durationUs, language,
        OFFSET_SAMPLE_RELATIVE);
  }

  public static MediaFormat createTextFormat(int trackId, String mimeType, int bitrate,
      long durationUs, String language, long subsampleOffsetUs) {
    return new MediaFormat(trackId, mimeType, bitrate, NO_VALUE, durationUs, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, language, subsampleOffsetUs, null, false, NO_VALUE,
        NO_VALUE);
  }

  public static MediaFormat createFormatForMimeType(int trackId, String mimeType, int bitrate,
      long durationUs) {
    return new MediaFormat(trackId, mimeType, bitrate, NO_VALUE, durationUs, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, null, OFFSET_SAMPLE_RELATIVE, null, false, NO_VALUE,
        NO_VALUE);
  }

  /* package */ MediaFormat(int trackId, String mimeType, int bitrate, int maxInputSize,
      long durationUs, int width, int height, int rotationDegrees, float pixelWidthHeightRatio,
      int channelCount, int sampleRate, String language, long subsampleOffsetUs,
      List<byte[]> initializationData, boolean adaptive, int maxWidth, int maxHeight) {
    this.trackId = trackId;
    this.mimeType = Assertions.checkNotEmpty(mimeType);
    this.bitrate = bitrate;
    this.maxInputSize = maxInputSize;
    this.durationUs = durationUs;
    this.width = width;
    this.height = height;
    this.rotationDegrees = rotationDegrees;
    this.pixelWidthHeightRatio = pixelWidthHeightRatio;
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
    this.language = language;
    this.subsampleOffsetUs = subsampleOffsetUs;
    this.initializationData = initializationData == null ? Collections.<byte[]>emptyList()
        : initializationData;
    this.adaptive = adaptive;
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
  }

  public MediaFormat copyWithMaxInputSize(int maxInputSize) {
    return new MediaFormat(trackId, mimeType, bitrate, maxInputSize, durationUs, width, height,
        rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate, language,
        subsampleOffsetUs, initializationData, adaptive, maxWidth, maxHeight);
  }

  public MediaFormat copyWithMaxVideoDimensions(int maxWidth, int maxHeight) {
    return new MediaFormat(trackId, mimeType, bitrate, maxInputSize, durationUs, width, height,
        rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate, language,
        subsampleOffsetUs, initializationData, adaptive, maxWidth, maxHeight);
  }

  public MediaFormat copyWithSubsampleOffsetUs(long subsampleOffsetUs) {
    return new MediaFormat(trackId, mimeType, bitrate, maxInputSize, durationUs, width, height,
        rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate, language,
        subsampleOffsetUs, initializationData, adaptive, maxWidth, maxHeight);
  }

  public MediaFormat copyWithDurationUs(long durationUs) {
    return new MediaFormat(trackId, mimeType, bitrate, maxInputSize, durationUs, width, height,
        rotationDegrees, pixelWidthHeightRatio, channelCount, sampleRate, language,
        subsampleOffsetUs, initializationData, adaptive, maxWidth, maxHeight);
  }

  public MediaFormat copyAsAdaptive() {
    return new MediaFormat(trackId, mimeType, NO_VALUE, NO_VALUE, durationUs, NO_VALUE, NO_VALUE,
        NO_VALUE, NO_VALUE, NO_VALUE, NO_VALUE, null, OFFSET_SAMPLE_RELATIVE, null, true, maxWidth,
        maxHeight);
  }

  /**
   * @return A {@link MediaFormat} representation of this format.
   */
  @SuppressLint("InlinedApi")
  @TargetApi(16)
  public final android.media.MediaFormat getFrameworkMediaFormatV16() {
    if (frameworkMediaFormat == null) {
      android.media.MediaFormat format = new android.media.MediaFormat();
      format.setString(android.media.MediaFormat.KEY_MIME, mimeType);
      maybeSetStringV16(format, android.media.MediaFormat.KEY_LANGUAGE, language);
      maybeSetIntegerV16(format, android.media.MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);
      maybeSetIntegerV16(format, android.media.MediaFormat.KEY_WIDTH, width);
      maybeSetIntegerV16(format, android.media.MediaFormat.KEY_HEIGHT, height);
      maybeSetIntegerV16(format, "rotation-degrees", rotationDegrees);
      maybeSetIntegerV16(format, android.media.MediaFormat.KEY_MAX_WIDTH, maxWidth);
      maybeSetIntegerV16(format, android.media.MediaFormat.KEY_MAX_HEIGHT, maxHeight);
      maybeSetIntegerV16(format, android.media.MediaFormat.KEY_CHANNEL_COUNT, channelCount);
      maybeSetIntegerV16(format, android.media.MediaFormat.KEY_SAMPLE_RATE, sampleRate);
      for (int i = 0; i < initializationData.size(); i++) {
        format.setByteBuffer("csd-" + i, ByteBuffer.wrap(initializationData.get(i)));
      }
      if (durationUs != C.UNKNOWN_TIME_US) {
        format.setLong(android.media.MediaFormat.KEY_DURATION, durationUs);
      }
      frameworkMediaFormat = format;
    }
    return frameworkMediaFormat;
  }

  /**
   * Sets the framework format returned by {@link #getFrameworkMediaFormatV16()}.
   *
   * @deprecated This method only exists for FrameworkSampleSource, which is itself deprecated.
   * @param format The framework format.
   */
  @Deprecated
  @TargetApi(16)
  /* package */ final void setFrameworkFormatV16(android.media.MediaFormat format) {
    frameworkMediaFormat = format;
  }

  @Override
  public String toString() {
    return "MediaFormat(" + trackId + ", " + mimeType + ", " + bitrate + ", " + maxInputSize
        + ", " + width + ", " + height + ", " + rotationDegrees + ", " + pixelWidthHeightRatio
        + ", " + channelCount + ", " + sampleRate + ", " + language + ", " + durationUs + ", "
        + adaptive + ", " + maxWidth + ", " + maxHeight + ")";
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 31 * result + trackId;
      result = 31 * result + (mimeType == null ? 0 : mimeType.hashCode());
      result = 31 * result + bitrate;
      result = 31 * result + maxInputSize;
      result = 31 * result + width;
      result = 31 * result + height;
      result = 31 * result + rotationDegrees;
      result = 31 * result + Float.floatToRawIntBits(pixelWidthHeightRatio);
      result = 31 * result + (int) durationUs;
      result = 31 * result + (adaptive ? 1231 : 1237);
      result = 31 * result + maxWidth;
      result = 31 * result + maxHeight;
      result = 31 * result + channelCount;
      result = 31 * result + sampleRate;
      result = 31 * result + (language == null ? 0 : language.hashCode());
      for (int i = 0; i < initializationData.size(); i++) {
        result = 31 * result + Arrays.hashCode(initializationData.get(i));
      }
      hashCode = result;
    }
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MediaFormat other = (MediaFormat) obj;
    if (adaptive != other.adaptive || bitrate != other.bitrate || maxInputSize != other.maxInputSize
        || width != other.width || height != other.height
        || rotationDegrees != other.rotationDegrees
        || pixelWidthHeightRatio != other.pixelWidthHeightRatio
        || maxWidth != other.maxWidth || maxHeight != other.maxHeight
        || channelCount != other.channelCount || sampleRate != other.sampleRate
        || trackId != other.trackId || !Util.areEqual(language, other.language)
        || !Util.areEqual(mimeType, other.mimeType)
        || initializationData.size() != other.initializationData.size()) {
      return false;
    }
    for (int i = 0; i < initializationData.size(); i++) {
      if (!Arrays.equals(initializationData.get(i), other.initializationData.get(i))) {
        return false;
      }
    }
    return true;
  }

  @TargetApi(16)
  private static final void maybeSetStringV16(android.media.MediaFormat format, String key,
      String value) {
    if (value != null) {
      format.setString(key, value);
    }
  }

  @TargetApi(16)
  private static final void maybeSetIntegerV16(android.media.MediaFormat format, String key,
      int value) {
    if (value != NO_VALUE) {
      format.setInteger(key, value);
    }
  }

}
