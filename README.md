# TAME Docs

Copyright (c) 2017-2018 Matt Tropiano. All rights reserved.  

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

	ant tamedocs

The build is placed in `build/tamedocs` by default.

### Other

This program and the accompanying materials
are made available under the terms of the GNU Lesser Public License v2.1
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

A copy of the LGPL should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 
