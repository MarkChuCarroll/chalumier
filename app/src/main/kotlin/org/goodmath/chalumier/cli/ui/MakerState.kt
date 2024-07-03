/*
 * Copyright 2024 Mark C. Chu-Carroll and Paul Francis Harrison
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
package org.goodmath.chalumier.cli.ui

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.widgets.ProgressBar

data class MakerState(
    val name: String,
    val stage: String,
    val total: Int,
    val current: Int,
    override val height: Int,
) : ProgressState {
    override fun columns(): List<ColumnWidth> {
        return listOf(
            ColumnWidth.Fixed(20),
            ColumnWidth.Expand(30),
        )
    }

    override fun header(): List<Any> {
        return listOf(TextStyles.bold(TextColors.brightBlue("stage")), TextStyles.bold(TextColors.brightMagenta("Progress Making: $name")))
    }

    override fun body(): List<Any> {
        return listOf(
            "$stage",
            ProgressBar(
                total = total.toLong(),
                completed = current.toLong(),
                pendingChar = "*",
                pendingStyle = TextColors.red,
                completeChar = "#",
                completeStyle = TextColors.brightGreen,
            ),
        )
    }
}
