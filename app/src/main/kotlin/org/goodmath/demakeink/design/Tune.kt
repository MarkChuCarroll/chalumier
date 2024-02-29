package org.goodmath.demakeink.design

/**
 * Modelling the mouthpiece is more difficult than modelling the body of an
 * instrument. Some parameters are most easily determined empirically.
 * This tool tries to explain observed frequencies obtained from an instrument
 * by tweaking parameters to do with the mouthpiece.
 *
 * Resultant parameters should then result in a correctly tuned instrument
 * when the design tool is run again.
 * @param tweak Comma separated list of parameters to tweak.
 * @param observations Comma separated lists of frequency followed by
 *          whether each finger hole is open (0) or closed (1)
 *          (from bottom to top).
 */
class Tune(tweak: List<String>) {

}
