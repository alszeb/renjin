#
# Renjin : JVM-based interpreter for the R language for the statistical analysis
# Copyright © 2010-2016 BeDataDriven Groep B.V. and contributors
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, a copy is available at
# https://www.gnu.org/licenses/gpl-2.0.txt
#


# During package build, GNU R orders files using LC_COLLATE=C, ensuring that
# ordering is done on a byte-by-byte basis, which means that upper case letters
# come _before_ lower case letters, among other differences.

defined.first <- 42


f <- local( {
         x <- 1
         function(v) {
            if (!missing(v)) x <<- v
            x
         }
     })

makeActiveBinding("abc", f, environment())