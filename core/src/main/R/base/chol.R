#  File src/library/base/R/chol.R
#  Part of the R package, http://www.R-project.org
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 2 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  A copy of the GNU General Public License is available at
#  http://www.r-project.org/Licenses/

chol <- function(x, ...) UseMethod("chol")

chol.default <- function(x, pivot = FALSE, LINPACK = FALSE, tol = -1, ...)
{
    if (is.complex(x))
        stop("complex matrices not permitted at present")

    .Internal(La_chol(as.matrix(x), pivot, tol))
}

chol2inv <- function(x, size = NCOL(x), LINPACK = FALSE)
    .Internal(La_chol2inv(x, size))
