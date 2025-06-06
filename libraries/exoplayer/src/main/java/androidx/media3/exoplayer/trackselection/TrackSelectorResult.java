/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.exoplayer.trackselection;

import static androidx.media3.common.util.Assertions.checkArgument;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.NullableType;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.RendererConfiguration;
import java.util.Objects;

/** The result of a {@link TrackSelector} operation. */
@UnstableApi
public final class TrackSelectorResult {

  /** The number of selections in the result. Greater than or equal to zero. */
  public final int length;

  /**
   * A {@link RendererConfiguration} for each renderer. A null entry indicates the corresponding
   * renderer should be disabled.
   */
  public final @NullableType RendererConfiguration[] rendererConfigurations;

  /** A {@link ExoTrackSelection} array containing the track selection for each renderer. */
  public final @NullableType ExoTrackSelection[] selections;

  /** Describe the tracks and which one were selected. */
  public final Tracks tracks;

  /**
   * An opaque object that will be returned to {@link TrackSelector#onSelectionActivated(Object)}
   * should the selections be activated.
   */
  @Nullable public final Object info;

  /**
   * @param rendererConfigurations A {@link RendererConfiguration} for each renderer. A null entry
   *     indicates the corresponding renderer should be disabled.
   * @param selections A {@link ExoTrackSelection} array containing the selection for each renderer.
   * @param info An opaque object that will be returned to {@link
   *     TrackSelector#onSelectionActivated(Object)} should the selection be activated. May be
   *     {@code null}.
   * @deprecated Use {@link #TrackSelectorResult(RendererConfiguration[], ExoTrackSelection[],
   *     Tracks, Object)}.
   */
  @Deprecated
  public TrackSelectorResult(
      @NullableType RendererConfiguration[] rendererConfigurations,
      @NullableType ExoTrackSelection[] selections,
      @Nullable Object info) {
    this(rendererConfigurations, selections, Tracks.EMPTY, info);
  }

  /**
   * @param rendererConfigurations A {@link RendererConfiguration} for each renderer. A null entry
   *     indicates the corresponding renderer should be disabled.
   * @param selections A {@link ExoTrackSelection} array containing the selection for each renderer.
   *     If the renderer is disabled with a null {@link RendererConfiguration}, then it must have a
   *     null selection too. It can also have a null selection if it has a {@linkplain
   *     RendererCapabilities#getTrackType() track type} of {@link C#TRACK_TYPE_NONE}.
   * @param tracks Description of the available tracks and which one were selected.
   * @param info An opaque object that will be returned to {@link
   *     TrackSelector#onSelectionActivated(Object)} should the selection be activated. May be
   *     {@code null}.
   */
  public TrackSelectorResult(
      @NullableType RendererConfiguration[] rendererConfigurations,
      @NullableType ExoTrackSelection[] selections,
      Tracks tracks,
      @Nullable Object info) {
    checkArgument(rendererConfigurations.length == selections.length);
    this.rendererConfigurations = rendererConfigurations;
    this.selections = selections.clone();
    this.tracks = tracks;
    this.info = info;
    length = rendererConfigurations.length;
  }

  /** Returns whether the renderer at the specified index is enabled. */
  public boolean isRendererEnabled(int index) {
    return rendererConfigurations[index] != null;
  }

  /**
   * Returns whether this result is equivalent to {@code other} for all renderers.
   *
   * @param other The other {@link TrackSelectorResult}. May be null, in which case {@code false}
   *     will be returned.
   * @return Whether this result is equivalent to {@code other} for all renderers.
   */
  public boolean isEquivalent(@Nullable TrackSelectorResult other) {
    if (other == null || other.selections.length != selections.length) {
      return false;
    }
    for (int i = 0; i < selections.length; i++) {
      if (!isEquivalent(other, i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns whether this result is equivalent to {@code other} for the renderer at the given index.
   * The results are equivalent if they have equal track selections and configurations for the
   * renderer.
   *
   * @param other The other {@link TrackSelectorResult}. May be null, in which case {@code false}
   *     will be returned.
   * @param index The renderer index to check for equivalence.
   * @return Whether this result is equivalent to {@code other} for the renderer at the specified
   *     index.
   */
  public boolean isEquivalent(@Nullable TrackSelectorResult other, int index) {
    if (other == null) {
      return false;
    }
    return Objects.equals(rendererConfigurations[index], other.rendererConfigurations[index])
        && Objects.equals(selections[index], other.selections[index]);
  }
}
