package org.goodmath.demakeink.shape.cpp

import java.nio.file.Path
import kotlin.io.path.exists

/* ph:
 *
 * Gwarggllykiiiiillllllmeeeeeeeeecougcougcough
 *
 * Note: pypy gc does not count c++ allocated memory, triggers major gc infrequently
 */

/*
 * MarkCC: Ok, if ph thinks this is bad, I'm *not* looking forward to this.
 */


fun updateFile(filename: Path, content: String) {
    if (filename.exists()) {
        val stored = filename.toFile().readText()
        if (stored != content) {
            filename.toFile().writeText(content)
        }
    } else {
        filename.toFile().writeText(content)
    }
}

