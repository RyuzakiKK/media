/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.media3.extractor.ts;

import static androidx.media3.common.util.Assertions.checkState;
import static androidx.media3.extractor.metadata.id3.Id3Decoder.ID3_HEADER_LENGTH;
import static androidx.media3.extractor.ts.TsPayloadReader.FLAG_DATA_ALIGNMENT_INDICATOR;
import static java.lang.Math.min;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.ts.TsPayloadReader.TrackIdGenerator;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** Parses ID3 data and extracts individual text information frames. */
@UnstableApi
public final class Id3Reader implements ElementaryStreamReader {

  private static final String TAG = "Id3Reader";

  private final String containerMimeType;
  private final ParsableByteArray id3Header;

  private @MonotonicNonNull TrackOutput output;

  // State that should be reset on seek.
  private boolean writingSample;

  // Per sample state that gets reset at the start of each sample.
  private long sampleTimeUs;
  private int sampleSize;
  private int sampleBytesRead;

  public Id3Reader(String containerMimeType) {
    this.containerMimeType = containerMimeType;
    id3Header = new ParsableByteArray(ID3_HEADER_LENGTH);
    sampleTimeUs = C.TIME_UNSET;
  }

  @Override
  public void seek() {
    writingSample = false;
    sampleTimeUs = C.TIME_UNSET;
  }

  @Override
  public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
    idGenerator.generateNewId();
    output = extractorOutput.track(idGenerator.getTrackId(), C.TRACK_TYPE_METADATA);
    output.format(
        new Format.Builder()
            .setId(idGenerator.getFormatId())
            .setContainerMimeType(containerMimeType)
            .setSampleMimeType(MimeTypes.APPLICATION_ID3)
            .build());
  }

  @Override
  public void packetStarted(long pesTimeUs, @TsPayloadReader.Flags int flags) {
    if ((flags & FLAG_DATA_ALIGNMENT_INDICATOR) == 0) {
      return;
    }
    writingSample = true;
    sampleTimeUs = pesTimeUs;
    sampleSize = 0;
    sampleBytesRead = 0;
  }

  @Override
  public void consume(ParsableByteArray data) {
    Assertions.checkStateNotNull(output); // Asserts that createTracks has been called.
    if (!writingSample) {
      return;
    }
    int bytesAvailable = data.bytesLeft();
    if (sampleBytesRead < ID3_HEADER_LENGTH) {
      // We're still reading the ID3 header.
      int headerBytesAvailable = min(bytesAvailable, ID3_HEADER_LENGTH - sampleBytesRead);
      System.arraycopy(
          data.getData(),
          data.getPosition(),
          id3Header.getData(),
          sampleBytesRead,
          headerBytesAvailable);
      if (sampleBytesRead + headerBytesAvailable == ID3_HEADER_LENGTH) {
        // We've finished reading the ID3 header. Extract the sample size.
        id3Header.setPosition(0);
        if ('I' != id3Header.readUnsignedByte()
            || 'D' != id3Header.readUnsignedByte()
            || '3' != id3Header.readUnsignedByte()) {
          Log.w(TAG, "Discarding invalid ID3 tag");
          writingSample = false;
          return;
        }
        id3Header.skipBytes(3); // version (2) + flags (1)
        sampleSize = ID3_HEADER_LENGTH + id3Header.readSynchSafeInt();
      }
    }
    // Write data to the output.
    int bytesToWrite = min(bytesAvailable, sampleSize - sampleBytesRead);
    output.sampleData(data, bytesToWrite);
    sampleBytesRead += bytesToWrite;
  }

  @Override
  public void packetFinished(boolean isEndOfInput) {
    Assertions.checkStateNotNull(output); // Asserts that createTracks has been called.
    if (!writingSample || sampleSize == 0 || sampleBytesRead != sampleSize) {
      return;
    }
    // packetStarted method must be called before consuming samples.
    checkState(sampleTimeUs != C.TIME_UNSET);
    output.sampleMetadata(sampleTimeUs, C.BUFFER_FLAG_KEY_FRAME, sampleSize, 0, null);
    writingSample = false;
  }
}
