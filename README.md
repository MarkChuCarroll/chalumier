# Chalumier

## WTF is this?

Chalumier started as a Kotlin port of Demakin.

Demakein (DEsign and MAke INstruments) is a software package originally
written by Paul Harrison (pfh@logarithmic.net). It's a pretty impressive
piece of work that performs an acoustic analysis and optimization process
to produce the profile and hole sizes for acoustic woodwinds instruments,
including conic flutes, straight-bore flutes, double-reeds, end-blown
flutes, and tinwhistles.

The original demakein has a couple of serious problems.

1. First and foremost, it's written in Python 2.7. That version of Python
  is well-past its end-of-life, and it's going to be harder and harder to
  make it work on modern systems. 
2. It's heavily dependent on another system that pnh built, called Nesoni.
  Nesoni is a system for performing gene-sequence analysis, which happens
  to include a framework for doing parallel computation in Python. Like
  demakein, nesoni is wriatten in Python 2.7. It's also a very complicated,
  tangled system; trying to make an entire genetic sequencing system work
  so that you can use it to generate musical instruments in the basis
  would be ridiculous.
3. Demakein is _not_ well-documented. In fact, it's barely documented at all.
  That's true on multiple levels; the code has no documentation of how it
  works, the system has no documentation of what parameters you can supply
  for an instrument, or of what the values of parameters mean.
4. You don't even really use it as a tool. To create a new instrument,
  you write and then run a new python programmer. I'd really like to be
  able to write an abstract specification of an instrument, and just
  let a program use that as an input.

After using demakein for a while (custom installing a Python2.7 interpreter
just to do it), I've gotten really frustrated. It often produces nonsensical
results (overlapping holes, or three holes in exactly the same position), 
or incorrect results (instruments that aren't intonated the way that it
claims that they are). It doesn't provide any feedback: an instrument design
analysis may take 5 minutes, or it make take 3 days, and there's no way to
know if it's continuing to make progress or not.

That bothered me enough that I decided that it needed to be updated. I
initially looked at updating to Python3, but ended up finding that that 
was harder than just rewriting in something different. For sloppy Python
code, Python2 and Python3 are close enough that it looks easy to convert,
but far enough that a naive conversion won't work correctly. It's also a 
painful conversion, because you need to be able to run tests of the 
existing code in Python2, and tests of the new in Python3.

I ended up deciding on Kotlin just because I happen to enjoy programming
in it. And as I got into the process, I kept getting really annoyed. Demakein
is a crazy creation of incredible brilliance intermixed with utter
bugf*ck crazy sloppiness.

I haven't just been slavishly copying code and translating into Kotlin;
I've been trying to understand it, refactor it, fix bugs, make it faster
and more user-friendly.

So far, the results are promising. Generating a simple major flute using 
demakein took around 20 minutes on my laptop. Using Chalumier, it finishes
in 5 minutes. But more impressively, when I tried to run a model for
a B-minor flute with cross-fingerings, demakein ran for 3 days without
completing. Giving Chalumier the same model, I had a result in 10 minutes.

## Why Chalumier?

I hate the name demakein. It's not evocative, it's got no style, and you
wouldn't ever guess that it's something to do with musical instruments based
on its name.

What it does is design and make musical instruments - specifically woodwinds. So what's
a person who make woodwinds called?

There's a word for someone who makes stringed instruments: they're called
a luthier. But there isn't a word for someone who makes woodwinds! But I found
a [reddit thread](https://www.reddit.com/r/fantasywriters/comments/1e69c2/whats_a_cool_sounding_word_for_a_maker_of_musical/)
about that, where someone proposed the name "chalumier". I immediately loved it.

I'm a clarinetist, and my eventual goal is to be able to use this
software to help design a modern 3d-printable basset horn. The precursor
to the entire clarinet family is called a chalumeau - so chalumier just
feels perfect.

## Building

Building is simple:

1. Run `gradle build`. This will compile Chalumier, and verify that all of its
   tests pass correctly.
2. Run `gradle shadowJar`. This will produce an executable jarfile in
   .`./app/build/libs/chalumier-<<<VERSION>>>.jar`


## Running Chalumier

The command line tool is still a work in progress. For now, there are two commands implemented: "design"
and "model". 

### Designing an Instrument

When you want to create an instrument, you'll always start with design. You create
an instrument specification file (see app/src/test/resources for a few examples), which
tells Chalumier the basics of what you want, and then you can run the designer with:

```bash
java -jar chalumier.jar design --output-dir _dirname_ _instrument_file_
```

The design process uses an evolutionary algorithm. It creates a pool of candidate
designs for the instrument. Then it starts randomly combining the candidates with
mutations. If a mutation produces something better than what's in the candidate pool,
the mutation will be added to the candidates, and the least good candidate will be ejected.
This process continues until it converges onto a solution. The termination point isn't
necessary a correct instrument, but rather "the process has reached a point where it's
no longer able to generate anything better". 

In the output directory that you specified, it produces two files:
* `_instrument_-design.svg`, which contains a diagram showing the design, and how
   close the intonations of each fingering are to the desired pitch;
* `_instrument_-parameters.json`, which contain the data describing the model in
  a format that's usable by other tools, including the Chalumier model generator.


