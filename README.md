# TAME Docs

Copyright (c) 2017-2019 Matt Tropiano.

### Required Libraries

Black Rook JSON  
[https://github.com/BlackRookSoftware/JSON](https://github.com/BlackRookSoftware/JSON)

TAME  
[https://github.com/TAME-IF/TAME](https://github.com/TAME-IF/TAME)

### Introduction

This makes the TAME documentation pages.

### Compiling with Apache Ant

First download and set up [Apache Ant](https://ant.apache.org/) if you haven't. 

To download the dependencies for this project (if you didn't set that up yourself already), type:

	ant dependencies

A *build.properties* file will be created/appended to with the *dev.base* property set.
	
To compile this project, type:

	ant compile

Then, to build TAME's documentation:

	ant tamedocs

The build is placed in `build/tamedocs` by default.

### Licensing

This program and the accompanying materials
are made available under the terms of the GNU Lesser Public License v2.1
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

A copy of the LGPL should have been included in this release (LICENSE.txt).
If it was not, please contact us for a copy, or to notify us of a distribution
that has not included it. 

This contains code copied from Black Rook Base, under the terms of the MIT License (docs/blackrook-base-license.txt).
