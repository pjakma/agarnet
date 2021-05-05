/* This file is part of 'agarnet'
 *
 * Copyright (c) Facebook, Inc. and its affiliates
 *
 * agarnet is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.
 * 
 * agarnet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with agarnet.  If not, see <http://www.gnu.org/licenses/>.
 */
#ifndef AGARNET_H
#define AGARNET_H

#include <stdlib.h>
#include <stdint.h>

/* see comments in flakey_proto.java */
struct native_buffer {
  uint8_t *data;
  size_t len;
  size_t capacity;
};

#include "flakey_proto.h"

#endif /* AGARNET_H */
