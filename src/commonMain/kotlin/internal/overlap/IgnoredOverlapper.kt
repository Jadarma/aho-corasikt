// Copyright © 2026 Dan Cîmpianu
// This code is licensed under the Apache 2.0 license, see LICENSE.md or https://www.apache.org/licenses/LICENSE-2.0.
package io.github.jadarma.ahocorasikt.internal.overlap

import io.github.jadarma.ahocorasikt.Overlap

/**
 * Implements [Overlap.Ignored] strategy; effectively a NOOP handler.
 * All candidates are valid.
 */
internal class IgnoredOverlapper : Overlapper() {
    override fun offer(range: IntRange) = promote(range)
}
