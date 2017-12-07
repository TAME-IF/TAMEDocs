# TAME Dox

Copyright (c) 2015-2017 Matt Tropiano. All rights reserved.  

### Required Libraries

Black Rook Commons 2.31.0+  
[https://github.com/BlackRookSoftware/Common](https://github.com/BlackRookSoftware/Common)

Black Rook Common I/O 2.5.0+  
[https://github.com/BlackRookSoftware/CommonIO](https://github.com/BlackRookSoftware/CommonIO)

Black Rook Common Lang 2.9.1+  
[https://github.com/BlackRookSoftware/CommonLang](https://github.com/BlackRookSoftware/CommonLang)

TAME 1.0+ 
[https://github.com/TAME-IF/TAME](https://github.com/TAME-IF/TAME)

### Introduction

This makes the TAME documentation pages.

### Compiling with Ant

To download the dependencies for this project (if you didn't set that up yourself already), type:

	ant dependencies

A *build.properties* file will be created/appended to with the *dev.base* property set.
	
To compile this project, type:

	ant compile

To build TAME's documentation:

	ant tamedox

The build is placed in *build/tamedox* by default.
