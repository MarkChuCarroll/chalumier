# Format for reading/writig intsrument specs.

I realize that this is ridiculous overkill, but I don't really
like any of the simple options for serialization. JSON is awful
for people to write, and it doesn't allow comments. YAML is flaky
as hell. ToML is just plain ugly.

What I'd really like my instrument specs to look like is something
along the lines of:

```
MyMinorFlute {
  basis = ConicFlute
  length = 123.45
  fingerings [
     C4 = XXXXXXO + 1
     D4 = OXXXXXO + 2
     Eb4 = OOXXXXO
     ...
  ]
  innerDiameters [
    [32.43, 43.12]
    ...
  
}  
```

So basically:
- You have names and values.
- If the value for a name is simple (ie, it's a string or a number), you write "name = value".
- If the value is complex (dict or array), you write name { contents }.
- Within {s, each value starts on a new line after the end of the last value.
- you can put multiple values on one line separated by ",".
- There's a special syntax for fingering lines to keep them compact.



